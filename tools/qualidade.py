"""Validação local completa: python tools/qualidade.py --offline --task 023."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

def classify(code, output):
    """Códigos: 0 sucesso, 2 ambiente, 3 teste, 4 build."""
    if code == 0:
        return 0
    if 'COMPILATION ERROR' in output or 'Could not resolve' in output:
        return 4
    if 'There are test failures' in output or re.search(r'FAILED \(', output):
        return 3
    return 4

def junit_totals(folder):
    reports = [ET.parse(p).getroot() for p in folder.glob('TEST-*.xml')]
    if not reports:
        raise ValueError('Relatórios JUnit ausentes')
    return {k: sum(int(r.get(k, '0')) for r in reports)
            for k in ('tests', 'failures', 'errors', 'skipped')}

def run(command, log, cwd=ROOT):
    result = subprocess.run(command, cwd=cwd, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, errors='replace')
    log.write_text(result.stdout, encoding='utf-8')
    return result.returncode, result.stdout

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offline', action='store_true')
    parser.add_argument('--task', default='local', type=str)
    args = parser.parse_args(argv)
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,40}', args.task):
        parser.error('Identificador inválido')
    prefix = ROOT / f'docs/TASK-{args.task}'
    report = {'schemaVersion': 1, 'exitCode': 2, 'tools': {}, 'checks': {}}
    try:
        for name, command in [('java', ['java', '-version']), ('maven', ['mvn', '-v']),
                              ('python', [sys.executable, '--version'])]:
            executable = shutil.which(command[0])
            if not executable:
                raise RuntimeError(f'Ferramenta ausente: {name}')
            command[0] = executable
            code, output = run(command, Path(str(prefix) + f'-{name}-version.log'))
            report['tools'][name] = output.strip()
            if code or (name == 'java' and not re.search(r'version "17[.]', output)):
                raise RuntimeError(f'Versão/ferramenta inválida: {name}')
        command = [shutil.which('mvn'), f'-Dmaven.repo.local={ROOT / ".maven-repository"}',
                   '-f', 'server/pom.xml']
        if args.offline:
            command.append('-o')
        code, output = run(command + ['clean', 'test'], Path(str(prefix) + '-java.log'))
        report['checks']['java'] = {'exitCode': code, 'category': classify(code, output)}
        if code == 0:
            report['checks']['java']['totals'] = junit_totals(ROOT / 'server/target/surefire-reports')
        code, output = run([sys.executable, '-m', 'unittest', 'discover', '-s', 'tools',
                            '-p', 'test_*.py', '-v'], Path(str(prefix) + '-python.log'))
        match = re.search(r'Ran (\d+) tests?', output)
        report['checks']['python'] = {'exitCode': code, 'category': 0 if code == 0 else 3,
                                     'tests': int(match[1]) if match else None}
        report['exitCode'] = max(c['category'] for c in report['checks'].values())
    except (OSError, RuntimeError, ValueError, ET.ParseError) as error:
        report['error'] = str(error)
        report['exitCode'] = 2
    Path(str(prefix) + '-qualidade.json').write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    summary = json.dumps({'exitCode': report['exitCode'], 'checks': report['checks']},
                         ensure_ascii=False, indent=2)
    Path(str(prefix) + '-testes.txt').write_text(summary + '\n', encoding='utf-8')
    print(summary)
    return report['exitCode']

if __name__ == '__main__':
    raise SystemExit(main())
