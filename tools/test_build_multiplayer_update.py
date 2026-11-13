"""Uma atualizacao deve empacotar o codigo novo inclusive depois da migracao do JAR."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

spec = importlib.util.spec_from_file_location("multiplayer_build", Path(__file__).with_name("build-multiplayer-update.py"))
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)


class MultiplayerBuildTest(unittest.TestCase):
    def test_new_jar_replaces_legacy_and_already_normalized_names(self):
        for jar_name in ("app/spirit-gate-rpg.jar", "app/spiritgate.jar"):
            with self.subTest(jar_name=jar_name), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                channel = root / "channel"
                payload = channel / ("release-" + "a" * 24)
                files = []
                for name, data in ((jar_name, b"old game"), ("app/client.cfg", b"classpath=spirit-gate-rpg.jar")):
                    file = payload / name
                    file.parent.mkdir(parents=True, exist_ok=True)
                    file.write_bytes(data)
                    files.append(dict(path=name, bytes=len(data), sha256=hashlib.sha256(data).hexdigest()))
                (channel / "latest.json").write_text(json.dumps(dict(basePath=payload.name, files=files)))
                metadata = root / "server/target/launcher/build.json"
                metadata.parent.mkdir(parents=True)
                metadata.write_text('{"fixture":true}')

                def compile_fixture(command, **kwargs):
                    if command[0] == "jar":
                        Path(command[3]).write_bytes(b"newly compiled game")

                feed = "https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json"

                def integrate_fixture(package, destination, version, feed_url):
                    self.assertEqual(feed, feed_url)
                    with zipfile.ZipFile(package) as archive:
                        prefix = "SpiritGateCorujaoVI-Anfitriao/"
                        self.assertEqual(b"newly compiled game", archive.read(prefix + "app/spiritgate.jar"))
                        self.assertEqual(b"classpath=spiritgate.jar", archive.read(prefix + "app/client.cfg"))
                        self.assertEqual(1, sum(name.endswith(".jar") for name in archive.namelist()))
                    return {"fixture": True}

                with patch.object(builder, "ROOT", root), patch.object(builder.subprocess, "run", side_effect=compile_fixture), patch.object(builder, "integrate", side_effect=integrate_fixture):
                    builder.build(channel, root / "evidence", root / "dist", "0.201.1", feed)

    def test_missing_jar_is_rejected_before_compiling(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "latest.json").write_text(json.dumps(dict(files=[])))
            with patch.object(builder.subprocess, "run") as compile_command:
                with self.assertRaises(ValueError):
                    builder.build(root, root / "evidence", root / "dist", "0.201.1")
                compile_command.assert_not_called()


if __name__ == "__main__":
    unittest.main()
