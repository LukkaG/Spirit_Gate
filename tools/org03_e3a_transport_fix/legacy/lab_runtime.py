"""Ciclo de vida E3-A: identidade exata, limites e processos próprios."""
import json
import os
import queue
import shutil
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path
from lifecycle import OwnedProcess, StderrCapture
from runtime import Cluster, Session, RUN, ROOT, BIN, EVIDENCE, PREFIX, ADMIN, WRITER, DB, HIDDEN, command, save, safe_path


def durable(path, value):
    """Persiste intenção/fronteira antes da operação que depende dela."""
    with path.open('w', encoding='utf8') as f:
        json.dump(value, f, ensure_ascii=False, sort_keys=True)
        f.flush()
        os.fsync(f.fileno())


def process_info(pid):
    """Consulta somente PID conhecido; hora de criação impede reuso silencioso."""
    assert isinstance(pid, int) and pid > 0
    ps = f"Get-CimInstance Win32_Process -Filter 'ProcessId={pid}' | Select-Object ProcessId,ParentProcessId,ExecutablePath,CommandLine,@{{n='Created';e={{$_.CreationDate.ToUniversalTime().ToString('o')}}}} | ConvertTo-Json -Compress"
    r = command(['powershell','-NoProfile','-Command',ps], timeout=8)
    assert r.returncode == 0, 'PROCESS_INSPECTION_FAILED'
    return json.loads(r.stdout) if r.stdout.strip() else None


def listeners(port):
    """Lê metadados apenas das duas portas aprovadas."""
    assert port in (55433,55434)
    ps = f"@(Get-NetTCPConnection -State Listen -LocalPort {port} -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess) | ConvertTo-Json -Compress"
    r = command(['powershell','-NoProfile','-Command',ps], timeout=8)
    assert r.returncode == 0
    x = json.loads(r.stdout) if r.stdout.strip() else []
    return x if isinstance(x,list) else [x]


