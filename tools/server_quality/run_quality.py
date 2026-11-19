"""Executa verificacoes do servidor em copia publica temporaria, sem saves ou saidas historicas."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
DEFAULT_TESTS = (
    "ServerHandshakeDeadlineTest,ProtocolPropertiesTest,NettyFrameCompatibilityTest,ProtocolFuzzTest,"
    "ProtocolTest,ProtocolBoundaryTest,SkillProtocolTest,LocalServerTest,NetworkRobustnessTest,"
    "FinalNetworkIsolationTest,Task225ProjectileTcpTest,RpgReconnectIsolationTest,"
    "Task208ProgressionTest,Task210QuestJourneyTest"
)


def copy_public(source: Path, destination: Path) -> None:
    """Copia somente arvores conhecidas e recusa links que escapariam do recorte publico."""
    if not source.exists():
        return
    for path in [source, *source.rglob("*")]:
        if path.is_symlink() or path.is_junction():
            raise RuntimeError(f"Link nao permitido na copia de teste: {path}")
    shutil.copytree(source, destination)


def command(argv: list[str], cwd: Path, log: Path, timeout: int = 300, fuzzing: bool = False) -> int:
    """Registra a saida e limita o processo sem iniciar janelas interativas."""
    print("Executando: " + argv[0] + " -> " + log.name, flush=True)
    environment = os.environ.copy()
    environment["JAZZER_FUZZ"] = "1" if fuzzing else "0"
    flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    with log.open("x", encoding="utf-8") as output:
        result = subprocess.run(argv, cwd=cwd, env=environment, stdout=output,
                                stderr=subprocess.STDOUT, timeout=timeout, creationflags=flags)
    print(f"Exit {result.returncode}; log: {log}", flush=True)
    return result.returncode


def totals(folder: Path) -> dict:
    """Soma somente XMLs deste modo/rodada; nao mistura repeticoes ou corpus com casos JUnit."""
    values = dict(tests=0, failures=0, errors=0, skipped=0)
    for report in folder.glob("TEST-*.xml"):
        root = ET.parse(report).getroot()
        for key in values:
            values[key] += int(root.get(key, 0))
    return values


def main() -> None:
    """Prepara snapshot novo e executa somente os modos explicitamente solicitados."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=["compile", "test", "fuzz", "spotbugs", "pmd", "codeql", "all"], default="test")
    parser.add_argument("--tests", default=DEFAULT_TESTS)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--codeql-licensed", action="store_true",
                        help="Opcao legada sem efeito: CodeQL desativado para este projeto fechado")
    args = parser.parse_args()
    output = args.output.resolve() if args.output else Path(tempfile.mkdtemp(prefix="spiritgate-quality-"))
    if args.output:
        output.mkdir(parents=True, exist_ok=False)
    if args.mode == "codeql":
        summary = dict(mode="codeql", output=str(output), steps={"codeql": dict(
            state="DESATIVADO", analysisExecuted=False, executableInvoked=False,
            reason="Projeto fechado: usar SpotBugs/Find Security Bugs, PMD e Jazzer")})
        (output / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
        print(json.dumps(summary, indent=2), flush=True)
        return
    game = output / "project"
    game.mkdir()
    for relative in ["server/src/main", "server/src/test", "assets"]:
        copy_public(ROOT / relative, game / relative)
    extra_tests = HERE / "src/test/java"
    if extra_tests.exists():
        for source in extra_tests.rglob("*.java"):
            target = game / "server/src/test/java" / source.relative_to(extra_tests)
            if target.exists():
                raise RuntimeError(f"Colisao de fonte de teste: {target.name}")
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
    for relative in ["docs", "server", "test-home", "test-logs"]:
        (game / relative).mkdir(parents=True, exist_ok=True)
    build = output / "build"
    maven = shutil.which("mvn.cmd" if os.name == "nt" else "mvn")
    if not maven:
        raise RuntimeError("Maven nao localizado")
    base = [maven, "-B", "-q", "-f", str(HERE / "pom.xml"),
            "-Dgame.root=" + str(game), "-Dquality.output=" + str(build)]
    if args.offline:
        base += ["-o"]
    summary = dict(mode=args.mode, output=str(output), gameRoot=str(game), steps={})
    modes = ["test", "spotbugs", "pmd", "fuzz"] if args.mode == "all" else [args.mode]
    if args.mode in ("spotbugs", "pmd"):
        modes.insert(0, "compile")
    for mode in modes:
        if mode == "compile":
            argv = base + ["-DskipTests", "compile"]
        elif mode == "test":
            argv = base + ["-Dtest=" + args.tests, "-Dquality.reports=" + str(output / "test-reports"), "test"]
        elif mode == "fuzz":
            argv = base + ["-Dtest=ProtocolFuzzTest", "-Dquality.fuzz=true",
                           "-Dquality.reports=" + str(output / "fuzz-reports"), "test"]
        else:
            tool_root = Path(os.environ.get("LOCALAPPDATA", Path.home() / "AppData/Local")) / "SpiritGate/quality-tools"
            if mode == "spotbugs":
                jars = list((tool_root / "spotbugs-4.10.5").rglob("spotbugs.jar"))
                plugins = list((tool_root / "findsecbugs-1.14.0").rglob("findsecbugs-plugin-1.14.0.jar"))
                if len(jars) != 1 or len(plugins) != 1:
                    raise RuntimeError("Instale SpotBugs e Find Security Bugs pelo instalador")
                argv = [shutil.which("java"), "-Xmx1g", "-jar", str(jars[0]), "-textui",
                        "-pluginList", str(plugins[0]), "-effort:max", "-low", "-exitcode",
                        "-xml:withMessages", "-output", str(output / "spotbugs.xml"), str(build / "classes")]
            else:
                pmd_jars = list((tool_root / "pmd-7.29.0").rglob("pmd-cli-7.29.0.jar"))
                if len(pmd_jars) != 1:
                    raise RuntimeError("Instale PMD pelo instalador")
                java_executable = Path(shutil.which("java")).resolve()
                jdk_modules = java_executable.parents[1] / "lib/jrt-fs.jar"
                if not jdk_modules.is_file():
                    raise RuntimeError("PMD exige o jrt-fs.jar do mesmo JDK usado nesta execucao")
                argv = [shutil.which("java"), "-Xmx1g", "-cp", str(pmd_jars[0].parent / "*"),
                        "net.sourceforge.pmd.cli.PmdCli", "check", "--dir",
                        str(game / "server/src/main/java"), "--rulesets", str(HERE / "pmd-ruleset.xml"),
                        "--use-version", "java-17", "--aux-classpath",
                        os.pathsep.join([str(build / "classes"), str(jdk_modules)]),
                        "--format", "xml", "--report-file", str(output / "pmd.xml"),
                        "--no-cache", "--no-progress", "--threads", "2"]
        code = command(argv, game / "server", output / (mode + ".log"), 360, mode == "fuzz")
        item = dict(exitCode=code)
        if mode in ("test", "fuzz"):
            item["junit"] = totals(output / (mode + "-reports"))
        if mode == "spotbugs" and (output / "spotbugs.xml").exists():
            report = ET.parse(output / "spotbugs.xml").getroot()
            item["findings"] = len(report.findall("BugInstance"))
            item["analysisErrors"] = len(report.findall("Errors/Error"))
            item["missingClasses"] = len(report.findall("Errors/MissingClass"))
        if mode == "pmd" and (output / "pmd.xml").exists():
            report = ET.parse(output / "pmd.xml").getroot()
            item["findings"] = len(report.findall(".//{*}violation"))
            item["analysisErrors"] = len(report.findall(".//{*}error"))
            item["configurationErrors"] = len(report.findall(".//{*}configerror"))
        summary["steps"][mode] = item
        (output / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
        # Bits conferidos em edu.umd.cs.findbugs.ExitCodes: 1=achados, 2=classe ausente, 4=erro.
        if code and not (mode == "spotbugs" and code == 1 or mode == "pmd" and code == 4):
            raise SystemExit(code)
    print(json.dumps(summary, indent=2), flush=True)


if __name__ == "__main__":
    main()

