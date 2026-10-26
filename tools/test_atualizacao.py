"""Isolated distribution tests; no publication or remote connections."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
from preparar_atualizacao import prepare


class UpdateDistributionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.package = self.root / "game.zip"

    def fixture(self, extra=None):
        with zipfile.ZipFile(self.package, "w") as archive:
            for name, data in {"runtime/bin/java.exe": b"runtime", "app/spirit-gate-rpg.jar": b"game",
                               **(extra or {})}.items():
                archive.writestr("SpiritGateRPG/" + name, data)

    def test_bootstrap_and_feed_hashes_agree(self):
        self.fixture({"assets/dev/player-base/frame.png": b"fixture"})
        target = self.root / "output"
        output = prepare(self.package, target, "1-test")
        raw = (target / "canal/latest.json").read_bytes()
        identity = hashlib.sha256(raw).hexdigest()
        app = target / "SpiritGate-Atualizavel"
        self.assertEqual(identity, (app / "active.txt").read_text())
        self.assertEqual(raw, (app / "versions" / identity / "installed-manifest.json").read_bytes())
        for item in json.loads(raw)["files"]:
            data = (app / "versions" / identity / item["path"]).read_bytes()
            self.assertEqual(item["sha256"], hashlib.sha256(data).hexdigest())
        with zipfile.ZipFile(output) as archive:
            self.assertIsNone(archive.testzip())
            self.assertFalse(any("/saves/" in name for name in archive.namelist()))
            self.assertIn("SpiritGate-Atualizavel/Jogar-SpiritGate.cmd", archive.namelist())
        self.assertEqual("", json.loads((app / "atualizacoes.json").read_text())["feedUrl"])

    def test_personal_saves_and_traversal_cannot_be_published(self):
        for index, name in enumerate(("saves/character", "data/saves/character", "../escape", "app/../../escape", "CON.txt")):
            self.fixture({name: b"private"})
            with self.assertRaises(ValueError):
                prepare(self.package, self.root / str(index), "1")
        self.assertFalse((self.root / "escape").exists())

    def test_case_collisions_are_rejected(self):
        self.fixture({"APP/SPIRIT-GATE-RPG.JAR": b"different"})
        with self.assertRaises(ValueError):
            prepare(self.package, self.root / "output", "1")

    def test_never_overwrites_a_previous_distribution(self):
        self.fixture()
        target = self.root / "output"
        output = prepare(self.package, target, "1")
        original = output.read_bytes()
        with self.assertRaises(FileExistsError):
            prepare(self.package, target, "2")
        self.assertEqual(original, output.read_bytes())

    def test_requires_https_and_safe_version_before_writing(self):
        self.fixture()
        for version, url in (("../bad", ""), ("1", "http://example.invalid/feed"),
                             ("1", "https://secret:password@example.invalid/feed")):
            target = self.root / "invalid"
            with self.assertRaises(ValueError):
                prepare(self.package, target, version, url)
            self.assertFalse(target.exists())

    def test_radmin_payload_is_normalized_for_old_updaters(self):
        with zipfile.ZipFile(self.package, "w") as archive:
            for name, content in {
                "runtime/bin/java.exe": b"runtime",
                "app/spiritgate.jar": b"radmin-game",
                "app/SpiritGate.cfg": b"app.classpath=$APPDIR\\spiritgate.jar\n",
                "SpiritGate.cmd": b'java -cp "app\\spiritgate.jar" Launcher',
                "manifest-sha256.json": b"old paths and hashes",
            }.items():
                archive.writestr("SpiritGateCorujaoV-Anfitriao/" + name, content)
        target = self.root / "modern"
        prepare(self.package, target, "0.177.0", role="convidado")
        manifest = json.loads((target / "canal/latest.json").read_text())
        release = target / "canal" / manifest["basePath"]
        self.assertEqual(b"radmin-game", (release / "app/spirit-gate-rpg.jar").read_bytes())
        self.assertFalse((release / "app/spiritgate.jar").exists())
        self.assertFalse((release / "manifest-sha256.json").exists())
        self.assertIn("spirit-gate-rpg.jar", (release / "app/SpiritGate.cfg").read_text())
        self.assertIn("spirit-gate-rpg.jar", (release / "SpiritGate.cmd").read_text())
        self.assertEqual("convidado", json.loads((target / "SpiritGate-Atualizavel/atualizacoes.json").read_text())["role"])
        for item in manifest["files"]:
            content = (release / item["path"]).read_bytes()
            self.assertEqual(hashlib.sha256(content).hexdigest(), item["sha256"])

    def test_both_roles_share_exactly_the_same_manifest(self):
        self.fixture()
        for role in ("anfitriao", "convidado"):
            prepare(self.package, self.root / role, "1", role=role)
        self.assertEqual((self.root / "anfitriao/canal/latest.json").read_bytes(),
                         (self.root / "convidado/canal/latest.json").read_bytes())

    def test_guest_only_payload_and_invalid_role_cannot_replace_common_channel(self):
        with zipfile.ZipFile(self.package, "w") as archive:
            archive.writestr("SpiritGateCorujaoV-Convidado/app/spiritgate.jar", b"guest")
        with self.assertRaises(ValueError):
            prepare(self.package, self.root / "guest-source", "1")
        with self.assertRaises(ValueError):
            prepare(self.package, self.root / "bad-role", "1", role="invalid")
        self.assertFalse((self.root / "bad-role").exists())
