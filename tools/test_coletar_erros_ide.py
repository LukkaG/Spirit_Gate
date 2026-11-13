"""Verifica a coleta da IDE com sessões artificiais, sem acessar logs reais."""
from __future__ import annotations

import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from coletar_erros_ide import collect


class IdeCollectorTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / "projeto com espaços"
        self.root.mkdir()
        self.appdata = self.base / "AppData"
        self.environment = patch.dict(os.environ, {"APPDATA": str(self.appdata)})
        self.environment.start()
        self.addCleanup(self.environment.stop)
        workspace = self.appdata / "Code/User/workspaceStorage/atual"
        workspace.mkdir(parents=True)
        (workspace / "workspace.json").write_text(json.dumps({"folder": self.root.as_uri()}), encoding="utf-8")
        self.userdir = workspace / "Oracle.oracle-java/userdir"
        self.logs = self.appdata / "Code/logs"
        self.source = self.make_log("20261101T222200", self.userdir)

    def make_log(self, session, userdir):
        path = self.logs / session / "window1/exthost/output_logging_01/2-Oracle Java SE Language Server.log"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"[2026-11-01T22:00:00Z] [INFO]: LSP server user directory: {userdir}\n", encoding="utf-8")
        return path

    def append(self, text, path=None):
        with (path or self.source).open("a", encoding="utf-8") as stream:
            stream.write(text)

    def test_importa_erro_com_contexto_e_stack_sem_repetir(self):
        self.append("Cannot run on older versions of Java than Java 21.\n"
                    "[2026-11-01T22:00:01Z] [ERROR]: Falha de inicialização\n"
                    "java.lang.IllegalStateException: causa\n\tat exemplo.Classe.iniciar(Classe.java:9)\n")
        reports = collect(self.root)
        self.assertEqual(1, len(reports))
        self.assertIn("Java 21", reports[0].read_text(encoding="utf-8"))
        self.assertIn("Classe.java:9", reports[0].read_text(encoding="utf-8"))
        self.assertEqual([], collect(self.root))

    def test_append_depois_reinicio_e_linha_incompleta(self):
        self.assertEqual([], collect(self.root))
        self.append("[2026-11-01T22:00:01Z] [ERROR]: falha parcial")
        self.assertEqual([], collect(self.root))
        self.append(" completa\n")
        self.assertEqual(1, len(collect(self.root)))
        self.append("[2026-11-01T22:00:02Z] [ERROR]: nova falha\n")
        self.assertEqual(1, len(collect(self.root)))
        self.assertEqual([], collect(self.root))

    def test_ignora_outro_workspace_e_sessao_antiga(self):
        other = self.make_log("20261102T222200", self.base / "outro/userdir")
        self.append("[ERROR]: alheio\n", other)
        old = self.make_log("20261001T222200", self.userdir)
        self.append("[ERROR]: antigo\n", old)
        self.assertEqual([], collect(self.root))
        self.assertEqual([], list((self.root / "logs/erros").glob("*.txt")))

    def test_truncamento_importa_erro_novo(self):
        self.append("[ERROR]: primeira falha longa\n" + "linha auxiliar\n" * 20)
        self.assertEqual(1, len(collect(self.root)))
        header = f"[INFO]: userdir: {self.userdir}\n"
        self.source.write_text(header + "[ERROR]: segunda falha\n", encoding="utf-8")
        reports = collect(self.root)
        self.assertEqual(1, len(reports))
        self.assertIn("segunda falha", reports[0].read_text(encoding="utf-8"))

    def test_stack_acrescentada_em_outra_varredura(self):
        self.append("[2026-11-01T22:00:01Z] [ERROR]: erro\n")
        self.assertEqual(1, len(collect(self.root)))
        self.append("\tat Classe.metodo(Classe.java:20)\n")
        reports = collect(self.root)
        self.assertEqual(1, len(reports))
        self.assertIn("Classe.java:20", reports[0].read_text(encoding="utf-8"))
        self.append("[2026-11-01T22:00:02Z] [INFO]: voltou\n")
        self.assertEqual([], collect(self.root))

    def test_destino_indisponivel_nao_avanca_cursor(self):
        self.append("[ERROR]: não pode perder\n")
        with patch("coletar_erros_ide._atomic", side_effect=OSError("disco cheio")):
            with self.assertRaises(OSError):
                collect(self.root)
        self.assertEqual(1, len(collect(self.root)))


if __name__ == "__main__":
    unittest.main()
