"""Auditoria final local: fontes, pacotes privados e teste do atualizador sem mudar política."""
from pathlib import Path
import runpy, json, subprocess, datetime, shutil
root=Path.cwd()
h=runpy.run_path(str(root/'.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/session.py'))
out=h['checkpoint']('200')
baseline=root/'.task-checkpoints/TASK-187/corujao-vi/run-20261025T222531Z-a2ddfc6e'
before=h['read'](baseline/'sources-before.json');after=h['sources']()
changed=[name for name in after if before.get(name)!=after[name]]
production=[n for n in changed if '/main/' in n]
expected={'server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java','server/src/main/java/com/projetok/multiplayer/NetworkDiagnostics.java',
    'server/src/main/java/com/projetok/multiplayer/RevisionCoordinator.java','server/src/main/java/com/projetok/studio/PreviewServer.java',
    'server/src/main/java/com/projetok/rpg/SyntheticOnlineRepository.java','server/src/main/java/com/projetok/rpg/SyntheticSessionLaboratory.java'}
if set(production)!=expected:raise RuntimeError('Delta de producao inesperado: '+repr(production))
h['write'](out/'session-source-delta.json',dict(baseline=str(baseline),production_changed=production,all_changed=changed,before=before,after=after,
    note='Baseline apos correcao186; LivePublishing da186 verificado separadamente'))
live=root/'server/src/main/java/com/projetok/studio/LivePublishing.java'
if h['sha'](live)!='70215e54170740cb4b9df140f19463b1445f3561411ebacd6dc0ec7964bfea6d':raise RuntimeError('Correcao186 mudou')
previous=h['checkpoint']('198');packages=h['read'](previous/'packages.json')
for package in packages:
    if h['sha'](root/package['path'])!=package['sha256']:raise RuntimeError('ZIP alterado')
    if h['sha'](root/package['image']/'app/spiritgate.jar')!=package['jar_sha256']:raise RuntimeError('Jar alterado')
h['write'](out/'packages-verified.json',packages)
updater=out/'updater-fixture'
args=['powershell','-NoProfile','-NonInteractive','-File',str(root/'tools/updater/Test-Atualizador.ps1'),'-EvidenceDir',str(updater)]
with (out/'updater-environment.log').open('xb') as stream:
    process=subprocess.run(args,cwd=root,stdout=stream,stderr=subprocess.STDOUT,timeout=120)
result=dict(command=args,exit_code=process.returncode,policy_changed=False,utc=datetime.datetime.now(datetime.timezone.utc).isoformat())
result_path=updater/'results.json'
if result_path.exists():
    result['checks']=json.loads(result_path.read_text(encoding='utf-8-sig'))['passed']
    result['status']='PASS' if process.returncode==0 else 'FAIL'
else:
    log=(out/'updater-environment.log').read_text(encoding='utf-8',errors='replace')
    policy_block='PSSecurityException' in log or 'UnauthorizedAccess' in log
    if not policy_block:raise RuntimeError('Falha nova do atualizador; analisar log')
    result.update(status='ABORTADO AMBIENTAL - POLITICA DE EXECUCAO',checks=0)
h['write'](out/'updater.environment.json',result)
if result['status']=='FAIL':raise RuntimeError('Regressao do atualizador')
for name in ('corujao-vi-approval193.py','corujao-vi-task-evidence.py','build-corujao-vi-private.py','smoke-corujao-vi-private.py','run-corujao-vi-integrated.py','corujao-vi-final-audit.py'):
    shutil.copy2(root/'tools'/name,out/name)
print(json.dumps(dict(production_delta=len(production),task186_hash_preserved=True,private_packages=2,updater=result),indent=2),flush=True)