class Lab(Cluster):
    """Um initdb, até dois immediate e quatro starts de recuperação/controle."""
    def __init__(self):
        super().__init__()
        self.crashes = self.restarts = self.workers_created = 0
        self.registry = []
        self.postmaster = None
        self.relay = None
        self.monitor_stop = threading.Event()
        self.quota_error = None
        self.scenario_start = None
        self.start_tests = time.monotonic()
        self.events = []
        self.event_lock = threading.Lock()
        self.relays_created = 0

    def event(self, kind, **data):
        """Sequência do supervisor; não depende do relógio de parede para ordenar."""
        with self.event_lock:
            record = dict(sequence=len(self.events)+1, monotonic=time.monotonic(), kind=kind, **data)
            self.events.append(record)
            with (EVIDENCE/'events.jsonl').open('a',encoding='utf8') as f:
                f.write(json.dumps(record,ensure_ascii=False)+'\n')
            return record

    def preflight(self):
        """Preserva proteções E2 e acrescenta autorização e porta do relay."""
        auth=json.loads((EVIDENCE/'authorization.json').read_text(encoding='utf8'))
        assert auth['responsavel'] and auth['run_id']==RUN.name, 'RESPONSIBLE_OR_RUN_MISSING'
        assert all(x=='AUTORIZADO' for x in auth['items'].values())
        assert not listeners(55434), 'RELAY_PORT_CONFLICT'
        with socket.socket() as s:
            s.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)
            s.bind(('127.0.0.1',55434))
        super().preflight()

    def monitor(self):
        """Fiscaliza disco e prazos durante esperas sem expandir quotas."""
        while not self.monitor_stop.wait(.5):
            try:
                self.quota()
                if self.scenario_start: assert time.monotonic()-self.scenario_start<=120,'SCENARIO_DEADLINE'
            except Exception as e:
                self.quota_error=str(e)
                return

    def check(self):
        assert not self.quota_error, self.quota_error
        self.quota()
        if self.scenario_start: assert time.monotonic()-self.scenario_start<=120,'SCENARIO_DEADLINE'

    def identity(self,database=DB):
        """Revalida SQL, listener, comando, criação, opções e PID do cluster."""
        x=super().identity(database)
        actual=process_info(self.pid)
        assert actual and safe_path(actual['ExecutablePath'])==safe_path(BIN/'postgres.exe')
        assert str(safe_path(self.data)).lower() in actual['CommandLine'].replace('/','\\').lower(), 'POSTMASTER_COMMAND_MISMATCH'
        assert int((self.data/'postmaster.pid').read_text().splitlines()[0])==self.pid
        options=(self.data/'postmaster.opts').read_text()
        assert str(self.data).replace('\\','/').lower() in options.replace('\\','/').lower()
        if self.postmaster:
            assert actual==self.postmaster,'POSTMASTER_IDENTITY_CHANGED'
        else:
            self.postmaster=actual
        status=command([BIN/'pg_ctl.exe','-D',self.data,'status'],env=self.env,timeout=5)
        assert status.returncode==0 and str(self.pid) in status.stdout
        self.event('IDENTITY_VERIFIED',process=actual,sql=x,options=options)
        return x

    def register(self,proc,kind):
        """Registra apenas processos criados pelo próprio supervisor."""
        record=process_info(proc.pid)
        assert record and record['ParentProcessId']==os.getpid()
        assert str(RUN).lower() in record['CommandLine'].lower() or safe_path(record['ExecutablePath'])==safe_path(BIN/'psql.exe')
        if kind=='worker':
            self.workers_created+=1
            assert self.workers_created<=32, 'WORKER_INSTANCE_QUOTA'
        item={'kind':kind,'identity':record,'proc':proc}
        item['lifecycle']=OwnedProcess(proc,record,process_info,self.event,
            barrier=lambda: getattr(self.relay,'last_barrier',None),
            stderr=lambda: self.relay.stderr.snapshot() if self.relay and self.relay.proc is proc else {},
            controller_pid=os.getpid())
        self.registry.append(item)
        save('process-manifest.json',[{'kind':v['kind'],'identity':v['identity']} for v in self.registry])
        self.event('PROCESS_CREATED',process=record,role=kind)
        return item

    def terminate_own(self,proc):
        """Encerra pelo handle/PID próprio, somente após identidade atual coincidir."""
        item=next(v for v in self.registry if v['proc'] is proc)
        return item['lifecycle'].stop(timeout=5)

    def profile(self,label):
        """Registra parâmetros efetivos e relações logged, sem prometer hardware."""
        self.close_sessions()
        sql="SELECT json_object_agg(name,json_build_object('setting',setting,'unit',unit,'source',source)) FROM pg_settings WHERE name IN ('server_version','fsync','synchronous_commit','full_page_writes','wal_level','wal_segment_size','min_wal_size','max_wal_size','wal_sync_method','wal_buffers','wal_writer_delay','wal_compression','archive_mode','checkpoint_timeout','checkpoint_completion_target','checkpoint_warning','shared_buffers','max_connections','default_transaction_isolation','statement_timeout','lock_timeout','idle_in_transaction_session_timeout','transaction_timeout');"
        data=json.loads(self.admin(sql))
        assert all(data[n]['setting']=='on' for n in ('fsync','synchronous_commit','full_page_writes'))
        rel=json.loads(self.admin("SELECT json_agg(json_build_object('name',relname,'persistence',relpersistence)) FROM pg_class JOIN pg_namespace n ON n.oid=relnamespace WHERE n.nspname='lab' AND relkind='r';"))
        assert all(r['persistence']=='p' for r in rel)
        save('profile-'+label+'.json',{'settings':data,'relations':rel,'filesystem':'NTFS','physical_cache_verified':False})

    def stop_db(self,mode):
        """Interrompe somente cluster comprovado; immediate não recebe fallback."""
        assert mode in ('immediate','fast')
        self.identity(DB)
        if mode=='immediate': self.check()
        if mode=='immediate':
            self.crashes+=1
            assert self.crashes<=2,'CRASH_QUOTA'
        pid=self.pid
        ps=f"@(Get-CimInstance Win32_Process -Filter 'ParentProcessId={pid}' | Select-Object ProcessId,ParentProcessId,CommandLine) | ConvertTo-Json -Compress"
        children=command(['powershell','-NoProfile','-Command',ps],timeout=8)
        assert children.returncode==0
        descendants=json.loads(children.stdout) if children.stdout.strip() else []
        if isinstance(descendants,dict): descendants=[descendants]
        self.event('DB_STOP_DISPATCHED',mode=mode,postmaster=self.postmaster,children=descendants)
        r=command([BIN/'pg_ctl.exe','-D',self.data,'-m',mode,'-w','-t','20','stop'],env=self.env,timeout=23)
        assert r.returncode==0, 'DB_STOP_FAILED_NO_FALLBACK'
        assert not (self.data/'postmaster.pid').exists()
        assert not listeners(55433) and process_info(pid) is None
        for child in descendants: assert process_info(child['ProcessId']) is None,'CHILD_REMAINS'
        assert command([BIN/'pg_ctl.exe','-D',self.data,'status'],env=self.env,timeout=5).returncode==3
        self.started=False
        self.event('DB_STOPPED',mode=mode,pid=pid,pid_absent=True,children_absent=True,stdout=r.stdout)
        self.postmaster=None

    def recover(self,label,crash):
        """Start no mesmo data, sem initdb, preservando log e estado anterior."""
        assert not self.started and not (self.data/'postmaster.pid').exists()
        assert not listeners(55433)
        self.restarts+=1
        assert self.restarts<=4,'RESTART_QUOTA'
        log=EVIDENCE/'postgres.log'
        offset=log.stat().st_size
        with (EVIDENCE/('pg-start-'+label+'.txt')).open('w',encoding='utf8') as f:
            r=subprocess.run([str(BIN/'pg_ctl.exe'),'-D',str(self.data),'-l',str(log),'-w','-t','45','start'],stdout=f,stderr=subprocess.STDOUT,env=self.env,timeout=48,creationflags=HIDDEN)
        if (self.data/'postmaster.pid').exists():
            self.pid=int((self.data/'postmaster.pid').read_text().splitlines()[0]); self.started=True
        assert r.returncode==0 and self.started,'RECOVERY_START_FAILED'
        identity=self.identity(DB)
        newlog=log.read_bytes()[offset:].decode('utf8',errors='replace')
        if crash: assert 'redo starts' in newlog and 'redo done' in newlog,'CRASH_RECOVERY_NOT_PROVEN'
        save('recovery-'+label+'.json',{'kind':'CRASH_RECOVERY' if crash else 'CLEAN_RESTART','identity':identity,'log_offset':offset,'log':newlog,'restart_number':self.restarts})
        self.profile(label)
        self.event('DB_RECOVERED',label=label,crash=crash,pid=self.pid)

    def finish(self):
        """Parada e descarte próprios com comprovação; preserva recursos na dúvida."""
        self.scenario_start=None
        self.monitor_stop.set()
        cleanup_errors=[]
        if self.relay:
            try: self.relay.close()
            except Exception as error:
                cleanup_errors.append(str(error))
                self.event('RELAY_CLEANUP_ERROR',error=str(error))
                # Falha diagnóstica não autoriza outro kill. A parada do cluster
                # continua pelo caminho normal e só há descarte com todos ausentes.
        self.close_sessions()
        if self.started: self.stop_db('fast')
        assert not listeners(55433) and not listeners(55434)
        assert all(v['proc'].poll() is not None for v in self.registry),'OWN_PROCESS_REMAINS'
        for v in self.registry: assert process_info(v['identity']['ProcessId']) is None,'PID_REUSED_OR_REMAINS'
        assert not any(v['lifecycle'].blocked for v in self.registry),'IDENTITY_DOUBT_PRESERVE_RESOURCES'
        discarded=[]
        assert not (self.data/'postmaster.pid').exists()
        for target in (self.data,self.secret_dir):
            if target.exists():
                actual=safe_path(target)
                assert actual.parent==safe_path(RUN) and actual.name in ('data','secrets')
                for p in target.rglob('*'): safe_path(p)
                shutil.rmtree(actual)
                discarded.append(actual.name)
        save('shutdown.json',{'run_id':RUN.name,'stopped':True,'discarded':discarded,'all_registered_processes_absent':True,'listeners':[],'crashes':self.crashes,'restarts':self.restarts,'relay_instances':self.relays_created,'worker_instances':self.workers_created,'peak_bytes':self.peak_bytes,'cleanup_errors':cleanup_errors})
        if cleanup_errors: raise RuntimeError('RELAY_DIAGNOSTIC_FAILURE_PRESERVED_AFTER_SAFE_SHUTDOWN')


