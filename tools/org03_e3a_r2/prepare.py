"""Prepara cópias R2 sem executar banco, relay ou workers."""
import ast
import difflib
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RUN = ROOT / 'temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902'
OLD = ROOT / 'temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/scripts'
R1 = ROOT / 'tools/org03_e3a_r1'


def main():
    """Cria uma única preparação; não sobrescreve nem reutiliza run existente."""
    assert not RUN.exists(), 'RUN_ALREADY_EXISTS'
    (RUN / 'scripts').mkdir(parents=True)
    ev = RUN / 'evidence'
    ev.mkdir()
    preserved = json.loads((R1/'preservation-before.json').read_text(encoding='utf8'))
    paths = {ROOT/p for p in preserved}
    for tree in (ROOT/'temp/org03-e2', ROOT/'temp/org03-e3a', R1):
        paths.update(p for p in tree.rglob('*') if p.is_file() and RUN not in p.parents)
    paths.update((ROOT/'docs').glob('ORG-03-E*.md'))
    before = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
              for p in sorted(paths) if p.is_file()}
    (ev/'preservation-before.json').write_text(json.dumps(before,indent=2),encoding='utf8')
    for name in ('runtime.py','tests.py','schema.sql'):
        (RUN/'scripts'/name).write_bytes((OLD/name).read_bytes())
    for name in ('lab_runtime.py','relay.py','lifecycle.py','test_harness.py'):
        (RUN/'scripts'/name).write_bytes((R1/name).read_bytes())

    path = RUN/'scripts/relay.py'
    src = path.read_text(encoding='utf8')
    start = src.index('def main():')
    src = src[:start] + '''def main():
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
'''
    path.write_text(src,encoding='utf8')

    path = RUN/'scripts/lab_runtime.py'
    src = path.read_text(encoding='utf8')
    src = src.replace('self.events = []', 'self.events = []\n        self.event_lock = threading.Lock()\n        self.relays_created = 0')
    a = src.index('        record = dict(sequence=')
    b = src.index('\n    def preflight', a)
    body = src[a:b]
    src = src[:a] + '        with self.event_lock:\n' + ''.join('    '+line+'\n' for line in body.rstrip().splitlines()) + src[b:]
    src = src.replace("assert lab.relay is None", "assert lab.relay is None\n        assert lab.relays_created < 2, 'RELAY_INSTANCE_QUOTA'\n        assert not listeners(55434), 'RELAY_PORT_CONFLICT'\n        lab.relays_created += 1")
    # Drena os dois pipes antes de avaliar EOF; preserva erro/exit não zero como falha.
    src = src.replace("threading.Thread(target=read,daemon=True).start()", "self.stdout_thread=threading.Thread(target=read,daemon=True)\n        self.stdout_thread.start()")
    src = src.replace("self.stderr_thread.join(timeout=2)", "self.stdout_thread.join(timeout=2)\n        if self.stdout_thread.is_alive(): raise RuntimeError('STDOUT_CAPTURE_TIMEOUT')\n        self.stderr_thread.join(timeout=2)")
    src = src.replace("'worker_instances':self.workers_created", "'relay_instances':self.relays_created,'worker_instances':self.workers_created")
    # CLI de C09 não depende do relay; operação separada e fronteira própria.
    path.write_text(src,encoding='utf8')

    path = RUN/'scripts/tests.py'
    src = path.read_text(encoding='utf8').replace("date='2026-11-14'", "date='2026-11-15'")
    src = src.replace("trials.append(self.lost('C09-uncertain',False))", '''i=self.intent('C09-uncertain',kind='potion',xp=0)
        before,prepared=self.prepared(i)
        dispatch=self.w.dispatch_commit()
        durable(EVIDENCE/'C09-uncertain-boundary.json',dict(status='UNCERTAIN',last_boundary='COMMIT_DISPATCHED',op=i['op']))
        backend=self.w.pid; old=self.w.proc.pid
        self.w.kill(); self.wait_gone(backend); self.replace('C09-uncertain-restarted')
        lookup=self.lookup(self.load(i))
        if lookup['status']=='CONFIRMED':
            self.retry_confirmed(i)
            assert self.image()['head']['er']==before[0]['image']['head']['er']+1
        else:
            assert lookup['status']=='NOT_FOUND' and self.snapshot()==before
        trials.append(dict(intent=i,last_boundary='COMMIT_DISPATCHED',caller_status='UNCERTAIN',prepared=prepared,dispatch=dispatch,old_pid=old,new_pid=self.w.proc.pid,reconciliation=lookup,before=before,after=self.snapshot(),ack_consumed_by_caller=False))''')
    # Fonte R1 requer double-close com join; teste inerte recebe thread simulada.
    path.write_text(src,encoding='utf8')
    path=RUN/'scripts/test_harness.py'
    src=path.read_text(encoding='utf8').replace('relay.stderr_thread = Mock()', 'relay.stdout_thread = Mock()\n        relay.stdout_thread.is_alive.return_value = False\n        relay.stderr_thread = Mock()')
    path.write_text(src,encoding='utf8')

    path=RUN/'scripts/runtime.py'
    src=path.read_text(encoding='utf8')
    src=src.replace("with socket.socket() as s: s.bind(('127.0.0.1',55433))", "with socket.socket() as s:\n            s.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)\n            s.bind(('127.0.0.1',55433))")
    src=src.replace("# Segunda checagem imediatamente antes de bind; nenhum fallback é permitido.\n        assert not listener(),'PORT_CONFLICT'", "# Segunda checagem imediatamente antes de bind; nenhum fallback é permitido.\n        assert not listener(),'PORT_CONFLICT'\n        with socket.socket() as s:\n            s.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)\n            s.bind(('127.0.0.1',55433))")
    src=src.replace("'run':str(RUN),'data':str(self.data),'links':False", "'run':str(RUN),'data':str(self.data),'links':False,'neutralized_pg_names':[k for k in os.environ if k.upper().startswith('PG')]")
    src=src.replace("assert acl.returncode==0, 'ACL_FAILED'", """assert acl.returncode==0, 'ACL_FAILED'
        ps = \"$acl=Get-Acl -LiteralPath '\"+str(self.secret_dir)+\"'; [pscustomobject]@{protected=$acl.AreAccessRulesProtected;owner=$acl.Owner;rules=@($acl.Access | ForEach-Object {[pscustomobject]@{sid=$_.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value;type=$_.AccessControlType.ToString();rights=$_.FileSystemRights.ToString();inherited=$_.IsInherited}})} | ConvertTo-Json -Depth 6 -Compress\"
        result=command(['powershell','-NoProfile','-Command',ps],timeout=8)
        assert result.returncode==0, 'ACL_INSPECTION_FAILED'
        observed=json.loads(result.stdout)
        assert observed['protected'] and observed['rules'], 'ACL_NOT_PROTECTED'
        assert all(r['sid']==sid and r['type']=='Allow' and not r['inherited'] and 'FullControl' in r['rights'] for r in observed['rules']), 'ACL_NOT_EXCLUSIVE'
        save('secret-acl.json',dict(sid=sid,observed=observed))""")
    path.write_text(src,encoding='utf8')
    for p in (RUN/'scripts').glob('*.py'):
        ast.parse(p.read_text(encoding='utf8'),filename=str(p))
    diff=[]
    for p in sorted((RUN/'scripts').iterdir()):
        source=R1/p.name if (R1/p.name).exists() else OLD/p.name
        diff.extend(difflib.unified_diff(source.read_text(encoding='utf8').splitlines(True),p.read_text(encoding='utf8').splitlines(True),fromfile=str(source.relative_to(ROOT)),tofile=str(p.relative_to(ROOT))))
    (ev/'integration.diff').write_text(''.join(diff),encoding='utf8')
    (ev/'prepared-source-sha256.json').write_text(json.dumps({p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (RUN/'scripts').iterdir()},indent=2),encoding='utf8')
    print(str(RUN))


if __name__ == '__main__':
    main()
