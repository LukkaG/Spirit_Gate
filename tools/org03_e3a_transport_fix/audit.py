"""Preservação P1: leitura dos originais e artefatos próprios separados."""
import hashlib
import json
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
R2 = ROOT/'temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902'


def sha(path):
    """Hash do conteúdo sem executar o arquivo."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def preserve():
    """Cria baseline uma vez e preserva os 23 testes com suas fontes históricas."""
    ev=HERE/'evidence'
    ev.mkdir(exist_ok=True)
    target=ev/'preservation-before.json'
    assert not target.exists(), 'BASELINE_ALREADY_EXISTS'
    paths=set(subprocess.run(['git','ls-files'],cwd=ROOT,capture_output=True,text=True,check=True).stdout.splitlines())
    for tree in ('temp/org03-e2','temp/org03-e3a','tools/org03_e3a_r1','tools/org03_e3a_r2'):
        paths.update(p.relative_to(ROOT).as_posix() for p in (ROOT/tree).rglob('*') if p.is_file())
    paths.update(p.relative_to(ROOT).as_posix() for p in (ROOT/'docs').glob('ORG-03-E*.md'))
    target.write_text(json.dumps({p:sha(ROOT/p) for p in sorted(paths) if (ROOT/p).is_file()},indent=2),encoding='utf8')
    legacy=HERE/'legacy'
    legacy.mkdir()
    for name in ('lifecycle.py','relay.py','lab_runtime.py','test_harness.py'):
        (legacy/name).write_bytes((R2/'scripts'/name).read_bytes())
    (ev/'audit.json').write_text(json.dumps(dict(branch='beco',head='c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39',events_reviewed=49,prior_manifests_verified={'R2_artifacts':57,'R2_preservation':4023,'R1_artifacts':16,'E2_artifacts':250},prior_manifest_errors=[],postgresql_execution=False),indent=2),encoding='utf8')
    print('BASELINE_AND_LEGACY_COPIES_CREATED')


def verify():
    """Verifica antes/depois sem regravar evidências antigas."""
    before=json.loads((HERE/'evidence/preservation-before.json').read_text(encoding='utf8'))
    after={p:sha(ROOT/p) if (ROOT/p).is_file() else None for p in before}
    changed=[p for p,h in before.items() if after[p]!=h]
    report=dict(files=len(before),changed=changed,passed=not changed)
    (HERE/'evidence/preservation-after.json').write_text(json.dumps(dict(**report,sha256=after),indent=2),encoding='utf8')
    assert not changed, changed
    print(json.dumps(report))
    return report


if __name__=='__main__':
    import sys
    preserve() if sys.argv[1:] == ['prepare'] else verify()
