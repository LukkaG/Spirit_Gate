"""Suíte combinada e evidências P1; SQL/cluster não são dependências."""
import ast
import difflib
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import uuid
from audit import HERE,ROOT,R2,verify,sha


def main():
    """Retém cada validação em diretório único; --with-fixture abre só loopback."""
    run=HERE/'evidence'/('validation-'+uuid.uuid4().hex[:12]); run.mkdir()
    for p in HERE.rglob('*.py'): ast.parse(p.read_text(encoding='utf8'),filename=str(p))
    units=[]
    for name,where,pattern in [('legacy',HERE/'legacy','test_harness.py'),('p1',HERE,'test_transport_fix.py')]:
        result=subprocess.run([sys.executable,'-B','-m','unittest','discover','-s',str(where),'-p',pattern,'-v'],cwd=ROOT,capture_output=True,text=True,encoding='utf8',timeout=30,creationflags=subprocess.CREATE_NO_WINDOW)
        (run/(name+'-tests.txt')).write_text(result.stdout+result.stderr,encoding='utf8')
        import re
        count=re.search(r'Ran (\d+) tests',result.stderr)
        units.append(dict(suite=name,tests=int(count.group(1)) if count else 0,exit_code=result.returncode,status='PASSOU' if result.returncode==0 else 'FALHOU'))
    fixture=dict(status='NAO_EXECUTADO')
    if '--with-fixture' in sys.argv[1:]:
        result=subprocess.run([sys.executable,'-B',str(HERE/'fixture.py')],cwd=ROOT,capture_output=True,text=True,encoding='utf8',timeout=60,creationflags=subprocess.CREATE_NO_WINDOW)
        (run/'fixture-output.txt').write_text(result.stdout+result.stderr,encoding='utf8')
        fixture=dict(status='PASSOU' if result.returncode==0 else 'FALHOU',exit_code=result.returncode)
        if result.returncode==0: fixture.update(json.loads(result.stdout))
    preservation=verify()
    for name in ('lifecycle.py','relay.py','lab_runtime.py','test_harness.py'):
        if sha(HERE/'legacy'/name)!=sha(R2/'scripts'/name): raise RuntimeError('LEGACY_COPY_CHANGED')
    deltas=[]
    for name in ('lifecycle.py','relay.py'):
        deltas.extend(difflib.unified_diff((R2/'scripts'/name).read_text(encoding='utf8').splitlines(True),(HERE/name).read_text(encoding='utf8').splitlines(True),fromfile='R2/'+name,tofile='P1/'+name))
    (HERE/'evidence/transport.diff').write_text(''.join(deltas),encoding='utf8')
    history=[]
    for path in sorted((HERE/'evidence').glob('fixture-*/result.json')):
        value=json.loads(path.read_text(encoding='utf8'))
        history.append(dict(path=path.relative_to(HERE).as_posix(),status=value['report']['status'],case=value['report']['case'],ports=value['ports'],cleanup_errors=value['cleanup_errors'],listeners_after=value['listeners_after'],own_handles_exited=value['own_handles_exited']))
    report=dict(date='2026-11-15',units=units,total_tests=sum(u['tests'] for u in units),fixture=fixture,fixture_history=history,preservation=preservation,postgresql=False,sql=False,cluster_created=False,crashes=0,e3_gate_approved=False,
                passed=all(u['exit_code']==0 for u in units) and fixture['status']!='FALHOU' and preservation['passed'])
    (run/'result.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    (HERE/'evidence/validation.json').write_text(json.dumps(dict(validation_path=run.relative_to(HERE).as_posix(),**report),indent=2),encoding='utf8')
    artifacts=[p for p in HERE.rglob('*') if p.is_file() and p.name!='artifact-sha256.json']
    artifacts+=list((ROOT/'docs').glob('ORG-03-E3-A-R2-P1*.md'))+list((ROOT/'docs').glob('ORG-03-E3-A-R3-PROPOSTA-EXECUCAO.md'))
    (HERE/'evidence/artifact-sha256.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in artifacts},indent=2),encoding='utf8')
    print(json.dumps(report,ensure_ascii=False))
    return 0 if report['passed'] else 1


if __name__=='__main__': sys.exit(main())
