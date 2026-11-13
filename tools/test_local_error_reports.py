"""Valida a captura real dos scripts em projetos descartáveis, sem abrir o jogo."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import unittest


TOOLS = Path(__file__).resolve().parent


@unittest.skipUnless(os.name == "nt" and shutil.which("powershell.exe"), "Exige PowerShell do Windows")
class LocalErrorReportsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="spirit gate erros ")
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / "tools").mkdir()
        for name in ("compile-local.ps1", "launch-local.ps1", "registrar-erro-local.ps1"):
            shutil.copyfile(TOOLS / name, self.root / "tools" / name)

    def run_script(self, name, *arguments):
        return subprocess.run(
            ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
             str(self.root / "tools" / name), *arguments],
            cwd=self.root, capture_output=True, timeout=60,
        )

    def test_report_is_complete_utf8_and_no_temporary_file_is_published(self):
        result = self.run_script("registrar-erro-local.ps1", "-Contexto", "teste local", "-Detalhes", "Falha: ação inválida")
        self.assertEqual(0, result.returncode, result.stderr)
        reports = list((self.root / "logs" / "erros").glob("*.txt"))
        self.assertEqual(1, len(reports))
        self.assertIn("Falha: ação inválida", reports[0].read_text(encoding="utf-8"))
        self.assertFalse(list(reports[0].parent.glob("*.tmp")))

    def test_unavailable_directory_preserves_original_failure_path(self):
        (self.root / "logs").write_text("bloqueio descartável", encoding="utf-8")
        result = self.run_script("registrar-erro-local.ps1", "-Contexto", "teste", "-Detalhes", "erro original")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn(b"Nao foi possivel gravar", result.stdout)
        self.assertEqual("bloqueio descartável", (self.root / "logs").read_text(encoding="utf-8"))

    def test_launcher_failure_is_saved_without_starting_java(self):
        # Um compilador descartável recusa a execução antes de qualquer cliente.
        (self.root / "tools" / "compile-local.ps1").write_text("throw 'falha controlada do build'\n", encoding="utf-8")
        result = self.run_script("launch-local.ps1", "-BuildOnly")
        self.assertEqual(1, result.returncode)
        reports = list((self.root / "logs" / "erros").glob("*.txt"))
        self.assertEqual(1, len(reports))
        self.assertIn("falha controlada do build", reports[0].read_text(encoding="utf-8"))

    @unittest.skipUnless(shutil.which("javac"), "Exige JDK no PATH")
    def test_javac_failure_keeps_diagnostic_and_preserves_previous_classes(self):
        source = self.root / "server" / "src" / "main" / "java"
        source.mkdir(parents=True)
        (source.parent / "resources").mkdir()
        (source / "Broken.java").write_text("public class Broken { invalid Java }", encoding="utf-8")
        previous = self.root / "server" / "target" / "launcher" / "classes" / "anterior.txt"
        previous.parent.mkdir(parents=True)
        previous.write_text("classe anterior preservada", encoding="utf-8")
        result = self.run_script("launch-local.ps1", "-BuildOnly")
        self.assertEqual(1, result.returncode)
        reports = list((self.root / "logs" / "erros").glob("*.txt"))
        self.assertEqual(1, len(reports), result.stdout + result.stderr)
        text = reports[0].read_text(encoding="utf-8")
        self.assertIn("compilacao javac", text)
        self.assertIn("Broken.java", text)
        self.assertEqual("classe anterior preservada", previous.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
