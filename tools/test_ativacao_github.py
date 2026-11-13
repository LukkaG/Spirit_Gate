import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from preparar_ativacao_cdn import prepare, provider_feed, validate_activation_zip


FEED = "https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json"


class GitHubActivationTest(unittest.TestCase):
    def test_github_kit_names_and_channel_are_consistent_without_cloudflare(self):
        with tempfile.TemporaryDirectory() as temporary:
            result = prepare(Path(temporary) / "kit", FEED, provider="github")
            archive = Path(result["archive"])
            self.assertEqual("SpiritGate-Ativar-GitHub.zip", archive.name)
            audit = validate_activation_zip(archive, FEED, provider="github")
            self.assertEqual(result["sha256"], audit["sha256"])
            with zipfile.ZipFile(archive) as package:
                self.assertTrue(all(name.startswith("SpiritGate-Ativar-GitHub/") for name in package.namelist()))
                config = json.loads(package.read("SpiritGate-Ativar-GitHub/canal.json"))
                self.assertEqual({"feedUrl": FEED}, config)
                for name in package.namelist():
                    if name.endswith((".ps1", ".json", ".txt", ".cmd")):
                        self.assertNotIn(b"cloudflare", package.read(name).lower())

    def test_github_activation_rejects_other_providers_and_redirect_aliases(self):
        for value in ("", "https://updates.example.com/latest.json", "https://project.pages.dev/latest.json",
                      "https://github.com/LukkaG/Spirit_Gate/raw/updates/latest.json",
                      FEED + "?token=secret"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                provider_feed(value, "github")


if __name__ == "__main__":
    unittest.main()
