"""Audita o pacote portatil local sem extrair nem executar seu conteudo.

Uso: python tools/auditar_rc.py --zip dist/pacote.zip --report docs/auditoria.json
Requer somente a biblioteca padrao do Python. Os hashes verificam integridade,
nao autenticidade; o relatorio explicita os limites da inspecao de segredos.
"""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import stat
import struct
import sys
import tempfile
import zipfile


ENTRYPOINT = "com.projetok.prototype.DebugLauncher"
MAIN_JAR = "app/spirit-gate-arena.jar"
CLASS_PREFIXES = ("com/projetok/domain/", "com/projetok/prototype/")
HEX_SHA256 = re.compile(r"^[0-9a-fA-F]{64}$")
PRIVATE_PARTS = {
    ".git", ".svn", ".hg", ".ssh", ".aws", ".codex", ".agents", ".m2",
    "target", "test", "tests", "test-classes", "node_modules",
}
PRIVATE_SUFFIXES = {".pem", ".key", ".p12", ".pfx", ".jks", ".keystore"}
SECRET_PATTERNS = {
    "marcador_de_chave_privada": re.compile(
        rb"-----BEGIN (?:RSA |EC |DSA |OPENSSH |ENCRYPTED )?PRIVATE KEY-----"
    ),
    "identificador_aws": re.compile(rb"(?<![A-Z0-9])(?:AKIA|ASIA)[A-Z0-9]{16}(?![A-Z0-9])"),
    "jdbc_com_credenciais_na_url": re.compile(
        rb"jdbc:[a-zA-Z0-9]+://[^\x00\s\"']{1,160}:[^\x00\s\"']{1,160}@"
    ),
    "jdbc_com_senha_na_query": re.compile(
        rb"jdbc:[^\x00\r\n]{1,500}[?;&](?:password|passwd|pwd)=[^\x00\s;&]+",
        re.IGNORECASE,
    ),
}
LIMITATIONS = [
    "Hashes e inventarios detectam divergencias, mas nao substituem assinatura "
    "digital nem autenticam um pacote e sidecars adulterados em conjunto.",
    "A busca de credenciais inspeciona somente o bytecode das classes do jogo, "
    "com padroes concretos; nao comprova ausencia absoluta de segredos, "
    "ofuscacao ou credenciais em outros formatos.",
    "A comparacao prova igualdade com server/target/classes no instante da "
    "auditoria; a compilacao recente dos fontes depende do mvn clean test "
    "registrado separadamente no processo de release.",
    "A auditoria nao executa o EXE e nao substitui a homologacao interativa "
    "em outro Windows sem Java ou Maven instalados.",
]


class AuditFailure(Exception):
    """Representa uma recusa de auditoria com mensagem sem dados sensiveis."""


def require(condition: bool, message: str) -> None:
    """Interrompe a verificacao quando uma garantia necessaria nao e atendida."""
    if not condition:
        raise AuditFailure(message)


def sha256_stream(stream) -> str:
    """Calcula SHA-256 por blocos, sem carregar o runtime inteiro na memoria."""
    digest = hashlib.sha256()
    for block in iter(lambda: stream.read(1024 * 1024), b""):
        digest.update(block)
    return digest.hexdigest()


def sha256_file(path: Path) -> str:
    """Calcula o resumo de um arquivo local somente para leitura."""
    with path.open("rb") as stream:
        return sha256_stream(stream)


def relative_name(name: str) -> str:
    """Valida um caminho de ZIP para extracao segura inclusive no Windows."""
    require(isinstance(name, str) and bool(name), "Caminho vazio no pacote.")
    require("\\" not in name and "\x00" not in name and ":" not in name,
            "Caminho nao portatil ou com fluxo alternativo no pacote.")
    require(not name.startswith("/"), "Caminho absoluto no pacote.")
    cleaned = name[:-1] if name.endswith("/") else name
    parts = cleaned.split("/")
    require(all(part not in ("", ".", "..") for part in parts),
            "Caminho relativo inseguro no pacote.")
    for part in parts:
        require(part.rstrip(" .") == part and not any(ord(c) < 32 for c in part) and
                not any(c in '<>"|?*' for c in part),
                "Nome incompativel com extracao segura no Windows.")
        stem = part.split(".", 1)[0].upper()
        require(not re.fullmatch(r"CON|PRN|AUX|NUL|CONIN\$|CONOUT\$|COM[1-9¹²³]|LPT[1-9¹²³]", stem),
                "Nome de dispositivo reservado no pacote.")
    return "/".join(parts)


