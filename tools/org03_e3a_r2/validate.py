"""Valida integração R2 e preservação, sem abrir recursos experimentais."""
import ast
import difflib
import hashlib
import json
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902'
EV=RUN/'evidence'


def sha(path):
    """Hash de um arquivo, sem escrita no alvo."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    """Confere originais e executa os 23 testes puros nas cópias integradas."""
    before=json.loads((EV/'preservation-before.json').read_text(encoding='utf8'))
    changed=[p for p,h in before.items() if not (ROOT/p).is_file() or sha(ROOT/p)!=h]
    scripts=RUN/'scripts'
    for p in scripts.glob('*.py'): ast.parse(p.read_text(encoding='utf8'))
    result=subprocess.run([sys.executable,'-B','-m','unittest','discover','-s',str(scripts),'-p','test_harness.py','-v'],capture_output=True,text=True,encoding='utf8',timeout=30,creationflags=subprocess.CREATE_NO_WINDOW)
    (EV/'test-output.txt').write_text(result.stdout+result.stderr,encoding='utf8')
    diff=[]
    old=ROOT/'temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/scripts'
    r1=ROOT/'tools/org03_e3a_r1'
    for p in sorted(scripts.iterdir()):
        if not p.is_file(): continue
        source=r1/p.name if (r1/p.name).exists() else old/p.name
        diff.extend(difflib.unified_diff(source.read_text(encoding='utf8').splitlines(True),p.read_text(encoding='utf8').splitlines(True),fromfile=str(source.relative_to(ROOT)),tofile=str(p.relative_to(ROOT))))
    (EV/'integration.diff').write_text(''.join(diff),encoding='utf8')
    (EV/'prepared-source-sha256.json').write_text(json.dumps({p.name:sha(p) for p in scripts.iterdir() if p.is_file()},indent=2),encoding='utf8')
    report=dict(preserved_files=len(before),changed=changed,syntax='PASSOU',unit_exit=result.returncode,tests=23,passed=not changed and result.returncode==0,cluster_created=False)
    (EV/'preparation-validation.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    print(json.dumps(report))
    return 0 if report['passed'] else 1


if __name__=='__main__': sys.exit(main())
