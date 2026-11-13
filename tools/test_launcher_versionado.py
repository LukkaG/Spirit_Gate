"""Regressao do bootstrap fixo: troca de interface sem reinstalar os executaveis."""
import hashlib
import json
import shutil
import subprocess
import tempfile
from pathlib import Path
import unittest
import zipfile

from preparar_atualizacao import prepare


class VersionedLauncherTest(unittest.TestCase):
    def test_boot_resolves_only_verified_versioned_ui_and_keeps_fixed_files(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            source = folder / "game.zip"
            with zipfile.ZipFile(source, "w") as archive:
                archive.writestr("SpiritGateRPG/runtime/bin/java.exe", b"fixture")
                archive.writestr("SpiritGateRPG/app/spirit-gate-rpg.jar", b"fixture")
            prepare(source, folder / "one", "1")
            prepare(source, folder / "two", "2")
            install = folder / "one/SpiritGate-Atualizavel"
            second = folder / "two/SpiritGate-Atualizavel"
            fixed = {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                     for p in install.iterdir() if p.is_file() and p.name != "active.txt"}

            def resolve():
                result = subprocess.run([
                    "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                    str(install / "Iniciar-SpiritGate.ps1"), "-InstallDir", str(install), "-ResolveOnly",
                ], capture_output=True, text=True, check=True)
                return Path(result.stdout.strip())

            first_id = (install / "active.txt").read_text().strip()
            self.assertEqual(install / "versions" / first_id / "launcher/Launcher-SpiritGate.ps1", resolve())
            second_id = (second / "active.txt").read_text().strip()
            shutil.copytree(second / "versions" / second_id, install / "versions" / second_id)
            (install / "active.txt").write_text(second_id)
            selected = install / "versions" / second_id / "launcher/Launcher-SpiritGate.ps1"
            self.assertEqual(selected, resolve())
            for name, expected in fixed.items():
                self.assertEqual(expected, hashlib.sha256((install / name).read_bytes()).hexdigest())
            selected.write_text("throw 'altered fixture'")
            self.assertEqual(install / "Launcher-SpiritGate.ps1", resolve())

    def test_ui_and_engine_are_listed_in_the_shared_game_manifest(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "game.zip"
            with zipfile.ZipFile(source, "w") as archive:
                archive.writestr("SpiritGateRPG/runtime/bin/java.exe", b"fixture")
                archive.writestr("SpiritGateRPG/app/spirit-gate-rpg.jar", b"fixture")
            prepare(source, root / "host", "1", role="anfitriao")
            prepare(source, root / "guest", "1", role="convidado")
            raw = (root / "host/canal/latest.json").read_bytes()
            self.assertEqual(raw, (root / "guest/canal/latest.json").read_bytes())
            names = {f["path"] for f in json.loads(raw)["files"]}
            self.assertTrue({"launcher/Launcher-SpiritGate.ps1", "launcher/Atualizar-SpiritGate.ps1"} <= names)


if __name__ == "__main__":
    unittest.main()
