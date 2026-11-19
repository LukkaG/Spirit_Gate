"""Instala ferramentas fixadas, verificando SHA-256 e isolando os binarios do jogo."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import tarfile
import urllib.request
import zipfile

SPEC = Path(__file__).with_name("toolchain.json")


def installation_root() -> Path:
    """Mantem os executaveis no perfil local, sem alterar o PATH da maquina."""
    return Path(os.environ.get("LOCALAPPDATA", Path.home() / "AppData/Local")) / "SpiritGate/quality-tools"


def checked_path(root: Path, name: str) -> Path:
    """Recusa caminhos externos, ambiguos no Windows e entradas de link."""
    parts = PurePosixPath(name).parts
    if not parts or "\\" in name or ":" in name or name.startswith("/") or ".." in parts:
        raise ValueError(f"Caminho de arquivo recusado: {name}")
    if any(p.rstrip(" .") != p for p in parts):
        raise ValueError(f"Caminho ambiguo recusado: {name}")
    reserved = r"(?:CON|PRN|AUX|NUL|CONIN\$|CONOUT\$|COM[1-9¹²³]|LPT[1-9¹²³])(?:\..*)?"
    if any(re.fullmatch(reserved, part, re.IGNORECASE) for part in parts):
        raise ValueError(f"Nome reservado do Windows recusado: {name}")
    target = root.joinpath(*parts).resolve()
    if not target.is_relative_to(root.resolve()):
        raise ValueError("Arquivo fora do destino")
    return target


def extract(archive: Path, target: Path, kind: str) -> None:
    """Extrai somente para pasta nova, sem sobrescrever uma instalacao anterior."""
    target.mkdir(parents=True, exist_ok=False)
    if kind == "zip":
        with zipfile.ZipFile(archive) as bundle:
            seen = set()
            for entry in bundle.infolist():
                destination = checked_path(target, entry.filename.rstrip("/"))
                if stat.S_ISLNK(entry.external_attr >> 16):
                    raise ValueError("Link simbolico recusado")
                key = str(destination).casefold()
                if key in seen:
                    raise ValueError("Caminhos duplicados no ZIP")
                seen.add(key)
                if entry.is_dir():
                    destination.mkdir(parents=True, exist_ok=True)
                else:
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    with bundle.open(entry) as src, destination.open("xb") as dst:
                        while block := src.read(1024 * 1024):
                            dst.write(block)
    else:
        with tarfile.open(archive, "r:gz") as bundle:
            for entry in bundle.getmembers():
                checked_path(target, entry.name.rstrip("/"))
                if not entry.isfile() and not entry.isdir():
                    raise ValueError("Tipo de entrada TAR recusado")
            bundle.extractall(target, filter="data")


def install(root: Path, item: dict) -> dict:
    """Baixa da release oficial e valida o checksum fixado antes da extracao."""
    destination = root / f"{item['name']}-{item['version']}"
    receipt = root / (destination.name + ".json")
    if receipt.exists():
        recorded = json.loads(receipt.read_text(encoding="utf-8"))
        if recorded.get("sha256") != item["sha256"] or not destination.is_dir():
            raise RuntimeError("Instalacao anterior nao corresponde ao manifesto")
        print(f"Ja instalado: {destination.name}", flush=True)
        return recorded
    if destination.exists():
        raise RuntimeError(f"Destino parcial existente; preservar e investigar: {destination}")
    downloads = root / "downloads"
    downloads.mkdir(parents=True, exist_ok=True)
    archive = downloads / item["archive"]
    if not archive.exists():
        partial = archive.with_suffix(archive.suffix + ".part")
        if partial.exists():
            raise RuntimeError(f"Download parcial existente; preservar e investigar: {partial}")
        request = urllib.request.Request(item["url"], headers={"User-Agent": "SpiritGate-quality-setup"})
        print(f"Baixando {item['name']} {item['version']}...", flush=True)
        with urllib.request.urlopen(request, timeout=60) as src, partial.open("xb") as dst:
            while block := src.read(1024 * 1024):
                dst.write(block)
        partial.rename(archive)
    with archive.open("rb") as downloaded:
        digest = hashlib.file_digest(downloaded, "sha256").hexdigest()
    if digest != item["sha256"]:
        raise RuntimeError(f"SHA-256 divergente: {archive.name}")
    extract(archive, destination, item["format"])
    recorded = dict(item, installedAt=str(destination))
    with receipt.open("x", encoding="utf-8") as out:
        json.dump(recorded, out, indent=2)
    print(f"Instalado e verificado: {destination}", flush=True)
    return recorded


def main() -> None:
    """Instala somente as ferramentas explicitamente descritas no manifesto local."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=installation_root())
    parser.add_argument("--tool", action="append", choices=["spotbugs", "jazzer", "findsecbugs", "codeql", "pmd"])
    args = parser.parse_args()
    root = args.root.resolve()
    root.mkdir(parents=True, exist_ok=True)
    spec = json.loads(SPEC.read_text(encoding="utf-8"))
    for item in spec["tools"]:
        if not args.tool or item["name"] in args.tool:
            if not item.get("enabled", True):
                print(f"Ignorado pela politica do projeto fechado: {item['name']}", flush=True)
                continue
            install(root, item)


if __name__ == "__main__":
    main()

