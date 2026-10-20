"""Publica uma entrega ja preparada em branch de distribuicao, sem tocar no checkout do jogo."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import zipfile


def git(*args, cwd=None, check=True):
    return subprocess.run(["git", *args], cwd=cwd, check=check, capture_output=True, text=True)


def publish(distribution, staging, remote, branch="updates"):
    if not re.fullmatch(r"[A-Za-z0-9_-]+", branch):
        raise ValueError("Branch invalida")
    channel = distribution / "canal"
    raw = (channel / "latest.json").read_bytes()
    manifest = json.loads(raw)
    if not re.fullmatch(r"release-[a-f0-9]{24}", manifest["basePath"]):
        raise ValueError("Diretorio de release invalido")
    for item in manifest["files"]:
        relative = Path(item["path"])
        if relative.is_absolute() or ".." in relative.parts or "\\" in item["path"]:
            raise ValueError("Caminho invalido")
        source = channel / manifest["basePath"] / relative
        if len(source.read_bytes()) != item["bytes"] or hashlib.sha256(source.read_bytes()).hexdigest() != item["sha256"]:
            raise ValueError("Arquivo divergente: " + item["path"])
    archive = distribution / f"SpiritGate-Atualizavel-{manifest['version']}-Windows64.zip"
    with zipfile.ZipFile(archive) as package:
        if package.testzip() is not None or any("/saves/" in n or "/data/" in n for n in package.namelist()):
            raise ValueError("Pacote inicial invalido")
        config = json.loads(package.read("SpiritGate-Atualizavel/atualizacoes.json"))
        expected = f"https://raw.githubusercontent.com/LukkaG/Spirit_Gate/{branch}/latest.json"
        if config["feedUrl"] != expected:
            raise ValueError("Canal do pacote difere do repositorio de destino")
    # Only the explicitly supplied distribution is copied, never user data or source workspace.
    staging.mkdir(parents=True, exist_ok=False)
    git("init", "--initial-branch", branch, cwd=staging)
    git("remote", "add", "origin", remote, cwd=staging)
    existing = git("ls-remote", "--exit-code", "--heads", "origin", branch, cwd=staging, check=False)
    if existing.returncode == 0:
        git("fetch", "--depth=1", "origin", branch, cwd=staging)
        git("checkout", "-B", branch, "FETCH_HEAD", cwd=staging)
    elif existing.returncode != 2:
        raise RuntimeError("Nao foi possivel verificar a branch remota")
    for setting in ("user.name", "user.email"):
        value = git("config", "--get", setting, check=False).stdout.strip()
        if not value:
            raise RuntimeError("Configure a identidade Git antes de publicar")
        git("config", setting, value, cwd=staging)
    (staging / ".gitattributes").write_text("* -text\n", encoding="ascii")
    shutil.copytree(channel / manifest["basePath"], staging / manifest["basePath"], dirs_exist_ok=True)
    (staging / "latest.json").write_bytes(raw)
    for name in (archive.name, "SpiritGate-Instalar-Windows64.zip"):
        shutil.copy2(archive, staging / name)
        (staging / (name + ".sha256")).write_text(hashlib.sha256(archive.read_bytes()).hexdigest() + "\n", encoding="ascii")
    (staging / "README.md").write_text(
        "# Spirit Gate - canal de testes\n\n"
        "Baixe [SpiritGate-Instalar-Windows64.zip](https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/SpiritGate-Instalar-Windows64.zip), "
        "extraia tudo e abra `Jogar-SpiritGate.cmd`. Java integrado.\n\n"
        "O iniciador verifica novas versoes ao abrir. Saves ficam em `data/saves`.\n\n"
        f"Versao atual: `{manifest['version']}`. Sprites experimentais; teste privado entre participantes.\n\n"
        "Esta branch contem somente distribuicao: nenhum save pessoal ou credencial foi incluido.\n",
        encoding="utf-8")
    git("add", "--all", cwd=staging)
    if git("diff", "--cached", "--quiet", cwd=staging, check=False).returncode == 0:
        print("Canal ja atualizado")
        return
    git("commit", "-m", f"Distribuir Spirit Gate {manifest['version']} com atualizacao automatica", cwd=staging)
    git("push", "origin", f"HEAD:refs/heads/{branch}", cwd=staging)
    print(json.dumps({"branch": branch, "commit": git("rev-parse", "HEAD", cwd=staging).stdout.strip(),
                      "feedUrl": expected, "version": manifest["version"]}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("distribution", type=Path)
    parser.add_argument("staging", type=Path)
    parser.add_argument("--remote", default="https://github.com/LukkaG/Spirit_Gate.git")
    args = parser.parse_args()
    publish(args.distribution.resolve(), args.staging.resolve(), args.remote)
