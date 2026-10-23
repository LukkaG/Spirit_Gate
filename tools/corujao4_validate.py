"""Validação gráfica e capturas usando classes recém-compiladas do runner local."""
from pathlib import Path
import os
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]


def validate(task, capture=False, full=False):
    run=max((ROOT/'server/target/local-tests').glob('run-*'),key=lambda p:p.stat().st_mtime)
    output=ROOT/f'.task-checkpoints/TASK-{task}/corujao-iv'
    output.mkdir(parents=True,exist_ok=True)
    cp=os.pathsep.join([str(run/'classes')]+[str(p) for p in (run/'dependencies').iterdir()])
    if capture:
        subprocess.run(['javac','--release','17','-encoding','UTF-8','-cp',str(run/'classes'),'-d',str(run/'classes'),str(ROOT/'tools/StudioCapture.java')],check=True)
        subprocess.run(['java','-cp',str(run/'classes'),'com.projetok.studio.StudioCapture',str(output/'captures'),str(ROOT),'after'],check=True)
        if int(task)>=139:
            subprocess.run(['java','-cp',str(run/'classes'),'com.projetok.studio.StudioCapture',str(output/'captures'),str(ROOT),'quest'],check=True)
    p=subprocess.run(['java','-Djava.awt.headless=false','-cp',cp,'LocalJunitRunner','com.projetok' if full else 'com.projetok.studio'],cwd=ROOT/'server',stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8')
    (output/('full-gui-tests.log' if full else 'gui-tests.log')).write_text(p.stdout,encoding='utf-8')
    (output/'java-tests.log').write_bytes((run/'test-results.log').read_bytes())
    print(p.stdout)
    return p.returncode


if __name__=='__main__':
    raise SystemExit(validate(sys.argv[1], '--capture' in sys.argv, '--full' in sys.argv))
