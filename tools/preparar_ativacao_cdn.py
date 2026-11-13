"""Monta um ativador pequeno para instalacoes existentes, sem copiar jogo ou runtime."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import urlsplit
import zipfile

from preparar_cdn import json_object, safe_name


ROOT = Path(__file__).resolve().parents[1]
FILES = ("Iniciar-SpiritGate.ps1", "Launcher-SpiritGate.ps1", "Atualizar-SpiritGate.ps1",
         "Jogar-SpiritGate.cmd", "SpiritGate-Launcher.exe")


def kit_name(provider):
    if provider not in ("cdn", "github"):
        raise ValueError("Provedor de atualizacao invalido")
    return "SpiritGate-Ativar-GitHub" if provider == "github" else "SpiritGate-Ativar-CDN"


def provider_feed(value, provider):
    kit_name(provider)
    value = valid_feed(value)
    if provider == "github" and not re.fullmatch(
            r"https://raw[.]githubusercontent[.]com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/[A-Za-z0-9_-]+/latest[.]json", value):
        raise ValueError("O kit GitHub exige o endereco raw do manifesto latest.json")
    return value


def valid_feed(value):
    if not value:
        return ""
    uri = urlsplit(value)
    if (uri.scheme != "https" or not uri.hostname or uri.username or uri.password or uri.query
            or uri.fragment or uri.port not in (None, 443) or "\\" in value or any(c.isspace() for c in value)):
        raise ValueError("Canal exige HTTPS sem credenciais, query ou fragmento")
    return value


def readme_text(feed_configured):
    return (
        "SPIRIT GATE - ATIVAR ATUALIZACOES NA INSTALACAO EXISTENTE\n\n"
        "Extraia todo o ZIP. Feche o jogo e o launcher. Abra Ativar-SpiritGate.exe\n"
        "e selecione a pasta atual que contem atualizacoes.json e versions.\n"
        "O ativador troca somente os arquivos pequenos do iniciador e configura o canal.\n"
        "Preserva o papel Anfitriao/Convidado, saves, personagens, jogo e Java instalado.\n"
        "Um backup dos arquivos anteriores fica em launcher-backups.\n"
        + ("O canal HTTPS ja esta configurado neste kit.\n" if feed_configured else
           "Informe a URL publica HTTPS do canal quando solicitada. Nao informe senhas ou tokens.\n")
        + "Depois abra SpiritGate-Launcher.exe na mesma pasta de sempre.\n")


def validate_activation_zip(archive, expected_feed, provider="cdn"):
    """Aceita somente o kit do bootstrap conhecido e vinculado ao mesmo canal do jogo."""
    archive = Path(archive)
    expected_feed = provider_feed(expected_feed, provider)
    if not expected_feed:
        raise ValueError("Publicacao do ativador exige o canal real configurado")
    if archive.is_symlink() or getattr(archive, "is_junction", lambda: False)():
        raise ValueError("Kit nao pode ser link")
    raw = archive.read_bytes()
    if not 1 <= len(raw) <= 2 * 1024 * 1024:
        raise ValueError("Kit de ativacao excessivo ou vazio")
    prefix = kit_name(provider) + "/"
    expected = {prefix + name for name in ("Ativar-SpiritGate.exe", "Ativar-SpiritGate.ps1",
                                            "manifest.json", "canal.json", "LEIA-ME.txt")}
    expected.update(prefix + "payload/" + name for name in FILES)
    contents = {}
    with zipfile.ZipFile(io.BytesIO(raw)) as package:
        if sum(entry.file_size for entry in package.infolist()) > 8 * 1024 * 1024:
            raise ValueError("Conteudo descompactado do ativador excessivo")
        for entry in package.infolist():
            if entry.is_dir():
                if entry.filename not in (prefix, prefix + "payload/"):
                    raise ValueError("Pasta inesperada no ativador")
                continue
            safe_name(entry.filename)
            if ((entry.external_attr >> 16) & 0o170000 == 0o120000
                    or entry.filename not in expected or entry.filename in contents):
                raise ValueError("Arvore do kit inesperada, repetida ou com links")
            contents[entry.filename] = package.read(entry)
    if set(contents) != expected:
        raise ValueError("Kit de ativacao incompleto")
    channel = json_object(contents[prefix + "canal.json"])
    if set(channel) != {"feedUrl"} or valid_feed(channel["feedUrl"]) != expected_feed:
        raise ValueError("Canal do ativador difere do plano da publicacao")
    manifest = json_object(contents[prefix + "manifest.json"])
    if set(manifest) != {"schemaVersion", "files"} or type(manifest["schemaVersion"]) is not int or manifest["schemaVersion"] != 1:
        raise ValueError("Manifesto do ativador incompativel")
    files = manifest["files"]
    if not isinstance(files, list) or len(files) != len(FILES):
        raise ValueError("Inventario do ativador invalido")
    seen = set()
    for item in files:
        if set(item) != {"path", "bytes", "sha256"} or item["path"] not in FILES or item["path"] in seen:
            raise ValueError("Arquivo inesperado no inventario do ativador")
        seen.add(item["path"])
        data = contents[prefix + "payload/" + item["path"]]
        if type(item["bytes"]) is not int or len(data) != item["bytes"] or hashlib.sha256(data).hexdigest() != item["sha256"]:
            raise ValueError("Payload do ativador divergente")
        if not item["path"].endswith(".exe") and data != (ROOT / "tools/updater" / item["path"]).read_bytes():
            raise ValueError("Script do kit difere do bootstrap administrativo conhecido")
    if contents[prefix + "Ativar-SpiritGate.ps1"] != (ROOT / "tools/updater/Ativar-SpiritGate.ps1").read_bytes():
        raise ValueError("Ativador difere da fonte administrativa conhecida")
    if contents[prefix + "LEIA-ME.txt"] != readme_text(True).encode("utf-8"):
        raise ValueError("Instrucoes inesperadas no kit")
    for name, entry_script in (("Ativar-SpiritGate.exe", "Ativar-SpiritGate.ps1"),
                               ("payload/SpiritGate-Launcher.exe", "Iniciar-SpiritGate.ps1")):
        data = contents[prefix + name]
        if len(data) < 64 or data[:2] != b"MZ" or entry_script.encode("utf-16-le") not in data:
            raise ValueError("Executavel do ativador/launcher incompativel")
        offset = int.from_bytes(data[60:64], "little")
        if data[offset:offset + 4] != b"PE\0\0":
            raise ValueError("Executavel PE invalido")
    return dict(bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest(), feedUrl=expected_feed)


def prepare(destination, feed_url="", provider="cdn"):
    feed_url = provider_feed(feed_url, provider)
    destination.mkdir(parents=True, exist_ok=False)
    kit = destination / kit_name(provider)
    payload = kit / "payload"
    payload.mkdir(parents=True)
    source = ROOT / "tools/updater"
    for name in FILES:
        if name.endswith(".exe"):
            continue
        shutil.copyfile(source / name, payload / name)
    for target, entry in ((payload / "SpiritGate-Launcher.exe", "Iniciar-SpiritGate.ps1"),
                          (kit / "Ativar-SpiritGate.exe", "Ativar-SpiritGate.ps1")):
        subprocess.run(["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                        str(source / "Build-Launcher.ps1"), "-Destination", str(target), "-EntryScript", entry],
                       check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    shutil.copyfile(source / "Ativar-SpiritGate.ps1", kit / "Ativar-SpiritGate.ps1")
    manifest = dict(schemaVersion=1, files=[dict(path=name, bytes=(payload / name).stat().st_size,
                                               sha256=hashlib.sha256((payload / name).read_bytes()).hexdigest())
                                          for name in FILES])
    (kit / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    (kit / "canal.json").write_text(json.dumps(dict(feedUrl=feed_url), indent=2) + "\n", encoding="utf-8")
    (kit / "LEIA-ME.txt").write_text(readme_text(bool(feed_url)), encoding="utf-8", newline="\n")
    archive = destination / (kit_name(provider) + ".zip")
    with zipfile.ZipFile(archive, "x", zipfile.ZIP_DEFLATED) as package:
        for path in sorted(kit.rglob("*")):
            if path.is_file():
                package.write(path, path.relative_to(destination).as_posix())
    result = dict(archive=str(archive.resolve()), bytes=archive.stat().st_size,
                  sha256=hashlib.sha256(archive.read_bytes()).hexdigest(), feedConfigured=bool(feed_url),
                  includesGame=False, includesRuntime=False)
    (destination / "activation.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    Path(str(archive) + ".sha256").write_text(result["sha256"] + "\n", encoding="ascii")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--feed-url", default="")
    parser.add_argument("--provider", choices=("cdn", "github"), default="cdn")
    args = parser.parse_args()
    print(json.dumps(prepare(args.destination.resolve(), args.feed_url, args.provider), indent=2))
