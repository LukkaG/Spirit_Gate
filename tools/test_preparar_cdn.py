"""Exercita staging e ordem de publicacao sem rede nem credenciais."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from preparar_cdn import prepare, execute_plan, IMMUTABLE_CACHE, MUTABLE_CACHE


def sha(data):
    return hashlib.sha256(data).hexdigest()


class CdnPreparationTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.distribution = self.root / "distribution"
        self.url = "https://updates.example.invalid/spiritgate"
        self.files = {"runtime/bin/java.exe": b"MZ-runtime", "app/spirit-gate-rpg.jar": b"game",
                      "assets/dev/reaper/frame.png": b"sprite"}
        self.manifest = dict(schemaVersion=1, version="0.201.0", basePath="release-" + "a" * 24,
                             files=[dict(path=name, bytes=len(data), sha256=sha(data)) for name, data in self.files.items()])
        self.raw = (json.dumps(self.manifest, indent=2) + "\n").encode()
        self.identity = sha(self.raw)
        self.archives = {}
        for role, label, suffix in (("anfitriao", "Anfitriao", ""), ("convidado", "Convidado", "-Convidado")):
            folder = self.distribution / label
            release = folder / "canal" / self.manifest["basePath"]
            release.mkdir(parents=True)
            (folder / "canal/latest.json").write_bytes(self.raw)
            for name, data in self.files.items():
                output = release / name
                output.parent.mkdir(parents=True, exist_ok=True)
                output.write_bytes(data)
            archive = folder / f"SpiritGate-Atualizavel-0.201.0{suffix}-Windows64.zip"
            self.archives[role] = archive
            with zipfile.ZipFile(archive, "w") as z:
                prefix = "SpiritGate-Atualizavel/"
                z.writestr(prefix + "active.txt", self.identity)
                z.writestr(prefix + "atualizacoes.json", json.dumps(dict(feedUrl="", role=role)))
                z.writestr(prefix + "SpiritGate-Launcher.exe", b"MZ-stub")
                z.writestr(prefix + "Launcher-SpiritGate.ps1", b"# UI")
                z.writestr(prefix + "Atualizar-SpiritGate.ps1", b"# updater")
                z.writestr(prefix + "versions/" + self.identity + "/installed-manifest.json", self.raw)
                for name, data in self.files.items():
                    z.writestr(prefix + "versions/" + self.identity + "/" + name, data)
        self.audit()

    def audit(self):
        (self.distribution / "integracao.json").write_text(json.dumps(dict(
            version=self.manifest["version"], manifestSha256=sha(self.raw),
            packages=[dict(role=role, sha256=sha(path.read_bytes())) for role, path in self.archives.items()])))

    def change_zip(self, role, replacements=None, extra=None):
        replacements = replacements or {}
        path = self.archives[role]
        with zipfile.ZipFile(path) as z:
            content = {entry.filename: z.read(entry) for entry in z.infolist()}
        content.update(replacements)
        content.update(extra or {})
        with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
            for name, data in content.items():
                z.writestr(name, data)
        self.audit()

    def stage(self):
        destination = self.root / "staged"
        return destination, prepare(self.distribution, destination, self.url)

    def test_bootstraps_share_exact_manifest_and_preserve_role_and_payload(self):
        original = {role: path.read_bytes() for role, path in self.archives.items()}
        destination, plan = self.stage()
        self.assertEqual(self.raw, (destination / "public/latest.json").read_bytes())
        for role, label in (("anfitriao", "Anfitriao"), ("convidado", "Convidado")):
            archive = destination / "public" / ("SpiritGate-" + label + "-Windows64.zip")
            with zipfile.ZipFile(archive) as z:
                prefix = "SpiritGate-Atualizavel/"
                config = json.loads(z.read(prefix + "atualizacoes.json"))
                self.assertEqual(role, config["role"])
                self.assertEqual(self.url + "/latest.json", config["feedUrl"])
                self.assertEqual(self.raw, z.read(prefix + "versions/" + self.identity + "/installed-manifest.json"))
                self.assertEqual(b"game", z.read(prefix + "versions/" + self.identity + "/app/spirit-gate-rpg.jar"))
            self.assertEqual(original[role], self.archives[role].read_bytes())
        self.assertFalse(plan["published"])

    def test_cache_and_upload_order_activate_latest_only_after_all_assets(self):
        destination, plan = self.stage()
        calls = []
        execute_plan(destination, lambda path, item: calls.append(item))
        commit = next(index for index, item in enumerate(calls) if item["key"] == "latest.json")
        self.assertEqual(2, calls[commit]["phase"])
        self.assertTrue(all(item["immutable"] for item in calls[:commit]))
        self.assertTrue(all(not item["immutable"] and item["phase"] == 3 for item in calls[commit + 1:]))
        self.assertTrue(calls[0]["key"].startswith(self.manifest["basePath"] + "/"))
        for item in calls:
            self.assertEqual(IMMUTABLE_CACHE if item["immutable"] else MUTABLE_CACHE, item["cacheControl"])
        self.assertEqual(plan["uploads"], calls)

    def test_failed_upload_never_activates_latest(self):
        destination, _ = self.stage()
        calls = []
        def upload(path, item):
            calls.append(item["key"])
            if len(calls) == 2:
                raise OSError("synthetic upload failure")
        with self.assertRaises(OSError):
            execute_plan(destination, upload)
        self.assertNotIn("latest.json", calls)

    def test_failed_installer_alias_keeps_the_new_game_channel_active(self):
        destination, _ = self.stage()
        calls = []
        def upload(path, item):
            calls.append(item["key"])
            if item["phase"] == 3:
                raise OSError("synthetic alias failure after game activation")
        with self.assertRaises(OSError):
            execute_plan(destination, upload)
        self.assertIn("latest.json", calls)
        self.assertLess(calls.index("latest.json"), len(calls) - 1)

    def test_tampered_staging_rejected_before_any_remote_write(self):
        destination, plan = self.stage()
        (destination / "public" / plan["uploads"][0]["key"]).write_bytes(b"tampered")
        calls = []
        with self.assertRaises(ValueError):
            execute_plan(destination, lambda path, item: calls.append(item))
        self.assertEqual([], calls)

    def test_wrong_order_rejected_before_any_upload(self):
        destination, plan = self.stage()
        pointer = next(item for item in plan["uploads"] if item["key"] == "latest.json")
        plan["uploads"].remove(pointer)
        plan["uploads"].insert(0, pointer)
        (destination / "deployment-plan.json").write_text(json.dumps(plan))
        with self.assertRaises(ValueError):
            execute_plan(destination, lambda path, item: self.fail("Upload must not run"))

    def test_invalid_https_base_urls_are_rejected_without_output(self):
        for url in ("http://updates.invalid", "https://user:secret@updates.invalid", "https://updates.invalid/?token=secret",
                    "https://updates.invalid/#fragment", "https://updates.invalid/a/../b", "https://updates.invalid/%2e%2e"):
            with self.subTest(url=url), self.assertRaises(ValueError):
                prepare(self.distribution, self.root / "invalid", url)
        self.assertFalse((self.root / "invalid").exists())

    def test_payload_hash_mismatch_is_rejected_before_writing(self):
        path = self.distribution / "Anfitriao/canal" / self.manifest["basePath"] / "app/spirit-gate-rpg.jar"
        path.write_bytes(b"changed")
        with self.assertRaises(ValueError):
            self.stage()
        self.assertFalse((self.root / "staged").exists())

    def test_extra_uninventoried_release_files_are_rejected(self):
        release = self.distribution / "Anfitriao/canal" / self.manifest["basePath"]
        (release / "private-notes.txt").write_text("not part of package")
        with self.assertRaises(ValueError):
            self.stage()

    def test_different_role_manifests_are_rejected(self):
        (self.distribution / "Convidado/canal/latest.json").write_bytes(self.raw + b" ")
        with self.assertRaises(ValueError):
            self.stage()

    def test_wrong_role_or_preexisting_different_feed_is_rejected(self):
        for config in (dict(feedUrl="", role="anfitriao"),
                       dict(feedUrl="https://other.invalid/latest.json", role="convidado")):
            self.change_zip("convidado", {"SpiritGate-Atualizavel/atualizacoes.json": json.dumps(config)})
            with self.assertRaises(ValueError):
                self.stage()

    def test_unsafe_paths_and_private_bootstrap_files_are_rejected(self):
        for index, name in enumerate(("SpiritGate-Atualizavel/../escape", "SpiritGate-Atualizavel/data/save",
                                      "SpiritGate-Atualizavel/.env", "SpiritGate-Atualizavel/private-notes.txt")):
            with self.subTest(name=name):
                self.change_zip("convidado", extra={name: b"private"})
                with self.assertRaises(ValueError):
                    prepare(self.distribution, self.root / str(index), self.url)
        self.assertFalse((self.root / "escape").exists())

    def test_invalid_manifest_schema_and_paths_are_rejected(self):
        for version in (2, True, 1.0):
            invalid = dict(self.manifest, schemaVersion=version)
            raw = json.dumps(invalid).encode()
            for label in ("Anfitriao", "Convidado"):
                (self.distribution / label / "canal/latest.json").write_bytes(raw)
            with self.assertRaises(ValueError):
                self.stage()

    def test_duplicate_json_fields_are_rejected(self):
        raw = self.raw.replace(b'"schemaVersion": 1', b'"schemaVersion": 2, "schemaVersion": 1')
        for label in ("Anfitriao", "Convidado"):
            (self.distribution / label / "canal/latest.json").write_bytes(raw)
        with self.assertRaises(ValueError):
            self.stage()

    def test_official_dispatcher_uses_ui_and_updater_from_the_versioned_payload(self):
        self.files.update({"launcher/Launcher-SpiritGate.ps1": b"# versioned UI",
                           "launcher/Atualizar-SpiritGate.ps1": b"# versioned updater"})
        self.manifest["files"] = [dict(path=name, bytes=len(data), sha256=sha(data)) for name, data in self.files.items()]
        self.raw = (json.dumps(self.manifest, indent=2) + "\n").encode()
        self.identity = sha(self.raw)
        for role, label in (("anfitriao", "Anfitriao"), ("convidado", "Convidado")):
            folder = self.distribution / label
            (folder / "canal/latest.json").write_bytes(self.raw)
            for name, data in self.files.items():
                target = folder / "canal" / self.manifest["basePath"] / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
            with zipfile.ZipFile(self.archives[role], "w") as z:
                prefix = "SpiritGate-Atualizavel/"
                z.writestr(prefix + "active.txt", self.identity)
                z.writestr(prefix + "atualizacoes.json", json.dumps(dict(feedUrl="", role=role)))
                z.writestr(prefix + "SpiritGate-Launcher.exe", b"MZ-official")
                z.writestr(prefix + "Iniciar-SpiritGate.ps1", b"# stable dispatcher")
                z.writestr(prefix + "versions/" + self.identity + "/installed-manifest.json", self.raw)
                for name, data in self.files.items():
                    z.writestr(prefix + "versions/" + self.identity + "/" + name, data)
        self.audit()
        destination, _ = self.stage()
        with zipfile.ZipFile(destination / "public/SpiritGate-Convidado-Windows64.zip") as z:
            self.assertIn("SpiritGate-Atualizavel/Iniciar-SpiritGate.ps1", z.namelist())
            self.assertNotIn("SpiritGate-Atualizavel/Launcher-SpiritGate.ps1", z.namelist())

    def test_installed_manifest_and_payload_must_match_the_feed(self):
        self.change_zip("convidado", {"SpiritGate-Atualizavel/versions/" + self.identity + "/installed-manifest.json": b"{}"})
        with self.assertRaises(ValueError):
            self.stage()

    def test_zip_audit_detects_changed_bootstrap(self):
        path = self.archives["anfitriao"]
        path.write_bytes(path.read_bytes() + b"changed after audit")
        with self.assertRaises(ValueError):
            self.stage()

    def test_previous_staging_is_never_overwritten(self):
        destination, _ = self.stage()
        before = (destination / "deployment-plan.json").read_bytes()
        with self.assertRaises(FileExistsError):
            prepare(self.distribution, destination, self.url)
        self.assertEqual(before, (destination / "deployment-plan.json").read_bytes())


if __name__ == "__main__":
    unittest.main()
