"""Validação loopback autorizada P1: dois processos próprios, nenhum SQL."""
import json
import os
from pathlib import Path
import queue
import socket
import subprocess
import sys
import threading
import time
import uuid
from lifecycle import OwnedProcess,StderrCapture
from transport import stop_client_with_permit,RelayCompletion,require

HERE=Path(__file__).resolve().parent
HIDDEN=getattr(subprocess,'CREATE_NO_WINDOW',0)


def inspect(pid):
    """Consulta apenas PID recém-criado pelo supervisor; nunca controla por PID."""
    script=f"Get-CimInstance Win32_Process -Filter 'ProcessId={pid}' | Select-Object ProcessId,ParentProcessId,ExecutablePath,CommandLine,@{{n='Created';e={{$_.CreationDate.ToUniversalTime().ToString('o')}}}} | ConvertTo-Json -Compress"
    result=subprocess.run(['powershell','-NoProfile','-Command',script],capture_output=True,text=True,encoding='utf8',timeout=8,creationflags=HIDDEN)
    require(result.returncode==0,'CIM_INSPECTION_FAILED')
    return json.loads(result.stdout) if result.stdout.strip() else None


class Peer:
    """Popen binário com dreno contínuo dos dois pipes e identidade completa."""
    def __init__(self,role,run,emit):
        self.role=role
        env={k:v for k,v in os.environ.items() if not k.upper().startswith('PG')}
        env['PYTHONDONTWRITEBYTECODE']='1'
        self.proc=subprocess.Popen([sys.executable,'-B',str(HERE/'fixture_peer.py'),role,str(run)],
            stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env,creationflags=HIDDEN)
        identity=inspect(self.proc.pid)
        require(identity and identity['ParentProcessId']==os.getpid(),'PEER_IDENTITY_MISMATCH')
        require(str(run) in identity['CommandLine'] and str(HERE/'fixture_peer.py') in identity['CommandLine'],'PEER_COMMAND_MISMATCH')
        self.owned=OwnedProcess(self.proc,identity,inspect,emit,controller_pid=os.getpid())
        emit('PROCESS_CREATED',role=role,identity=identity)
        self.capture=StderrCapture(self.proc.stderr,emit)
        self.stderr_thread=threading.Thread(target=self.capture.drain,daemon=True)
        self.stderr_thread.start()
        self.events=queue.Queue(maxsize=256); self.stdout_error=None
        def drain():
            try:
                while True:
                    line=self.proc.stdout.readline(16385)
                    if not line: break
                    require(len(line)<=16384,'PEER_STDOUT_LIMIT')
                    value=json.loads(line.decode('utf8'))
                    emit('WIRE_EVENT',role=role,metadata=value)
                    self.events.put_nowait(value)
            except Exception as error:
                self.stdout_error=type(error).__name__
                emit('PEER_STDOUT_ERROR',role=role,error=self.stdout_error)
        self.stdout_thread=threading.Thread(target=drain,daemon=True); self.stdout_thread.start()

    def read(self,event):
        """ACKs ordenam a fixture; timeout nunca vira êxito."""
        value=self.events.get(timeout=5)
        require(value['event']==event,'UNEXPECTED_PEER_EVENT_'+value['event'])
        return value

    def send(self,value):
        """Canal stdin privado; não é socket externo nem configuração de produção."""
        line=json.dumps(value) if self.role=='relay' or not isinstance(value,str) else value
        self.proc.stdin.write((line+'\n').encode('utf8')); self.proc.stdin.flush()

    def joined(self):
        """Exige drenagem, EOF e captura íntegra; não encerra processos externos."""
        self.proc.wait(timeout=5)
        self.stdout_thread.join(2); self.stderr_thread.join(2)
        require(not self.stdout_thread.is_alive() and not self.stderr_thread.is_alive() and not self.stdout_error,'PIPE_CAPTURE_INCOMPLETE')
        return self.capture.validate()


