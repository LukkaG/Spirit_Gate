"""Regressoes de seguranca do instalador: nenhum download ou arquivo real do usuario e utilizado."""
import io
import json
from pathlib import Path
import stat
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import install_tools
from install_tools import checked_path, extract, install


class InstallerSafetyTest(unittest.TestCase):
    """Protege a fronteira de extracao e o comportamento de nao sobrescrever."""

    def test_external_and_ambiguous_paths_are_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            for name in ["../outside", "/outside", "C:/outside", "a\\b", "a/../b", "a. /b",
                         "a/NUL.txt", "a/COM1", "a/CONOUT$"]:
                with self.subTest(name=name), self.assertRaises(ValueError):
                    checked_path(Path(folder), name)

    def test_zip_path_escape_does_not_write_outside_target(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            archive = root / "bad.zip"
            with zipfile.ZipFile(archive, "w") as out:
                out.writestr("../outside", b"synthetic")
            with self.assertRaises(ValueError):
                extract(archive, root / "tool", "zip")
            self.assertFalse((root / "outside").exists())

    def test_zip_symlink_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            archive = root / "link.zip"
            entry = zipfile.ZipInfo("link")
            entry.create_system = 3
            entry.external_attr = (stat.S_IFLNK | 0o777) << 16
            with zipfile.ZipFile(archive, "w") as out:
                out.writestr(entry, "target")
            with self.assertRaises(ValueError):
                extract(archive, root / "tool", "zip")

    def test_case_collision_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            archive = root / "case.zip"
            with zipfile.ZipFile(archive, "w") as out:
                out.writestr("a", b"one")
                out.writestr("A", b"two")
            with self.assertRaises(ValueError):
                extract(archive, root / "tool", "zip")

    def test_existing_installation_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            target = root / "example-1"
            target.mkdir()
            sentinel = target / "keep.txt"
            sentinel.write_text("preservado")
            with self.assertRaises(RuntimeError):
                install(root, dict(name="example", version="1"))
            self.assertEqual("preservado", sentinel.read_text())

    def test_disabled_tool_is_not_installed_even_if_explicitly_selected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            spec = root / "spec.json"
            spec.write_text(json.dumps(dict(tools=[dict(name="codeql", enabled=False)])), encoding="utf-8")
            with patch.object(install_tools, "SPEC", spec), patch.object(install_tools, "install") as download:
                with patch("sys.argv", ["install_tools.py", "--root", str(root / "install"), "--tool", "codeql"]):
                    install_tools.main()
                download.assert_not_called()


if __name__ == "__main__":
    unittest.main()

