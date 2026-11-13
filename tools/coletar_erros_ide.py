"""Importa erros do Oracle Java do workspace atual, sem alterar o VSCode."""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import uuid
from urllib.parse import unquote, urlparse
from urllib.request import url2pathname

LIMIT = 1024 * 1024
ERROR = re.compile(r"\[(?:ERROR|SEVERE)\]|\b\w*(?:Exception|Error)\b|Cannot run on older versions", re.I)
HEADER = re.compile(r"^\[\d{4}-\d{2}-\d{2}T")


def _atomic(path: Path, text: str) -> None:
    """Publica o arquivo completo para o monitor nunca ler uma gravação parcial."""
    temporary = path.with_name(path.name + "." + uuid.uuid4().hex + ".tmp")
    try:
        temporary.write_text(text, encoding="utf-8")
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


def _workspace_dirs(storage: Path, root: Path) -> list[Path]:
    """Relaciona apenas userdirs cujo workspace.json identifica esta pasta."""
    matches = []
    if not storage.is_dir():
        return matches
    for folder in storage.iterdir():
        metadata = folder / "workspace.json"
        if not metadata.is_file() or metadata.stat().st_size > 16384:
            continue
        try:
            uri = urlparse(json.loads(metadata.read_text(encoding="utf-8"))["folder"])
            if uri.scheme != "file" or uri.netloc:
                continue
            target = Path(url2pathname(unquote(uri.path))).resolve()
            if target == root:
                matches.append(folder / "Oracle.oracle-java" / "userdir")
        except (OSError, ValueError, KeyError, TypeError):
            continue  # Metadados de outros workspaces não impedem a coleta local.
    return matches


def _error_context(text: str, previous: str, active: bool = False) -> tuple[str, bool]:
    """Retém mensagens de erro, contexto imediato e a stack até o próximo evento."""
    prior = previous.splitlines()
    lines = prior + text.splitlines()
    selected: set[int] = set()
    for index in range(len(prior), len(lines)):
        found = bool(ERROR.search(lines[index]))
        if HEADER.match(lines[index]):
            active = found
        elif found:
            active = True
        if active:
            selected.update(range(max(0, index - 4), index + 1))
    return "\n".join(lines[index] for index in sorted(selected)), active


def collect(root: Path) -> list[Path]:
    """Importa novos trechos de erro e preserva um cursor por log de origem.

    A descoberta fica nas duas últimas sessões do VSCode. O cabeçalho precisa
    mencionar o userdir vinculado à pasta do projeto. Retorna somente TXT novos.
    Erros de disco são propagados para o monitor reportar e tentar novamente.
    """
    root = root.resolve()
    appdata = os.environ.get("APPDATA")
    if not appdata:
        return []
    code = Path(appdata) / "Code"
    logs = code / "logs"
    userdirs = _workspace_dirs(code / "User" / "workspaceStorage", root)
    if not logs.is_dir() or not userdirs:
        return []
    inbox = root / "logs" / "erros"
    monitor = root / "logs" / "monitor"
    # Recusa redirecionar relatórios/cursores por symlink para fora do projeto.
    if not inbox.resolve().is_relative_to(root) or not monitor.resolve().is_relative_to(root):
        raise ValueError("Destino de logs da IDE fora da pasta do projeto.")
    inbox.mkdir(parents=True, exist_ok=True)
    monitor.mkdir(parents=True, exist_ok=True)
    state_path = monitor / "ide-cursors.json"
    if not state_path.resolve().is_relative_to(root):
        raise ValueError("Arquivo de cursores da IDE fora da pasta do projeto.")
    state = json.loads(state_path.read_text(encoding="utf-8")) if state_path.exists() else {}
    if not isinstance(state, dict):
        raise ValueError("Cursores da IDE inválidos; preserve o arquivo para investigar.")
    sessions = sorted((entry for entry in logs.iterdir() if entry.is_dir()),
                      key=lambda entry: entry.name, reverse=True)[:2]
    linked = [str(path).replace("\\", "/").casefold() for path in userdirs]
    created = []
    for session in sessions:
        for source in session.glob("window*/exthost/output_logging_*/*Oracle Java SE Language Server.log"):
            if not source.resolve().is_relative_to(logs.resolve()):
                continue
            with source.open("rb") as stream:
                header = stream.read(65536).decode("utf-8", errors="replace").replace("\\", "/").casefold()
                if not any(path in header for path in linked):
                    continue
                key = str(source)
                old = state.get(key, {})
                if not isinstance(old, dict):
                    raise ValueError("Registro de cursor da IDE inválido.")
                size = source.stat().st_size
                # A assinatura inicial também reconhece uma substituição maior.
                stream.seek(0)
                prefix = stream.read(min(size, 256))
                signature = hashlib.sha256(prefix).hexdigest()
                offset = old.get("offset", 0)
                if not isinstance(offset, int) or offset < 0:
                    raise ValueError("Offset do cursor da IDE inválido.")
                previous = old.get("context", "")
                active = old.get("active_error", False)
                old_prefix_length = old.get("prefix_length", 0)
                if (not isinstance(previous, str) or not isinstance(old_prefix_length, int)
                        or not 0 <= old_prefix_length <= 256):
                    raise ValueError("Contexto do cursor da IDE inválido.")
                stream.seek(0)
                previous_signature = hashlib.sha256(stream.read(old_prefix_length)).hexdigest()
                if size < offset or (old_prefix_length and old.get("signature") != previous_signature):
                    offset, previous, active = 0, "", False
                stream.seek(offset)
                data = stream.read(LIMIT)
                if not data:
                    continue
                # Deixa uma linha ainda em gravação para a próxima varredura.
                newline = data.rfind(b"\n")
                if newline < 0 and len(data) < LIMIT:
                    continue
                if newline >= 0:
                    data = data[:newline + 1]
                text = data.decode("utf-8", errors="replace")
                context, active = _error_context(text, previous, active)
                end = offset + len(data)
                if context:
                    digest = hashlib.sha256((key + str(offset)).encode() + data).hexdigest()[:24]
                    report = inbox / f"ide-oracle-{digest}.txt"
                    if not report.exists():
                        _atomic(report, f"Origem: Oracle Java SE Language Server (workspace atual)\n"
                                f"Arquivo: {source}\nTrecho em bytes: {offset}-{end}\n\n{context}\n")
                        created.append(report)
                state[key] = {"offset": end, "context": "\n".join(text.splitlines()[-4:]),
                              "signature": signature, "prefix_length": len(prefix), "active_error": active}
    _atomic(state_path, json.dumps(state, ensure_ascii=False, indent=2))
    return created


if __name__ == "__main__":
    for report in collect(Path(__file__).resolve().parents[1]):
        print(report)
