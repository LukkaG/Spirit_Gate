"""Valida manifests e executa runtime/JAR dos dois pacotes em três processos locais."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]


def smoke(distribution, evidence):
    distribution, evidence = Path(distribution).resolve(), Path(evidence).resolve()
    evidence.mkdir(parents=True, exist_ok=False)
    roots = []
    checks = []
    for role in ("Anfitriao", "Convidado"):
        channel = distribution / role / "canal"
        raw = (channel / "latest.json").read_bytes()
        manifest = json.loads(raw)
        payload = channel / manifest["basePath"]
        for entry in manifest["files"]:
            path = (payload / entry["path"]).resolve()
            if not path.is_relative_to(payload.resolve()):
                raise ValueError("Caminho fora do payload")
            data = path.read_bytes()
            if len(data) != entry["bytes"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
                raise ValueError(entry["path"])
        roots.append(payload)
        checks.append({"role": role, "files": len(manifest["files"]), "manifestSha256": hashlib.sha256(raw).hexdigest()})
    if checks[0]["manifestSha256"] != checks[1]["manifestSha256"]:
        raise ValueError("Canais diferentes")
    classes = evidence / "probe"
    classes.mkdir()
    jar = roots[0] / "app/spirit-gate-rpg.jar"
    subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-cp", str(jar), "-d", str(classes),
                    str(ROOT / "tools/CorujaoSevenPackageProbe.java")], check=True)
    processes = []
    handles = []

    def start(root, mode, race=""):
        log = (evidence / (mode + race + ".log")).open("w", encoding="utf-8")
        handles.append(log)
        args = [str(root / "runtime/bin/java.exe"), "-Djava.awt.headless=true", "-cp",
                str(root / "app/spirit-gate-rpg.jar") + os.pathsep + str(classes), "CorujaoSevenPackageProbe",
                mode, str(root), str(evidence)]
        if race:
            args.append(race)
        process = subprocess.Popen(args, cwd=root, stdout=log, stderr=subprocess.STDOUT)
        processes.append(process)
        return process

    try:
        server = start(roots[0], "server")
        deadline = time.monotonic() + 20
        while not (evidence / "port.txt").exists() and server.poll() is None and time.monotonic() < deadline:
            time.sleep(.05)
        if not (evidence / "port.txt").exists():
            raise RuntimeError("Servidor do pacote não iniciou")
        host = start(roots[0], "client", "reaper")
        guest = start(roots[1], "client", "human")
        codes = [host.wait(timeout=30), guest.wait(timeout=30), server.wait(timeout=40)]
        if codes != [0, 0, 0]:
            raise RuntimeError(codes)
        result = {"checks": checks, "exitCodes": codes, "server": json.loads((evidence / "server.json").read_text()),
                  "clients": [json.loads((evidence / (race + ".json")).read_text()) for race in ("reaper", "human")],
                  "physicalRadminTested": False, "nativeExecutableTested": False}
        (evidence / "result.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
        print(json.dumps(result, indent=2))
    finally:
        for process in processes:
            if process.poll() is None:
                process.terminate()
                process.wait(timeout=10)
        for handle in handles:
            handle.close()


if __name__ == "__main__":
    smoke(*sys.argv[1:])
