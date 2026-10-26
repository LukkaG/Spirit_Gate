"""Fecha a 200 somente após verificar os resultados reais, sem iniciar outra task."""
from pathlib import Path
import runpy, re, shutil, json, difflib
root=Path.cwd()
h=runpy.run_path(str(root/'.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/session.py'))
out=h['checkpoint']('200')
def junit(tag):
    text=(out/(tag+'.log')).read_text(encoding='utf-8',errors='replace')
    match=re.search(r'JUnit: (\d+) executed, (\d+) passed, (\d+) aborted, (\d+) failures, (\d+) skipped',text)
    if not match:raise RuntimeError('Resultado JUnit ausente '+tag)
    return dict(zip(('executed','passed','aborted','failures','skipped'),map(int,match.groups())))
specific,headless,gui=[junit(tag) for tag in ('specific','java-full-headless','java-full-graphics')]
assert specific==dict(executed=6,passed=6,aborted=0,failures=0,skipped=0)
assert headless==dict(executed=613,passed=603,aborted=10,failures=0,skipped=0)
assert gui==dict(executed=613,passed=613,aborted=0,failures=0,skipped=0)
python=(out/'python.log').read_text(encoding='utf-8',errors='replace')
assert 'Ran 33 tests' in python and '\nOK\n' in python
integrity=h['read'](out/'protected-full-final.json')
assert integrity['full'] and integrity['rehashed']==31629 and integrity['changes']==[]
assert h['sources']()==h['read'](out/'compiled-sources.json')
assert h['git']('branch','--show-current')=='beco' and h['git']('rev-parse','HEAD')=='ad66caed14e3f94bc369f7edd9861900d5a68636'
for package in h['read'](out/'packages-verified.json'):
    assert h['sha'](root/package['path'])==package['sha256']
updater=h['read'](out/'updater.environment.json')
assert updater['status']=='ABORTADO AMBIENTAL - POLITICA DE EXECUCAO' and updater['checks']==0
h['write'](out/'verdict.json',dict(status='VALIDADA COM RESSALVAS',specific=specific,headless=headless,graphics=gui,python_passed=33,
    protected_rehashed=31629,protected_changes=0,private_packages_verified=2,updater=updater['status'],radmin_physical='MANUAL PENDENTE',task201_started=False))
report=root/'docs/CORUJAO-VI-RELATORIO-FINAL.md'
text=report.read_text(encoding='utf-8').replace('Veredito condicionado aos resultados anexados ao checkpoint final TASK-200:','Veredito final da TASK-200:')
text=text.replace('Fechamento e veredito nos resultados finais anexados; sem TASK-201','VALIDADA COM RESSALVAS; 613 Java gráficos, 33 Python, 31.629 hashes; sem TASK-201')
result='''## Regressão final executada

TASK-200: seis testes específicos da confirmação negativa/faults aprovados; Java headless 613 executados, 603 aprovados, dez abortos GUI, zero falhas/skips; Java gráfico 613/613, zero abortos/falhas/skips; Python 33/33 com parser Java recém-compilado. Rehash integral de 31.629 arquivos sem divergência e dois ZIPs/jars conferidos novamente. Atualizador PowerShell abortado pela política vigente, zero checks, sem bypass. Evidência final: `'''+out.relative_to(root).as_posix()+'''`, verdict.json e logs/commands.

'''
text=text.replace('## Resultado funcional',result+'## Resultado funcional')
report.write_text(text,encoding='utf-8')
progress=root/'docs/CORUJAO-VI-PROGRESSO.md'
text=progress.read_text(encoding='utf-8').replace('| 198 | EM EXECUÇÃO | Pacotes privados novos no checkpoint, dist preservado |','| 198 | VALIDADA COM RESSALVAS | [Pacotes novos verificados](TASK-198-RELATORIO-FINAL.md) |')
text=text.replace('| 199–200 | NÃO INICIADAS | Dependem dos gates anteriores |','| 199 | VALIDADA COM RESSALVAS | [Studio/Preview e jogo reais](TASK-199-RELATORIO-FINAL.md) |\n| 200 | VALIDADA COM RESSALVAS | [Consolidação](TASK-200-RELATORIO-FINAL.md): 613 Java gráficos, 33 Python e 31.629 hashes |')
text+='\n\nEncerramento: TASK-200 concluída; Gates D/E atendidos no escopo local e sintético. Radmin físico, política do atualizador e demais ressalvas em CORUJAO-VI-PENDENCIAS.md. Nenhuma TASK-201, commit, push ou publicação. ADR-193 aprovado e decisão preservada; não há aprovação pendente para este bloco executado.\n'
progress.write_text(text,encoding='utf-8')
template=out/'report-base.md'
template.write_text('''# TASK-200 — consolidação final

Estado: **VALIDADA COM RESSALVAS**. Branch beco e HEAD preservados; nenhum commit/push/release/publicação ou TASK-201.

Nova compilação integral da árvore real e cache existente Java 17. Confirmação negativa TASK-186: 6/6, inclusive accepted=false com hash correspondente, hash divergente com accepted=true, fase inválida, AFTER_STAGE, AFTER_ACTIVATION e BEFORE_POINTER. Java headless 613/603 aprovados + 10 abortos GUI, zero falhas/skips. Java gráfico 613/613, sem falhas/abortos/skips. Python 33/33; parser de mapas recompilado das fontes atuais.

Rehash integral de 31.629 protegidos sem divergências; os dois ZIPs privados/jars continuam com seus hashes. Mudanças de produção desde a entrada da sessão: seis arquivos identificados em session-source-delta.json; LivePublishing da correção 186 preservado com seu SHA exato. Nenhuma nova referência do launcher ao laboratório de personagens. Schema/protocolo/progressão/assets pessoais preservados.

Atualizador: abortamento ambiental pela política PowerShell, exit 1 e zero checks, idêntico à limitação da baseline; política intacta. Radmin físico continua MANUAL PENDENTE. Nenhuma ressalva é tratada como teste aprovado.

Entregas: CORUJAO-VI-RELATORIO-FINAL.md, CORUJAO-VI-HOMOLOGACAO.md, CORUJAO-VI-PENDENCIAS.md, CORUJAO-VI-PACOTES.md e índice de progresso. Não houve mudança negociada de protocolo que exija novo documento de wire. Gates D/E fechados somente nas condições locais e sintéticas documentadas; integração a contas/launcher real permanece fora da autorização. Falhas/tentativas originais e logs preservados.
''',encoding='utf-8')
docs=out/'docs-final';docs.mkdir()
names=['CORUJAO-VI-RELATORIO-FINAL.md','CORUJAO-VI-HOMOLOGACAO.md','CORUJAO-VI-PENDENCIAS.md','CORUJAO-VI-PACOTES.md','CORUJAO-VI-PROGRESSO.md','ADR-193-PERSISTENCIA-SINTETICA.md']
for name in names:shutil.copy2(root/'docs'/name,docs/name)
shutil.copy2(root/'tools/corujao-vi-close200.py',out/'corujao-vi-close200.py')
historical={}
verified=out/'verified-diffs';verified.mkdir()
baselines={'194':root/'.task-checkpoints/TASK-194/corujao-vi/run-20261026T000504Z-bdc91ff5',
           '195':h['checkpoint']('194'),'196':h['checkpoint']('195'),'197':h['checkpoint']('196')}
