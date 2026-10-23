"""Registra resultados reais por tarefa; recusa promover regressões incompletas."""
from pathlib import Path
import json
import re
import shutil
import sys

ROOT=Path(__file__).resolve().parents[1]


def report(task, description, limitations):
    output=ROOT/f'.task-checkpoints/TASK-{task}/corujao-iv'
    run=max((ROOT/'server/target/local-tests').glob('run-*'),key=lambda p:p.stat().st_mtime)
    log=(run/'test-results.log').read_text(encoding='utf-8')
    matches=re.findall(r'JUnit: (\d+) executed, (\d+) passed, (\d+) aborted, (\d+) failures, (\d+) skipped',log)
    if not matches or int(matches[-1][3]):
        raise ValueError('Regressão completa ausente ou com falhas')
    shutil.copyfile(run/'test-results.log',output/'java-tests.log')
    gui=(output/'gui-tests.log').read_text(encoding='utf-8') if (output/'gui-tests.log').exists() else 'Não executado nesta tarefa.'
    summary=log.strip().splitlines()[-1]
    (ROOT/f'docs/TASK-{task}.md').write_text(f'# TASK-{task}\n\n{description}\n\nBaseline anterior: checkpoint before e regressão da tarefa precedente. Checkpoints incrementais baseados no ZIP anterior da TASK-134, com manifesto SHA-256 e instruções de restauração em pasta nova.\n',encoding='utf-8')
    (ROOT/f'docs/TASK-{task}-TESTES.md').write_text(f'# TASK-{task} — Testes\n\nCompilação nova via `python tools/run-local-tests.py`, --release 17.\n\n{summary}\n\nStudio gráfico: {gui.strip()}\n\nLogs: `.task-checkpoints/TASK-{task}/corujao-iv/`. Os abortados headless continuam classificados como dependentes de ambiente.\n',encoding='utf-8')
    (ROOT/f'docs/TASK-{task}-RELATORIO-FINAL.md').write_text(f'# TASK-{task} — Relatório\n\n{description}\n\nLimitações: {limitations}\n\nValidação: {summary}. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.\n',encoding='utf-8')
    p=ROOT/'docs/CORUJAO-IV-PROGRESSO.json'; progress=json.loads(p.read_text(encoding='utf-8'))
    progress['tasks'][f'TASK-{task}']={'status':'implementada e testada','limitations':limitations,'testRun':run.name,'java':dict(zip(('executed','passed','aborted','failed','skipped'),map(int,matches[-1])))}
    p.write_text(json.dumps(progress,indent=2,ensure_ascii=False),encoding='utf-8')


if __name__=='__main__':
    report(*sys.argv[1:])
