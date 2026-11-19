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
    """R1 não abre socket por CLI; ativação depende de pacote futuro autorizado."""
    raise SystemExit('R1_PREPARATION_ONLY: nova rodada exige autorização e integração')


if __name__ == '__main__':
    main()
