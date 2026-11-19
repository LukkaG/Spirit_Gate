"""Integra cópias R3-01; nenhuma ação operacional ou socket nesta preparação."""
import ast
import hashlib
import json
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70'
R2=ROOT/'temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/scripts'
P1=ROOT/'tools/org03_e3a_transport_fix'


def main():
    """Uso único de preparação; originais são somente lidos e hashados."""
    assert not RUN.exists(),'RUN_ALREADY_EXISTS'
    scripts=RUN/'scripts'; scripts.mkdir(parents=True)
    ev=RUN/'preparation'; ev.mkdir()
    paths=set(subprocess.run(['git','ls-files'],cwd=ROOT,capture_output=True,text=True,check=True).stdout.splitlines())
    for tree in ('temp/org03-e2','temp/org03-e3a','tools/org03_e3a_r1','tools/org03_e3a_r2','tools/org03_e3a_transport_fix'):
        paths.update(p.relative_to(ROOT).as_posix() for p in (ROOT/tree).rglob('*') if p.is_file() and RUN not in p.parents)
    paths.update(p.relative_to(ROOT).as_posix() for p in (ROOT/'docs').glob('ORG-03-E*.md'))
    hashes={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in sorted(paths) if (ROOT/p).is_file()}
    (ev/'preservation-before.json').write_text(json.dumps(hashes,indent=2),encoding='utf8')
    for name in ('runtime.py','lab_runtime.py','tests.py'):
        target={'runtime.py':'runtime_base.py','lab_runtime.py':'lab_base.py','tests.py':'runner_base.py'}[name]
        source=(R2/name).read_text(encoding='utf8').replace('55433','55435').replace('55434','55436')
        (scripts/target).write_text(source,encoding='utf8')
    (scripts/'schema.sql').write_bytes((R2/'schema.sql').read_bytes())
    for name in ('lifecycle.py','transport.py','relay.py','c02_integration.py','test_transport_fix.py'):
        (scripts/name).write_bytes((P1/name).read_bytes())
    (scripts/'legacy').mkdir()
    for p in (P1/'legacy').glob('*.py'): (scripts/'legacy'/p.name).write_bytes(p.read_bytes())
    # Guardas de autorização antes dos caminhos herdados que podem tocar recursos.
    p=scripts/'runtime_base.py'; source=p.read_text(encoding='utf8')
    source=source.replace('import concurrent.futures','from config import require_operational\nimport concurrent.futures')
    source=source.replace('    return subprocess.run([str(x) for x in args]', '    require_operational(claim=True)\n    return subprocess.run([str(x) for x in args]')
    tree=ast.parse(source); lines=source.splitlines(True)
    inserts=[]
    for node in ast.walk(tree):
        if isinstance(node,ast.ClassDef) and node.name=='Cluster':
            for method in node.body:
                if isinstance(method,ast.FunctionDef) and method.name in ('start','preflight','admin','identity','argv','stop'):
                    first=method.body[1] if isinstance(method.body[0],ast.Expr) and isinstance(method.body[0].value,ast.Constant) else method.body[0]
                    inserts.append(first.lineno-1)
    for index in sorted(inserts,reverse=True): lines.insert(index,'        require_operational(claim=True)\n')
    source=''.join(lines)
    # O Session base fica disponível apenas como referência; o novo Worker é binário.
    source=source.replace("        assert len(cluster.sessions)<3,'SESSION_QUOTA'", "        require_operational(claim=True)\n        assert len(cluster.sessions)<3,'SESSION_QUOTA'")
    (scripts/'runtime_base.py').write_text(source,encoding='utf8')
    p=scripts/'lab_base.py'; source=p.read_text(encoding='utf8')
    source=source.replace('from lifecycle import OwnedProcess, StderrCapture','from lifecycle import OwnedProcess, StderrCapture\nfrom config import require_operational')
    # Classes legadas Worker/Relay não ficam como caminhos alternativos ativos.
    source=source[:source.index('\nclass Worker(Session):')]
    source=source.replace("        auth=json.loads((EVIDENCE/'authorization.json').read_text(encoding='utf8'))", "        require_operational(claim=True)\n        auth=json.loads((RUN/'operational-authorization.json').read_text(encoding='utf8'))")
    for method in ('stop_db','recover','finish'):
        tree=ast.parse(source); lines=source.splitlines(True)
        node=next(n for n in ast.walk(tree) if isinstance(n,ast.FunctionDef) and n.name==method)
        lines.insert(node.body[1].lineno-1,'        require_operational(claim=True)\n'); source=''.join(lines)
    (scripts/'lab_base.py').write_text(source,encoding='utf8')
    p=scripts/'runner_base.py'; source=p.read_text(encoding='utf8')
    source=source[:source.index('\ndef main():')]
    source=source.replace("status='UNCERTAIN'", "status='ECONOMIC_UNCERTAIN'")
    (scripts/'runner_base.py').write_text(source,encoding='utf8')
    for p in scripts.rglob('*.py'): ast.parse(p.read_text(encoding='utf8'))
    (RUN/'preparation-authorization.json').write_text(json.dumps(dict(responsavel='Lucas',date='2026-11-15',run_id=RUN.name,items={'R3-01':'AUTORIZADO','R3-02':'PENDENTE','R3-03':'PENDENTE','R3-04':'PENDENTE','R3-05':'PENDENTE'},scope='PREPARATION_ONLY_NO_POSTGRESQL_SQL_SOCKETS'),indent=2),encoding='utf8')
    print(str(RUN))


if __name__=='__main__': main()
