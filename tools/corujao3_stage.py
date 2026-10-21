"""Registra evidências reais por tarefa sem sobrescrever relatórios anteriores."""
import argparse
import json
from pathlib import Path
from corujao_checkpoint import run, ROOT


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('task')
    parser.add_argument('validation')
    parser.add_argument('description')
    parser.add_argument('--status', default='VALIDADA COM RESSALVAS')
    parser.add_argument('--defer-checkpoint', action='store_true')
    parser.add_argument('--notes', default='Homologação visual e teclado físico pendentes; printAll não valida monitor físico.')
    args = parser.parse_args()
    report = json.loads((ROOT / f'docs/TASK-{args.validation}-qualidade.json').read_text(encoding='utf-8'))
    if report['exitCode'] != 0:
        raise RuntimeError('Regressão falhou; etapa não pode ser consolidada')
    prefix = ROOT / f'docs/TASK-{args.task}'
    bodies = {
        '.md': args.description + '\n\nPreservados domínio, saves, backups, assets originais, Coop, arena e protocolos. Sem push ou publicação.',
        '-TESTES.md': f'Validação: `python tools/qualidade.py --offline --task {args.validation}`\n\n```json\n' + json.dumps(report['checks'], ensure_ascii=False, indent=2) + '\n```\n\n' + args.notes,
        '-RELATORIO-FINAL.md': f'Estado: **{args.status}**.\n\n' + args.description + '\n\n' + args.notes + f'\n\nLogs: `docs/TASK-{args.validation}-*`. Checkpoints recuperáveis, inventários SHA-256, diff isolado e preservação: `.task-checkpoints/TASK-{args.task}/corujao-i/`. O nome histórico da pasta pertence à ferramenta reutilizada; o conteúdo corresponde à tarefa indicada.\n\n[Testes](TASK-' + args.task + '-TESTES.md).',
    }
    for suffix in bodies:
        if Path(str(prefix) + suffix).exists():
            raise FileExistsError(str(prefix) + suffix)
    for suffix, body in bodies.items():
        Path(str(prefix) + suffix).write_text(f'# TASK-{args.task} — Corujão III\n\n{body}\n', encoding='utf-8')
    progress = ROOT / 'docs/CORUJAO-III-PROGRESSO.json'
    stages = json.loads(progress.read_text(encoding='utf-8')) if progress.exists() else []
    stages.append({'task': args.task, 'status': args.status, 'description': args.description, 'validation': args.validation, 'notes': args.notes})
    progress.write_text(json.dumps(stages, ensure_ascii=False, indent=2), encoding='utf-8')
    if not args.defer_checkpoint:
        run(args.task, 'after')


if __name__ == '__main__':
    main()
