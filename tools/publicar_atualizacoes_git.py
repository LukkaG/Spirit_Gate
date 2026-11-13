"""Prepara um canal GitHub auditado; somente --publish faz commit e push normal."""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import uuid
import zipfile

from preparar_cdn import (bootstrap, check_payload, digest, json_object, read_local,
                          read_manifest, safe_name, verified_file)

DEFAULT_REPO = "LukkaG/Spirit_Gate"
MAX_BLOB_BYTES = 100 * 1024**2 - 1
ATTRIBUTES = b"* -text -filter\n"
ALLOWED_EXISTING_ATTRIBUTES = (ATTRIBUTES, b"* -text\n", b"* -text\r\n")
ALIASES = {"SpiritGate-Instalar-Windows64.zip", "SpiritGate-Anfitriao-Windows64.zip",
           "SpiritGate-Convidado-Windows64.zip", "SpiritGate-Ativar-GitHub.zip"}
ROOT_PAYLOAD = {"LEIA-ME.txt", "SpiritGate.exe", "SpiritGateRPG.exe", "Jogar-SpiritGate.cmd",
                "Launcher-SpiritGate.ps1", "Atualizar-SpiritGate.ps1",
                "Abrir-Geometrico.cmd", "ROTEIRO-RPG-OFFLINE.md"}
PRIVATE_PARTS = {".ssh", ".npmrc", ".pypirc", ".netrc", ".codex", ".agents", ".vscode", ".idea",
                 "node_modules", "target", "build", ".task-checkpoints", "secrets", "secrets.json",
                 "secret.json", "passwords", "passwords.txt", "credentials", "credentials.json"}
PRIVATE_SUFFIXES = {".pem", ".key", ".pfx", ".p12", ".sqlite", ".sqlite3", ".db", ".log", ".bak"}
TEXT_SUFFIXES = {".json", ".ps1", ".cmd", ".cfg", ".xml", ".txt", ".md", ".properties"}
SECRET_PATTERN = re.compile(rb"(?:-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|"
                            rb"\bAKIA[A-Z0-9]{16}\b|\bgh[pousr]_[A-Za-z0-9]{30,}\b|"
                            rb"\bgithub_pat_[A-Za-z0-9_]{30,}\b)")


def target(repo=DEFAULT_REPO, branch="updates", remote=None):
    if (not isinstance(repo, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_.-]+", repo)
            or repo.split("/")[1] in (".", "..") or repo.endswith(".git")):
        raise ValueError("Use --repo no formato DONO/REPOSITORIO")
    if (not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]{0,99}", branch) or ".." in branch
            or branch.endswith((".", ".lock")) or branch == "HEAD"):
        raise ValueError("Branch de distribuicao invalida")
    canonical = f"https://github.com/{repo}.git"
    accepted = {canonical, canonical[:-4], f"git@github.com:{repo}.git", f"git@github.com:{repo}"}
    if remote is not None and remote not in accepted:
        raise ValueError("Remoto difere de --repo ou contem parametros/credenciais")
    base = f"https://raw.githubusercontent.com/{repo}/{branch}"
    return dict(repo=repo, branch=branch, remote=remote or canonical, baseUrl=base, feedUrl=base + "/latest.json")


def audit_payload_name(name):
    safe_name(name)
    parts = name.split("/")
    if any(part.lower() in PRIVATE_PARTS for part in parts) or Path(name).suffix.lower() in PRIVATE_SUFFIXES:
        raise ValueError("Arquivo privado recusado: " + name)
    root = parts[0]
    allowed = (name in ROOT_PAYLOAD
               or name in {"app/.jpackage.xml", "app/SpiritGate.cfg", "app/SpiritGateRPG.cfg", "app/spirit-gate-rpg.jar"}
               or name in {"launcher/Launcher-SpiritGate.ps1", "launcher/Atualizar-SpiritGate.ps1"}
               or (root == "runtime" and len(parts) > 1)
               or (root == "assets" and len(parts) > 1 and Path(name).suffix.lower() in {".png", ".json", ".txt", ".md"})
               or (name.startswith("assets/dev/generated/") and parts[-1] == "manifest.properties")
               or (root == "docs" and len(parts) == 2 and re.fullmatch(r"TASK-[0-9]+-[A-Za-z0-9_-]+[.]md", parts[-1])))
    if not allowed:
        raise ValueError("Arquivo fora do payload publico permitido: " + name)


