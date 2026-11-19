"""Cópia R1 do relay E3-A: fluxo opaco com diagnóstico e falhas explícitas."""
import json
import queue
import select
import socket
import sys
import threading


def pump(client, server, commands, reply, select_ready=select.select):
    """Encaminha bytes sem conteúdo em logs; EOF não prova sucesso econômico."""
    paused = False
    buffered = bytearray()
    sent = received = 0
    source_name = 'control'

    def report(kind, **extra):
        reply(kind, paused=paused, forwarded_server_bytes=sent,
              received_server_bytes=received, held_bytes=len(buffered), **extra)

    try:
        while True:
            try:
                cmd = commands.get_nowait()
            except queue.Empty:
                cmd = None
            if cmd is not None:
                if cmd not in ('pause', 'stats'):
                    raise ValueError('RELAY_UNKNOWN_COMMAND')
                if cmd == 'pause':
                    paused = True
                report(cmd.upper())
            ready, _, _ = select_ready([client, server], [], [], .01)
            for source in ready:
                source_name = 'client' if source is client else 'server'
                data = source.recv(65536)
                if not data:
                    report('EOF', source=source_name)
                    return 'EOF'
                if source is client:
                    server.sendall(data)
                else:
                    received += len(data)
                    if paused:
                        buffered.extend(data)
                    else:
                        client.sendall(data)
                        sent += len(data)
                if len(buffered) > 1024 * 1024:
                    raise RuntimeError('RELAY_BUFFER_LIMIT')
    except Exception as exc:
        # Conserva tipo/errno e barreira, sem SQL, autenticação ou mensagem arbitrária.
        report('RELAY_ERROR', source=source_name, error=type(exc).__name__,
               errno=getattr(exc, 'errno', None))
        print(json.dumps(dict(event='RELAY_ERROR', error=type(exc).__name__,
                              errno=getattr(exc, 'errno', None))), file=sys.stderr, flush=True)
        raise


def main():
    """Ativa exclusivamente o listener e upstream fixos deste run autorizado."""
    from pathlib import Path
    run = Path(__file__).resolve().parents[1]
    assert run.name == '20261115-lucas-e3a-r2-8c71d4a902'
    assert len(sys.argv) == 2 and sys.argv[1] == run.name
    auth = json.loads((run/'evidence/authorization.json').read_text(encoding='utf8'))
    assert auth['run_id'] == run.name and auth['items']['R2-03'] == 'AUTORIZADO'
    assert (run/'execution.claim').is_file()
    commands = queue.Queue()
    def read_commands():
        for line in sys.stdin:
            commands.put(line.strip())
    threading.Thread(target=read_commands,daemon=True).start()
    def reply(kind, **data):
        print(json.dumps(dict(event=kind, **data)),flush=True)
    with socket.socket() as listener:
        listener.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)
        listener.bind(('127.0.0.1',55434))
        listener.listen(1)
        reply('LISTENING')
        client, address = listener.accept()
        assert address[0] == '127.0.0.1'
        with client, socket.create_connection(('127.0.0.1',55433),timeout=3) as server:
            client.settimeout(2)
            server.settimeout(2)
            pump(client,server,commands,reply)


if __name__ == '__main__':
    main()
