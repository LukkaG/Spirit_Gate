"""Dois papéis exclusivos da fixture P1, sem PostgreSQL ou SQL."""
import json
import os
from pathlib import Path
import queue
import socket
import struct
import sys
import threading
from relay import pump
from transport import TransportContext, require


def output(event,**data):
    """Somente metadados do protocolo privado da fixture."""
    print(json.dumps(dict(event=event,**data)),flush=True)


def main():
    """Endpoints devem constar no claim próprio; portas de serviço são recusadas."""
    role,path=sys.argv[1:]
    run=Path(path).resolve()
    require(run.parent==Path(__file__).resolve().parent/'evidence','FIXTURE_PATH_MISMATCH')
    claim=json.loads((run/'claim.json').read_text(encoding='utf8'))
    require(claim['controller_pid']==os.getppid(),'FIXTURE_PARENT_MISMATCH')
    require(role in ('client','relay'),'FIXTURE_ROLE')
    if role=='client':
        port=claim['relay_port']
        require(port not in (5432,55433,55434),'SERVICE_PORT_FORBIDDEN')
        with socket.create_connection(('127.0.0.1',port),timeout=3) as client:
            client.setsockopt(socket.SOL_SOCKET,socket.SO_LINGER,struct.pack('HH' if os.name=='nt' else 'ii',1,0))
            client.sendall(b'PREPARE')
            require(client.recv(8)==b'PREPARED','FIXTURE_HANDSHAKE')
            output('CLIENT_READY',peer=list(client.getsockname()))
            for line in sys.stdin:
                require(line.strip()=='commit','FIXTURE_CLIENT_COMMAND')
                client.sendall(b'COMMIT')
                output('DISPATCHED')
    else:
        upstream=claim['upstream_port']
        require(upstream not in (5432,55433,55434),'SERVICE_PORT_FORBIDDEN')
        commands=queue.Queue()
        def control():
            try:
                for line in sys.stdin: commands.put(json.loads(line))
            except Exception:
                commands.put(dict(command='CONTROL_PROTOCOL_ERROR'))
        threading.Thread(target=control,daemon=True).start()
        with socket.socket() as listener:
            listener.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)
            listener.bind(('127.0.0.1',0)); listener.listen(1); listener.settimeout(10)
            require(listener.getsockname()[1] not in (5432,55433,55434),'SERVICE_PORT_FORBIDDEN')
            output('LISTENING',port=listener.getsockname()[1])
            client,peer=listener.accept()
            output('ACCEPTED',peer=list(peer))
            with client,socket.create_connection(('127.0.0.1',upstream),timeout=3) as server:
                client.settimeout(3); server.settimeout(3)
                pump(client,server,commands,output,TransportContext(peer,claim['connection_id']))


if __name__=='__main__':
    try: main()
    except Exception as error:
        print(json.dumps(dict(event='RELAY_ERROR',error=type(error).__name__,errno=getattr(error,'errno',None))),file=sys.stderr,flush=True)
        raise SystemExit(1)