def audit_bytes(name, data):
    if len(data) > MAX_BLOB_BYTES:
        raise ValueError("Arquivo excede o limite de blob GitHub: " + name)
    if Path(name).suffix.lower() in TEXT_SUFFIXES:
        if SECRET_PATTERN.search(data):
            raise ValueError("Possivel credencial em arquivo publico: " + name)
        if name.endswith(".json"):
            try:
                value = json.loads(data)
            except (ValueError, UnicodeError):
                raise ValueError("JSON publico invalido: " + name) from None
            def visit(item):
                if isinstance(item, dict):
                    for key, value in item.items():
                        if key.lower() in {"password", "secret", "token", "accesstoken", "apikey", "awssecretaccesskey"} and value:
                            raise ValueError("Campo de credencial recusado: " + name)
                        visit(value)
                elif isinstance(item, list):
                    for value in item:
                        visit(value)
            visit(value)


def audit_public_name(name):
    if name == ".gitattributes":
        return
    safe_name(name)
    if name in {"latest.json", "README.md"}:
        return
    plain = name.removesuffix(".sha256")
    if plain in ALIASES or re.fullmatch(r"SpiritGate-Atualizavel-[A-Za-z0-9._-]{1,80}(?:-Convidado)?-Windows64[.]zip", plain):
        return
    prefix, separator, relative = name.partition("/")
    if separator and re.fullmatch(r"release-[a-f0-9]{24}", prefix):
        audit_payload_name(relative)
        return
    raise ValueError("Arquivo fora da arvore publica: " + name)


def public_files(root, *, checkout=False):
    found, names = {}, set()
    if root.is_symlink() or getattr(root, "is_junction", lambda: False)():
        raise ValueError("Diretorio publico nao pode ser link")
    for path in root.rglob("*"):
        relative = path.relative_to(root)
        if checkout and relative.parts[0] == ".git":
            continue
        if path.is_symlink() or getattr(path, "is_junction", lambda: False)():
            raise ValueError("Link recusado na arvore publica")
        if path.is_file():
            name = relative.as_posix()
            audit_public_name(name)
            if name.lower() in names:
                raise ValueError("Arquivo publico duplicado")
            names.add(name.lower())
            data = path.read_bytes()
            audit_bytes(name, data)
            if name == ".gitattributes" and data not in ALLOWED_EXISTING_ATTRIBUTES:
                raise ValueError("Filtros Git inesperados na distribuicao")
            found[name] = data
        elif path.is_dir():
            for part in relative.parts:
                safe_name(part)
                if part.lower() in PRIVATE_PARTS:
                    raise ValueError("Diretorio privado recusado")
    return found


def source_packages(distribution, guest_distribution):
    if (distribution / "integracao.json").is_file():
        if guest_distribution is not None:
            raise ValueError("Raiz integrada ja inclui o pacote convidado")
        return [("anfitriao", distribution / "Anfitriao"), ("convidado", distribution / "Convidado")]
    return [("anfitriao", distribution)] + ([("convidado", guest_distribution)] if guest_distribution else [])


