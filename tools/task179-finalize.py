"""Audit TASK-179 against its dirty entry baseline. Never publish or touch saves."""
from pathlib import Path
import difflib
import hashlib
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
CP = ROOT / '.task-checkpoints/TASK-179/world-studio-live'
initial = json.loads((CP / 'baseline-sha256.json').read_text())
diffs = CP / 'file-diffs'
diffs.mkdir(exist_ok=True)
paths = list((ROOT / 'server/src').rglob('*.java'))
paths += list((ROOT / 'docs').glob('TASK-179*.md'))
paths += list((ROOT / 'tools').glob('*179*'))
changes = []
for path in sorted(paths):
    if not path.is_file():
        continue
    relative = path.relative_to(ROOT).as_posix()
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if initial.get(relative) == digest:
        continue
    before = CP / 'baseline' / relative
    # Only source files had full baseline copies; new task docs/tools are new.
    assert relative not in initial or before.exists(), relative
    old = before.read_text(encoding='utf-8') if before.exists() else ''
    delta = ''.join(difflib.unified_diff(old.splitlines(True), path.read_text(encoding='utf-8').splitlines(True),
                                       fromfile='TASK178/' + relative, tofile='TASK179/' + relative))
    (diffs / (relative.replace('/', '__') + '.diff')).write_text(delta, encoding='utf-8')
    changes.append(dict(path=relative, before=initial.get(relative), after=digest))

protected = [p for p in initial if p.startswith('assets/dev/player-base/') or
             p.endswith(('/Protocol.java', '/RpgSave.java', '/LocalSaveStore.java'))]
for relative in protected:
    assert hashlib.sha256((ROOT / relative).read_bytes()).hexdigest() == initial[relative], relative
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip() == (CP / 'head.txt').read_text().strip()
run = ROOT / 'server/target/local-tests/run-fz03wygo'
headless = (run / 'test-results.log').read_text(encoding='utf-8')
gui = (CP / 'gui-tests.log').read_text(encoding='utf-8')
assert 'JUnit: 532 executed, 523 passed, 9 aborted, 0 failures, 0 skipped' in headless
assert 'JUnit: 532 executed, 532 passed, 0 aborted, 0 failures, 0 skipped' in gui
shutil.copyfile(run / 'test-results.log', CP / 'headless-tests.log')
benchmark = json.loads((CP / 'final-probe/benchmark.json').read_text())
assert benchmark['updates'] == 20 and benchmark['syntheticActivationConfirmed'] and benchmark['rollbackConfirmed']
assert benchmark['beforeSaveTargetUnchanged'] and benchmark['draftSaveNotPublished'] and not benchmark['productionActivated']
assert benchmark['historyEntries'] == 3 and benchmark['separatePorts']
captures = ['before-edit.png', 'live-edit-unpublished.png', 'saved-and-rollback.png']
for capture in captures:
    assert (CP / 'final-probe' / capture).is_file()
subprocess.run(['git', 'diff', '--check'], cwd=ROOT, check=True)
summary = dict(task='TASK-179', technicallyComplete=False,
               reason='Production content/revision contract requires explicit authorization; production Save blocked',
               tests=dict(total=532, guiPassed=532, headlessPassed=523, headlessAborted=9, failures=0),
               sourceAndDocumentationDeltas=changes, protectedHashesVerified=len(protected),
               benchmark=benchmark, captures=captures,
               syntheticProject='.task-checkpoints/TASK-179/world-studio-live/synthetic-project-ui-final',
               productionActivated=False, pushed=False, released=False, personalSavesModified=False,
               limitations=['Production revision distribution not implemented', 'Human visual approval pending',
                            'Radmin remote test pending', 'Same JVM isolation is not fatal process isolation',
                            'Nonzero offsets and alternate slide unsupported', 'Editor triggers use exported playtest'])
(CP / 'validation-summary.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
(CP / 'final-status.txt').write_bytes(subprocess.check_output(['git', 'status', '--short'], cwd=ROOT))
print(json.dumps({k: summary[k] for k in ['task', 'technicallyComplete', 'tests', 'protectedHashesVerified', 'productionActivated']}, indent=2))
