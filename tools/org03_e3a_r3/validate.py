"""Validação R3-01: Python puro, rede/processos operacionais proibidos nos testes."""
import ast
import difflib
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import uuid
from prepare import ROOT,RUN,R2,P1


def sha(path):
    """Hash do arquivo sem importar/executar seu conteúdo."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def preservation():
    """Compara todo o baseline e registra os hashes posteriores completos."""
    ev=RUN/'preparation'
    before=json.loads((ev/'preservation-before.json').read_text(encoding='utf8'))
    after={name:sha(ROOT/name) if (ROOT/name).is_file() else None for name in before}
    changed=[name for name in before if before[name]!=after[name]]
    (ev/'preservation-after.json').write_text(json.dumps(after,indent=2),encoding='utf8')
    return dict(files=len(before),changed=changed,passed=not changed)


def seal():
    """Hashes de preparação/entrega; nunca cria claim ou autorização operacional."""
    ev=RUN/'preparation'
    docs=list((ROOT/'docs').glob('ORG-03-E3-A-R3-01*.md'))
    files=[p for p in RUN.rglob('*') if p.is_file() and p.name!='artifact-sha256.json' and '__pycache__' not in p.parts]
    files+=list(Path(__file__).parent.glob('*.py'))+docs
    analysis=ROOT/'logs/analises/20261115-org03-e3a-r3-01.md'
    if analysis.is_file(): files.append(analysis)
    manifest={p.relative_to(ROOT).as_posix():sha(p) for p in files}
    (ev/'artifact-sha256.json').write_text(json.dumps(manifest,indent=2),encoding='utf8')
    return manifest


def main():
    """Três suítes em processos Python separados; cada uma proíbe rede e subprocessos."""
    ev=RUN/'preparation'; scripts=RUN/'scripts'
    if '--seal-only' in sys.argv[1:]:
        check=preservation(); assert check['passed']
        manifest=seal(); print(json.dumps(dict(preservation=check,artifacts=len(manifest)))); return 0
    attempt=ev/('validation-'+uuid.uuid4().hex[:12]); attempt.mkdir()
    ast_files=[]
    for p in sorted(scripts.rglob('*.py')):
        ast.parse(p.read_text(encoding='utf8'),filename=str(p)); ast_files.append(p.relative_to(scripts).as_posix())
    results=[]
    for suite in ('legacy','p1','integration'):
        value=subprocess.run([sys.executable,'-B',str(Path(__file__).with_name('safe_tests.py')),suite],cwd=ROOT,capture_output=True,text=True,encoding='utf8',timeout=30,creationflags=subprocess.CREATE_NO_WINDOW)
        output=value.stdout+value.stderr
        (attempt/(suite+'.txt')).write_text(output,encoding='utf8')
        count=re.search(r'Ran (\d+) tests',output)
        results.append(dict(suite=suite,tests=int(count.group(1)) if count else 0,exit_code=value.returncode,status='PASSOU' if value.returncode==0 else 'FALHOU'))
    origins={}
    delta=[]
    mapping={'runtime_base.py':R2/'runtime.py','lab_base.py':R2/'lab_runtime.py','runner_base.py':R2/'tests.py'}
    mapping.update({name:P1/name for name in ('transport.py','relay.py','lifecycle.py','c02_integration.py','test_transport_fix.py')})
    mapping['schema.sql']=R2/'schema.sql'
    for p in sorted(scripts.rglob('*')):
        if not p.is_file(): continue
        name=p.relative_to(scripts).as_posix()
        source=P1/name if name.startswith('legacy/') else mapping.get(name)
        origins[name]=dict(origin=source.relative_to(ROOT).as_posix() if source else 'NEW_R3',sha256=sha(p),identical=sha(source)==sha(p) if source else False)
        previous=source.read_text(encoding='utf8') if source else ''
        delta.extend(difflib.unified_diff(previous.splitlines(True),p.read_text(encoding='utf8').splitlines(True),fromfile=origins[name]['origin'],tofile='R3/'+name))
    (ev/'source-origins.json').write_text(json.dumps(origins,indent=2),encoding='utf8')
    (ev/'integration.diff').write_text(''.join(delta),encoding='utf8')
    hashes={p.relative_to(scripts).as_posix():sha(p) for p in scripts.rglob('*') if p.is_file()}
    (ev/'source-sha256.json').write_text(json.dumps(hashes,indent=2),encoding='utf8')
    check=preservation()
    forbidden=[name for name in ('data','secrets','execution.claim','evidence','operational-authorization.json') if (RUN/name).exists()]
    expected_counts=[23,48,46]
    report=dict(run_id=RUN.name,validation_path=attempt.relative_to(RUN).as_posix(),syntax_files=ast_files,
                suites=results,total_tests=sum(x['tests'] for x in results),preservation=check,forbidden_resources_present=forbidden,
                network_calls='FORBIDDEN_IN_TEST_PROCESS',operational_subprocesses='FORBIDDEN_IN_TEST_PROCESS',
                operational_execution=False,postgresql=False,sql=False,fixtures_real='NAO_EXECUTADO',crashes=0,
                c01_c09_postgresql='NAO_EXECUTADO',e3_gate_approved=False,
                passed=all(x['exit_code']==0 for x in results) and [x['tests'] for x in results]==expected_counts and check['passed'] and not forbidden)
    (attempt/'result.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    (ev/'validation.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    seal()
    print(json.dumps(report,ensure_ascii=False))
    return 0 if report['passed'] else 1


if __name__=='__main__': raise SystemExit(main())
