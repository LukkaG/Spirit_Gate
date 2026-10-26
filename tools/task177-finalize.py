"""Record final evidence only after successful extracted-package smoke and GUI suite."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
CP=ROOT/'.task-checkpoints/TASK-177/network-collision'
metadata=ROOT/sys.argv[1]
packages=json.loads(metadata.read_text(encoding='utf-8'))
smoke=sorted(CP.glob(metadata.stem+'-smoke-*/result.json'))[-1]
result=json.loads(smoke.read_text(encoding='utf-8'))
assert result['zipAndManifestVerified'] and result['clientExitCodes']==[0,0] and result['serverExitCode']==0
log=(CP/'gui-tests.log').read_text(encoding='utf-8')
assert 'JUnit: 499 executed, 499 passed, 0 aborted, 0 failures, 0 skipped' in log
for package in packages:
    assert hashlib.sha256((ROOT/package['path']).read_bytes()).hexdigest()==package['sha256']
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT)==(CP/'initial-head.txt').read_bytes()
shutil.copyfile(ROOT/'server/target/local-tests/run-cht4w7xp/test-results.log',CP/'baseline-tests.log')
summary=dict(task='TASK-177',guiTests=dict(executed=499,passed=499,aborted=0,failures=0,log='gui-tests.log'),
             originalAssetsVerified=20,sourceDeltas='final-deltas.json',fileDiffs='file-diffs',
             packages=packages,extractedPackageSmoke=dict(path=str(smoke.relative_to(ROOT)),**result),
             evidence=dict(networkBefore='baseline.md',networkAfter='after-10hz-valid.txt',networkRepeat=['baseline-repeat.txt','after-repeat.txt'],
                           snapshot15Hz='after-15hz.txt',presentation='presentation.txt',spriteMeasurements='sprite-measurements.csv',
                           render=['baseline-render.txt','updated-render.txt','debug-render.txt'],debugCapture='debug-render.png'),
             limitations=['Radmin between two physical computers pending','Human visual approval pending','No full allocation/GC profile','Per-input ACK absent from historical protocol'],
             pushed=False,published=False,personalSavesModified=False)
(CP/'validation-summary.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
(CP/'RESULTADO.md').write_text('# TASK-177 — Validação local\n\n499/499 testes GUI aprovados; ZIPs, manifestos, launchers nativos, Java integrado e dois processos clientes validados após extração. JAR idêntico nos dois papéis.\n\nRTT localhost mediano/p95: 54.096/95.638 → .133/.237 ms. Apresentação controlada: 100 → 76.389 ms. Footprint estável: raio 12 (antes 18).\n\nRadmin entre duas máquinas e aprovação visual humana permanecem pendentes. Ver validation-summary.json e docs/TASK-177-TESTES.md.\n',encoding='utf-8')
print(json.dumps(summary,indent=2))
