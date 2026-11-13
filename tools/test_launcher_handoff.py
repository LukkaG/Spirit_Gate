"""A interface fecha depois da abertura real e deixa o processo do jogo independente."""
import ctypes
from ctypes import wintypes
from contextlib import contextmanager
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import unittest


ROOT = Path(__file__).resolve().parents[1]


@contextmanager
def handoff_temporary_directory():
    temporary = tempfile.TemporaryDirectory()
    target = Path(temporary.name).resolve()
    try:
        yield temporary.name
    finally:
        # O Windows pode conservar brevemente a imagem do EXE mesmo apos exit.
        # Nao ignora erros: a limpeza e limitada e preserva qualquer falha do teste.
        until = time.monotonic() + 8
        while True:
            if Path(temporary.name).resolve() != target:
                raise RuntimeError("Diretorio temporario mudou durante a verificacao")
            try:
                temporary.cleanup()
                break
            except PermissionError:
                if time.monotonic() >= until:
                    raise
                time.sleep(0.1)


def process_alive(pid):
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.OpenProcess.argtypes = (wintypes.DWORD, wintypes.BOOL, wintypes.DWORD)
    kernel.OpenProcess.restype = wintypes.HANDLE
    kernel.GetExitCodeProcess.argtypes = (wintypes.HANDLE, ctypes.POINTER(wintypes.DWORD))
    kernel.CloseHandle.argtypes = (wintypes.HANDLE,)
    handle = kernel.OpenProcess(0x1000, False, pid)
    if not handle:
        return False
    try:
        exit_code = wintypes.DWORD()
        return bool(kernel.GetExitCodeProcess(handle, ctypes.byref(exit_code))) and exit_code.value == 259
    finally:
        kernel.CloseHandle(handle)


@unittest.skipUnless(os.name == "nt", "Launcher WinForms requer Windows")
class LauncherHandoffTest(unittest.TestCase):
    def run_fixture(self, folder, mode="success", role="convidado"):
        output = folder.parent / (folder.name + "-parent.log")
        # Esperar EOF de um pipe herdado esperaria tambem o helper. O contrato
        # e o encerramento do processo da interface, conferido por wait().
        with output.open("w", encoding="utf-8") as log:
            process = subprocess.Popen([
                "powershell.exe", "-NoProfile", "-STA", "-ExecutionPolicy", "Bypass", "-File",
                str(ROOT / "tools/updater/Test-Launcher-Handoff.ps1"),
                "-FixtureRoot", str(folder), "-Case", mode, "-Role", role,
            ], stdout=log, stderr=subprocess.STDOUT)
            process.wait(timeout=40)
        text = output.read_text(encoding="utf-8", errors="replace")
        self.assertEqual(0, process.returncode, text)
        return json.loads(text)

    def test_game_outlives_the_ui_and_preserves_arguments_roles_and_lock(self):
        for role in ("anfitriao", "convidado"):
            with self.subTest(role=role), handoff_temporary_directory() as temporary:
                folder = Path(temporary) / "instalacao com espacos & simbolos"
                result = self.run_fixture(folder, role=role)
                self.assertTrue(result["childAlive"])
                self.assertTrue(result["gameLockHeld"])
                self.assertTrue(result["interfaceLockReleased"])
                self.assertEqual(0, result["remainingLaunchSignals"])
                self.assertTrue(process_alive(result["childPid"]), "O helper deve continuar apos o processo WinForms sair")
                arguments = (folder / "argv.txt").read_text(encoding="utf-8-sig").splitlines()
                self.assertIn("-Dspiritgate.rpg.dataDir=" + str(folder / "data"), arguments)
                self.assertEqual(role == "convidado", "-Dspiritgate.guest=true" in arguments)
                self.assertEqual("com.projetok.prototype.rpg.RpgLauncher", arguments[-1])
                self.assertEqual(str(folder), (folder / "cwd.txt").read_text(encoding="utf-8-sig"))
                until = time.monotonic() + 12
                while process_alive(result["childPid"]) and time.monotonic() < until:
                    time.sleep(0.1)
                self.assertFalse(process_alive(result["childPid"]))
                self.assertTrue((folder / "exited.txt").is_file())
                # O engine independente tambem deve liberar o lock depois do helper.
                until = time.monotonic() + 4
                while time.monotonic() < until:
                    try:
                        with (folder / "launcher.lock").open("r+b"):
                            break
                    except PermissionError:
                        time.sleep(0.1)
                else:
                    self.fail("Lock da sessao nao foi liberado apos o jogo sair")

    def test_launch_failures_keep_the_real_form_visible_and_recoverable(self):
        for mode in ("invalid-executable", "exit-early"):
            with self.subTest(mode=mode), handoff_temporary_directory() as temporary:
                result = self.run_fixture(Path(temporary) / "falha de abertura", mode=mode)
                self.assertTrue(result["recovery"]["visible"])
                self.assertTrue(result["recovery"]["playEnabled"])
                self.assertTrue(result["recovery"]["retryEnabled"])
                self.assertEqual(0, result["remainingLaunchSignals"])
                if mode == "invalid-executable":
                    outside = Path(temporary) / (".launch-ready-" + "a" * 32 + ".json")
                    forbidden = subprocess.run([
                        "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                        str(Path(temporary) / "falha de abertura/Atualizar-SpiritGate.ps1"),
                        "-InstallDir", str(Path(temporary) / "falha de abertura"), "-SkipUpdate",
                        "-LaunchReadyFile", str(outside),
                    ], capture_output=True, text=True, errors="replace", timeout=15)
                    self.assertNotEqual(0, forbidden.returncode)
                    self.assertIn("Confirmacao de abertura", forbidden.stdout + forbidden.stderr)
                    self.assertFalse(outside.exists())

    def test_ack_failure_stops_only_the_new_child_before_releasing_the_lock(self):
        with handoff_temporary_directory() as temporary:
            result = self.run_fixture(Path(temporary) / "falha no ACK", mode="ack-failure")
            self.assertTrue(result["failureObserved"])
            self.assertTrue(result["newChildStopped"])
            self.assertTrue(result["preexistingChildPreserved"])
            self.assertTrue(result["lockHeldBeforeFailureReturned"])
            self.assertTrue(result["killFailureWaited"])
            self.assertEqual(0, result["remainingLaunchSignals"])


if __name__ == "__main__":
    unittest.main()
