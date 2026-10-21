"""Audita e inicia o EXE privado com Java integrado e diretório de dados temporário."""
from pathlib import Path
import json
import os
import shutil
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1]
folder = root/'docs/task-133'
lines = (folder/'package-build-final.log').read_text(encoding='utf-16' if (folder/'package-build-final.log').read_bytes().startswith(b'\xff\xfe') else 'utf-8-sig').splitlines()
exe = Path(next(line.split('=',1)[1] for line in lines if line.startswith('RPG_EXE=')))
package = Path(next(line.split('=',1)[1] for line in lines if line.startswith('RPG_ZIP=')))
subprocess.run([sys.executable,str(root/'tools/corujao3_package_audit.py'),str(package),str(folder/'package-audit.json')],cwd=root,check=True)
data = Path(tempfile.mkdtemp(prefix='corujao3-packaged-'))
environment = dict(os.environ)
environment['JAVA_TOOL_OPTIONS'] = '-Dspiritgate.rpg.dataDir='+str(data)
environment.pop('_JAVA_OPTIONS',None)
environment.pop('JDK_JAVA_OPTIONS',None)
capture = folder/'packaged-smoke.png'
startup = subprocess.STARTUPINFO()
startup.dwFlags |= subprocess.STARTF_USESHOWWINDOW
startup.wShowWindow = subprocess.SW_HIDE
result = subprocess.run([str(exe),'--smoke','Homologacao sintetica',str(capture)],cwd=exe.parent,env=environment,capture_output=True,text=True,errors='replace',timeout=90,startupinfo=startup)
(folder/'packaged-smoke.log').write_text(result.stdout+result.stderr,encoding='utf-8')
assert result.returncode == 0, result.returncode
state = json.loads(Path(str(capture)+'.json').read_text(encoding='utf-8'))
assert state['windowShowing'] and state['ticks']>0 and state['componentCapture']
assert not (exe.parent/'saves').exists(), 'O smoke não deve criar dados no pacote'
assert Path(str(capture)+'.components.png').is_file()
for suffix in ('.manifest.json','.sha256'):
    shutil.copyfile(Path(str(package)+suffix),folder/('package'+suffix))
(folder/'package-smoke.json').write_text(json.dumps({'exe':str(exe),'package':str(package),'exitCode':result.returncode,'syntheticData':str(data),'state':state,'offlineRuntimeBundled':True,'externalPublication':False},indent=2),encoding='utf-8')
print('PASS: EXE com Java integrado iniciou e encerrou; dados temporários; ZIP íntegro.')
