"""Compila fontes locais e executa classes JUnit em checkpoints separados, sem rede."""
from pathlib import Path
import importlib.util
import json
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def run(task, names, gui=False, cycle="corujao-vii"):
    spec = importlib.util.spec_from_file_location("local_tests", ROOT / "tools/run-local-tests.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    evidence = ROOT / ".task-checkpoints" / task / cycle
    evidence.mkdir(parents=True, exist_ok=True)
    build = evidence / "verification"
    build.mkdir(exist_ok=True)
    classes = build / "classes"
    classes.mkdir(exist_ok=True)
    # Reutiliza dependências já extraídas pelo runner local; nenhuma instalação.
    candidates = list((ROOT / "server/target/local-tests").glob("run-*/dependencies"))
    dependencies = max(candidates, key=lambda p: p.stat().st_mtime)
    cp = os.pathsep.join(str(p) for p in dependencies.iterdir())
    runner = build / "Corujao7Runner.java"
    runner.write_text(module.RUNNER.replace("public final class LocalJunitRunner", "public final class Corujao7Runner")
                      .replace('DiscoverySelectors.selectPackage(args.length == 0 ? "com.projetok" : args[0])',
                               'DiscoverySelectors.selectClass(args[0])'), encoding="utf-8")
    sources = sorted((ROOT / "server/src/main/java").rglob("*.java"))
    tests = sorted((ROOT / "server/src/test/java").rglob("*.java"))
    args = build / "sources.args"
    args.write_text("\n".join('"' + p.as_posix() + '"' for p in sources + tests + [runner]), encoding="utf-8")
    result = subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-cp", cp,
                             "-d", str(classes), "@" + str(args)], capture_output=True, text=True)
    (evidence / "compile.log").write_text(result.stdout + result.stderr, encoding="utf-8")
    if result.returncode:
        print(result.stderr)
        return result.returncode
    shutil.copytree(ROOT / "server/src/main/resources", classes, dirs_exist_ok=True)
    summaries = []
    failed = False
    for name in names:
        result = subprocess.run(["java", "-Djava.awt.headless=" + str(not gui).lower(),
                                 "-Dspiritgate.test.evidenceRoot=" + str(evidence / "ui-regression"), "-cp",
                                 str(classes) + os.pathsep + cp, "Corujao7Runner", name],
                                cwd=ROOT / "server", capture_output=True, text=True, encoding="utf-8", errors="replace")
        (evidence / (name.rsplit(".", 1)[-1] + ("-gui" if gui else "") + ".log")).write_text(result.stdout + result.stderr, encoding="utf-8")
        summary = next((line for line in result.stdout.splitlines() if line.startswith("JUnit:")), result.stderr[-1000:])
        print(name, summary, flush=True)
        summaries.append({"class": name, "result": summary, "exit": result.returncode})
        if result.returncode:
            print((result.stdout + result.stderr)[-6000:])
            failed = True
    (evidence / ("verification-gui.json" if gui else "verification.json")).write_text(
        json.dumps({"sources": len(sources), "testFiles": len(tests), "gui": gui, "tests": summaries}, indent=2), encoding="utf-8")
    return int(failed)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("task")
    parser.add_argument("names", nargs="+")
    parser.add_argument("--gui", action="store_true")
    parser.add_argument("--cycle", choices=("corujao-vii", "corujao-viii"), default="corujao-vii")
    args = parser.parse_args()
    raise SystemExit(run(args.task, args.names, args.gui, args.cycle))