def prepare(distribution, staging, remote=None, branch="updates", guest_distribution=None,
            *, repo=DEFAULT_REPO, activation_zip=None):
    """Dry-run concreto: nenhum processo Git, conexao ou credencial e utilizado."""
    distribution, staging = Path(distribution).resolve(), Path(staging).resolve()
    guest_distribution = Path(guest_distribution).resolve() if guest_distribution else None
    endpoint = target(repo, branch, remote)
    if staging.exists():
        raise FileExistsError("Use uma pasta nova para preparar; --publish reutiliza o plano revisado")
    roles = source_packages(distribution, guest_distribution)
    raw = read_local(roles[0][1], "canal/latest.json")
    manifest = read_manifest(raw)
    for item in manifest["files"]:
        audit_payload_name(item["path"])
    integration = None
    if len(roles) == 2 and (distribution / "integracao.json").is_file():
        integration = json_object(read_local(distribution, "integracao.json"))
        packages = integration.get("packages", [])
        if (integration.get("version") != manifest["version"] or integration.get("manifestSha256") != digest(raw)
                or len(packages) != 2 or {item["role"] for item in packages} != {"anfitriao", "convidado"}):
            raise ValueError("Auditoria da integracao diverge da entrega")
    files, immutable = {}, set()
    def add(name, data, fixed=False):
        audit_public_name(name)
        audit_bytes(name, data)
        files[name] = data
        if fixed:
            immutable.add(name)
    for role, folder in roles:
        if read_local(folder, "canal/latest.json") != raw:
            raise ValueError("Anfitriao e convidado devem compartilhar o mesmo manifesto")
        check_payload(folder / "canal", manifest)
        suffix = "-Convidado" if role == "convidado" else ""
        name = f"SpiritGate-Atualizavel-{manifest['version']}{suffix}-Windows64.zip"
        source = read_local(folder, name)
        if integration and digest(source) != next(item["sha256"] for item in integration["packages"] if item["role"] == role):
            raise ValueError("SHA-256 do pacote inicial diverge da integracao")
        contents = bootstrap(folder / name, raw, manifest, role, endpoint["feedUrl"])
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as archive:
            for entry, data in contents:
                audit_bytes(entry.filename, data)
                if entry.filename.endswith("/atualizacoes.json") and not set(json_object(data)) <= {"role", "feedUrl"}:
                    raise ValueError("Configuracao publica do bootstrap possui campos extras")
                archive.writestr(entry, data)
        zipped = buffer.getvalue()
        aliases = ("SpiritGate-Anfitriao-Windows64.zip", "SpiritGate-Instalar-Windows64.zip") if role == "anfitriao" else ("SpiritGate-Convidado-Windows64.zip",)
        for filename in (name, *aliases):
            add(filename, zipped, filename == name)
            add(filename + ".sha256", (digest(zipped) + "\n").encode("ascii"), filename == name)
    for item in manifest["files"]:
        add(manifest["basePath"] + "/" + item["path"], verified_file(roles[0][1] / "canal" / manifest["basePath"], item), True)
    add("latest.json", raw)
    add(".gitattributes", ATTRIBUTES)
    readme = ("# Spirit Gate - atualizacoes\n\n"
              f"Repositorio: https://github.com/{repo}; branch `{branch}`.\n\n"
              f"[Instalar anfitriao]({endpoint['baseUrl']}/SpiritGate-Instalar-Windows64.zip). "
              "Extraia tudo e abra `SpiritGate-Launcher.exe`. Java integrado.\n\n")
    if len(roles) == 2:
        readme += f"[Instalar convidado]({endpoint['baseUrl']}/SpiritGate-Convidado-Windows64.zip).\n\n"
    if activation_zip:
        from preparar_ativacao_cdn import validate_activation_zip
        archive = Path(activation_zip)
        checked = validate_activation_zip(archive, endpoint["feedUrl"], provider="github")
        data = archive.read_bytes()
        if digest(data) != checked["sha256"]:
            raise ValueError("Kit mudou durante a auditoria")
        add("SpiritGate-Ativar-GitHub.zip", data)
        add("SpiritGate-Ativar-GitHub.zip.sha256", (digest(data) + "\n").encode("ascii"))
        readme += f"[Ativar instalacao existente]({endpoint['baseUrl']}/SpiritGate-Ativar-GitHub.zip).\n\n"
    readme += (f"Versao atual: `{manifest['version']}`. Canal: {endpoint['feedUrl']}.\n\n"
               "O iniciador verifica novas versoes ao abrir. Saves ficam em `data/saves`.\n"
               "Esta branch contem somente os arquivos de distribuicao auditados.\n")
    add("README.md", readme.encode("utf-8"))
    staging.mkdir(parents=True, exist_ok=False)
    public = staging / "public"
    public.mkdir()
    for name, data in files.items():
        output = public / name
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(data)
    plan = dict(schemaVersion=1, **endpoint, version=manifest["version"], manifestSha256=digest(raw),
                sourceDistribution=str(distribution), published=False, dryRun=True,
                files=[dict(path=name, bytes=len(data), sha256=digest(data), immutable=name in immutable)
                       for name, data in sorted(files.items())])
    (staging / "git-publication-plan.json").write_text(json.dumps(plan, indent=2) + "\n", encoding="utf-8")
    return plan


