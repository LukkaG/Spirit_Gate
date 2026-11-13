"""Canal GitHub: nenhuma chamada Git real ou acesso remoto nos testes."""
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zipfile

import publicar_atualizacoes_git as publisher


def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


class FakeGit:
    def __init__(self, existing=None, *, unchanged=False, reject_push=False, bad_index=False):
        self.existing = existing
        self.unchanged, self.reject_push, self.bad_index = unchanged, reject_push, bad_index
        self.calls = []
        self.old = "1" * 40
        self.new = "2" * 40
        self.committed = False

    def __call__(self, *args, cwd=None, check=True):
        self.calls.append(args)
        output, code = "", 0
        if args[0] == "init":
            (cwd / ".git").mkdir()
        elif args[0] == "ls-remote":
            output, code = ((self.old + "\t" + args[-1] + "\n", 0)
                            if self.existing is not None else ("", 2))
        elif args[0] == "rev-parse":
            output = (self.new if self.committed else self.old) + "\n"
        elif args[0] == "ls-tree":
            output = "".join("100644 blob " + blob(data) + "\t" + name + "\0"
                             for name, data in self.existing.items())
        elif args[0] == "show":
            output = self.existing[".gitattributes"].decode()
        elif args[0] == "checkout":
            for name, data in self.existing.items():
                path = cwd / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
        elif args[0] == "ls-files":
            files = publisher.public_files(cwd, checkout=True)
            output = "".join("100644 " + ("0" * 40 if self.bad_index else blob(data)) + " 0\t" + name + "\0"
                             for name, data in files.items())
        elif args[0] == "diff":
            code = 0 if self.unchanged else 1
        elif args[0] == "config" and args[1] == "--get":
            output = "Fixture\n"
        elif args[0] == "commit":
            self.committed = True
        elif args[0] == "push" and self.reject_push:
            raise subprocess.CalledProcessError(1, ["git", *args], stderr="non-fast-forward")
        return SimpleNamespace(stdout=output, stderr="", returncode=code)


class GitPublicationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.distribution = self.root / "distribution"
        self.stage = self.root / "review"
        self.files = {"runtime/bin/java.exe": b"MZ-runtime", "app/spirit-gate-rpg.jar": b"game",
                      "assets/dev/reaper/frame.png": b"sprite", "launcher/Launcher-SpiritGate.ps1": b"# UI",
                      "launcher/Atualizar-SpiritGate.ps1": b"# updater"}
        self.feed = publisher.target()["feedUrl"]
        self.build_source()

    def build_source(self, feed="", extra_zip=None):
        manifest = dict(schemaVersion=1, version="0.201.0", basePath="release-" + "a" * 24,
                        files=[dict(path=name, bytes=len(data), sha256=publisher.digest(data)) for name, data in self.files.items()])
        self.raw = (json.dumps(manifest, indent=2) + "\n").encode()
        self.manifest = manifest
        identity = publisher.digest(self.raw)
        packages = []
        for role, label, suffix in (("anfitriao", "Anfitriao", ""), ("convidado", "Convidado", "-Convidado")):
            folder = self.distribution / label
            release = folder / "canal" / manifest["basePath"]
            release.mkdir(parents=True, exist_ok=True)
            (folder / "canal/latest.json").write_bytes(self.raw)
            for name, data in self.files.items():
                path = release / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
            archive = folder / f"SpiritGate-Atualizavel-0.201.0{suffix}-Windows64.zip"
            with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
                prefix = "SpiritGate-Atualizavel/"
                z.writestr(prefix + "active.txt", identity)
                z.writestr(prefix + "atualizacoes.json", json.dumps(dict(feedUrl=feed, role=role)))
                z.writestr(prefix + "Iniciar-SpiritGate.ps1", b"# dispatcher")
                z.writestr(prefix + "SpiritGate-Launcher.exe", b"MZ-launcher")
                z.writestr(prefix + "versions/" + identity + "/installed-manifest.json", self.raw)
                for name, data in self.files.items():
                    z.writestr(prefix + "versions/" + identity + "/" + name, data)
                for name, data in (extra_zip or {}).items():
                    z.writestr(name, data)
            packages.append(dict(role=role, sha256=publisher.digest(archive.read_bytes())))
        (self.distribution / "integracao.json").write_text(json.dumps(dict(
            version=manifest["version"], manifestSha256=publisher.digest(self.raw), packages=packages)))

    def stage_plan(self, **kwargs):
        return publisher.prepare(self.distribution, self.stage, **kwargs)

    def old_tree(self):
        old = dict(self.manifest, version="0.200.9", basePath="release-" + "b" * 24)
        return {".gitattributes": b"* -text\n", "README.md": b"old public readme",
                "latest.json": json.dumps(old).encode(),
                old["basePath"] + "/runtime/bin/java.exe": b"old-runtime"}

    def test_default_dry_run_creates_reviewable_tree_without_git_and_preserves_sources(self):
        originals = {path: path.read_bytes() for path in self.distribution.rglob("*.zip")}
        with patch.object(publisher, "git") as git:
            plan = publisher.publish(self.distribution, self.stage)
        git.assert_not_called()
        self.assertTrue(plan["dryRun"])
        self.assertFalse(plan["published"])
        self.assertFalse((self.stage / "publication.json").exists())
        self.assertTrue((self.stage / "git-publication-plan.json").is_file())
        self.assertEqual(self.raw, (self.stage / "public/latest.json").read_bytes())
        for label, role in (("Anfitriao", "anfitriao"), ("Convidado", "convidado")):
            with zipfile.ZipFile(self.stage / f"public/SpiritGate-{label}-Windows64.zip") as z:
                config = json.loads(z.read("SpiritGate-Atualizavel/atualizacoes.json"))
                self.assertEqual(dict(feedUrl=self.feed, role=role), config)
                self.assertIn("SpiritGate-Atualizavel/Iniciar-SpiritGate.ps1", z.namelist())
        self.assertEqual(originals, {path: path.read_bytes() for path in originals})

    def test_repo_branch_remote_feed_and_readme_share_one_destination(self):
        plan = self.stage_plan(repo="AnotherOwner/AnotherRepo", branch="preview-201")
        expected = publisher.target("AnotherOwner/AnotherRepo", "preview-201")
        self.assertTrue(all(plan[key] == value for key, value in expected.items()))
        readme = (self.stage / "public/README.md").read_text()
        self.assertIn(expected["baseUrl"] + "/SpiritGate-Convidado-Windows64.zip", readme)
        self.assertNotIn("LukkaG", readme)

    def test_divergent_or_credentialed_remote_and_invalid_branch_fail_before_git(self):
        for arguments in (dict(remote="https://github.com/Other/Repo.git"),
                          dict(remote="https://token@github.com/LukkaG/Spirit_Gate.git"),
                          dict(branch="--evil"), dict(branch="../main"), dict(branch="HEAD")):
            with self.subTest(arguments=arguments), patch.object(publisher, "git") as git, self.assertRaises(ValueError):
                publisher.publish(self.distribution, self.stage, **arguments)
            git.assert_not_called()
        self.assertFalse(self.stage.exists())

    def test_legacy_pair_of_role_folders_is_supported(self):
        plan = publisher.prepare(self.distribution / "Anfitriao", self.stage,
                                 guest_distribution=self.distribution / "Convidado")
        self.assertEqual("0.201.0", plan["version"])
        publisher.audit_plan(self.stage, publisher.target())

    def test_generated_sprite_manifest_is_allowed_but_other_properties_are_not(self):
        name = "assets/dev/generated/corujao-iv-v1/manifest.properties"
        self.files[name] = b"frame.width=64\nframe.height=64\n"
        self.build_source()
        self.stage_plan()
        self.assertEqual(self.files[name], (self.stage / "public" / self.manifest["basePath"] / name).read_bytes())
        with self.assertRaises(ValueError):
            publisher.audit_payload_name("assets/dev/generated/corujao-iv-v1/private.properties")

    def test_known_legacy_offline_files_are_retained_without_allowing_arbitrary_scripts(self):
        existing = self.old_tree()
        prefix = "release-" + "b" * 24 + "/"
        existing[prefix + "Abrir-Geometrico.cmd"] = b"@echo off\njava -DexperimentalSprites=false RpgLauncher\n"
        existing[prefix + "ROTEIRO-RPG-OFFLINE.md"] = b"# Roteiro offline\nWASD para mover.\n"
        self.stage_plan()
        transport = FakeGit(existing)
        with patch.object(publisher, "git", side_effect=transport):
            publisher.publish(self.distribution, self.stage, apply=True)
        checkout = next(self.stage.glob("checkout-*"))
        for name, data in existing.items():
            if name.startswith(prefix):
                self.assertEqual(data, (checkout / name).read_bytes())
        with self.assertRaises(ValueError):
            publisher.audit_payload_name("Executar-Arbitrario.cmd")

    def test_private_paths_in_manifest_are_rejected_even_with_matching_hashes(self):
        for name in ("runtime/.ssh/config", "assets/secrets.json", "runtime/client.pem", "app/source.java"):
            with self.subTest(name=name):
                self.files[name] = b"private"
                self.build_source()
                with self.assertRaises(ValueError):
                    self.stage_plan()
                del self.files[name]
        self.assertFalse(self.stage.exists())

    def test_undeclared_payload_and_extra_private_zip_entry_are_rejected(self):
        extra = self.distribution / "Anfitriao/canal" / self.manifest["basePath"] / "private.txt"
        extra.write_bytes(b"do not publish")
        with self.assertRaises(ValueError):
            self.stage_plan()
        extra.unlink()
        self.build_source(extra_zip={"SpiritGate-Atualizavel/.ssh/config": b"private"})
        with self.assertRaises(ValueError):
            self.stage_plan()
        self.assertFalse(self.stage.exists())

    def test_secret_text_and_oversized_git_blobs_fail_before_output(self):
        self.files["app/SpiritGate.cfg"] = b"ghp_" + b"X" * 36
        self.build_source()
        with self.assertRaisesRegex(ValueError, "credencial"):
            self.stage_plan()
        self.files["app/SpiritGate.cfg"] = b"public config"
        self.build_source()
        with patch.object(publisher, "MAX_BLOB_BYTES", 4), self.assertRaisesRegex(ValueError, "limite"):
            self.stage_plan()
        self.assertFalse(self.stage.exists())

    def test_existing_dry_run_is_never_overwritten(self):
        self.stage_plan()
        before = (self.stage / "git-publication-plan.json").read_bytes()
        with self.assertRaises(FileExistsError):
            self.stage_plan()
        self.assertEqual(before, (self.stage / "git-publication-plan.json").read_bytes())

    def test_changed_staging_and_wrong_destination_fail_before_any_git(self):
        self.stage_plan()
        (self.stage / "public/secret.txt").write_text("private")
        with patch.object(publisher, "git") as git, self.assertRaises(ValueError):
            publisher.publish(self.distribution, self.stage, apply=True)
        git.assert_not_called()
        with patch.object(publisher, "git") as git, self.assertRaises(ValueError):
            publisher.publish(self.distribution, self.stage, branch="other", apply=True)
        git.assert_not_called()

    def test_push_preserves_old_releases_and_uses_one_normal_commit(self):
        self.stage_plan()
        existing = self.old_tree()
        transport = FakeGit(existing)
        with patch.object(publisher, "git", side_effect=transport):
            receipt = publisher.publish(self.distribution, self.stage, apply=True)
        self.assertTrue(receipt["published"])
        self.assertFalse(receipt["onlineVerified"])
        self.assertEqual(transport.old, receipt["previousCommit"])
        self.assertEqual(transport.new, receipt["commit"])
        checkout = next(self.stage.glob("checkout-*"))
        for name, data in existing.items():
            if name.startswith("release-"):
                self.assertEqual(data, (checkout / name).read_bytes())
        self.assertEqual([("push", "origin", "HEAD:refs/heads/updates")],
                         [call for call in transport.calls if call[0] == "push"])
        self.assertEqual(1, len([call for call in transport.calls if call[0] == "commit"]))

    def test_windows_crlf_legacy_attributes_are_accepted_and_replaced_by_strict_filter_free_attributes(self):
        self.stage_plan()
        existing = self.old_tree()
        existing[".gitattributes"] = b"* -text\r\n"
        transport = FakeGit(existing)
        with patch.object(publisher, "git", side_effect=transport):
            receipt = publisher.publish(self.distribution, self.stage, apply=True)
        self.assertTrue(receipt["published"])
        checkout = next(self.stage.glob("checkout-*"))
        self.assertEqual(publisher.ATTRIBUTES, (checkout / ".gitattributes").read_bytes())
        self.assertEqual(publisher.ATTRIBUTES, (self.stage / "public/.gitattributes").read_bytes())
        (self.stage / "public/.gitattributes").write_bytes(b"* -text\r\n")
        with self.assertRaises(ValueError):
            publisher.audit_plan(self.stage, publisher.target())

    def test_new_distribution_branch_can_be_created_without_force(self):
        self.stage_plan()
        transport = FakeGit()
        with patch.object(publisher, "git", side_effect=transport):
            receipt = publisher.publish(self.distribution, self.stage, apply=True)
        self.assertIsNone(receipt["previousCommit"])
        self.assertFalse(any("--force" in call for call in transport.calls))

    def test_conflicting_immutable_release_is_rejected_without_commit_or_push(self):
        self.stage_plan()
        existing = {item["path"]: (self.stage / "public" / item["path"]).read_bytes()
                    for item in json.loads((self.stage / "git-publication-plan.json").read_bytes())["files"]}
        existing[self.manifest["basePath"] + "/app/spirit-gate-rpg.jar"] = b"conflicting old game"
        transport = FakeGit(existing)
        with patch.object(publisher, "git", side_effect=transport), self.assertRaisesRegex(ValueError, "imutavel"):
            publisher.publish(self.distribution, self.stage, apply=True)
        self.assertFalse(any(call[0] in {"commit", "push"} for call in transport.calls))

    def test_lfs_index_transformation_is_rejected_before_commit_or_push(self):
        self.stage_plan()
        transport = FakeGit(bad_index=True)
        with patch.object(publisher, "git", side_effect=transport), self.assertRaisesRegex(ValueError, "LFS"):
            publisher.publish(self.distribution, self.stage, apply=True)
        self.assertFalse(any(call[0] in {"commit", "push"} for call in transport.calls))

    def test_unchanged_remote_never_creates_a_new_commit_or_push(self):
        self.stage_plan()
        _, existing = publisher.audit_plan(self.stage, publisher.target())
        transport = FakeGit(existing, unchanged=True)
        with patch.object(publisher, "git", side_effect=transport):
            receipt = publisher.publish(self.distribution, self.stage, apply=True)
        self.assertEqual("unchanged", receipt["status"])
        self.assertFalse(receipt["pushed"])
        self.assertFalse(any(call[0] in {"commit", "push"} for call in transport.calls))

    def test_concurrent_non_fast_forward_fails_without_force_or_success_receipt(self):
        self.stage_plan()
        transport = FakeGit(self.old_tree(), reject_push=True)
        with patch.object(publisher, "git", side_effect=transport), self.assertRaises(subprocess.CalledProcessError):
            publisher.publish(self.distribution, self.stage, apply=True)
        self.assertFalse(any("--force" in call for call in transport.calls))
        self.assertFalse((self.stage / "publication.json").exists())

    def test_activation_uses_github_provider_and_is_in_the_same_commit_tree(self):
        kit = self.root / "activation.zip"
        kit.write_bytes(b"fixture-kit-validated-by-provider")
        with patch("preparar_ativacao_cdn.validate_activation_zip", return_value=dict(sha256=publisher.digest(kit.read_bytes()))) as validate:
            self.stage_plan(activation_zip=kit)
        validate.assert_called_once_with(kit, self.feed, provider="github")
        self.assertEqual(kit.read_bytes(), (self.stage / "public/SpiritGate-Ativar-GitHub.zip").read_bytes())
        self.assertEqual((publisher.digest(kit.read_bytes()) + "\n").encode(),
                         (self.stage / "public/SpiritGate-Ativar-GitHub.zip.sha256").read_bytes())


if __name__ == "__main__":
    unittest.main()