def reject_private_path(name: str) -> None:
    """Recusa arquivos privados, fontes de teste e saidas de desenvolvimento."""
    for part in PurePosixPath(name).parts:
        lowered = part.casefold()
        require(lowered not in PRIVATE_PARTS and not lowered.startswith(".env"),
                f"Entrada privada ou de desenvolvimento: {name}")
        require(PurePosixPath(lowered).suffix not in PRIVATE_SUFFIXES,
                f"Extensao privada recusada: {name}")
    require(not name.casefold().endswith(("test.class", "tests.class")),
            f"Classe de teste recusada: {name}")


def archive_files(archive: zipfile.ZipFile) -> dict[str, zipfile.ZipInfo]:
    """Valida entradas antes de ler conteudo e detecta colisoes de nomes."""
    require(len(archive.infolist()) <= 50_000, "Numero excessivo de entradas no ZIP.")
    seen: set[str] = set()
    files = {}
    total_size = 0
    for item in archive.infolist():
        name = relative_name(item.orig_filename)
        require(name.casefold() not in seen, "Entradas duplicadas ou ambiguas no ZIP.")
        seen.add(name.casefold())
        reject_private_path(name)
        mode = item.external_attr >> 16
        require(not stat.S_ISLNK(mode), "Link simbolico recusado no pacote.")
        require(not item.flag_bits & 1, "Entrada criptografada recusada no pacote.")
        require(item.file_size <= 512 * 1024 * 1024, "Entrada excede o limite de auditoria.")
        total_size += item.file_size
        if not item.is_dir():
            files[name] = item
    require(total_size <= 2 * 1024 * 1024 * 1024, "Pacote excede o limite de auditoria.")
    folded_files = {name.casefold() for name in files}
    for name in files:
        require(not any(parent.as_posix().casefold() in folded_files
                        for parent in PurePosixPath(name).parents
                        if parent != PurePosixPath(".")),
                "Arquivo usado tambem como diretorio no pacote.")
    return files


def json_object(data: bytes) -> dict:
    """Le JSON sem chaves duplicadas que tornariam o inventario ambiguo."""
    def unique_pairs(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, "Chave duplicada no manifest.")
            result[key] = value
        return result

    result = json.loads(data.decode("utf-8-sig"), object_pairs_hook=unique_pairs)
    require(isinstance(result, dict), "O manifest deve ser um objeto JSON.")
    return result


def main_manifest(data: bytes) -> dict[str, str]:
    """Interpreta o cabecalho principal do JAR, incluindo continuacoes."""
    lines = []
    for line in data.decode("utf-8").splitlines():
        if not line:
            break
        if line.startswith(" "):
            require(bool(lines), "Continuacao invalida no manifest do JAR.")
            lines[-1] += line[1:]
        else:
            lines.append(line)
    result = {}
    for line in lines:
        require(": " in line, "Cabecalho invalido no manifest do JAR.")
        key, value = line.split(": ", 1)
        require(key.casefold() not in result, "Cabecalho duplicado no manifest do JAR.")
        result[key.casefold()] = value
    return result


