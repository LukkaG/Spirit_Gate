"""Audita e monta uma entrega HTTPS para CDN; nao envia arquivos ou credenciais."""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
from pathlib import Path, PurePosixPath
import re
from typing import Callable
from urllib.parse import urlsplit, urlunsplit
import zipfile


IMMUTABLE_CACHE = "public, max-age=31536000, immutable"
MUTABLE_CACHE = "no-store, max-age=0, must-revalidate"
RESERVED = {".git", ".aws", "saves", "data", "studio-projects", "src", "credentials",
            "credentials.json", "credentials.ini", "id_rsa", "id_ed25519"}
BOOTSTRAP_FILES = {"active.txt", "atualizacoes.json", "SpiritGate-Launcher.exe", "LEIA-ME.txt",
                   "Iniciar-SpiritGate.ps1", "Launcher-SpiritGate.ps1", "Atualizar-SpiritGate.ps1",
                   "Jogar-SpiritGate.cmd"}


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def json_object(raw: bytes | str) -> dict:
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("Campo JSON duplicado")
            result[key] = value
        return result
    value = json.loads(raw, object_pairs_hook=unique)
    if not isinstance(value, dict):
        raise ValueError("Objeto JSON esperado")
    return value


def safe_name(value: str) -> str:
    if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z0-9_. /-]+", value):
        raise ValueError("Caminho de distribuicao invalido")
    path = PurePosixPath(value)
    if path.is_absolute() or not path.parts or str(path) != value:
        raise ValueError("Caminho nao canonico")
    for part in path.parts:
        if (part in (".", "..") or part.rstrip(" .") != part or part.lower() in RESERVED
                or part.lower().startswith(".env")
                or re.match(r"^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])([.]|$)", part, re.I)):
            raise ValueError("Caminho reservado ou privado")
    return value


def base_url(value: str) -> str:
    parsed = urlsplit(value)
    if (parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password
            or parsed.query or parsed.fragment or parsed.port not in (None, 443)
            or any(part in (".", "..") for part in parsed.path.split("/"))
            or "%" in parsed.path or "\\" in parsed.path):
        raise ValueError("Informe a URL HTTPS da CDN, sem credenciais, query ou fragmento")
    if parsed.path.rstrip("/"):
        safe_name(parsed.path.strip("/"))
    return urlunsplit(("https", parsed.netloc, parsed.path.rstrip("/"), "", ""))


def read_manifest(raw: bytes) -> dict:
    if len(raw) > 1048576:
        raise ValueError("Manifesto excessivo")
    manifest = json_object(raw)
    if (type(manifest.get("schemaVersion")) is not int or manifest["schemaVersion"] != 1
            or not isinstance(manifest.get("version"), str)
            or not re.fullmatch(r"[A-Za-z0-9._-]{1,80}", manifest["version"])
            or not re.fullmatch(r"release-[a-f0-9]{24}", str(manifest.get("basePath", "")))):
        raise ValueError("Manifesto incompativel")
    files = manifest.get("files")
    if not isinstance(files, list) or not 1 <= len(files) <= 2000:
        raise ValueError("Inventario invalido")
    names, total = set(), 0
    for item in files:
        name = safe_name(item["path"])
        size = item["bytes"]
        if (name.lower() in names or name.lower() == "installed-manifest.json"
                or not isinstance(size, int) or isinstance(size, bool) or not 0 <= size <= 134217728
                or not re.fullmatch(r"[a-f0-9]{64}", str(item["sha256"]))):
            raise ValueError("Arquivo duplicado, hash ou tamanho invalido")
        names.add(name.lower()); total += size
    if total > 536870912 or not {"runtime/bin/java.exe", "app/spirit-gate-rpg.jar"} <= names:
        raise ValueError("Jogo/runtime ausente ou distribuicao excessiva")
    return manifest


def read_local(root: Path, name: str) -> bytes:
    path = root / safe_name(name)
    if not path.resolve().is_relative_to(root.resolve()):
        raise ValueError("Arquivo fora da distribuicao")
    cursor = path
    while True:
        if cursor.is_symlink() or getattr(cursor, "is_junction", lambda: False)():
            raise ValueError("Link de arquivo recusado")
        if cursor == root:
            break
        cursor = cursor.parent
    return path.read_bytes()


def verified_file(root: Path, item: dict) -> bytes:
    data = read_local(root, item["path"])
    if len(data) != item["bytes"] or digest(data) != item["sha256"]:
        raise ValueError("Payload divergente: " + item["path"])
    return data


