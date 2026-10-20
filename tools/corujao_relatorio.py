"""Registra uma etapa somente apos resultado automatizado real e aprovado."""
from pathlib import Path
import sys,json
from corujao_checkpoint import run,ROOT

task,status,description=sys.argv[1:]
report=json.loads((ROOT/'docs'/('TASK-corujao-'+task+'-qualidade.json')).read_text(encoding='utf-8'))
assert report['exitCode']==0,report
folder=ROOT/'docs'/('corujao-i' if task=='079' else '')
folder.mkdir(exist_ok=True)
prefix=folder/('TASK-'+task)
for suffix,body in [('.md',description),('-TESTES.md','Comando: `python tools/qualidade.py --offline --task corujao-'+task+'`\n\n```json\n'+json.dumps(report['checks'],indent=2)+'\n```'),('-RELATORIO-FINAL.md','Estado: **'+status+'**.\n\n'+description+'\n\nCheckpoints recuperaveis, hashes e diff isolado: `.task-checkpoints/TASK-'+task+'/corujao-i/`. Logs da suite: `docs/TASK-corujao-'+task+'-*`. A aprovacao artistica permanece humana; os testes nao equivalem a homologacao perceptiva.')]:
 p=Path(str(prefix)+suffix)
 if p.exists():raise ValueError('Relatorio historico existente: '+str(p))
 p.write_text('# TASK-'+task+' - Corujao I\n\n'+body+'\n',encoding='utf-8')
progress=ROOT/'docs/CORUJAO-I-PROGRESSO.json'
data=json.loads(progress.read_text()) if progress.exists() else []
data.append({'task':task,'status':status,'description':description,'checks':report['checks']})
progress.write_text(json.dumps(data,indent=2),encoding='utf-8')
run(task,'after')
