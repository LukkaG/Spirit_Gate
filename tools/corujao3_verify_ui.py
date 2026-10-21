"""Executa probes reais em sequência, preservando logs e interrompendo na primeira falha."""
from pathlib import Path
import json
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
destination = ROOT/sys.argv[1]
destination.mkdir(parents=True, exist_ok=False)
classes = 'server/target/classes;server/target/test-classes'
names = ['SpiritGateSelectionVisualProbe','SpiritGateCorujao3Probe','SpiritGateWindowsProbe','SpiritGateCreationProbe','SpiritGateTradeProbe','SpiritGateOptionsProbe','SpiritGatePerformanceProbe','SpiritGateFinalJourneyProbe']
command = ['javac','-encoding','UTF-8','-cp',classes,'-d','server/target/test-classes']+[f'tools/{name}.java' for name in names]
result = subprocess.run(command,cwd=ROOT,capture_output=True,text=True,errors='replace',timeout=120)
(destination/'compile.log').write_text(result.stdout+result.stderr,encoding='utf-8')
if result.returncode:raise SystemExit(result.returncode)
summary = []
for name,scale in [('SpiritGateCreationProbe',None),('SpiritGateCorujao3Probe',None),('SpiritGateWindowsProbe',None),('SpiritGateWindowsProbe','1.5'),('SpiritGateTradeProbe',None),('SpiritGateOptionsProbe',None),('SpiritGateFinalJourneyProbe',None),('SpiritGatePerformanceProbe',None)]:
    folder = destination/(name+('-dpi150' if scale else ''))
    print('RUN',folder.name,flush=True)
    command = ['java']+(['-Dsun.java2d.uiScale='+scale] if scale else [])+['-cp',classes,'com.projetok.prototype.rpg.'+name,str(folder)]
    result = subprocess.run(command,cwd=ROOT,capture_output=True,text=True,errors='replace',timeout=120)
    (destination/(folder.name+'.log')).write_text(result.stdout+result.stderr,encoding='utf-8')
    summary.append({'probe':name,'java2dScale':scale or '1','exitCode':result.returncode,'folder':folder.relative_to(ROOT).as_posix()})
    (destination/'summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
    if result.returncode:raise SystemExit(result.returncode)
print('PASS',len(summary),'probes',flush=True)