def trial(expected):
    """Uma conexão TCP sintética; caso negativo mantém reset como erro."""
    run=HERE/'evidence'/('fixture-20261115-'+uuid.uuid4().hex[:12])
    run.mkdir()
    events=[]; lock=threading.Lock()
    def emit(kind,**data):
        with lock:
            value=dict(sequence=len(events)+1,monotonic=time.monotonic(),kind=kind,**data)
            events.append(value)
            with (run/'events.jsonl').open('a',encoding='utf8') as stream:
                stream.write(json.dumps(value)+'\n')
            return value
    peers=[]; server_error=[]; ports=[]; report=None
    started=time.monotonic()
    try:
        with socket.socket() as upstream:
            upstream.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)
            upstream.bind(('127.0.0.1',0)); upstream.listen(1); upstream.settimeout(10)
            upstream_port=upstream.getsockname()[1]
            require(upstream_port not in (5432,55433,55434),'SERVICE_PORT_FORBIDDEN')
            ports.append(upstream_port)
            claim=dict(controller_pid=os.getpid(),upstream_port=upstream_port,connection_id=run.name)
            (run/'claim.json').write_text(json.dumps(claim),encoding='utf8')
            def server():
                try:
                    conn,_=upstream.accept()
                    with conn:
                        conn.settimeout(10)
                        def receive(size):
                            data=b''
                            while len(data)<size:
                                part=conn.recv(size-len(data))
                                require(bool(part),'FIXTURE_UPSTREAM_EARLY_EOF'); data+=part
                            return data
                        require(receive(7)==b'PREPARE','FIXTURE_PREPARE_MISMATCH')
                        conn.sendall(b'PREPARED')
                        require(receive(6)==b'COMMIT','FIXTURE_DISPATCH_MISMATCH')
                        conn.sendall(b'ACK-COMMIT')
                        emit('FIXTURE_RESPONSE_SENT',bytes=10,sql=False)
                        require(conn.recv(1)==b'','UPSTREAM_UNEXPECTED_BYTES')
                except Exception as error:
                    server_error.append(type(error).__name__)
                    emit('FIXTURE_UPSTREAM_ERROR',error=type(error).__name__)
            server_thread=threading.Thread(target=server,daemon=True); server_thread.start()
            relay=Peer('relay',run,emit); peers.append(relay)
            port=relay.read('LISTENING')['port']; ports.append(port)
            claim['relay_port']=port
            (run/'claim.json').write_text(json.dumps(claim),encoding='utf8')
            client=Peer('client',run,emit); peers.append(client)
            ready=client.read('CLIENT_READY'); accepted=relay.read('ACCEPTED')
            require(ready['peer']==accepted['peer'],'ACCEPTED_PEER_MISMATCH')
            # A declaração do cliente é conferida contra metadados SO do socket.
            port_local=ready['peer'][1]
            script=f"@(Get-NetTCPConnection -State Established -LocalAddress 127.0.0.1 -LocalPort {port_local} -RemoteAddress 127.0.0.1 -RemotePort {port} -ErrorAction SilentlyContinue | Select-Object OwningProcess,LocalAddress,LocalPort) | ConvertTo-Json -Compress"
            result=subprocess.run(['powershell','-NoProfile','-Command',script],capture_output=True,text=True,encoding='utf8',timeout=8,creationflags=HIDDEN)
            require(result.returncode==0,'SOCKET_INSPECTION_FAILED')
            sockets=json.loads(result.stdout); sockets=sockets if isinstance(sockets,list) else [sockets]
            require(len(sockets)==1 and sockets[0]['OwningProcess']==client.proc.pid,'SOCKET_OWNERSHIP_MISMATCH')
            emit('SOCKET_BOUND_TO_CLIENT',owner_pid=client.proc.pid,peer=ready['peer'])
            relay.send('pause'); pause=emit('RELAY_PAUSE',metadata=relay.read('PAUSE'))
            client.send('commit'); client.read('DISPATCHED')
            dispatch=emit('COMMIT_DISPATCHED',pid=client.proc.pid,sql=False)
            deadline=time.monotonic()+5
            while True:
                relay.send('stats'); stats_value=relay.read('STATS')
                if stats_value['held_bytes']>0: break
                require(time.monotonic()<deadline,'HELD_RESPONSE_TIMEOUT')
            stats=emit('RELAY_STATS',metadata=stats_value)
            emit('CALLER_STATE',status='ECONOMIC_UNCERTAIN',sql=False)
            context=dict(scenario='FIXTURE_EXPECTED',operation_id=run.name,phase='COMMIT_DISPATCHED_CLIENT_UNCERTAIN',caller_status='ECONOMIC_UNCERTAIN',
                connection_id=run.name,peer=ready['peer'],binding=dict(owner_pid=client.proc.pid,peer=ready['peer']),pause=pause,dispatch=dispatch,stats=stats)
            armed=[]
            if expected:
                def arm(permit):
                    relay.send(dict(command='expect_disconnect',permit=permit))
                    ack=relay.read('EXPECT_DISCONNECT'); armed.append(permit)
                    emit('DISCONNECT_ARM_ACK',metadata=ack)
                    return ack
                stop_client_with_permit(client.owned,context,arm)
            else: client.owned.stop(timeout=5)
            terminal=relay.read('EXPECTED_CLIENT_DISCONNECT' if expected else 'RELAY_ERROR')
            require(terminal['source']=='client' and terminal['operation']=='recv_client' and terminal['error']=='ConnectionResetError' and terminal['errno']==10054,'REAL_RESET_NOT_REPRODUCED')
            client.joined(); relay.joined()
            process=relay.owned.observe()
            require(process['state']=='EXITED_CONFIRMED','RELAY_HANDLE_NOT_EXITED')
            if expected:
                completion=RelayCompletion(relay.owned,relay.capture,armed[0]).close(terminal)
                require(completion['economic_test']=='NOT_EVALUATED','FIXTURE_CANNOT_APPROVE_ECONOMY')
            else:
                require(process['exit_code']==1 and terminal['termination_requested'] is False and terminal['transport_result']=='UNEXPECTED_CONNECTION_RESET','UNEXPECTED_RESET_WAS_IGNORED')
                completion=dict(transport_result='UNEXPECTED_CONNECTION_RESET',process_result='RELAY_EXITED_CONFIRMED',economic_test='NOT_EVALUATED',exit_code=process['exit_code'])
            server_thread.join(3)
            require(not server_thread.is_alive() and not server_error,'UPSTREAM_FIXTURE_INCOMPLETE')
            report=dict(status='PASSOU',case='EXPECTED_RESET' if expected else 'UNEXPECTED_RESET',terminal=terminal,completion=completion)
    except Exception as error:
        report=dict(status='FALHOU',case='EXPECTED_RESET' if expected else 'UNEXPECTED_RESET',error=type(error).__name__,reason=str(error))
        raise
    finally:
        cleanup_errors=[]
        for peer in reversed(peers):
            try:
                # A mesma guarda original classifica; sem kill alternativo.
                peer.owned.stop(timeout=5)
                peer.joined()
            except Exception as error: cleanup_errors.append(type(error).__name__+': '+str(error))
        script="@(Get-NetTCPConnection -State Listen -LocalPort "+','.join(str(p) for p in ports)+" -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess) | ConvertTo-Json -Compress"
        result=subprocess.run(['powershell','-NoProfile','-Command',script],capture_output=True,text=True,encoding='utf8',timeout=8,creationflags=HIDDEN)
        listeners=json.loads(result.stdout) if result.stdout.strip() else []
        require(result.returncode==0,'CLEANUP_LISTENER_INSPECTION_FAILED')
        final=dict(report=report,ports=ports,cleanup_errors=cleanup_errors,listeners_after=listeners,
                   own_handles_exited=all(p.proc.poll() is not None for p in peers),elapsed_s=round(time.monotonic()-started,3),postgresql=False,sql=False,
                   relay_terminate_count=sum(e['kind']=='PROCESS_TERMINATE' and e['expected']['ProcessId']==peers[0].proc.pid for e in events) if peers else 0)
        (run/'result.json').write_text(json.dumps(final,indent=2),encoding='utf8')
        require(not cleanup_errors and not listeners and final['own_handles_exited'],'FIXTURE_CLEANUP_UNPROVEN')
        require(final['relay_terminate_count']==0,'UNEXPECTED_RELAY_TERMINATE')
    return dict(path=run.relative_to(HERE).as_posix(),**final)


def main():
    """Dois controles reais, sem repetição automática para obter aprovação."""
    results=[]
    try:
        for expected in (True,False): results.append(trial(expected))
    finally:
        (HERE/'evidence/fixture-results.json').write_text(json.dumps(dict(trials=results,postgresql=False,sql=False),indent=2),encoding='utf8')
    print(json.dumps(dict(trials=[dict(path=x['path'],status=x['report']['status'],case=x['report']['case'],ports=x['ports'],elapsed_s=x['elapsed_s']) for x in results]),indent=2))


if __name__=='__main__': main()
