"""Fixtures puras: não abrem sockets, CIM, subprocessos nem banco."""
import io
import queue
import subprocess
import unittest
import importlib.util
import sys
import types
from pathlib import Path
from unittest.mock import Mock, patch
from lifecycle import OwnedProcess, StderrCapture
from relay import pump

IDENTITY = dict(ProcessId=42, ParentProcessId=7, ExecutablePath='python.exe',
                CommandLine='python relay.py fixture-r1', Created='2026-11-15T00:00:00Z')


class ProcessTests(unittest.TestCase):
    """Identidade, vínculo ao handle, timeout e ausência de ações indevidas."""
    def fixture(self, polls=None, observed=IDENTITY):
        proc = Mock(pid=42)
        proc.poll.side_effect = polls
        proc.poll.return_value = None
        inspect = Mock(return_value=observed)
        emit = Mock()
        owned = OwnedProcess(proc, IDENTITY, inspect, emit,
                             barrier=lambda: {'event': 'STATS', 'held_bytes': 18},
                             controller_pid=7)
        return proc, inspect, emit, owned

    def test_01_running_match(self):
        p, _, _, o = self.fixture()
        self.assertEqual(o.observe()['state'], 'RUNNING_MATCH')
        p.terminate.assert_not_called()

    def test_02_exited_before_inspection(self):
        p, inspect, _, o = self.fixture([1])
        self.assertEqual(o.stop()['exit_code'], 1)
        inspect.assert_not_called()
        p.terminate.assert_not_called()

    def test_03_exit_between_observations(self):
        p, _, _, o = self.fixture([None, 1], None)
        self.assertEqual(o.stop()['state'], 'EXITED_CONFIRMED')
        p.terminate.assert_not_called()

    def test_04_reused_pid(self):
        p, _, _, o = self.fixture(observed=dict(IDENTITY, Created='other'))
        with self.assertRaisesRegex(RuntimeError, 'IDENTITY_MISMATCH'):
            o.stop()
        p.terminate.assert_not_called()

    def test_05_divergent_command(self):
        p, _, _, o = self.fixture(observed=dict(IDENTITY, CommandLine='other'))
        with self.assertRaisesRegex(RuntimeError, 'IDENTITY_MISMATCH'):
            o.stop()
        p.terminate.assert_not_called()

    def test_06_cim_absent(self):
        p, _, _, o = self.fixture(observed=None)
        with self.assertRaisesRegex(RuntimeError, 'UNKNOWN'):
            o.stop()
        p.terminate.assert_not_called()

    def test_07_original_handle_exit(self):
        p, _, _, o = self.fixture([0])
        self.assertEqual(o.stop()['state'], 'EXITED_CONFIRMED')
        p.terminate.assert_not_called()

    def test_08_handle_inconclusive(self):
        p, _, _, o = self.fixture(observed=None)
        p.poll.side_effect = OSError('handle unavailable')
        with self.assertRaisesRegex(RuntimeError, 'UNKNOWN'):
            o.stop()
        p.terminate.assert_not_called()

    def test_09_metadata_error(self):
        p, inspect, emit, o = self.fixture()
        inspect.side_effect = ValueError('bad metadata')
        with self.assertRaisesRegex(RuntimeError, 'UNKNOWN'):
            o.stop()
        self.assertIn('ValueError', emit.call_args.kwargs['error'])
        p.terminate.assert_not_called()

    def test_13_two_stop_requests(self):
        p, _, _, o = self.fixture([None, None, None, 1, 1])
        p.wait.return_value = 1
        self.assertTrue(o.stop()['terminated_by_controller'])
        self.assertEqual(o.stop()['state'], 'EXITED_CONFIRMED')
        p.terminate.assert_called_once()
        p.wait.assert_called_once_with(timeout=5)

    def test_15_mismatch_latched(self):
        p, inspect, _, o = self.fixture(observed=dict(IDENTITY, Created='other'))
        for _ in range(2):
            with self.assertRaisesRegex(RuntimeError, 'IDENTITY_MISMATCH'):
                o.stop()
        inspect.assert_called_once()
        p.terminate.assert_not_called()

    def test_exit_just_before_terminate(self):
        p, _, _, o = self.fixture([None, None, 0, 0])
        self.assertEqual(o.stop()['state'], 'EXITED_CONFIRMED')
        p.terminate.assert_not_called()

    def test_mismatch_even_if_handle_exits(self):
        p, _, _, o = self.fixture([None, 0], dict(IDENTITY, Created='other'))
        with self.assertRaisesRegex(RuntimeError, 'IDENTITY_MISMATCH'):
            o.stop()
        p.terminate.assert_not_called()

    def test_unlinked_original(self):
        p, inspect, _, o = self.fixture([0])
        o.controller_pid = 99
        with self.assertRaisesRegex(RuntimeError, 'UNKNOWN'):
            o.stop()
        inspect.assert_not_called()
        p.terminate.assert_not_called()

    def test_timeout_no_fallback(self):
        p, _, emit, o = self.fixture()
        p.wait.side_effect = subprocess.TimeoutExpired('fixture', 5)
        with self.assertRaises(subprocess.TimeoutExpired):
            o.stop()
        p.terminate.assert_called_once()
        p.kill.assert_not_called()
        self.assertIn('PROCESS_STOP_ERROR', [x.args[0] for x in emit.call_args_list])