def audit_plan(staging, endpoint):
    plan = json_object((staging / "git-publication-plan.json").read_bytes())
    if plan.get("schemaVersion") != 1 or any(plan.get(key) != value for key, value in endpoint.items()):
        raise ValueError("Destino do plano diverge dos parametros GitHub")
    actual = public_files(staging / "public")
    expected = plan.get("files", [])
    if len(expected) != len(actual) or len({item["path"] for item in expected}) != len(expected):
        raise ValueError("Inventario do plano publico divergente")
    for item in expected:
        data = actual.get(item["path"])
        if (data is None or len(data) != item["bytes"] or digest(data) != item["sha256"]
                or type(item.get("immutable")) is not bool):
            raise ValueError("Arquivo publico mudou apos o dry-run")
        should_fix = item["path"].startswith("release-") or item["path"].startswith("SpiritGate-Atualizavel-")
        if item["immutable"] != should_fix:
            raise ValueError("Politica de imutabilidade divergente")
    raw = actual.get("latest.json", b"")
    manifest = read_manifest(raw)
    if manifest["version"] != plan["version"] or digest(raw) != plan["manifestSha256"]:
        raise ValueError("Manifesto difere do plano")
    check_payload(staging / "public", manifest)
    if actual.get(".gitattributes") != ATTRIBUTES:
        raise ValueError("Filtros Git devem estar desativados")
    for name in actual:
        if name.endswith(".zip") and name != "SpiritGate-Ativar-GitHub.zip":
            role = "convidado" if "Convidado" in name else "anfitriao"
            contents = bootstrap(staging / "public" / name, raw, manifest, role, endpoint["feedUrl"])
            for entry, data in contents:
                audit_bytes(entry.filename, data)
            with zipfile.ZipFile(staging / "public" / name) as archive:
                config = json_object(archive.read("SpiritGate-Atualizavel/atualizacoes.json"))
                if config.get("feedUrl") != endpoint["feedUrl"] or not set(config) <= {"role", "feedUrl"}:
                    raise ValueError("Configuracao publica do ZIP difere do plano")
        if name == "SpiritGate-Ativar-GitHub.zip":
            from preparar_ativacao_cdn import validate_activation_zip
            validate_activation_zip(staging / "public" / name, endpoint["feedUrl"], provider="github")
        if name.endswith(".zip.sha256"):
            if actual[name] != (digest(actual[name[:-7]]) + "\n").encode("ascii"):
                raise ValueError("Sidecar do ZIP difere do pacote")
    return plan, actual


def git(*args, cwd=None, check=True):
    environment = dict(os.environ, GIT_LFS_SKIP_SMUDGE="1", GIT_TERMINAL_PROMPT="0")
    return subprocess.run(["git", "-c", "core.autocrlf=false", "-c", "core.safecrlf=false",
                           "-c", "core.hooksPath=.git/disabled-hooks", "-c", "core.attributesFile=.git/empty-attributes",
                           *args], cwd=cwd, check=check, capture_output=True, text=True, env=environment)


def check_immutable(plan, existing, incoming):
    base = read_manifest(incoming["latest.json"])["basePath"] + "/"
    old_release = {name for name in existing if name.startswith(base)}
    new_release = {name for name in incoming if name.startswith(base)}
    if old_release and old_release != new_release:
        raise ValueError("Release existente possui inventario diferente; gere nova versao")
    for item in plan["files"]:
        name = item["path"]
        if item["immutable"] and name in existing and incoming[name] != existing[name]:
            raise ValueError("Release/pacote imutavel existente nao pode ser sobrescrito: " + name)


