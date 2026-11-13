"""Migracao do bootstrap em fixtures: nenhum jogo, save pessoal ou rede e usado."""
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import zipfile

from preparar_ativacao_cdn import prepare, FILES


class ActivationKitTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workspace = tempfile.TemporaryDirectory()
        cls.base = Path(cls.workspace.name)
        cls.metadata = prepare(cls.base / "kit")
        cls.kit = cls.base / "kit/SpiritGate-Ativar-CDN"

    @classmethod
    def tearDownClass(cls):
        cls.workspace.cleanup()

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.install = self.root / "Instalacao com espacos"
        self.install.mkdir()
        self.identity = "b" * 64
        self.version = self.install / "versions" / self.identity
        (self.version / "app").mkdir(parents=True)
        (self.version / "app/game.jar").write_bytes(b"existing-game")
        (self.version / "runtime").mkdir()
        (self.version / "runtime/java.exe").write_bytes(b"existing-runtime")
        (self.install / "active.txt").write_text(self.identity)
        self.config = dict(feedUrl="", role="convidado", options=dict(volume=37, language="pt-BR"))
        self.write_config()
        (self.install / "data/saves").mkdir(parents=True)
        (self.install / "data/saves/personal.fixture").write_bytes(b"save-sentinel")
        for name in FILES:
            if name != "Iniciar-SpiritGate.ps1":
                (self.install / name).write_bytes(("previous " + name).encode())
        self.feed = "https://updates.example.invalid/latest.json"

    def write_config(self):
        (self.install / "atualizacoes.json").write_text(json.dumps(self.config), encoding="utf-8")

    def run_kit(self, *, kit=None, feed=None, cwd=None):
        return subprocess.run(["powershell.exe", "-NoProfile", "-STA", "-ExecutionPolicy", "Bypass", "-File",
                               str((kit or self.kit) / "Ativar-SpiritGate.ps1"), "-InstallDir", str(self.install),
                               "-FeedUrl", feed if feed is not None else self.feed, "-NoGui"],
                              cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                              encoding="utf-8", errors="replace", timeout=30)

    def protected(self):
        return {path.relative_to(self.install).as_posix(): path.read_bytes()
                for folder in (self.install / "versions", self.install / "data")
                for path in folder.rglob("*") if path.is_file()}

    def test_small_kit_has_no_game_or_runtime_and_no_placeholder_channel(self):
        self.assertLess(self.metadata["bytes"], 1024 * 1024)
        self.assertFalse(self.metadata["includesGame"])
        self.assertFalse(self.metadata["includesRuntime"])
        self.assertEqual("", json.loads((self.kit / "canal.json").read_text())["feedUrl"])
        with zipfile.ZipFile(self.metadata["archive"]) as archive:
            self.assertIsNone(archive.testzip())
            self.assertFalse(any(part in ("versions", "runtime", "saves", "data")
                                 for name in archive.namelist() for part in Path(name).parts))
            self.assertEqual(b"MZ", archive.read("SpiritGate-Ativar-CDN/Ativar-SpiritGate.exe")[:2])

    def test_host_and_guest_preserve_role_preferences_versions_and_saves(self):
        for role in ("anfitriao", "convidado"):
            self.config["role"] = role
            self.write_config()
            protected = self.protected()
            result = self.run_kit()
            self.assertEqual(0, result.returncode, result.stdout)
            config = json.loads((self.install / "atualizacoes.json").read_text(encoding="utf-8-sig"))
            self.assertEqual(role, config["role"])
            self.assertEqual(self.config["options"], config["options"])
            self.assertEqual(self.feed, config["feedUrl"])
            self.assertEqual(protected, self.protected())
            for name in FILES:
                self.assertEqual((self.kit / "payload" / name).read_bytes(), (self.install / name).read_bytes())
            backups = list((self.install / "launcher-backups").iterdir())
            self.assertGreaterEqual(len(backups), 1)

    def test_invalid_url_never_changes_bootstrap_or_config(self):
        before = (self.install / "atualizacoes.json").read_bytes()
        for url in ("http://example.invalid/latest.json", "https://user:password@example.invalid/latest.json",
                    "https://example.invalid/latest.json?token=private", "https://example.invalid/latest.json#secret"):
            result = self.run_kit(feed=url)
            self.assertNotEqual(0, result.returncode)
            self.assertEqual(before, (self.install / "atualizacoes.json").read_bytes())
            self.assertFalse((self.install / "Iniciar-SpiritGate.ps1").exists())

    def test_tampered_payload_is_rejected_before_installation_is_touched(self):
        kit = self.root / "tampered"
        shutil.copytree(self.kit, kit)
        (kit / "payload/Launcher-SpiritGate.ps1").write_text("tampered")
        result = self.run_kit(kit=kit)
        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertFalse((self.install / "launcher-backups").exists())
        self.assertFalse((self.install / "Iniciar-SpiritGate.ps1").exists())

    def test_embedded_public_channel_is_used_without_passing_a_feed(self):
        kit = self.root / "configured"
        shutil.copytree(self.kit, kit)
        (kit / "canal.json").write_text(json.dumps(dict(feedUrl=self.feed)))
        result = subprocess.run(["powershell.exe", "-NoProfile", "-STA", "-ExecutionPolicy", "Bypass", "-File",
                                 str(kit / "Ativar-SpiritGate.ps1"), "-InstallDir", str(self.install), "-NoGui"],
                                capture_output=True, text=True, timeout=30)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual(self.feed, json.loads((self.install / "atualizacoes.json").read_text())["feedUrl"])

    def test_open_launcher_or_game_lock_refuses_activation(self):
        runner = self.root / "hold-lock.ps1"
        runner.write_text("""param($Lock,$Activator,$Installation,$Feed)
$handle=[IO.File]::Open($Lock,[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try { & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $Activator -InstallDir $Installation -FeedUrl $Feed -NoGui; $result=$LASTEXITCODE } finally {$handle.Dispose()}
exit $result
""", encoding="utf-8")
        for lock in ("interface.lock", "launcher.lock"):
            result = subprocess.run(["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(runner),
                                     "-Lock", str(self.install / lock), "-Activator", str(self.kit / "Ativar-SpiritGate.ps1"),
                                     "-Installation", str(self.install), "-Feed", self.feed],
                                    capture_output=True, text=True, timeout=30)
            self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
            self.assertFalse((self.install / "Iniciar-SpiritGate.ps1").exists())

    def test_failure_to_activate_config_rolls_back_all_bootstrap_files(self):
        old = {name: (self.install / name).read_bytes() for name in FILES if (self.install / name).exists()}
        before = (self.install / "atualizacoes.json").read_bytes()
        protected = self.protected()
        runner = self.root / "hold-config.ps1"
        runner.write_text("""param($Config,$Activator,$Installation,$Feed)
$handle=[IO.File]::Open($Config,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::Read)
try { & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $Activator -InstallDir $Installation -FeedUrl $Feed -NoGui; $result=$LASTEXITCODE } finally {$handle.Dispose()}
exit $result
""", encoding="utf-8")
        result = subprocess.run(["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(runner),
                                 "-Config", str(self.install / "atualizacoes.json"), "-Activator", str(self.kit / "Ativar-SpiritGate.ps1"),
                                 "-Installation", str(self.install), "-Feed", self.feed],
                                capture_output=True, text=True, timeout=30)
        self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertTrue((self.install / "launcher-backups").exists(), "Failure must happen after staging the new bootstrap")
        for name, data in old.items():
            self.assertEqual(data, (self.install / name).read_bytes())
        self.assertFalse((self.install / "Iniciar-SpiritGate.ps1").exists())
        self.assertEqual(before, (self.install / "atualizacoes.json").read_bytes())
        self.assertEqual(protected, self.protected())

    def test_builder_rejects_non_https_channels_and_never_overwrites(self):
        for value in ("http://example.invalid/feed", "https://name:password@example.invalid/feed",
                      "https://example.invalid/feed?token=x", "https://example.invalid/feed#secret"):
            with self.assertRaises(ValueError):
                prepare(self.root / "invalid", value)
        self.assertFalse((self.root / "invalid").exists())
        with self.assertRaises(FileExistsError):
            prepare(self.base / "kit")


if __name__ == "__main__":
    unittest.main()