class RelayTests(unittest.TestCase):
    """Transporte simulado com os mesmos caminhos de EOF/reset e retenção."""
    def test_10_socket_error(self):
        client, server, reply = Mock(), Mock(), Mock()
        client.recv.side_effect = ConnectionResetError(10054, 'fixture')
        with patch('sys.stderr', new_callable=io.StringIO) as err:
            with self.assertRaises(ConnectionResetError):
                pump(client, server, queue.Queue(), reply,
                     lambda *a: ([client], [], []))
            self.assertIn('ConnectionResetError', err.getvalue())
        self.assertEqual(reply.call_args.args[0], 'RELAY_ERROR')
        server.sendall.assert_not_called()

    def test_11_client_eof(self):
        client, server, reply = Mock(), Mock(), Mock()
        client.recv.return_value = b''
        self.assertEqual(pump(client, server, queue.Queue(), reply,
                              lambda *a: ([client], [], [])), 'EOF')
        self.assertEqual(reply.call_args.kwargs['source'], 'client')

    def test_12_client_exits_while_response_held(self):
        client, server, reply = Mock(), Mock(), Mock()
        server.recv.return_value = b'opaque-response'
        client.recv.return_value = b''
        commands = queue.Queue()
        commands.put('pause')
        ready = iter([([server], [], []), ([client], [], [])])
        pump(client, server, commands, reply, lambda *a: next(ready))
        final = reply.call_args.kwargs
        self.assertEqual(final['held_bytes'], len(b'opaque-response'))
        self.assertEqual(final['forwarded_server_bytes'], 0)
        client.sendall.assert_not_called()

    def test_14_stderr_preserved_sanitized(self):
        emit = Mock()
        capture = StderrCapture(io.StringIO('ValueError\npassword=abc token=xyz\n'), emit)
        capture.drain()
        self.assertIn('ValueError', capture.text)
        self.assertNotIn('abc', capture.text)
        self.assertNotIn('xyz', capture.text)
        self.assertEqual(emit.call_args.args[0], 'RELAY_STDERR')

    def test_unknown_error_not_masked(self):
        commands = queue.Queue()
        commands.put('invalid')
        with patch('sys.stderr', new_callable=io.StringIO):
            with self.assertRaises(ValueError):
                pump(Mock(), Mock(), commands, Mock())


class AdapterTests(unittest.TestCase):
    """Exercita a cópia real de Lab/Relay com runtime inerte, sem cluster."""
    def adapter(self):
        runtime = types.ModuleType('runtime')
        runtime.Cluster = type('Cluster', (), {})
        runtime.Session = type('Session', (), {})
        for name in ('RUN', 'ROOT', 'BIN', 'EVIDENCE', 'PREFIX', 'ADMIN',
                     'WRITER', 'DB', 'HIDDEN', 'command', 'save', 'safe_path'):
            setattr(runtime, name, Mock())
        spec = importlib.util.spec_from_file_location(
            'r1_adapter_fixture', Path(__file__).with_name('lab_runtime.py'))
        module = importlib.util.module_from_spec(spec)
        with patch.dict(sys.modules, runtime=runtime):
            spec.loader.exec_module(module)
        return module

    def fixture(self, code=0, barrier=None):
        module = self.adapter()
        lab = module.Lab.__new__(module.Lab)
        proc = Mock(pid=42)
        proc.poll.return_value = code
        lab.event = Mock()
        owned = OwnedProcess(proc, IDENTITY, Mock(), lab.event, controller_pid=7)
        lab.registry = [dict(proc=proc, lifecycle=owned)]
        relay = module.Relay.__new__(module.Relay)
        relay.lab, relay.proc = lab, proc
        relay.closed = False
        relay.last_barrier = barrier
        relay.stderr = StderrCapture(io.StringIO('fixture diagnostic\n'), lab.event)
        relay.stderr.drain()
        relay.stderr_thread = Mock()
        relay.stderr_thread.is_alive.return_value = False
        lab.relay = relay
        module.listeners = Mock(return_value=[])
        return lab, relay, proc

    def test_adapter_exited_client_eof_twice(self):
        lab, relay, proc = self.fixture(barrier=dict(event='EOF', source='client'))
        relay.close()
        relay.close()
        proc.terminate.assert_not_called()
        self.assertIsNone(lab.relay)
        final = [x for x in lab.event.call_args_list if x.args[0]=='RELAY_FINAL_DIAGNOSTIC']
        self.assertEqual(len(final), 1)
        self.assertIn('fixture diagnostic', final[0].kwargs['stderr'])

    def test_adapter_nonzero_is_not_success(self):
        _, relay, proc = self.fixture(1, dict(event='EOF', source='client'))
        with self.assertRaisesRegex(RuntimeError, 'NONZERO'):
            relay.close()
        proc.terminate.assert_not_called()

    def test_adapter_zero_unexpected_exit(self):
        _, relay, proc = self.fixture(0, dict(event='STATS', held_bytes=18))
        with self.assertRaisesRegex(RuntimeError, 'UNEXPECTED_EXIT'):
            relay.close()
        proc.terminate.assert_not_called()


if __name__ == '__main__':
    unittest.main(verbosity=2)
