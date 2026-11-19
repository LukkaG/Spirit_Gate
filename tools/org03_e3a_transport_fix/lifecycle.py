"""Guardas R1 intactas e captura binária limitada, sem stderr bruto persistido."""
import codecs
import hashlib
import json
import threading
from legacy.lifecycle import OwnedProcess


def safe_diagnostic(text,limit):
    """Projeta só diagnóstico JSON conhecido; texto livre/SQL/segredos são omitidos."""
    lines=[]; size=0
    for line in text.splitlines():
        try: value=json.loads(line)
        except ValueError: value=None
        if isinstance(value,dict) and value.get('event') in ('RELAY_ERROR','RELAY_DIAGNOSTIC'):
            error=value.get('error')
            error=error if error in ('ConnectionResetError','ConnectionAbortedError','TimeoutError','OSError','ValueError','RuntimeError','AssertionError','BrokenPipeError') else 'UNCLASSIFIED_ERROR'
            errno=value.get('errno')
            rendered=json.dumps(dict(event=value['event'],error=error,errno=errno if type(errno) is int else None))
        elif line:
            rendered='[UNSTRUCTURED_STDERR_REDACTED]'
        else: continue
        size+=len(rendered.encode('utf8'))+(1 if lines else 0)
        if size>limit: return '[SANITIZED_TEXT_LIMIT]',True
        lines.append(rendered)
    return '\n'.join(lines),False


class StderrCapture:
    """Drena bytes em blocos de 4 KiB; prefixo de 64 KiB fica só em memória."""
    def __init__(self,stream,emit,limit=65536):
        if not 1<=limit<=65536: raise ValueError('STDERR_LIMIT')
        self.stream,self.emit,self.limit=stream,emit,limit
        self._prefix=bytearray()
        self._hash=hashlib.sha256()
        self._decoder=codecs.getincrementaldecoder('utf8')('surrogateescape')
        self.total_bytes=self.invalid_utf8_bytes=0
        self.eof=False
        self.read_error=None
        self.lock=threading.Lock()

    def _count_invalid(self,text):
        """Conta bytes inválidos; U+FFFD válido não conta como substituição."""
        self.invalid_utf8_bytes+=sum(0xdc80<=ord(c)<=0xdcff for c in text)

    def drain(self):
        """Erros ficam no estado compartilhado; validate os propaga ao controlador."""
        try:
            while True:
                chunk=self.stream.read(4096)
                if not isinstance(chunk,bytes): raise TypeError('BINARY_STDERR_REQUIRED')
                with self.lock:
                    if not chunk:
                        self._count_invalid(self._decoder.decode(b'',final=True))
                        self.eof=True
                        break
                    self.total_bytes+=len(chunk)
                    self._hash.update(chunk)
                    self._count_invalid(self._decoder.decode(chunk))
                    self._prefix.extend(chunk[:max(0,self.limit-len(self._prefix))])
        except Exception as error:
            with self.lock: self.read_error=type(error).__name__
            self.emit('RELAY_STDERR_READ_ERROR',error=self.read_error)
        finally:
            self.emit('RELAY_STDERR',**self.snapshot())

    def snapshot(self):
        """Retorna hash de tudo que foi lido e apenas projeção textual sanitizada."""
        with self.lock:
            truncated=self.total_bytes>self.limit
            decoder=codecs.getincrementaldecoder('utf8')('surrogateescape')
            text=decoder.decode(bytes(self._prefix),final=self.eof and not truncated)
            text=''.join('\ufffd' if 0xdc80<=ord(c)<=0xdcff else c for c in text)
            omitted=not (self.eof and not truncated) and bool(text) and not text.endswith('\n')
            if omitted: text=text.rpartition('\n')[0]
            sanitized,text_truncated=safe_diagnostic(text,self.limit)
            return dict(stderr=sanitized,bytes_read=self.total_bytes,retained_bytes=len(self._prefix),
                        sha256=self._hash.hexdigest(),hash_scope='ALL_OBSERVED_BYTES',
                        invalid_utf8_bytes=self.invalid_utf8_bytes,decode_replacements=self.invalid_utf8_bytes,
                        text_lossy=bool(self.invalid_utf8_bytes),truncated=truncated,
                        text_truncated=text_truncated,text_fragment_omitted=omitted,
                        stream_complete=self.eof,capture_complete=self.eof and not self.read_error and not truncated and not text_truncated,
                        read_error=self.read_error,raw_bytes_persisted=False)

    def validate(self):
        """Recusa erro, ausência de EOF e truncamento; UTF-8 inválido é explícito."""
        value=self.snapshot()
        if not value['capture_complete']: raise RuntimeError('STDERR_CAPTURE_INCOMPLETE')
        return value
