"""Recompila o jogo e reutiliza somente arquivos verificados de um canal local anterior."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import zipfile

from integrar_launcher_radmin import integrate

ROOT = Path(__file__).resolve().parents[1]


def build(base_channel, evidence, destination, version, feed_url=""):
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,80}", version):
        raise ValueError("Versao invalida")
    if evidence.exists() or destination.exists():
        raise FileExistsError("Use pastas novas para preservar entregas existentes")
    raw = (base_channel / "latest.json").read_bytes()
    manifest = json.loads(raw)
    jar_names = ("app/spirit-gate-rpg.jar", "app/spiritgate.jar")
    if sum(item["path"] in jar_names for item in manifest["files"]) != 1:
        raise ValueError("O canal deve conter exatamente um JAR principal reconhecido")
    if not re.fullmatch(r"release-[a-f0-9]{24}", manifest["basePath"]):
        raise ValueError("Release invalida")
    payload = (base_channel / manifest["basePath"]).resolve()
    for item in manifest["files"]:
        path = (payload / item["path"]).resolve()
        if not path.is_relative_to(payload) or path.is_symlink():
            raise ValueError("Arquivo fora do payload")
        data = path.read_bytes()
        if len(data) != item["bytes"] or hashlib.sha256(data).hexdigest() != item["sha256"]:
            raise ValueError("Payload divergente: " + item["path"])
    evidence.mkdir(parents=True)
    subprocess.run(["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                    str(ROOT / "tools/compile-local.ps1")], cwd=ROOT, check=True)
    jar = evidence / "spiritgate.jar"
    subprocess.run(["jar", "--create", "--file", str(jar), "-C",
                    str(ROOT / "server/target/launcher/classes"), "."], cwd=ROOT, check=True)
    package = evidence / "host.zip"
    with zipfile.ZipFile(package, "x", zipfile.ZIP_DEFLATED) as archive:
        for item in manifest["files"]:
            name = item["path"]
            data = (payload / name).read_bytes()
            if name in jar_names:
                name, data = "app/spiritgate.jar", jar.read_bytes()
            elif name.endswith(".cfg"):
                data = data.replace(b"spirit-gate-rpg.jar", b"spiritgate.jar")
            archive.writestr("SpiritGateCorujaoVI-Anfitriao/" + name, data)
    result = integrate(package, destination, version, feed_url)
    (evidence / "build-evidence.json").write_text(json.dumps({
        "baseManifestSha256": hashlib.sha256(raw).hexdigest(),
        "jarSha256": hashlib.sha256(jar.read_bytes()).hexdigest(),
        "sourceBuild": json.loads((ROOT / "server/target/launcher/build.json").read_text()),
        "packages": result,
    }, indent=2), encoding="utf-8")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("base_channel", type=Path)
    parser.add_argument("evidence", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--feed-url", default="")
    args = parser.parse_args()
    build(args.base_channel.resolve(), args.evidence.resolve(), args.destination.resolve(), args.version, args.feed_url)