def check_payload(channel: Path, manifest: dict) -> None:
    release = channel / manifest["basePath"]
    expected = set()
    for item in manifest["files"]:
        verified_file(release, item)
        expected.add(item["path"])
    actual = {path.relative_to(release).as_posix() for path in release.rglob("*") if path.is_file()}
    if actual != expected:
        raise ValueError("Release possui arquivos fora do inventario")


def bootstrap(archive: Path, manifest_raw: bytes, manifest: dict, role: str, feed: str) -> list[tuple[zipfile.ZipInfo, bytes]]:
    """Confere cada byte do payload no ZIP e muda somente a URL do canal na copia."""
    identity = digest(manifest_raw)
    prefix = "SpiritGate-Atualizavel/"
    version_prefix = prefix + "versions/" + identity + "/"
    expected = {version_prefix + item["path"]: item for item in manifest["files"]}
    contents, names = [], set()
    if archive.is_symlink() or getattr(archive, "is_junction", lambda: False)():
        raise ValueError("ZIP inicial nao pode ser link")
    with zipfile.ZipFile(archive) as source:
        entries = [entry for entry in source.infolist() if not entry.is_dir()]
        if len(entries) > 2100 or sum(entry.file_size for entry in entries) > 550 * 1024**2:
            raise ValueError("ZIP inicial excessivo")
        for entry in entries:
            safe_name(entry.filename)
            if (not entry.filename.startswith(prefix) or entry.filename.lower() in names
                    or (entry.external_attr >> 16) & 0o170000 == 0o120000):
                raise ValueError("ZIP com caminho inesperado ou duplicado")
            relative = entry.filename[len(prefix):]
            if not relative.startswith("versions/") and relative not in BOOTSTRAP_FILES:
                raise ValueError("Arquivo extra fora do bootstrap")
            names.add(entry.filename.lower())
            data = source.read(entry)
            item = expected.get(entry.filename)
            if item and (len(data) != item["bytes"] or digest(data) != item["sha256"]):
                raise ValueError("Payload instalado difere do canal")
            if entry.filename.startswith(prefix + "versions/") and not item:
                if entry.filename != version_prefix + "installed-manifest.json" or data != manifest_raw:
                    raise ValueError("Versao extra ou manifesto instalado divergente")
            if entry.filename == prefix + "active.txt" and data.decode("ascii").strip() != identity:
                raise ValueError("Ponteiro ativo divergente")
            if entry.filename == prefix + "atualizacoes.json":
                config = json_object(data)
                if config.get("role", "anfitriao") != role or config.get("feedUrl", "") not in ("", feed):
                    raise ValueError("Papel ou canal do pacote divergente")
                config["feedUrl"] = feed
                data = (json.dumps(config, indent=2) + "\n").encode("utf-8")
            contents.append((entry, data))
    required = {prefix + name for name in ("active.txt", "atualizacoes.json", "SpiritGate-Launcher.exe")}
    if (prefix + "Iniciar-SpiritGate.ps1").lower() in names:
        required.update(version_prefix + name for name in ("launcher/Launcher-SpiritGate.ps1", "launcher/Atualizar-SpiritGate.ps1"))
    else:
        required.update(prefix + name for name in ("Launcher-SpiritGate.ps1", "Atualizar-SpiritGate.ps1"))
    required.update(expected)
    required.add(version_prefix + "installed-manifest.json")
    if not {name.lower() for name in required} <= names:
        raise ValueError("ZIP inicial incompleto")
    return contents


