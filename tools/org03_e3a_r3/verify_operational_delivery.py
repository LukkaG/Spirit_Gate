"""Auditoria pós-rodada R3: somente evidências e metadados próprios, sem SQL/start."""
import hashlib
import json
from pathlib import Path
import subprocess
from datetime import datetime,timezone

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70'
EV=RUN/'evidence'


def read(name):
    """Evidência operacional própria, já encerrada."""
    return json.loads((EV/name).read_text(encoding='utf8'))


def sha(path):
    """Hash sem executar ou regravar originais."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def canonical(value):
    """Mesmo hash canônico dos snapshots do runner."""
    return hashlib.sha256(json.dumps(value,sort_keys=True,separators=(',',':')).encode()).hexdigest()


def metadata(script):
    """CIM/TCP em leitura; sem kill, SQL, conexão TCP ou consulta a 5432."""
    result=subprocess.run(['powershell','-NoProfile','-Command',script],capture_output=True,text=True,encoding='utf8',timeout=8,creationflags=subprocess.CREATE_NO_WINDOW)
    assert result.returncode==0,'DELIVERY_METADATA_FAILED'
    value=json.loads(result.stdout) if result.stdout.strip() else []
    return value if isinstance(value,list) else [value]


def validate_snapshot(value):
    """Confere revisões, versões, recursos e bijeção de recibos/índices/grants."""
    assert len(value)==4
    for item in value:
        image=item['image']; head=image['head']
        receipts={r['op']:r for r in item['receipts']}
        operations={o['op']:o for o in item['operations']}
        grants={g['op']:g for g in item['grants']}
        assert receipts.keys()==operations.keys()==grants.keys()
        assert head['er']==len(receipts)==image['receipts']==image['operations']==image['grants']
        assert len(item['grants'])==len(grants)
        versions={v['sr']:v for v in item['versions']}
        assert versions[head['sr']]['payload']==image['state'] and versions[head['sr']]['er']==head['er']
        for op,r in receipts.items():
            version=versions[r['sr']]
            assert version['er']==r['er']
            receipt=r['result']
            if receipt is None:
                archive=EV/r['archive']
                assert archive.resolve().parent==EV.resolve() and sha(archive)==r['digest']
                receipt=json.loads(archive.read_text(encoding='utf8'))
            assert receipt['op']==op and receipt['state']==version['payload']
            assert operations[op]['intent']['op']==op
            assert operations[op]['intent']['grant']==grants[op]['grant_id']
            assert operations[op]['intent']['origin']==grants[op]['origin']


def main():
    """Todas as asserções precisam passar; falha não reabre a rodada encerrada."""
    before=json.loads((RUN/'preparation/preservation-before.json').read_text(encoding='utf8'))
    changed=[p for p,h in before.items() if not (ROOT/p).is_file() or sha(ROOT/p)!=h]
    prepared=json.loads((RUN/'preparation/artifact-sha256.json').read_text(encoding='utf8'))
    prepared_errors=[p for p,h in prepared.items() if sha(ROOT/p)!=h]
    sources=json.loads((RUN/'preparation/source-sha256.json').read_text(encoding='utf8'))
    source_errors=[p for p,h in sources.items() if sha(RUN/'scripts'/p)!=h]
    executed=read('source-sha256.json')
    assert all(executed[p]==h for p,h in sources.items() if '/' not in p)
    assert not (changed or prepared_errors or source_errors)
    results=read('results.json'); shutdown=read('shutdown.json'); final=read('final-integrity.json'); controls=read('controls.json')
    assert results['fatal'] is None and results['gate_approved']
    assert [r['case'] for r in results['results']]==[f'C{n:02}' for n in range(1,10)]
    assert all(r['status']=='PASSOU' and 0<r['duration_s']<=120 for r in results['results'])
    assert results['elapsed_s']<=1200
    assert results['crashes']==2 and results['restarts']==4 and results['workers_created']==28 and results['relay_instances']==2
    assert results['unique_intentions']==15 and max(results['attempts'].values())<=3
    assert shutdown['stopped'] and shutdown['discarded']==['data','secrets'] and not shutdown['cleanup_errors']
    assert shutdown['all_registered_processes_absent'] and not shutdown['listeners']
    assert all(not (RUN/p).exists() for p in ('data','secrets'))
    assert final['receipts']==final['operations']==final['grants']==final['economic_revision']==11
    assert final['orphan_operations']==final['orphan_grants']==final['orphan_heads']==final['pending_transactions']==0
    assert all(h['sr']==h['er']==0 and h['generation']==h['fence']==1 for h in final['heads'][4:])
    assert controls['stale_authority_sqlstate']=='P0003' and controls['checkpoint']['status']=='CONFLICT'
    assert not any(controls['flags'].values()) and controls['direct_read_denied']==controls['cross_owner_denied']=='42501'
    events=[json.loads(line) for line in (EV/'events.jsonl').read_text(encoding='utf8').splitlines()]
    assert [e['sequence'] for e in events]==list(range(1,len(events)+1))
    assert all(e['state'] in ('RUNNING_MATCH','EXITED_CONFIRMED') for e in events if e['kind']=='PROCESS_OBSERVATION')
    assert not any('ERROR' in e['kind'] for e in events)
    snapshots=[e for e in events if e['kind']=='SNAPSHOT_VALIDATED']
    for record in snapshots:
        value=read(record['file']); assert canonical(value)==record['sha256']; validate_snapshot(value)
    cases={r['case']:r['evidence'] for r in results['results']}
    for key in ('C01','C03','C04','C08'): assert cases[key]['before']==cases[key]['after']
    assert cases['C01']['reconciliation']['status']==cases['C03']['reconciliation']['status']=='NOT_FOUND'
    assert cases['C01']['old_pid']!=cases['C01']['new_pid']
    c02=cases['C02']; assert c02['postcommit']==c02['after']
    assert c02['result']['operation_id']==c02['intent']['op']==c02['permit']['operation_id']=='C02-2'
    assert c02['result']['economic_test']=='SCENARIO_PASSED'
    assert c02['completion']['sql_result']=='ECONOMIC_UNCERTAIN' and c02['completion']['economic_test']=='NOT_EVALUATED'
    assert c02['postcommit'][0]['image']['head']['er']==c02['before'][0]['image']['head']['er']+1
    terminals=[e['metadata'] for e in events if e['kind']=='RELAY_WIRE_EVENT' and e['metadata'].get('event')=='EXPECTED_CLIENT_DISCONNECT']
    assert len(terminals)==2
    for term in terminals:
        assert term['source']=='client' and term['operation']=='recv_client' and term['errno']==10054
        assert term['termination_requested'] and term['paused'] and term['held_bytes']==18
        assert term['received_server_bytes']==term['forwarded_server_bytes']+term['held_bytes']
    assert terminals[0]['permit_sha256']==canonical(c02['permit'])
    for case in ('C02','C05'):
        complete=cases[case]['completion']
        assert complete['process_result']=='RELAY_EXITED_CONFIRMED' and complete['process']['exit_code']==0
        assert complete['stderr']['capture_complete'] and complete['stderr']['read_error'] is None
        assert not complete['stderr']['truncated'] and not complete['stderr']['raw_bytes_persisted']
    assert cases['C05']['reconciliation']['status']=='CONFIRMED' and cases['C05']['caller_status_before_lookup']=='ECONOMIC_UNCERTAIN'
    assert cases['C06']['identified_pending']==['C06-B-7']
    assert cases['C06']['final'][2]==cases['C06']['partial'][2]
    assert cases['C06']['final'][3]['image']['head']['er']==cases['C06']['partial'][3]['image']['head']['er']+1
    assert len(cases['C07']['pending_after_restart'])==1 and cases['C07']['pending_after_restart'][0]['grant_id']=='C07-old'
    assert cases['C07']['partial'][0]['details']['watermarks'][0]['prefix']==0
    assert cases['C07']['final'][0]['details']['watermarks'][0]['prefix']==2
    assert all(obligation['paid_op'] for obligation in cases['C07']['final'][0]['details']['obligations'])
    assert cases['C08']['unavailable']==dict(status='UNCERTAIN',reason='ARCHIVE_UNAVAILABLE')
    assert cases['C08']['retry']['status']=='ARCHIVED'
    assert sha(EV/(cases['C08']['intent']['op']+'.archive.json'))==cases['C08']['archive_sha256']
    trials=cases['C09']['trials']
    assert [t['last_boundary'] for t in trials]==['PREPARED','COMMIT_ACK','COMMIT_DISPATCHED']
    assert trials[0]['classification']=='NOT_CONFIRMED' and trials[1]['classification']=='CONFIRMED'
    assert trials[2]['caller_status']=='ECONOMIC_UNCERTAIN' and trials[2]['reconciliation']['status']=='CONFIRMED' and trials[2]['ack_consumed_by_caller'] is False
    stops=[e for e in events if e['kind']=='DB_STOP_DISPATCHED']
    assert [e['mode'] for e in stops]==['immediate','immediate','fast','fast','fast']
    recoveries=[]
    for key in ('C03','C04','C06-clean','C07-clean'):
        value=read('recovery-'+key+'.json')
        assert Path(value['identity']['data']).resolve()==(RUN/'data').resolve() and value['identity']['port']=='55435'
        if key in ('C03','C04'):
            assert value['kind']=='CRASH_RECOVERY' and all(text in value['log'] for text in ('automatic recovery in progress','redo starts','redo done','ready to accept connections'))
        else: assert value['kind']=='CLEAN_RESTART'
        recoveries.append(dict(label=key,kind=value['kind'],restart_number=value['restart_number']))
    profiles=[]
    for path in EV.glob('profile-*.json'):
        profile=read(path.name)
        assert all(profile['settings'][key]['setting']=='on' for key in ('fsync','synchronous_commit','full_page_writes'))
        assert all(row['persistence']=='p' for row in profile['relations'])
        profiles.append(path.name)
    capture_events=[e for e in events if e['kind']=='RELAY_STDERR']
    assert all(e['capture_complete'] and not e['truncated'] and not e['read_error'] for e in capture_events)
    log=(EV/'postgres.log').read_text(encoding='utf8')
    assert 'PANIC:' not in log and 'FATAL:' not in log
    transcript=[json.loads(line) for line in (EV/'sql-transcript.jsonl').read_text(encoding='utf8').splitlines()]
    assert set(r['sqlstate'] for r in transcript)<={'00000','P0003','42501'}
    registry=read('process-manifest.json')
    expected={item['identity']['ProcessId']:item['identity'] for item in registry}
    expected.update({e['process']['ProcessId']:e['process'] for e in events if e['kind']=='IDENTITY_VERIFIED'})
    pids=sorted(expected)
    current=metadata("@(Get-CimInstance Win32_Process -Filter '"+' OR '.join('ProcessId='+str(pid) for pid in pids)+"' | Select-Object ProcessId,ParentProcessId,@{n='Created';e={$_.CreationDate.ToUniversalTime().ToString('o')}}) | ConvertTo-Json -Compress")
    reused=[]
    for process in current:
        old=expected[process['ProcessId']]
        assert process['Created']>old['Created'],'ORIGINAL_PROCESS_REMAINS_OR_UNCERTAIN'
        reused.append(dict(previous=old,current=process,action='NO_CONTROL',classification='PID_REUSED_AFTER_ORIGINAL_EXIT'))
    listeners=metadata('@(Get-NetTCPConnection -State Listen -LocalPort 55435,55436 -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess) | ConvertTo-Json -Compress')
    assert not listeners
    launch=json.loads((RUN/'operational-preflight/controller-completion.json').read_text(encoding='utf8'))
    assert launch['returncode']==0
    intents=list(EV.glob('*.intent.json')); assert len(intents)==15
    assert all(p.stat().st_size<=16384 for p in intents)
    tree=sum(p.stat().st_size for p in RUN.rglob('*') if p.is_file())
    evidence_bytes=sum(p.stat().st_size for p in RUN.rglob('*') if p.is_file() and 'scripts' not in p.relative_to(RUN).parts)
    assert tree<=256*1024**2 and evidence_bytes<=32*1024**2 and shutdown['peak_bytes']<=256*1024**2
    report=dict(run_id=RUN.name,verified_utc=datetime.now(timezone.utc).isoformat(),verdict='APROVADO',scope='ORG-03-E3-A synthetic lab only',
        cases=[dict(case=r['case'],status=r['status'],duration_s=r['duration_s']) for r in results['results']],
        preserved_files=len(before),preservation_changes=changed,prepared_artifact_errors=prepared_errors,source_errors=source_errors,
        events=len(events),snapshots=len(snapshots),profiles=profiles,sql_transcript_rows=len(transcript),sqlstates=sorted({r['sqlstate'] for r in transcript}),
        final_integrity=final,recoveries=recoveries,crashes=2,restarts=4,worker_instances=28,relay_instances=2,
        intentions=15,attempts_total=sum(results['attempts'].values()),attempts_max=max(results['attempts'].values()),
        elapsed_s=results['elapsed_s'],peak_tree_bytes=shutdown['peak_bytes'],current_tree_bytes=tree,all_logs_evidence_bytes=evidence_bytes,
        capture_streams=len(capture_events),capture_errors=[],terminal_relays=terminals,post_run_pid_reuse=reused,
        lab_listeners=[],own_original_processes_remaining=[],data_absent=True,secrets_absent=True,
        old_postgres_5432='NO_CONNECTION_QUERY_OR_CONTROL',no_following_gate=True,passed=True)
    (EV/'delivery-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(report,ensure_ascii=False))


if __name__=='__main__': main()
