"""Pump opaco P1; CLI operacional SQL deliberadamente indisponível."""
import json
import queue
import select
import sys
from transport import TransportContext, require, digest


def pump(client,server,commands,reply,context,select_ready=select.select):
    """Só recv no cliente armado pode concluir como desconexão esperada."""
    paused=False
    buffered=bytearray()
    sent=received=client_received=client_forwarded=0
    source='control'; operation='control'

    def report(kind,**extra):
        reply(kind,paused=paused,forwarded_server_bytes=sent,received_server_bytes=received,
              held_bytes=len(buffered),received_client_bytes=client_received,
              forwarded_client_bytes=client_forwarded,termination_requested=context.permit is not None,
              permit_sha256=digest(context.permit) if context.permit else None,**extra)

    def disconnect(error):
        if context.expected(source,operation,error,paused,sent,received,len(buffered)):
            report('EXPECTED_CLIENT_DISCONNECT',transport_result='TRANSPORT_EXPECTED_DISCONNECT',
                   source=source,operation=operation,error=type(error).__name__ if error else 'EOF',
                   errno=getattr(error,'errno',None))
            return True
        return False

    try:
        while True:
            source='control'; operation='control'
            try: cmd=commands.get_nowait()
            except queue.Empty: cmd=None
            if cmd is not None:
                name=cmd if isinstance(cmd,str) else cmd['command']
                if name=='pause':
                    require(not paused,'RELAY_ALREADY_PAUSED')
                    paused=True
                elif name=='expect_disconnect':
                    context.arm(cmd['permit'],paused,sent,received,len(buffered))
                else: require(name=='stats','RELAY_UNKNOWN_COMMAND')
                report({'pause':'PAUSE','stats':'STATS','expect_disconnect':'EXPECT_DISCONNECT'}[name])
            operation='select'; source='multiplexer'
            ready,_,_=select_ready([client,server],[],[],.01)
            for sock in ready:
                source='client' if sock is client else 'server'
                operation='recv_'+source
                try: data=sock.recv(4096)
                except ConnectionResetError as error:
                    if disconnect(error): return 'EXPECTED_CLIENT_DISCONNECT'
                    raise
                if not data:
                    if disconnect(None): return 'EXPECTED_CLIENT_DISCONNECT'
                    raise RuntimeError('UNEXPECTED_EOF')
                if sock is client:
                    client_received+=len(data)
                    source='server'; operation='send_server'
                    server.sendall(data)
                    client_forwarded+=len(data)
                else:
                    received+=len(data)
                    if paused:
                        require(len(buffered)+len(data)<=16384,'RELAY_BUFFER_LIMIT')
                        buffered.extend(data)
                    else:
                        source='client'; operation='send_client'
                        client.sendall(data)
                        sent+=len(data)
    except Exception as error:
        report('RELAY_ERROR',transport_result='UNEXPECTED_CONNECTION_RESET' if isinstance(error,ConnectionResetError) else 'TRANSPORT_FAILED',source=source,operation=operation,error=type(error).__name__,errno=getattr(error,'errno',None))
        raise


if __name__=='__main__':
    raise SystemExit('P1_PREPARATION_ONLY: usar fixture isolada; PostgreSQL/R3 não autorizados')