def audit(zip_path: Path, report: dict) -> None:
    """Executa a auditoria em etapas; falhas preservam os resultados anteriores."""
    def passed(identifier: str, **details) -> None:
        report["checks"].append({"id": identifier, "status": "PASS", **details})

    # --- Integridade do ZIP e estrutura segura antes de ler arquivos internos. ---
    require(zip_path.is_file(), "ZIP de entrada inexistente.")
    digest = sha256_file(zip_path)
    report["zipSha256"] = digest
    sidecar = Path(str(zip_path) + ".sha256")
    require(sidecar.is_file(), "Sidecar SHA-256 do ZIP ausente.")
    checksum_line = sidecar.read_text(encoding="utf-8-sig").strip()
    matched = re.fullmatch(r"([a-fA-F0-9]{64})[ \t]+\*?([^\r\n]+)", checksum_line)
    require(matched is not None, "Formato invalido no sidecar SHA-256.")
    require(matched.group(1).lower() == digest, "SHA-256 externo divergente do ZIP.")
    require(matched.group(2) == zip_path.name, "Sidecar SHA-256 identifica outro ZIP.")
    passed("zip_sha256_sidecar", sha256=digest, sizeBytes=zip_path.stat().st_size)

    with zipfile.ZipFile(zip_path) as archive:
        files = archive_files(archive)
        require(bool(files), "ZIP sem arquivos.")
        roots = {name.split("/", 1)[0] for name in files}
        require(roots == {"SpiritGate"}, "Esperada somente a raiz SpiritGate no ZIP.")
        relative = {name[len("SpiritGate/"):]: item for name, item in files.items()}
        required = {"manifest.json", "VERSION.txt", "INVENTARIO-SHA256.txt",
                    "LEIA-ME-TESTER.txt", "SpiritGate.exe", "app/SpiritGate.cfg",
                    MAIN_JAR, "runtime/release", "runtime/bin/server/jvm.dll"}
        require(required <= relative.keys(), "Arquivo obrigatorio ausente no pacote.")
        for name in relative:
            require(not name.casefold().endswith(".class"),
                    "Classe solta fora do JAR autorizado no pacote.")
            if name.casefold().endswith(".jar"):
                require(name in (MAIN_JAR, "runtime/lib/jrt-fs.jar"),
                        f"JAR extra nao autorizado no pacote: {name}")
        passed("estrutura_segura", files=len(files), root="SpiritGate")

        def read(name: str) -> bytes:
            return archive.read(relative[name])

        visual_names = {name for name in relative if name.startswith('visual/')}
        if visual_names:
            require(visual_names == {'visual/character.png', 'visual/character.properties', 'visual/review.json'},
                    'Whitelist do pacote visual divergente.')
            from pacote_visual import stage
            with tempfile.TemporaryDirectory(prefix='spirit-visual-audit-') as directory:
                base = Path(directory)
                for name in ('character.png', 'character.properties', 'review.json'):
                    (base/name).write_bytes(read('visual/'+name))
                config = json_object((base/'review.json').read_bytes())
                require(len(config.get('assets', [])) == 1 and config['assets'][0].get('path') == 'character.png',
                        'Manifest visual deve usar imagem local fixa.')
                stage(base, base/'review.json', base/'checked')
                for name in ('character.png', 'character.properties', 'review.json'):
                    require((base/name).read_bytes() == (base/'checked'/name).read_bytes(),
                            'Pacote visual diverge da revisao por SHA.')
            passed('pacote_visual_aprovado_sha', files=3)

        # --- Manifest cobre o inventario; o inventario evita a circularidade. ---
        manifest_bytes = read("manifest.json")
        side_manifest = Path(str(zip_path) + ".manifest.json")
        require(side_manifest.is_file(), "Sidecar do manifest ausente.")
        require(side_manifest.read_bytes() == manifest_bytes,
                "Sidecar do manifest diverge do arquivo distribuido.")
        manifest = json_object(manifest_bytes)
        require(manifest.get("schemaVersion") == 1, "Schema desconhecido no manifest.")
        require(manifest.get("entrypoint") == ENTRYPOINT, "Entrypoint inesperado no manifest.")
        require(manifest.get("mainJar") == MAIN_JAR, "JAR principal inesperado no manifest.")
        require(manifest.get("platform") == "windows-x64", "Plataforma inesperada no manifest.")
        version = manifest.get("version")
        require(isinstance(version, str) and bool(version), "Versao invalida no manifest.")
        require(read("VERSION.txt").decode("utf-8-sig").strip() == version,
                "VERSION.txt diverge de manifest.version.")
        report["version"] = version
        listed = {}
        entries = manifest.get("files")
        require(isinstance(entries, list), "Inventario de arquivos ausente no manifest.")
        for item in entries:
            require(isinstance(item, dict), "Item invalido no inventario do manifest.")
            name = relative_name(item.get("path"))
            require(name not in listed and name != "manifest.json",
                    "Manifest lista a si mesmo ou contem duplicata.")
            require(name in relative, f"Manifest referencia arquivo inexistente: {name}")
            require(type(item.get("sizeBytes")) is int and item["sizeBytes"] >= 0,
                    f"Tamanho invalido no manifest: {name}")
            require(isinstance(item.get("sha256"), str) and HEX_SHA256.fullmatch(item["sha256"]),
                    f"SHA-256 invalido no manifest: {name}")
            require(relative[name].file_size == item["sizeBytes"],
                    f"Tamanho divergente do manifest: {name}")
            with archive.open(relative[name]) as stream:
                actual_hash = sha256_stream(stream)
            require(actual_hash == item["sha256"].lower(),
                    f"SHA-256 divergente do manifest: {name}")
            listed[name] = actual_hash
        require(set(listed) == set(relative) - {"manifest.json"},
                "Manifest nao cobre exatamente todos os arquivos exceto si mesmo.")
        inventory = {}
        for line in read("INVENTARIO-SHA256.txt").decode("utf-8-sig").splitlines():
            matched = re.fullmatch(r"([a-fA-F0-9]{64})  (.+)", line)
            require(matched is not None, "Linha invalida no inventario SHA-256.")
            name = relative_name(matched.group(2))
            require(name not in inventory, "Arquivo duplicado no inventario SHA-256.")
            inventory[name] = matched.group(1).lower()
        expected_inventory = {name: value for name, value in listed.items()
                              if name != "INVENTARIO-SHA256.txt"}
        require(inventory == expected_inventory,
                "Inventario SHA-256 divergente ou com circularidade.")
        passed("manifest_e_inventario", manifestFiles=len(listed),
               inventoryFiles=len(inventory), version=version,
               manifestSha256=hashlib.sha256(manifest_bytes).hexdigest())

        # --- JVM integrada, modulos e launcher: somente dados, sem execucao. ---
        release = dict(re.findall(r'^([A-Z_]+)="([^"\r\n]*)"',
                                 read("runtime/release").decode("utf-8"), re.MULTILINE))
        require(re.fullmatch(r"17(?:\.[0-9A-Za-z.+_-]+)?", release.get("JAVA_VERSION", "")),
                "Runtime integrado nao e Java 17.")
        require(manifest.get("javaVersion") == release["JAVA_VERSION"],
                "Versao Java do runtime diverge do manifest.")
        modules = release.get("MODULES", "").split()
        declared_modules = manifest.get("javaModules")
        require(isinstance(declared_modules, list) and
                all(isinstance(module, str) for module in declared_modules),
                "Lista de modulos invalida no manifest.")
        # O manifest registra as raizes passadas ao jlink; runtime/release inclui
        # tambem dependencias transitivas resolvidas, como java.datatransfer.
        require(len(set(declared_modules)) == len(declared_modules) and
                set(declared_modules) <= set(modules),
                "Modulo declarado no manifest ausente do runtime.")
        require({"java.base", "java.desktop", "jdk.random"} <= set(declared_modules),
                "Modulo obrigatorio nao declarado no manifest.")
        require({"java.base", "java.desktop", "jdk.random"} <= set(modules),
                "Modulo obrigatorio ausente no runtime integrado.")
        cfg = read("app/SpiritGate.cfg").decode("utf-8-sig")
        cfg_values = dict(re.findall(r"^([^=\r\n]+)=([^\r\n]*)", cfg, re.MULTILINE))
        require(cfg_values.get("app.mainclass") == ENTRYPOINT,
                "Launcher nao aponta para DebugLauncher.")
        require(cfg_values.get("app.classpath") in
                ("$APPDIR\\spirit-gate-arena.jar", "$APPDIR/spirit-gate-arena.jar"),
                "Classpath inesperado no launcher.")
        exe = read("SpiritGate.exe")
        require(len(exe) >= 64 and exe[:2] == b"MZ", "Launcher nao possui cabecalho PE.")
        pe_offset = struct.unpack_from("<I", exe, 0x3C)[0]
        require(pe_offset + 6 <= len(exe) and exe[pe_offset:pe_offset + 4] == b"PE\x00\x00" and
                struct.unpack_from("<H", exe, pe_offset + 4)[0] == 0x8664,
                "Launcher nao e Windows PE x64.")
        passed("runtime_e_launcher", javaVersion=release["JAVA_VERSION"],
               javaModules=sorted(modules), declaredRootModules=sorted(declared_modules),
               mainClass=ENTRYPOINT,
               launcherArchitecture="PE x64 (0x8664)")

        # --- Whitelist e igualdade de TODAS as classes com o checkout compilado. ---
        classes_directory = Path(__file__).resolve().parent.parent / "server" / "target" / "classes"
        expected_classes = {}
        for prefix in CLASS_PREFIXES:
            package = classes_directory.joinpath(*PurePosixPath(prefix).parts)
            require(package.is_dir(), "Classes locais ausentes; execute mvn clean test antes.")
            # A arena inclui domain e somente a raiz prototype. O novo subpacote
            # prototype/rpg pertence ao pacote offline separado.
            paths = package.glob("*.class") if prefix == "com/projetok/prototype/" else package.rglob("*.class")
            for path in paths:
                require(not path.is_symlink(), "Link simbolico nas classes locais.")
                expected_classes[path.relative_to(classes_directory).as_posix()] = path
        require(bool(expected_classes), "Nenhuma classe local encontrada para comparacao.")
        jar_classes = {}
        with zipfile.ZipFile(io.BytesIO(read(MAIN_JAR))) as jar:
            jar_files = archive_files(jar)
            for item in jar.infolist():
                name = relative_name(item.orig_filename)
                require(name == "META-INF" or name.startswith("META-INF/") or
                        name in ("com/projetok/domain", "com/projetok/prototype") or
                        name.startswith(CLASS_PREFIXES), f"Entrada fora da whitelist do JAR: {name}")
            require("META-INF/MANIFEST.MF" in jar_files, "Manifest principal ausente no JAR.")
            attributes = main_manifest(jar.read("META-INF/MANIFEST.MF"))
            require(attributes.get("main-class") == ENTRYPOINT, "Main-Class inesperada no JAR.")
            for name in jar_files:
                if name.startswith("META-INF/"):
                    require(not name.casefold().endswith((".class", ".jar")),
                            "Codigo oculto sob META-INF no JAR.")
                    continue
                require(name.endswith(".class"), f"Recurso nao autorizado no JAR: {name}")
                data = jar.read(name)
                require(data.startswith(b"\xca\xfe\xba\xbe"), f"Bytecode invalido: {name}")
                require(len(data) >= 8 and struct.unpack_from(">H", data, 6)[0] <= 61,
                        f"Classe exige Java posterior ao 17: {name}")
                forbidden = (b"com/esotericsoftware/kryonet", b"com.esotericsoftware.kryonet",
                             b"org/postgresql", b"org.postgresql", b"DatabaseManager")
                require(not any(marker in data for marker in forbidden),
                        f"Referencia legada de banco ou rede no bytecode: {name}")
                for label, pattern in SECRET_PATTERNS.items():
                    require(not pattern.search(data),
                            f"Padrao concreto de credencial detectado ({label}) em {name}; "
                            "conteudo omitido.")
                jar_classes[name] = data
        require(set(jar_classes) == set(expected_classes),
                "Inventario de classes do JAR diverge das classes atuais domain/prototype.")
        for name, path in sorted(expected_classes.items()):
            require(jar_classes[name] == path.read_bytes(),
                    f"Bytecode do JAR nao corresponde ao checkout compilado atual: {name}")
        passed("jar_whitelist_e_bytecode_atual", classesCompared=len(jar_classes),
               comparison="Byte a byte, domain recursivo e prototype raiz da arena, sem omissoes.",
               allowedPrefixes=list(CLASS_PREFIXES), mainClass=ENTRYPOINT)
        passed("credenciais_padronizadas_no_bytecode_do_jogo", classesScanned=len(jar_classes),
               patterns=list(SECRET_PATTERNS), findings=0,
               scope="Somente classes domain/prototype; conteudo encontrado nunca e impresso.")


