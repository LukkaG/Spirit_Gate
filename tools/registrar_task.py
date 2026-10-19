"""Documenta resultados reais e reutiliza o gerador PDF e checkpoints existentes."""
import argparse
from pathlib import Path
from gerar_pdf_task002 import gerar_pdf
from checkpoint_rodada import checkpoint

ROOT = Path(__file__).resolve().parents[1]

def register(task, text):
    path = ROOT / f'docs/TASK-{task}.md'
    path.write_text(text, encoding='utf-8')
    gerar_pdf(path, ROOT / f'docs/Spirit_Gate_TASK-{task}.pdf', f'TASK-{task}')
    checkpoint(task, True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('task')
    parser.add_argument('text_file', type=Path)
    args = parser.parse_args()
    register(args.task, args.text_file.read_text(encoding='utf-8-sig'))
