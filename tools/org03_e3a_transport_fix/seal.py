"""Conferência de entrega P1: hashes/links/processos, sem repetir testes ou SQL."""
import json
import re
import subprocess
from audit import HERE,ROOT,R2,sha,verify


def main():
    """Sela os artefatos finais e distingue eventual PID reutilizado, sem controle."""
    preservation=verify()
    ev=HERE/'evidence'
    validation=json.loads((ev/'validation.json').read_text(encoding='utf8'))
    assert validation['passed'] and validation['total_tests']==71
    assert validation['fixture']['status']=='PASSOU' and not validation['postgresql']
    docs=list((ROOT/'docs').glob('ORG-03-E3-A-R2-P1*.md'))+[ROOT/'docs/ORG-03-E3-A-R3-PROPOSTA-EXECUCAO.md']
    broken=[]
    for path in docs:
        for link in re.findall(r'\]\(([^)]+)\)',path.read_text(encoding='utf8')):
            if not link.startswith('https://') and not (path.parent/link).exists(): broken.append(str(path)+': '+link)
    identities=[]
    for path in ev.glob('fixture-*/events.jsonl'):
        for line in path.read_text(encoding='utf8').splitlines():
            event=json.loads(line)
            if event['kind']=='PROCESS_CREATED': identities.append(event['identity'])
    pids=sorted({identity['ProcessId'] for identity in identities})
    script="@(Get-CimInstance Win32_Process -Filter '"+' OR '.join('ProcessId='+str(pid) for pid in pids)+"' | Select-Object ProcessId,ParentProcessId,@{n='Created';e={$_.CreationDate.ToUniversalTime().ToString('o')}}) | ConvertTo-Json -Compress"
    result=subprocess.run(['powershell','-NoProfile','-Command',script],capture_output=True,text=True,encoding='utf8',timeout=8,creationflags=subprocess.CREATE_NO_WINDOW)
    assert result.returncode==0
    current=json.loads(result.stdout) if result.stdout.strip() else []
    if isinstance(current,dict): current=[current]
    own_remaining=[value for value in current for previous in identities if value['ProcessId']==previous['ProcessId'] and value['ParentProcessId']==previous['ParentProcessId'] and value['Created']==previous['Created']]
    prior_artifacts=json.loads((R2/'evidence/artifact-sha256.json').read_text(encoding='utf8'))
    prior_errors=[p for p,h in prior_artifacts.items() if sha(ROOT/p)!=h]
    legacy_errors=[p for p in ('lifecycle.py','relay.py','lab_runtime.py','test_harness.py') if sha(HERE/'legacy'/p)!=sha(R2/'scripts'/p)]
    assert not broken and not own_remaining and not prior_errors and not legacy_errors
    report=dict(preservation=preservation,tests=71,fixture_final='PASSOU',own_fixture_processes_remaining=own_remaining,
                pids_checked=pids,current_pid_metadata=current,process_action='READ_ONLY_NO_CONTROL',prior_r2_artifact_errors=prior_errors,
                legacy_copy_errors=legacy_errors,documents=len(docs),broken_links=broken,postgresql=False,sql=False,cluster_created=False,e3_gate_approved=False,passed=True)
    (ev/'delivery-validation.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    files=[p for p in HERE.rglob('*') if p.is_file() and p.name!='artifact-sha256.json' and '__pycache__' not in p.parts]
    files+=docs+[ROOT/'logs/analises/20261115-org03-e3a-r2-p1.md']
    artifacts={p.relative_to(ROOT).as_posix():sha(p) for p in files}
    (ev/'artifact-sha256.json').write_text(json.dumps(artifacts,indent=2),encoding='utf8')
    errors=[p for p,h in artifacts.items() if sha(ROOT/p)!=h]
    assert not errors
    print(json.dumps(dict(**report,artifact_files=len(artifacts),artifact_errors=errors)))


if __name__=='__main__': main()
