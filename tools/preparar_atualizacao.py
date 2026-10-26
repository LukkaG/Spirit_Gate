"""Prepara canal estatico e pacote inicial do atualizador, sem publicar ou acessar rede."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import shutil
import zipfile


def prepare(package, destination, version, feed_url="", role="anfitriao"):
    import re
    from urllib.parse import urlsplit
    if not re.fullmatch(r"[A-Za-z0-9._-]{1,80}", version):
        raise ValueError("Versao invalida")
    if role not in ("anfitriao", "convidado"):
        raise ValueError("Papel invalido")
    if feed_url:
        parsed = urlsplit(feed_url)
        if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.password:
            raise ValueError("Canal exige HTTPS sem credenciais")
    destination.mkdir(parents=True, exist_ok=False)
    channel = destination / "canal"
    channel.mkdir()
    package_hash = hashlib.sha256(package.read_bytes()).hexdigest()
    release_name = "release-" + package_hash[:24]
    release = channel / release_name
    release.mkdir()
    files, names = [], set()
    with zipfile.ZipFile(package) as archive:
        entries = [entry for entry in archive.infolist() if not entry.is_dir()]
        roots = {PurePosixPath(entry.filename).parts[0] for entry in entries}
        # One common payload/channel for both roles. Guest-only source packages
        # cannot silently restrict the host's next update.
        if len(roots) != 1 or not roots <= {"SpiritGateRPG", "SpiritGateCorujaoV-Anfitriao"}:
            raise ValueError("Use pacote legado RPG ou Anfitriao completo")
        modern = roots == {"SpiritGateCorujaoV-Anfitriao"}
        if len(entries) > 2000 or sum(e.file_size for e in entries) > 512 * 1024**2:
            raise ValueError("Pacote excessivo")
        for entry in entries:
            path = PurePosixPath(entry.filename)
            if len(path.parts) < 2:
                raise ValueError("Raiz inesperada")
            name = "/".join(path.parts[1:])
            if modern and name == "manifest-sha256.json":
                continue  # The normalized files get the updater's verified manifest.
            if modern and name == "app/spiritgate.jar":
                name = "app/spirit-gate-rpg.jar"
            if (not re.fullmatch(r"[A-Za-z0-9_. /-]+", name)
                    or any(p in ("", ".", "..") or p.rstrip(" .") != p
                           or re.match(r"^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])([.]|$)", p, re.I)
                           for p in name.split("/"))
                    or name.lower().split("/")[0] in ("saves", "data", "versions", "staging")
                    or name.lower() == "installed-manifest.json"
                    or name.lower() in names
                    or (entry.external_attr >> 16) & 0o170000 == 0o120000
                    or entry.file_size > 128 * 1024**2):
                raise ValueError("Caminho/arquivo inseguro: " + name)
            names.add(name.lower())
            data = archive.read(entry)
            if modern and path.suffix.lower() in (".cmd", ".ps1", ".cfg"):
                data = data.replace(b"spiritgate.jar", b"spirit-gate-rpg.jar")
            output = release / name
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(data)
            files.append(dict(path=name, bytes=len(data), sha256=hashlib.sha256(data).hexdigest()))
    if not {"runtime/bin/java.exe", "app/spirit-gate-rpg.jar"} <= names:
        raise ValueError("Runtime ou jogo ausente")
    manifest = dict(schemaVersion=1, version=version, basePath=release_name,
                    files=sorted(files, key=lambda f: f["path"]))
    manifest_bytes = (json.dumps(manifest, ensure_ascii=True, indent=2) + "\n").encode()
    (channel / "latest.json").write_bytes(manifest_bytes)
    identity = hashlib.sha256(manifest_bytes).hexdigest()
    launcher = destination / "SpiritGate-Atualizavel"
    initial = launcher / "versions" / identity
    shutil.copytree(release, initial)
    (initial / "installed-manifest.json").write_bytes(manifest_bytes)
    (launcher / "active.txt").write_text(identity, encoding="ascii")
    (launcher / "atualizacoes.json").write_text(json.dumps(dict(feedUrl=feed_url, role=role), indent=2), encoding="utf-8")
    for name in ("Atualizar-SpiritGate.ps1", "Jogar-SpiritGate.cmd"):
        shutil.copy2(Path(__file__).parent / "updater" / name, launcher / name)
    (launcher / "LEIA-ME.txt").write_text(
        "SPIRIT GATE - INICIADOR COM ATUALIZACAO\n"
        "Extraia tudo e abra Jogar-SpiritGate.cmd. Windows 64 bits; Java integrado.\n"
        f"Papel desta instalacao: {role}. O canal do jogo e comum aos dois papeis.\n"
        "O launcher inclui os botoes Radmin da versao instalada; o iniciador atualiza antes de abri-lo.\n"
        "Saves e preferencias ficam em data/saves, fora das versoes.\n" +
        ("Canal HTTPS configurado; atualizacoes sao verificadas ao abrir o iniciador.\n" if feed_url else
         "Canal localmente preparado; sem hospedagem configurada nao ha download automatico.\n") +
        "Se a internet falhar, a versao instalada continua disponivel.\n"
        "Nao copie saves para versions. Para importar saves antigos, com o jogo fechado,\n"
        "copie a pasta saves para data/saves e mantenha backup da origem.\n"
        "Nao abra o EXE dentro de versions: use o iniciador para manter os saves em data.\n",
        encoding="utf-8")
    suffix = "-Convidado" if role == "convidado" else ""
    output_zip = destination / f"SpiritGate-Atualizavel-{version}{suffix}-Windows64.zip"
    with zipfile.ZipFile(output_zip, "x", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(launcher.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(destination).as_posix())
    (Path(str(output_zip) + ".sha256")).write_text(hashlib.sha256(output_zip.read_bytes()).hexdigest() + "\n")
    return output_zip


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--feed-url", default="")
    parser.add_argument("--role", choices=("anfitriao", "convidado"), default="anfitriao")
    args = parser.parse_args()
    print(prepare(args.package, args.destination, args.version, args.feed_url, args.role))
