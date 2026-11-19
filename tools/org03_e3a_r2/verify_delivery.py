"""Audita a rodada encerrada; não inicia banco nem altera fontes executadas."""
import hashlib
import json
import os
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902'
EV=RUN/'evidence'


def read(name):
    """Lê somente evidência própria já persistida."""
    return json.loads((EV/name).read_text(encoding='utf-8-sig'))


def sha(path):
    """Confere SHA-256 sem executar as fontes."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def ps(script):
    """Inspeção de metadados, sem comandos de controle de processo."""
    result=subprocess.run(['powershell','-NoProfile','-Command',script],capture_output=True,text=True,encoding='utf8',timeout=8,creationflags=subprocess.CREATE_NO_WINDOW)
    assert result.returncode==0, 'DELIVERY_INSPECTION_FAILED'
    return json.loads(result.stdout) if result.stdout.strip() else []


def main():
    """Compara manifestos e ausência de recursos, conservando falhas técnicas."""
    before=read('preservation-before.json')
    changed=[p for p,h in before.items() if not (ROOT/p).is_file() or sha(ROOT/p)!=h]
    executed=read('source-sha256.json')
    source_errors=[p for p,h in executed.items() if sha(RUN/'scripts'/p)!=h]
    reviewed=read('prepared-source-sha256.json')
    assert reviewed==executed, 'EXECUTED_SOURCES_DIFFER_FROM_REVIEW'
    events=[json.loads(line) for line in (EV/'events.jsonl').read_text(encoding='utf8').splitlines()]
    assert [e['sequence'] for e in events]==list(range(1,len(events)+1))
    results=read('results.json')
    shutdown=read('shutdown.json')
    snapshots=[]
    for event in events:
        if event['kind']!='SNAPSHOT_VALIDATED': continue
        value=read(event['file'])
        canonical=json.dumps(value,sort_keys=True,separators=(',',':')).encode()
        assert hashlib.sha256(canonical).hexdigest()==event['sha256']
        snapshots.append(dict(file=event['file'],sha256=event['sha256']))
    manifest=read('process-manifest.json')
    pids={item['identity']['ProcessId'] for item in manifest}
    for event in events:
        if event['kind']=='DB_STOP_DISPATCHED':
            pids.add(event['postmaster']['ProcessId'])
            pids.update(c['ProcessId'] for c in event['children'])
    current=ps("@(Get-CimInstance Win32_Process -Filter '"+' OR '.join('ProcessId='+str(p) for p in sorted(pids))+"' | Select-Object ProcessId,ParentProcessId,@{n='Created';e={$_.CreationDate.ToUniversalTime().ToString('o')}}) | ConvertTo-Json -Compress")
    if isinstance(current,dict): current=[current]
    expected={item['identity']['ProcessId']:item['identity'] for item in manifest}
    reused=[]
    remaining=[]
    for process in current:
        previous=expected.get(process['ProcessId'])
        if previous and process['Created']>previous['Created'] and process['ParentProcessId']!=previous['ParentProcessId']:
            reused.append(dict(previous=previous,current=process,action='READ_ONLY_NO_CONTROL',classification='PID_REUSED_AFTER_ORIGINAL_EXIT'))
        else: remaining.append(process)
    listeners=ps('@(Get-NetTCPConnection -State Listen -LocalPort 5432,55433,55434 -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess) | ConvertTo-Json -Compress')
    if isinstance(listeners,dict): listeners=[listeners]
    (EV/'listeners-after.json').write_text(json.dumps(listeners,indent=2),encoding='utf8')
    old_listeners=read('listeners-before.json')
    key=lambda x:(x['LocalPort'],x['LocalAddress'],x['OwningProcess'])
    existing_preserved=sorted(filter(lambda x:x['LocalPort']==5432,listeners),key=key)==sorted(filter(lambda x:x['LocalPort']==5432,old_listeners),key=key)
    lab_listeners=[x for x in listeners if x['LocalPort'] in (55433,55434)]
    absent={name:not (RUN/name).exists() for name in ('data','secrets')}
    assert not changed and not source_errors and not remaining and not lab_listeners and all(absent.values()) and existing_preserved
    # O resumo não muda resultados operacionais, nem interpreta fast como crash.
    stops=[e for e in events if e['kind']=='DB_STOP_DISPATCHED']
    assert len(stops)==1 and stops[0]['mode']=='fast'
    assert results['crashes']==results['restarts']==0
    c01=read('C01.json')
    assert c01['evidence']['before']==c01['evidence']['after']
    assert c01['evidence']['reconciliation']['status']=='NOT_FOUND'
    transcript=[json.loads(line) for line in (EV/'sql-transcript.jsonl').read_text(encoding='utf8').splitlines()]
    observed=[row for row in transcript if row['session']=='initial-observer' and row['sql']=="SELECT lab.lookup(1,'C02-2');"]
    observer=json.loads(observed[-1]['data'][0])
    assert observer['status']=='CONFIRMED'
    (EV/'C02-partial-evidence.json').write_text(json.dumps(dict(run_id=RUN.name,status='FALHOU',observer=observer,barriers=[e for e in events if 28<=e['sequence']<=45],missing=['new_worker_lookup','idempotent_retry','postcommit_full_snapshot'],stderr_incomplete=True),indent=2),encoding='utf8')
    total=sum(p.stat().st_size for p in RUN.rglob('*') if p.is_file())
    ev_size=sum(p.stat().st_size for p in EV.rglob('*') if p.is_file())
    assert total<256*1024**2 and ev_size<32*1024**2
    report=dict(run_id=RUN.name,delivery_verified=True,technical_gate_approved=False,preserved_files=len(before),changed=changed,executed_source_errors=source_errors,events=len(events),snapshots=snapshots,checked_process_ids=sorted(pids),remaining_processes=remaining,reused_pids=reused,lab_listeners=lab_listeners,existing_5432_listener_preserved=existing_preserved,absent=absent,shutdown_proven=shutdown['stopped'],crashes=0,restarts=0,elapsed_s=results['elapsed_s'],peak_run_bytes=shutdown['peak_bytes'],current_run_bytes=total,current_evidence_bytes=ev_size,results=[dict(case=x['case'],status=x['status']) for x in results['results']],sql_final_integrity='NOT_EXECUTED',crash_recovery='NOT_EXECUTED',stderr_complete=False)
    (EV/'delivery-validation.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    artifacts=list(RUN.rglob('*'))+list((ROOT/'docs').glob('ORG-03-E3-A-R2*.md'))+list(Path(__file__).parent.glob('*.py'))
    (EV/'artifact-sha256.json').write_text(json.dumps({p.relative_to(ROOT).as_posix():sha(p) for p in artifacts if p.is_file() and p.name!='artifact-sha256.json'},indent=2),encoding='utf8')
    print(json.dumps(report,ensure_ascii=False))


if __name__=='__main__': main()
