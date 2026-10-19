"""Fecha uma etapa somente depois da suite completa aprovada; sem Git ou rede."""
from pathlib import Path
import argparse
import json
import subprocess
import sys
from checkpoint_rodada import checkpoint
from gerar_pdf_task002 import gerar_pdf

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('task')
    parser.add_argument('--existing', action='store_true')
    args = parser.parse_args()
    task = args.task
    if not args.existing:
        result = subprocess.run([sys.executable, 'tools/qualidade.py', '--offline', '--task', task], cwd=ROOT)
        if result.returncode:
            raise SystemExit(result.returncode)
    report = json.loads((ROOT / f'docs/TASK-{task}-qualidade.json').read_text(encoding='utf-8'))
    if report['exitCode'] != 0:
        raise SystemExit('Etapa bloqueada: suite reprovada')
    document = ROOT / f'docs/TASK-{task}.md'
    if not document.exists():
        raise SystemExit('Relatorio da etapa ausente')
    gerar_pdf(document, ROOT / f'docs/Spirit_Gate_TASK-{task}.pdf', f'TASK-{task}')
    checkpoint(task, True)

if __name__ == '__main__':
    main()