def execute_publication(staging, endpoint):
    """Unica etapa com acesso remoto; nos testes o Git e substituido por mock."""
    plan, incoming = audit_plan(staging, endpoint)
    checkout = staging / ("checkout-" + uuid.uuid4().hex[:12])
    checkout.mkdir(exist_ok=False)
    git("init", "--template=", "--object-format=sha1", "--initial-branch", endpoint["branch"], cwd=checkout)
    git("remote", "add", "origin", endpoint["remote"], cwd=checkout)
    ref = "refs/heads/" + endpoint["branch"]
    remote = git("ls-remote", "--exit-code", "--heads", "origin", ref, cwd=checkout, check=False)
    previous = None
    if remote.returncode == 0:
        rows = [line.split() for line in remote.stdout.splitlines()]
        if len(rows) != 1 or len(rows[0]) != 2 or rows[0][1] != ref or not re.fullmatch(r"[a-f0-9]{40}", rows[0][0]):
            raise ValueError("Referencia remota inesperada")
        previous = rows[0][0]
        git("fetch", "--depth=1", "--no-tags", "origin", ref, cwd=checkout)
        if git("rev-parse", "FETCH_HEAD", cwd=checkout).stdout.strip() != previous:
            raise RuntimeError("Branch mudou durante a leitura; prepare uma nova tentativa")
        listing = git("ls-tree", "-r", "-z", "--full-tree", "FETCH_HEAD", cwd=checkout).stdout
        for entry in listing.split("\0"):
            if not entry:
                continue
            header, name = entry.split("\t", 1)
            mode, kind, _ = header.split()
            if mode not in {"100644", "100755"} or kind != "blob":
                raise ValueError("Links/submodulos recusados na branch publica")
            audit_public_name(name)
        attributes = git("show", "FETCH_HEAD:.gitattributes", cwd=checkout, check=False)
        if attributes.returncode != 0 or attributes.stdout.encode() not in ALLOWED_EXISTING_ATTRIBUTES:
            raise ValueError("Filtros da branch existente nao sao os permitidos")
        git("checkout", "-B", endpoint["branch"], "FETCH_HEAD", cwd=checkout)
    elif remote.returncode != 2:
        raise RuntimeError("Nao foi possivel verificar a branch remota")
    existing = public_files(checkout, checkout=True)
    check_immutable(plan, existing, incoming)
    audit_plan(staging, endpoint)
    for name, data in incoming.items():
        path = checkout / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    final = public_files(checkout, checkout=True)
    if final != (existing | incoming):
        raise ValueError("Arvore final difere dos arquivos auditados")
    git("add", "--all", cwd=checkout)
    listing = git("ls-files", "--stage", "-z", cwd=checkout).stdout
    indexed = set()
    for entry in listing.split("\0"):
        if not entry:
            continue
        header, name = entry.split("\t", 1)
        mode, blob, stage = header.split()
        data = final.get(name)
        if (data is None or stage != "0" or mode not in {"100644", "100755"}
                or hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest() != blob):
            raise ValueError("Indice Git transformou os bytes; filtros/LFS recusados")
        indexed.add(name)
    if indexed != set(final):
        raise ValueError("Indice Git possui inventario diferente")
    diff = git("diff", "--cached", "--quiet", cwd=checkout, check=False)
    if diff.returncode not in (0, 1):
        raise RuntimeError("Nao foi possivel comparar o commit preparado")
    changed = diff.returncode == 1
    if changed:
        for setting in ("user.name", "user.email"):
            value = git("config", "--get", setting, check=False).stdout.strip()
            if not value:
                raise RuntimeError("Configure a identidade Git antes de publicar")
            git("config", setting, value, cwd=checkout)
        git("commit", "-m", f"Distribuir Spirit Gate {plan['version']} com atualizacao automatica", cwd=checkout)
        git("push", "origin", f"HEAD:{ref}", cwd=checkout)
    commit = git("rev-parse", "HEAD", cwd=checkout).stdout.strip()
    receipt = dict(schemaVersion=1, **endpoint, version=plan["version"], manifestSha256=plan["manifestSha256"],
                   previousCommit=previous, commit=commit, published=True, pushed=changed,
                   onlineVerified=False, status="published" if changed else "unchanged")
    (staging / "publication.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    return receipt


def publish(distribution, staging, remote=None, branch="updates", guest_distribution=None,
            *, repo=DEFAULT_REPO, apply=False, activation_zip=None):
    staging = Path(staging).resolve()
    endpoint = target(repo, branch, remote)
    if not apply or not staging.exists():
        plan = prepare(distribution, staging, remote, branch, guest_distribution, repo=repo, activation_zip=activation_zip)
        if not apply:
            return plan
    return execute_publication(staging, endpoint)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("distribution", type=Path, help="Raiz integrada ou pasta Anfitriao preparada")
    parser.add_argument("staging", type=Path, help="Nova pasta local para public/ e plano revisavel")
    parser.add_argument("--repo", default=DEFAULT_REPO, help="DONO/REPOSITORIO GitHub")
    parser.add_argument("--branch", default="updates")
    parser.add_argument("--remote", help="HTTPS ou SSH GitHub; deve corresponder a --repo")
    parser.add_argument("--guest-distribution", type=Path)
    parser.add_argument("--activation-zip", type=Path, help="Kit GitHub validado para o mesmo canal")
    action = parser.add_mutually_exclusive_group()
    action.add_argument("--dry-run", action="store_true", help="Padrao: prepara tudo sem rede ou Git")
    action.add_argument("--publish", action="store_true", help="Audita plano e publica commit normal, sem force")
    args = parser.parse_args()
    result = publish(args.distribution, args.staging, args.remote, args.branch, args.guest_distribution,
                     repo=args.repo, apply=args.publish, activation_zip=args.activation_zip)
    print(json.dumps({key: value for key, value in result.items() if key != "files"}, indent=2))