class Worker(Session):
    """Cliente psql como processo worker; conexão nunca passa por serviço existente."""
    def __init__(self,cluster,label,relay=False):
        self.use_relay=relay
        # Mantém o argv fixo por instância, sem modificar o cluster globalmente.
        self.cluster,self.label=cluster,label
        assert len(cluster.sessions)<3,'WORKER_CONCURRENCY_QUOTA'
        assert cluster.workers_created<32,'WORKER_INSTANCE_QUOTA'
        self.lines=queue.Queue()
        args=cluster.argv(WRITER,DB)
        if relay: args[args.index('-p')+1]='55434'
        self.proc=subprocess.Popen(args,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf8',bufsize=1,env=cluster.env,creationflags=HIDDEN)
        cluster.register(self.proc,'worker')
        threading.Thread(target=self._read,daemon=True).start()
        cluster.sessions.append(self)
        self.pid=int(self.query('SELECT pg_backend_pid()')['data'][0])
        settings=self.value("SELECT json_build_object('fsync',current_setting('fsync'),'synchronous_commit',current_setting('synchronous_commit'),'isolation',current_setting('transaction_isolation')); ")
        assert settings['fsync']=='on' and settings['synchronous_commit']=='on'
        cluster.event('WORKER_READY',pid=self.proc.pid,backend=self.pid,label=label,settings=settings)

    def query(self,sql):
        self.cluster.check()
        return super().query(sql)

    def dispatch_commit(self):
        """Envia COMMIT sem ler confirmação; só se usa com relay já bloqueado."""
        self.proc.stdin.write('COMMIT;\n'); self.proc.stdin.flush()
        return self.cluster.event('COMMIT_DISPATCHED',pid=self.proc.pid,backend=self.pid)

    def kill(self):
        self.cluster.terminate_own(self.proc)
        if self in self.cluster.sessions: self.cluster.sessions.remove(self)

    def close(self):
        if self.proc.poll() is None:
            self.proc.stdin.write('ROLLBACK;\n\\q\n'); self.proc.stdin.flush()
            self.proc.wait(timeout=6)
        if self in self.cluster.sessions: self.cluster.sessions.remove(self)


class Relay:
    """Controla proxy independente, sem registrar dados de autenticação ou SQL."""
    def __init__(self,lab):
        assert lab.relay is None
        assert lab.relays_created < 2, 'RELAY_INSTANCE_QUOTA'
        assert not listeners(55434), 'RELAY_PORT_CONFLICT'
        lab.relays_created += 1
        self.lab=lab
        self.proc=subprocess.Popen([sys.executable,str(RUN/'scripts/relay.py'),RUN.name],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,encoding='utf8',bufsize=1,creationflags=HIDDEN)
        self.closed=False
        self.last_barrier=None
        self.stderr=StderrCapture(self.proc.stderr,lab.event)
        self.stderr_thread=threading.Thread(target=self.stderr.drain,daemon=True)
        self.stderr_thread.start()
        lab.register(self.proc,'relay')
        self.lines=queue.Queue()
        def read():
            for line in self.proc.stdout:
                try:
                    value=json.loads(line)
                except ValueError as exc:
                    lab.event('RELAY_PROTOCOL_ERROR',error=type(exc).__name__)
                    self.lines.put({'event':'RELAY_PROTOCOL_ERROR'})
                    return
                self.last_barrier=value
                lab.event('RELAY_WIRE_EVENT',metadata=value)
                self.lines.put(value)
        self.stdout_thread=threading.Thread(target=read,daemon=True)
        self.stdout_thread.start()
        x=self.lines.get(timeout=5)
        assert x['event']=='LISTENING'
        found=listeners(55434)
        assert len(found)==1 and found[0]['OwningProcess']==self.proc.pid and found[0]['LocalAddress']=='127.0.0.1'
        lab.relay=self

    def control(self,cmd):
        self.proc.stdin.write(cmd+'\n'); self.proc.stdin.flush()
        x=self.lines.get(timeout=5)
        assert x['event']==cmd.upper(),x
        self.lab.event('RELAY_'+cmd.upper(),pid=self.proc.pid,metadata=x)
        return x

    def close(self):
        if self.closed: return
        record=self.lab.terminate_own(self.proc)
        self.stdout_thread.join(timeout=2)
        if self.stdout_thread.is_alive(): raise RuntimeError('STDOUT_CAPTURE_TIMEOUT')
        self.stderr_thread.join(timeout=2)
        if self.stderr_thread.is_alive(): raise RuntimeError('STDERR_CAPTURE_TIMEOUT')
        self.lab.event('RELAY_FINAL_DIAGNOSTIC',process=record,last_tcp_barrier=self.last_barrier,**self.stderr.snapshot())
        if record['exit_code'] not in (0,None) and not record.get('terminated_by_controller'):
            raise RuntimeError('RELAY_NONZERO_EXIT_REQUIRES_DIAGNOSIS')
        if not record.get('terminated_by_controller') and not (
                self.last_barrier and self.last_barrier.get('event')=='EOF' and
                self.last_barrier.get('source')=='client'):
            raise RuntimeError('RELAY_UNEXPECTED_EXIT')
        assert not listeners(55434)
        self.closed=True
        self.lab.relay=None
