"""Verify extracted bootstraps, old updater migration, incremental delivery and Radmin UI locally."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import os
import shutil
import socket
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def extract(archive, target):
    target.mkdir(parents=True)
    with zipfile.ZipFile(archive) as z:
        for member in z.infolist():
            if not (target / member.filename).resolve().is_relative_to(target.resolve()):
                raise RuntimeError("Unsafe archive member")
        z.extractall(target)


def run(command, log, cwd=ROOT, **kwargs):
    result = subprocess.run([str(x) for x in command], cwd=cwd, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace", timeout=90, **kwargs)
    log.write_text(result.stdout, encoding="utf-8")
    if result.returncode:
        raise RuntimeError(str(command[0]) + " failed: " + result.stdout[-3000:])
    return result.stdout


def update(install, feed, log):
    return run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                install / "Atualizar-SpiritGate.ps1", "-InstallDir", install,
                "-LocalFeed", feed, "-NoLaunch"], log)


def active(root):
    return root / "versions" / (root / "active.txt").read_text().strip()


def main(metadata_path):
    metadata = json.loads(metadata_path.read_text())
    evidence = ROOT / ".task-checkpoints/radmin-atualizavel" / datetime.now(timezone.utc).strftime("smoke-%Y%m%dT%H%M%SZ")
    evidence.mkdir(parents=True)
    feed = Path(metadata["commonChannel"]) / "latest.json"
    raw = feed.read_bytes()
    identity = hashlib.sha256(raw).hexdigest()
    installs = {}
    for package in metadata["packages"]:
        archive = Path(package["archive"])
        assert digest(archive) == package["sha256"]
        target = evidence / package["role"]
        extract(archive, target)
        install = target / "SpiritGate-Atualizavel"
        installs[package["role"]] = install
        config = json.loads((install / "atualizacoes.json").read_text())
        assert config["role"] == package["role"]
        assert not config["feedUrl"], "Unpublished local bootstraps must not consult a live older feed"
        assert (install / "active.txt").read_text().strip() == identity
        update(install, feed, evidence / (package["role"] + "-verify.log"))
    host, guest = installs["anfitriao"], installs["convidado"]
    jar = active(host) / "app/spirit-gate-rpg.jar"
    assert digest(jar) == digest(active(guest) / "app/spirit-gate-rpg.jar")
    classes = evidence / "probe-classes"
    classes.mkdir()
    run(["javac", "--release", "17", "-cp", jar, "-d", classes,
         ROOT / "tools/UpdatableRadminUiProbe.java", ROOT / "tools/Task176PackageClientProbe.java"], evidence / "compile.log")
    for role, install in installs.items():
        output = evidence / (role + "-ui")
        run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
             ROOT / "tools/updater/Test-Radmin-Launch.ps1", "-InstallDir", install,
             "-ProbeClasses", classes, "-OutputDir", output], evidence / (role + "-ui.log"))
        ui = json.loads((output / "result.json").read_text())
        assert ui["ok"] and ui["guest"] == (role == "convidado")

    # Use the actual previously delivered bootstrap, not the edited updater source.
    old_folder = ROOT / "dist/SpiritGate-Atualizacoes-GitHub-0.78.1-rc1"
    old_archive = next(old_folder.glob("SpiritGate-Atualizavel-*-Windows64.zip"))
    legacy_target = evidence / "legacy"
    extract(old_archive, legacy_target)
    legacy = legacy_target / "SpiritGate-Atualizavel"
    old_id = (legacy / "active.txt").read_text().strip()
    old_script_hash = digest(legacy / "Atualizar-SpiritGate.ps1")
    sentinel = legacy / "data/saves/characters/migration-sentinel.properties"
    sentinel.parent.mkdir(parents=True)
    sentinel.write_text("isolated fixture; must remain unchanged")
    saved_hash = digest(sentinel)
    update(legacy, feed, evidence / "legacy-migration.log")
    assert (legacy / "active.txt").read_text().strip() == identity
    assert (legacy / "versions" / old_id).is_dir()
    assert digest(legacy / "Atualizar-SpiritGate.ps1") == old_script_hash
    assert digest(active(legacy) / "app/spirit-gate-rpg.jar") == digest(jar)
    assert digest(sentinel) == saved_hash
    # Actual legacy launcher arguments still load Radmin classes/resources after migration.
    run([active(legacy) / "runtime/bin/java.exe", "-Djpackage.app-path=" + str(active(legacy) / "SpiritGateRPG.exe"),
         "-cp", str(active(legacy) / "app/spirit-gate-rpg.jar") + os.pathsep + str(classes),
         "UpdatableRadminUiProbe"], evidence / "legacy-ui.log",
        env={**os.environ, "SPIRITGATE_PROBE_OUTPUT": str(evidence / "legacy-ui")})

    # One changed, valid JAR; all other feed files deliberately absent to prove reuse.
    probe_feed = evidence / "incremental-feed"
    release = probe_feed / "release-probe"
    (release / "app").mkdir(parents=True)
    new_jar = release / "app/spirit-gate-rpg.jar"
    shutil.copyfile(jar, new_jar)
    with zipfile.ZipFile(new_jar, "a") as z:
        z.writestr("updater-test-only.txt", "isolated incremental probe; not a distributed release")
    manifest = json.loads(raw)
    manifest.update(version="0.177.0-incremental-probe", basePath="release-probe")
    for entry in manifest["files"]:
        if entry["path"] == "app/spirit-gate-rpg.jar":
            entry.update(bytes=new_jar.stat().st_size, sha256=digest(new_jar))
    local_feed = probe_feed / "latest.json"
    local_feed.write_text(json.dumps(manifest))
    guest_save = guest / "data/saves/migration-sentinel.properties"
    guest_save.parent.mkdir(parents=True, exist_ok=True)
    guest_save.write_text("guest fixture")
    guest_save_hash = digest(guest_save)
    stdout = update(guest, local_feed, evidence / "incremental.log")
    assert "1 arquivos baixados" in stdout
    assert digest(active(guest) / "app/spirit-gate-rpg.jar") == digest(new_jar)
    assert json.loads((guest / "atualizacoes.json").read_text())["role"] == "convidado"
    run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
         ROOT / "tools/updater/Test-Radmin-Launch.ps1", "-InstallDir", guest,
         "-ProbeClasses", classes, "-OutputDir", evidence / "guest-after-update-ui"], evidence / "guest-after-update-ui.log")
    good_id = (guest / "active.txt").read_text()
    manifest.update(version="0.177.0-broken-probe", basePath="release-broken")
    # Use a new expected hash to force a download which fails integrity validation.
    for entry in manifest["files"]:
        if entry["path"] == "app/spirit-gate-rpg.jar":
            entry["sha256"] = "a" * 64
    bad = probe_feed / "release-broken/app/spirit-gate-rpg.jar"
    bad.parent.mkdir(parents=True)
    bad.write_bytes(b"broken")
    local_feed.write_text(json.dumps(manifest))
    update(guest, local_feed, evidence / "failed-update.log")
    assert (guest / "active.txt").read_text() == good_id
    assert digest(guest_save) == guest_save_hash

    # Run a packaged server and two independent packaged-runtime clients.
    with socket.socket() as reserve:
        reserve.bind(("127.0.0.1", 0))
        port = reserve.getsockname()[1]
    server_image = active(host)
    java = server_image / "runtime/bin/java.exe"
    network = evidence / "two-clients"
    network.mkdir()
    server_log = (network / "server.log").open("w")
    server = subprocess.Popen([str(java), "-cp", str(jar), "com.projetok.multiplayer.LocalSimulationServer",
                               "--combat", "--cosmetics", "--port", str(port), "--run-seconds", "30"],
                              cwd=server_image, stdout=server_log, stderr=subprocess.STDOUT)
    clients, logs = [], []
    try:
        # This readiness check is loopback TCP only; full client admission is checked below.
        import time
        deadline = time.monotonic() + 25
        while True:
            try:
                with socket.create_connection(("127.0.0.1", port), timeout=.2):
                    break
            except OSError:
                if time.monotonic() >= deadline:
                    raise
                time.sleep(.1)
        for label, install in (("a", host), ("b", guest)):
            image = active(install)
            log = (network / (label + ".log")).open("w")
            logs.append(log)
            clients.append(subprocess.Popen([str(image / "runtime/bin/java.exe"), "-cp",
                str(image / "app/spirit-gate-rpg.jar") + os.pathsep + str(classes),
                "com.projetok.prototype.rpg.Task176PackageClientProbe", str(port), label, str(network)],
                cwd=image, stdout=log, stderr=subprocess.STDOUT))
        assert [p.wait(timeout=35) for p in clients] == [0, 0]
        assert server.wait(timeout=35) == 0
        for label in ("a", "b"):
            state = json.loads((network / ("client-" + label + ".png.json")).read_text())
            assert state["players"] == 3 and state["appearances"] == 2
    finally:
        for process in clients + [server]:
            if process.poll() is None:
                process.terminate()
                process.wait(timeout=5)
        for log in logs + [server_log]:
            log.close()
    result = dict(bootstrapsVerified=2, sharedManifest=True, legacyMigration=True, legacyUpdaterUnchanged=True,
                  legacyVersionPreserved=True, sentinelSavesPreserved=True, rolesVerified=True,
                  radminUiVerified=True, incrementalDownloads=1, incrementalReused=len(manifest["files"])-1,
                  failedUpdateKeepsInstalledVersion=True, packagedClients=2, sharedEnemies=1,
                  radminRemoteTested=False, published=False)
    (evidence / "result.json").write_text(json.dumps(result, indent=2))
    print(str(evidence))
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main(Path(sys.argv[1]).resolve())
