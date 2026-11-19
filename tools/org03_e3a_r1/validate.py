"""Validação R1 sem processos, sockets, banco ou alterações nos originais."""
import ast
import difflib
import hashlib
import io
import json
import sys
import unittest
from pathlib import Path


def digest(path):
    """Calcula SHA-256 do conteúdo existente."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    """Registra testes, sintaxe, preservação e diferenças das cópias."""
    here = Path(__file__).resolve().parent
    root = here.parent.parent
    old = root / 'temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb'
    before = json.loads((here / 'preservation-before.json').read_text(encoding='utf8'))
    after = {name: digest(root/name) if (root/name).is_file() else None for name in before}
    changed = [name for name in before if before[name] != after[name]]
    original_sources = json.loads((old/'evidence/source-sha256.json').read_text(encoding='utf8'))
    source_errors = [name for name, value in original_sources.items()
                     if digest(old/'scripts'/name) != value]
    e2 = json.loads((root/'temp/org03-e2/20261114-lucas-600aa9e066/evidence/artifact-sha256.json').read_text(encoding='utf8'))
    e2_errors = [name for name, value in e2.items() if digest(root/name) != value]
    sources = sorted(here.glob('*.py'))
    for path in sources:
        ast.parse(path.read_text(encoding='utf8'), filename=str(path))
    for name in ('lab_runtime.py', 'relay.py'):
        delta = ''.join(difflib.unified_diff(
            (old/'scripts'/name).read_text(encoding='utf8').splitlines(True),
            (here/name).read_text(encoding='utf8').splitlines(True),
            fromfile='original/'+name, tofile='R1/'+name))
        (here/(name+'.diff')).write_text(delta, encoding='utf8')
    output = io.StringIO()
    suite = unittest.defaultTestLoader.discover(str(here), pattern='test_harness.py')
    result = unittest.TextTestRunner(stream=output, verbosity=2).run(suite)
    (here/'test-output.txt').write_text(output.getvalue(), encoding='utf8')
    report = dict(date='2026-11-15', preserved_files=len(before), changed=changed,
                  original_e3_sources=len(original_sources), source_errors=source_errors,
                  e2_manifest_entries=len(e2), e2_errors=e2_errors,
                  ast_files=[p.name for p in sources], tests=result.testsRun,
                  failures=len(result.failures), errors=len(result.errors),
                  skipped=len(result.skipped), execution='PURE_MOCKS_NO_CLUSTER_NO_SQL',
                  passed=result.wasSuccessful() and not (changed or source_errors or e2_errors))
    (here/'preservation-after.json').write_text(json.dumps(after, indent=2), encoding='utf8')
    (here/'validation.json').write_text(json.dumps(report, indent=2), encoding='utf8')
    artifacts = [p for p in here.iterdir() if p.is_file() and p.name != 'artifact-sha256.json']
    artifacts += list((root/'docs').glob('ORG-03-E3-A-R1*.md'))
    (here/'artifact-sha256.json').write_text(json.dumps(
        {p.relative_to(root).as_posix(): digest(p) for p in artifacts}, indent=2), encoding='utf8')
    print(json.dumps(report, indent=2))
    return 0 if report['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
