"""Uma chamada ao runner revisado; conserva stdout/stderr do controlador em bytes."""
import json
import os
from pathlib import Path
import subprocess
import sys
from datetime import datetime,timezone


def main():
    """Sem retry, sinal alternativo ou mudança nas fontes revisadas."""
    root=Path(__file__).resolve().parents[2]
    run=root/'temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70'
    sys.path.insert(0,str(run/'scripts'))
    import config
    config.require_operational(); config.verify_prepared_sources()
    assert not (run/'execution.claim').exists(),'RUN_ALREADY_USED'
    assert json.loads((run/'operational-preflight/preclaim-preflight.json').read_text())['passed']
    env=dict(os.environ,PYTHONDONTWRITEBYTECODE='1')
    with (run/'controller-output.txt').open('xb') as output:
        proc=subprocess.Popen([sys.executable,'-B',str(run/'scripts/runner.py')],cwd=root,
            stdout=output,stderr=subprocess.STDOUT,env=env,creationflags=subprocess.CREATE_NO_WINDOW)
        record=dict(controller_pid=proc.pid,launcher_pid=os.getpid(),start_utc=datetime.now(timezone.utc).isoformat(),
                    argv=[sys.executable,'-B',str(run/'scripts/runner.py')],output='controller-output.txt')
        (run/'operational-preflight/controller-launch.json').write_text(json.dumps(record,indent=2),encoding='utf8')
        print('CONTROLLER_STARTED '+str(proc.pid),flush=True)
        result=proc.wait()
    record.update(returncode=result,end_utc=datetime.now(timezone.utc).isoformat())
    (run/'operational-preflight/controller-completion.json').write_text(json.dumps(record,indent=2),encoding='utf8')
    print('CONTROLLER_EXIT '+str(result),flush=True)
    return result


if __name__=='__main__': raise SystemExit(main())