def prepare(distribution: Path, destination: Path, url: str) -> dict:
    url = base_url(url)
    feed = url + "/latest.json"
    host_channel = distribution / "Anfitriao/canal"
    raw = read_local(distribution, "Anfitriao/canal/latest.json")
    if read_local(distribution, "Convidado/canal/latest.json") != raw:
        raise ValueError("Anfitriao e convidado possuem manifestos diferentes")
    manifest = read_manifest(raw)
    integration = json_object(read_local(distribution, "integracao.json"))
    if integration.get("version") != manifest["version"] or integration.get("manifestSha256") != digest(raw):
        raise ValueError("Auditoria da integracao difere do canal")
    package_audit = {item["role"]: item["sha256"] for item in integration["packages"]}
    if len(integration["packages"]) != 2 or set(package_audit) != {"anfitriao", "convidado"}:
        raise ValueError("Auditoria dos papeis incompleta")
    for label in ("Anfitriao", "Convidado"):
        check_payload(distribution / label / "canal", manifest)
    packages = []
    for role, label, suffix in (("anfitriao", "Anfitriao", ""), ("convidado", "Convidado", "-Convidado")):
        filename = f"SpiritGate-Atualizavel-{manifest['version']}{suffix}-Windows64.zip"
        if digest(read_local(distribution, label + "/" + filename)) != package_audit[role]:
            raise ValueError("SHA-256 do ZIP inicial divergente")
        packages.append((role, filename, bootstrap(distribution / label / filename, raw, manifest, role, feed)))
    destination.mkdir(parents=True, exist_ok=False)
    public = destination / "public"
    public.mkdir()
    uploads = []

    def add(key: str, data: bytes, phase: int, immutable: bool) -> None:
        key = safe_name(key)
        target = public / key
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        uploads.append(dict(key=key, bytes=len(data), sha256=digest(data), immutable=immutable,
                            cacheControl=IMMUTABLE_CACHE if immutable else MUTABLE_CACHE,
                            contentType="application/json" if key.endswith(".json") else
                            mimetypes.guess_type(key)[0] or "application/octet-stream", phase=phase))

    for item in manifest["files"]:
        add(manifest["basePath"] + "/" + item["path"], verified_file(host_channel / manifest["basePath"], item), 0, True)
    downloads = {}
    for role, filename, contents in packages:
        archive = destination / filename
        with zipfile.ZipFile(archive, "x", zipfile.ZIP_DEFLATED) as package:
            for entry, data in contents:
                package.writestr(entry, data)
        data = archive.read_bytes()
        archive.unlink()
        versioned = "downloads/" + digest(data) + "/" + filename
        add(versioned, data, 1, True)
        alias = "SpiritGate-Anfitriao-Windows64.zip" if role == "anfitriao" else "SpiritGate-Convidado-Windows64.zip"
        for key in (alias, "SpiritGate-Instalar-Windows64.zip") if role == "anfitriao" else (alias,):
            add(key, data, 3, False)
            add(key + ".sha256", (digest(data) + "\n").encode("ascii"), 3, False)
        downloads[role] = dict(url=url + "/" + alias, immutableUrl=url + "/" + versioned, sha256=digest(data))
    add("latest.json", raw, 2, False)
    uploads.sort(key=lambda item: (item["phase"], item["key"]))
    plan = dict(schemaVersion=1, version=manifest["version"], baseUrl=url, feedUrl=feed,
                manifestSha256=digest(raw), published=False, downloads=downloads, uploads=uploads)
    (destination / "deployment-plan.json").write_text(json.dumps(plan, indent=2) + "\n", encoding="utf-8")
    return plan


def execute_plan(destination: Path, upload: Callable[[Path, dict], None]) -> dict:
    """Audita tudo e ativa apos os imutaveis, antes de promover os aliases dos instaladores."""
    plan = json_object((destination / "deployment-plan.json").read_text(encoding="utf-8"))
    base_url(plan["baseUrl"])
    uploads = plan["uploads"]
    activation = [index for index, item in enumerate(uploads) if item["key"] == "latest.json"]
    if len(activation) != 1:
        raise ValueError("latest.json deve ser publicado uma unica vez")
    commit = activation[0]
    if (uploads[commit]["phase"] != 2 or uploads[commit]["immutable"]
            or any(not item["immutable"] or item["phase"] not in (0, 1) for item in uploads[:commit])
            or any(item["immutable"] or item["phase"] != 3 for item in uploads[commit + 1:])):
        raise ValueError("Ative apos os imutaveis e antes dos aliases de instalacao")
    if [item["phase"] for item in uploads] != sorted(item["phase"] for item in uploads):
        raise ValueError("Ordem de publicacao invalida")
    for item in uploads:
        if item["cacheControl"] != (IMMUTABLE_CACHE if item["immutable"] else MUTABLE_CACHE):
            raise ValueError("Politica de cache divergente")
        data = read_local(destination / "public", item["key"])
        if len(data) != item["bytes"] or digest(data) != item["sha256"]:
            raise ValueError("Artefato alterado antes da publicacao")
    for item in uploads:
        upload(destination / "public" / item["key"], dict(item))
    return plan


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("distribution", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--base-url", required=True)
    args = parser.parse_args()
    result = prepare(args.distribution.resolve(), args.destination.resolve(), args.base_url)
    print(json.dumps({key: value for key, value in result.items() if key != "uploads"}, indent=2))
