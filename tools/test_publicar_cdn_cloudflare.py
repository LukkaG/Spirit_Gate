import hashlib
import io
import json
from pathlib import Path
from types import SimpleNamespace
import tempfile
import unittest
import zipfile
from unittest.mock import patch

from publicar_cdn_cloudflare import WranglerUploader, publish, verify_remote
from preparar_ativacao_cdn import prepare as prepare_activation
from preparar_cdn import IMMUTABLE_CACHE, MUTABLE_CACHE


class CloudflareUploadTest(unittest.TestCase):
    def test_hash_is_checked_before_remote_write_and_metadata_uses_separate_arguments(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            fixture = root / "file with spaces.dll"
            fixture.write_bytes(b"fixture")
            wrangler = root / "wrangler.js"
            wrangler.write_text("// fixture")
            item = dict(key="release-" + "a" * 24 + "/runtime/file with spaces.dll", bytes=7,
                        sha256=hashlib.sha256(b"fixture").hexdigest(), immutable=True,
                        cacheControl="public, max-age=31536000, immutable", contentType="application/octet-stream", phase=0)
            with patch("publicar_cdn_cloudflare.WRANGLER", wrangler), patch("publicar_cdn_cloudflare.shutil.which", return_value="node"), patch("publicar_cdn_cloudflare.subprocess.run") as remote:
                remote.return_value = SimpleNamespace(returncode=0, stdout="fixture")
                uploader = WranglerUploader("spiritgate-updates", root)
                uploader(fixture, item)
                arguments = remote.call_args.args[0]
                self.assertEqual(str(fixture), arguments[arguments.index("--file") + 1])
                self.assertEqual(item["cacheControl"], arguments[arguments.index("--cache-control") + 1])
                uploader(fixture, item)
                self.assertEqual(1, remote.call_count)
                changed = dict(item, immutable=False, key="latest.json", phase=2, cacheControl="no-store, max-age=0, must-revalidate")
                uploader(fixture, changed)
                uploader(fixture, changed)
                self.assertEqual(3, remote.call_count)
                fixture.write_bytes(b"altered fixture")
                with self.assertRaises(ValueError):
                    uploader(fixture, item)
                self.assertEqual(3, remote.call_count)

    def test_failed_upload_is_not_recorded_as_complete(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            fixture = root / "payload"
            fixture.write_bytes(b"x")
            wrangler = root / "wrangler.js"
            wrangler.write_text("// fixture")
            item = dict(key="latest.json", bytes=1, sha256=hashlib.sha256(b"x").hexdigest(), immutable=False,
                        cacheControl="no-store", contentType="application/json", phase=2)
            with patch("publicar_cdn_cloudflare.WRANGLER", wrangler), patch("publicar_cdn_cloudflare.shutil.which", return_value="node"), patch("publicar_cdn_cloudflare.subprocess.run", return_value=SimpleNamespace(returncode=1, stdout="fixture error")):
                uploader = WranglerUploader("spiritgate-updates", root)
                with self.assertRaises(RuntimeError):
                    uploader(fixture, item)
                self.assertFalse(uploader.progress.exists())


class ActivationCloudflareUploadTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory()
        cls.kit_root = Path(cls.temporary.name)
        cls.feed = "https://updates.example.invalid/latest.json"
        cls.kit = Path(prepare_activation(cls.kit_root / "kit", cls.feed)["archive"])

    @classmethod
    def tearDownClass(cls):
        cls.temporary.cleanup()

    def setUp(self):
        self.workspace = tempfile.TemporaryDirectory()
        self.addCleanup(self.workspace.cleanup)
        self.root = Path(self.workspace.name)
        public = self.root / "public"
        self.content = {"release-" + "a" * 24 + "/app/game.jar": b"game", "latest.json": b'{"version":"0.201.0"}',
                        "SpiritGate-Anfitriao-Windows64.zip": b"host", "SpiritGate-Convidado-Windows64.zip": b"guest"}
        uploads = []
        for key, data in self.content.items():
            immutable = key.startswith("release-")
            path = public / key
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
            uploads.append(dict(key=key, bytes=len(data), sha256=hashlib.sha256(data).hexdigest(),
                                immutable=immutable, cacheControl=IMMUTABLE_CACHE if immutable else MUTABLE_CACHE,
                                contentType="application/octet-stream", phase=0 if immutable else 2 if key == "latest.json" else 3))
        self.plan = dict(schemaVersion=1, baseUrl="https://updates.example.invalid", feedUrl=self.feed,
                         version="0.201.0", uploads=uploads)
        self.save_plan()

    def save_plan(self):
        (self.root / "deployment-plan.json").write_text(json.dumps(self.plan), encoding="utf-8")

    def change_zip(self, replacements=None, extra=None):
        with zipfile.ZipFile(self.kit) as package:
            data = {entry.filename: package.read(entry) for entry in package.infolist()}
        data.update(replacements or {})
        data.update(extra or {})
        target = self.root / "altered-kit.zip"
        with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as package:
            for name, content in data.items():
                package.writestr(name, content)
        return target

    def test_activation_is_uploaded_after_main_plan_with_no_store_and_receipt_is_verified(self):
        calls = []
        def uploader(path, item):
            self.assertEqual(item["sha256"], hashlib.sha256(path.read_bytes()).hexdigest())
            calls.append(item)
        result = dict(verified=list(self.content)[1:] + ["SpiritGate-Ativar-CDN.zip", "SpiritGate-Ativar-CDN.zip.sha256"])
        with patch("publicar_cdn_cloudflare.verify_remote", return_value=result) as check:
            self.assertEqual(result, publish(self.root, uploader, self.kit))
        self.assertEqual(["SpiritGate-Ativar-CDN.zip", "SpiritGate-Ativar-CDN.zip.sha256"], [item["key"] for item in calls[-2:]])
        self.assertEqual([item["key"] for item in self.plan["uploads"]], [item["key"] for item in calls[:-2]])
        for item in calls[-2:]:
            self.assertFalse(item["immutable"])
            self.assertEqual(MUTABLE_CACHE, item["cacheControl"])
            self.assertEqual(4, item["phase"])
        check.assert_called_once_with(self.root, self.kit)
        receipt = json.loads((self.root / "publication.json").read_text())
        self.assertTrue(receipt["activationUploaded"])
        self.assertTrue(receipt["published"])
        self.assertTrue(receipt["onlineVerified"])

    def test_invalid_kit_tree_is_rejected_before_any_upload(self):
        bad = self.change_zip(extra={"SpiritGate-Ativar-CDN/data/private.txt": b"private"})
        calls = []
        with self.assertRaises(ValueError):
            publish(self.root, lambda path, item: calls.append(item), bad)
        self.assertEqual([], calls)
        self.assertFalse((self.root / "publication.json").exists())

    def test_kit_for_another_feed_is_rejected_before_any_upload(self):
        self.plan["feedUrl"] = "https://other.example.invalid/latest.json"
        self.save_plan()
        calls = []
        with self.assertRaises(ValueError):
            publish(self.root, lambda path, item: calls.append(item), self.kit)
        self.assertEqual([], calls)

    def test_even_self_consistent_arbitrary_payload_code_is_rejected(self):
        with zipfile.ZipFile(self.kit) as package:
            manifest = json.loads(package.read("SpiritGate-Ativar-CDN/manifest.json"))
        for item in manifest["files"]:
            if item["path"] == "Atualizar-SpiritGate.ps1":
                item.update(bytes=7, sha256=hashlib.sha256(b"unknown").hexdigest())
        bad = self.change_zip(replacements={"SpiritGate-Ativar-CDN/payload/Atualizar-SpiritGate.ps1": b"unknown",
                                           "SpiritGate-Ativar-CDN/manifest.json": json.dumps(manifest).encode()})
        calls = []
        with self.assertRaises(ValueError):
            publish(self.root, lambda path, item: calls.append(item), bad)
        self.assertEqual([], calls)

    def test_remote_verification_reads_both_activation_aliases_when_supplied(self):
        remote = dict(self.content)
        data = self.kit.read_bytes()
        remote["SpiritGate-Ativar-CDN.zip"] = data
        remote["SpiritGate-Ativar-CDN.zip.sha256"] = (hashlib.sha256(data).hexdigest() + "\n").encode("ascii")
        requested = []
        def response(request, **kwargs):
            key = request.full_url.removeprefix(self.plan["baseUrl"] + "/")
            requested.append(key)
            value = io.BytesIO(remote[key])
            value.url = request.full_url
            value.headers = {"Cache-Control": MUTABLE_CACHE}
            return value
        with patch("publicar_cdn_cloudflare.urllib.request.urlopen", side_effect=response):
            result = verify_remote(self.root, self.kit)
        self.assertEqual(["latest.json", "SpiritGate-Anfitriao-Windows64.zip", "SpiritGate-Convidado-Windows64.zip",
                          "SpiritGate-Ativar-CDN.zip", "SpiritGate-Ativar-CDN.zip.sha256"], requested)
        self.assertEqual(requested, result["verified"])

    def test_tampered_remote_activation_zip_is_rejected(self):
        def response(request, **kwargs):
            key = request.full_url.removeprefix(self.plan["baseUrl"] + "/")
            value = io.BytesIO(b"changed" if key == "SpiritGate-Ativar-CDN.zip" else self.content[key])
            value.url = request.full_url
            value.headers = {"Cache-Control": MUTABLE_CACHE}
            return value
        with patch("publicar_cdn_cloudflare.urllib.request.urlopen", side_effect=response), self.assertRaises(ValueError):
            verify_remote(self.root, self.kit)
        self.assertFalse((self.root / "online-verification.json").exists())

    def test_failed_verification_does_not_claim_published(self):
        with patch("publicar_cdn_cloudflare.verify_remote", side_effect=OSError("synthetic HTTP error")), self.assertRaises(OSError):
            publish(self.root, lambda path, item: None, self.kit)
        receipt = json.loads((self.root / "publication.json").read_text())
        self.assertTrue(receipt["uploaded"])
        self.assertTrue(receipt["activationUploaded"])
        self.assertFalse(receipt["onlineVerified"])
        self.assertFalse(receipt.get("published", False))


if __name__ == "__main__":
    unittest.main()
