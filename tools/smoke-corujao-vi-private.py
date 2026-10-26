"""Verifica/extrai somente os novos ZIPs da 198 e exercita Java integrado em processos próprios."""
from pathlib import Path, PurePosixPath
import runpy, json, zipfile, subprocess, socket, os
root=Path.cwd()
h=runpy.run_path(str(root/'.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/session.py'))
out=h['checkpoint']('198')
smoke=out/'smoke';smoke.mkdir()
packages=h['read'](out/'packages.json')
for package in packages:
    archive=root/package['path']
    if h['sha'](archive)!=package['sha256']:raise RuntimeError('Hash ZIP divergente')
    with zipfile.ZipFile(archive) as zipped:
        names=zipped.namelist()
        if len(names)!=len(set(names)):raise RuntimeError('Entradas duplicadas')
        for name in names:
            parts=PurePosixPath(name)
            if parts.is_absolute() or '..' in parts.parts or '\\' in name:raise RuntimeError('Caminho invalido')
        manifest_name=next(n for n in names if n.endswith('/manifest-sha256.json'))
        prefix=manifest_name.rsplit('/',1)[0]+'/'
        manifest=json.loads(zipped.read(manifest_name))
        if set(names)!={prefix+e['path'] for e in manifest}|{manifest_name}:raise RuntimeError('ZIP fora do manifest')
        import hashlib
        for entry in manifest:
            content=zipped.read(prefix+entry['path'])
            if len(content)!=entry['bytes'] or hashlib.sha256(content).hexdigest()!=entry['sha256']:raise RuntimeError('Manifest divergente')
        destination=smoke/package['role'];destination.mkdir();zipped.extractall(destination)
        package['extracted']=next(destination.iterdir())
    image=package['extracted']
    check=smoke/(package['role']+'-native-check.json')
    h['command'](out,'native-check-'+package['role'],[str(image/'SpiritGate.exe'),'--verify-install',str(check)],image,45)
    if not h['read'](check)['ok']:raise RuntimeError('Self check nativo falhou')
host,guest=[p['extracted'] for p in packages]
if h['sha'](host/'app/spiritgate.jar')!=h['sha'](guest/'app/spiritgate.jar'):raise RuntimeError('Jars divergentes')
probe=smoke/'probe-classes';probe.mkdir()
h['command'](out,'probe-compile',['javac','--release','17','-encoding','UTF-8','-cp',str(host/'app/spiritgate.jar'),'-d',str(probe),str(root/'tools/Task180PackagedProbe.java')],root)
with socket.socket() as reserve:reserve.bind(('127.0.0.1',0));port=reserve.getsockname()[1]
processes=[];logs=[];commands=[]
try:
    for mode,image in [('host',host),('a',host),('b',guest)]:
        log=(smoke/(mode+'.log')).open('xb');logs.append(log)
        args=[str(image/'runtime/bin/java.exe'),'-Dspiritgate.network.debug=true']
        if mode=='b':args+=['-Dspiritgate.guest=true']
        args+=['-cp',os.pathsep.join([str(image/'app/spiritgate.jar'),str(probe)]),'com.projetok.studio.Task180PackagedProbe',mode,str(port),str(image),str(smoke)]
        commands.append(dict(args=args,cwd=str(image)))
        processes.append(subprocess.Popen(args,cwd=image,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW))
    codes=[process.wait(timeout=120) for process in processes]
    if codes!=[0,0,0]:raise RuntimeError('Smoke falhou: '+repr(codes))
    reports=[h['read'](smoke/name) for name in ('host-result.json','client-a-result.json','client-b-result.json')]
    if not all(r['publication'] and r['rollback'] and r['application']==4 for r in reports):raise RuntimeError('Commit/rollback nao confirmado')
    if not reports[2]['guestAdministrativeDenied'] or reports[0]['failureRecoveryMs']<=0:raise RuntimeError('Negativo nao comprovado')
    result=dict(zip_manifest_verified=True,clean_extraction=True,native_self_checks=2,exit_codes=codes,real_server=True,real_client_processes=2,
        production_renderer=True,publication=True,rollback=True,failure_after_activation_recovered=True,guest_administration_rejected=True,radmin_physical=False)
    h['write'](smoke/'result.json',result);print(json.dumps(result),flush=True)
finally:
    for process in processes:
        if process.poll() is None:process.terminate();process.wait(timeout=5)
    for log in logs:log.close()
    h['write'](smoke/'processes.json',dict(commands=commands,exit_codes=[p.poll() for p in processes]))