for task,baseline in baselines.items():
    old=h['read'](baseline/'compiled-sources.json')
    current=h['read'](h['checkpoint'](task)/'compiled-sources.json')
    changed=sorted(n for n in old.keys()|current.keys() if old.get(n)!=current.get(n))
    patch=[]
    for name in changed:
        prior=baseline/'isolated'/name;latest=h['checkpoint'](task)/'isolated'/name
        a=prior.read_text(encoding='utf-8').splitlines(True) if prior.exists() else []
        b=latest.read_text(encoding='utf-8').splitlines(True) if latest.exists() else []
        patch.extend(difflib.unified_diff(a,b,fromfile='a/'+name,tofile='b/'+name))
    (verified/('TASK-'+task+'.patch')).write_text(''.join(patch),encoding='utf-8')
    h['write'](verified/('TASK-'+task+'.json'),dict(baseline=str(baseline),comparison='exact compiled source snapshots on both sides',changed=changed,before_sha256=old,after_sha256=current))
for task in list(map(str,range(187,200)))+['193-approval']:
    checkpoint=h['checkpoint'](task)
    for path in checkpoint.rglob('*'):
        if not path.is_file():continue
        parts=path.relative_to(checkpoint).parts
        if any(part in {'isolated','build','probe-classes','audit-classes','Anfitriao','Convidado'} for part in parts):continue
        historical[path.relative_to(root).as_posix()]=h['sha'](path)
h['write'](out/'historical-evidence-sha256.json',historical)
h['finish']('200',template,'VALIDADA COM RESSALVAS - GATE E / ENCERRADO')
shutil.copy2(root/'docs/TASK-200-RELATORIO-FINAL.md',docs/'TASK-200-RELATORIO-FINAL.md')
final=h['read'](out/'after.json')
final['documentation_sha256']={p.relative_to(root).as_posix():h['sha'](p) for p in (root/'docs').glob('*') if p.is_file() and (p.name in names or re.match(r'TASK-19[3-9].*\.md',p.name) or p.name=='TASK-200-RELATORIO-FINAL.md')}
final['documentation_snapshot_sha256']={p.name:h['sha'](p) for p in docs.iterdir()}
final['final_verdict']='VALIDADA COM RESSALVAS; Radmin fisico e atualizador ambiental pendentes; encerrado sem TASK-201'
h['write'](out/'after.json',final)
print('CORUJAO VI ENCERRADO',out,flush=True)
