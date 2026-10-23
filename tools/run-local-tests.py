"""Compile and run the Java suite with cached JUnit, without Maven or network access."""
from __future__ import annotations

import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path.home() / ".m2" / "repository"
ARTIFACTS = (
    "org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar",
    "org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar",
    "org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar",
    "org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar",
    "org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar",
    "org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar",
    "org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar",
)

RUNNER = r'''
import java.util.*;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;

public final class LocalJunitRunner {
    public static void main(String[] args) {
        var configuration = new ConfigurationParameters() {
            public Optional<String> get(String key) { return Optional.empty(); }
            public Optional<Boolean> getBoolean(String key) { return Optional.empty(); }
            public int size() { return 0; }
            public Set<String> keySet() { return Set.of(); }
        };
        var selector = DiscoverySelectors.selectPackage(args.length == 0 ? "com.projetok" : args[0]);
        var request = new EngineDiscoveryRequest() {
            public <T extends DiscoverySelector> List<T> getSelectorsByType(Class<T> type) {
                return type.isInstance(selector) ? List.of(type.cast(selector)) : List.of();
            }
            public <T extends DiscoveryFilter<?>> List<T> getFiltersByType(Class<T> type) { return List.of(); }
            public ConfigurationParameters getConfigurationParameters() { return configuration; }
        };
        var engine = new JupiterTestEngine();
        var descriptor = engine.discover(request, UniqueId.forEngine(engine.getId()));
        int[] results = new int[5];
        var listener = new EngineExecutionListener() {
            public void executionFinished(TestDescriptor test, TestExecutionResult result) {
                if (test.isTest()) results[0]++;
                switch (result.getStatus()) {
                    case SUCCESSFUL -> { if(test.isTest()) results[1]++; }
                    case ABORTED -> { if(test.isTest()) { results[2]++; System.out.println("ABORTED: " + test.getUniqueId() + " " + result.getThrowable().map(Throwable::toString).orElse("")); } }
                    case FAILED -> {
                        results[3]++;
                        System.err.println("FAILED: " + test.getUniqueId());
                        result.getThrowable().ifPresent(error -> {
                            System.err.println(error);
                            var trace=error.getStackTrace();
                            for(int i=0;i<Math.min(8,trace.length);i++) System.err.println("  at " + trace[i]);
                            if(error.getCause()!=null) System.err.println("  caused by: " + error.getCause());
                        });
                    }
                }
            }
            public void executionSkipped(TestDescriptor test, String reason) {
                if(test.isTest()) results[4]++;
                System.out.println("SKIPPED: " + test.getUniqueId() + " " + reason);
            }
        };
        engine.execute(new ExecutionRequest(descriptor, listener, configuration));
        System.out.printf("JUnit: %d executed, %d passed, %d aborted, %d failures, %d skipped%n",
            results[0], results[1], results[2], results[3], results[4]);
        System.exit(results[3] == 0 && results[0] > 0 ? 0 : 1);
    }
}
'''


def main() -> int:
    java = shutil.which("java")
    javac = shutil.which("javac")
    if not java or not javac:
        raise RuntimeError("Java e javac 17+ devem estar no PATH.")
    jars = [CACHE / artifact for artifact in ARTIFACTS]
    missing = [str(jar) for jar in jars if not jar.is_file()]
    if missing:
        raise RuntimeError("JUnit nao esta no cache local:\n" + "\n".join(missing))

    build = ROOT / "server" / "target" / "local-tests"
    build.mkdir(parents=True, exist_ok=True)
    run = Path(tempfile.mkdtemp(prefix="run-", dir=build))
    dependencies = run / "dependencies"
    dependencies.mkdir()
    classpath = []
    # Directory classpaths avoid Windows sandbox ZipFS failures seen when javac opens cached JARs.
    for jar in jars:
        destination = dependencies / jar.stem
        destination.mkdir()
        with zipfile.ZipFile(jar) as archive:
            for member in archive.infolist():
                target = (destination / member.filename).resolve()
                if not target.is_relative_to(destination.resolve()):
                    raise RuntimeError(f"Caminho invalido no JAR: {member.filename}")
            archive.extractall(destination)
        classpath.append(str(destination))

    classes = run / "classes"
    classes.mkdir()
    runner = run / "LocalJunitRunner.java"
    runner.write_text(RUNNER, encoding="utf-8")
    sources = sorted((ROOT / "server" / "src" / "main" / "java").rglob("*.java"))
    tests = sorted((ROOT / "server" / "src" / "test" / "java").rglob("*.java"))
    arguments = run / "sources.args"
    arguments.write_text("\n".join('"' + path.as_posix() + '"' for path in sources + tests + [runner]), encoding="utf-8")
    print(f"Compilando {len(sources)} fontes e {len(tests)} arquivos de teste para Java 17...", flush=True)
    compiled = subprocess.run([javac, "--release", "17", "-encoding", "UTF-8", "-cp", os.pathsep.join(classpath), "-d", str(classes), "@" + str(arguments)], cwd=ROOT)
    if compiled.returncode:
        return compiled.returncode
    shutil.copytree(ROOT / "server" / "src" / "main" / "resources", classes, dirs_exist_ok=True)
    print(f"Resultados e classes: {run}", flush=True)
    with (run / "test-results.log").open("w", encoding="utf-8") as log:
        process = subprocess.Popen([java, "-Djava.awt.headless=true", "-cp", os.pathsep.join([str(classes)] + classpath), "LocalJunitRunner", *sys.argv[1:]], cwd=ROOT / "server", stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
        assert process.stdout is not None
        for line in process.stdout:
            print(line, end="", flush=True)
            log.write(line)
            log.flush()
        return process.wait()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, RuntimeError) as error:
        print(error, file=sys.stderr)
        raise SystemExit(1)
