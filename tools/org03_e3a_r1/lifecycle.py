"""Classificação conservadora, sem descoberta ou encerramento por PID genérico."""
import re
import subprocess
import threading


def sanitize(text):
    """Remove valores potencialmente sensíveis; não registra payload TCP."""
    text = re.sub(r'(?i)(password|pgpassword|secret|token)(\s*[=:]\s*)([^\s,;]+)',
                  r'\1\2[REDACTED]', text)
    return re.sub(r'(?i)(postgres(?:ql)?://)[^\s]+', r'\1[REDACTED]', text)


class StderrCapture:
    """Drena pipe continuamente e conserva diagnóstico limitado e sanitizado."""
    def __init__(self, stream, emit, limit=65536):
        self.stream, self.emit, self.limit = stream, emit, limit
        self.text = ''
        self.truncated = False
        self.lock = threading.Lock()

    def drain(self):
        """Consome até EOF; erro de leitura também fica explícito na evidência."""
        try:
            for line in self.stream:
                with self.lock:
                    value = sanitize(line)
                    remaining = self.limit - len(self.text)
                    self.text += value[:remaining]
                    self.truncated |= len(value) > remaining
        except (OSError, UnicodeError) as exc:
            self.emit('RELAY_STDERR_READ_ERROR', error=type(exc).__name__)
            raise
        finally:
            self.emit('RELAY_STDERR', **self.snapshot())

    def snapshot(self):
        """Obtém o trecho já drenado sem depender de EOF do processo."""
        with self.lock:
            return dict(stderr=self.text, truncated=self.truncated)


class OwnedProcess:
    """Vincula manifesto ao objeto Popen original; dúvidas bloqueiam a ação."""
    fields = ('ProcessId', 'ParentProcessId', 'ExecutablePath', 'CommandLine', 'Created')

    def __init__(self, proc, expected, inspect, emit, barrier=lambda: None,
                 stderr=lambda: {}, controller_pid=None):
        self.proc, self.expected = proc, dict(expected)
        self.inspect, self.emit = inspect, emit
        self.barrier, self.stderr = barrier, stderr
        self.controller_pid = controller_pid
        self.lock = threading.Lock()
        self.blocked = None

    def observe(self):
        """Verifica handle antes/depois de CIM; ausência sozinha nunca basta."""
        before = after = None
        observed = None
        error = None
        try:
            linked = (self.proc.pid == self.expected.get('ProcessId') and
                      self.controller_pid is not None and
                      self.expected.get('ParentProcessId') == self.controller_pid and
                      all(self.expected.get(k) for k in self.fields))
            if not linked:
                state = 'UNKNOWN'
            else:
                before = self.proc.poll()
                if before is not None:
                    state = 'EXITED_CONFIRMED'
                    after = before
                else:
                    try:
                        observed = self.inspect(self.proc.pid)
                    except Exception as exc:
                        # Exceção desconhecida bloqueia e é preservada, nunca vira ausência.
                        error = type(exc).__name__ + ': ' + sanitize(str(exc))
                    after = self.proc.poll()
                    if observed is not None and observed != self.expected:
                        state = 'IDENTITY_MISMATCH'
                    elif error:
                        state = 'UNKNOWN'
                    elif after is not None:
                        state = 'EXITED_CONFIRMED'
                    elif observed == self.expected:
                        state = 'RUNNING_MATCH'
                    else:
                        state = 'UNKNOWN'
        except Exception as exc:
            state = 'UNKNOWN'
            error = type(exc).__name__ + ': ' + sanitize(str(exc))
        record = dict(state=state, expected=self.expected, observed=observed,
                      poll_before=before, poll_after=after, exit_code=after,
                      error=error, last_tcp_barrier=self.barrier(), **self.stderr())
        self.emit('PROCESS_OBSERVATION', **record)
        return record

    def stop(self, timeout=5):
        """Encerra só RUNNING_MATCH pelo handle original, sem fallback ou retry."""
        with self.lock:
            if self.blocked:
                raise RuntimeError(self.blocked)
            record = self.observe()
            state = record['state']
            if state == 'EXITED_CONFIRMED':
                return record
            if state != 'RUNNING_MATCH':
                self.blocked = state
                raise RuntimeError(state)
            # Fecha a janela após a consulta sem selecionar outro processo por PID.
            code = self.proc.poll()
            if code is not None:
                self.emit('PROCESS_EXIT_BEFORE_TERMINATE', exit_code=code)
                return self.observe()
            self.emit('PROCESS_TERMINATE', expected=self.expected)
            try:
                self.proc.terminate()
                code = self.proc.wait(timeout=timeout)
            except (OSError, subprocess.TimeoutExpired) as exc:
                self.blocked = 'UNKNOWN'
                self.emit('PROCESS_STOP_ERROR', error=type(exc).__name__,
                          diagnostic=self.observe())
                raise
            self.emit('PROCESS_EXIT', exit_code=code)
            result = self.observe()
            result['terminated_by_controller'] = True
            return result
