"""Verifica a politica do projeto fechado sem iniciar analisadores ou copiar o jogo."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import run_quality


class ClosedProjectPolicyTest(unittest.TestCase):
    """Uma opcao legada nao pode reativar CodeQL por acidente."""

    def test_codeql_mode_does_not_copy_sources_or_invoke_any_executable(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "run"
            with patch("sys.argv", ["run_quality.py", "--mode", "codeql", "--codeql-licensed",
                                    "--output", str(output)]):
                with patch.object(run_quality, "command") as execute, patch.object(run_quality, "copy_public") as copy:
                    run_quality.main()
                    execute.assert_not_called()
                    copy.assert_not_called()
            result = json.loads((output / "summary.json").read_text(encoding="utf-8"))
            self.assertEqual("DESATIVADO", result["steps"]["codeql"]["state"])
            self.assertFalse(result["steps"]["codeql"]["analysisExecuted"])
            self.assertFalse(result["steps"]["codeql"]["executableInvoked"])


if __name__ == "__main__":
    unittest.main()

