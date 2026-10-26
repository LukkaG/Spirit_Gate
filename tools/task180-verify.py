"""Run compiled JUnit suite with real Swing, using the same cached dependencies."""
from pathlib import Path
import subprocess
import os
import sys
ROOT=Path(__file__).resolve().parents[1]
run=ROOT/sys.argv[1]
cp=os.pathsep.join([str(run/'classes')]+[str(p) for p in (run/'dependencies').iterdir()])
target=ROOT/'.task-checkpoints/TASK-180/live-publishing/gui-tests.log'
with target.open('w',encoding='utf-8') as log:
    result=subprocess.run(['java','-Djava.awt.headless=false','-cp',cp,'LocalJunitRunner'],cwd=ROOT/'server',stdout=log,stderr=subprocess.STDOUT)
print(target.read_text(encoding='utf-8')[-8000:])
raise SystemExit(result.returncode)