def main() -> int:
    """Grava evidencia JSON tanto em sucesso quanto em recusa da auditoria."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--zip", required=True, type=Path, help="ZIP portatil e seus sidecars.")
    parser.add_argument("--report", required=True, type=Path, help="Relatorio JSON de saida.")
    args = parser.parse_args()
    zip_path = args.zip.resolve()
    report_path = args.report.resolve()
    protected = {zip_path, Path(str(zip_path) + ".sha256"), Path(str(zip_path) + ".manifest.json")}
    if report_path in protected:
        parser.error("O relatorio nao pode sobrescrever o ZIP nem seus sidecars.")
    report = {
        "schemaVersion": 1,
        "task": "TASK-012_016",
        "auditedAtUtc": datetime.now(timezone.utc).isoformat(),
        "zip": str(zip_path),
        "status": "FAIL",
        "checks": [],
        "limitations": LIMITATIONS,
        "executedPackagedCode": False,
    }
    try:
        audit(zip_path, report)
        report["status"] = "PASS"
    except AuditFailure as failure:
        report["error"] = str(failure)
    except (OSError, ValueError, KeyError, TypeError, struct.error,
            zipfile.BadZipFile, RuntimeError) as failure:
        # Nao reproduz dados JSON, conteudo binario nem mensagens de bibliotecas
        # que possam conter trechos de um arquivo privado adulterado.
        report["error"] = "Falha de leitura ou formato: " + type(failure).__name__
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Auditoria {report['status']}: {report_path}")
    if report["status"] != "PASS":
        print(report["error"], file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
