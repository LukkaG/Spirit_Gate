"""Preflight autorizado antes do claim; não cria cluster, SQL ou processo persistente."""
import hashlib
import json
from pathlib import Path
import shutil
import socket
import subprocess
import sys
from datetime import datetime,timezone,timedelta
from contextlib import ExitStack

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70'
SOURCE=Path('C:/Users/lucas/.codex/attachments/6747ad46-c476-4f6b-8a05-f94d21203023/Pasted text.txt')


def execute(args,timeout=8):
    """Somente metadados/versões; nenhum comando SQL, start ou stop."""
    value=subprocess.run([str(x) for x in args],capture_output=True,text=True,encoding='utf8',timeout=timeout,creationflags=subprocess.CREATE_NO_WINDOW)
    assert value.returncode==0, 'PREFLIGHT_COMMAND_FAILED'
    return value


def sha(path):
    """Hash sem alteração do alvo."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    """Registra o aceite recebido e prova disponibilidade/isolo antes de uso único."""
    sys.path.insert(0,str(RUN/'scripts'))
    import config
    assert datetime.now(timezone(timedelta(hours=-3))).strftime('%Y%m%d')==RUN.name[:8], 'AUTHORIZED_DATE_EXPIRED'
    assert not any((RUN/name).exists() for name in ('data','secrets','execution.claim','evidence','operational-authorization.json'))
    prepared=json.loads((RUN/'preparation/artifact-sha256.json').read_text(encoding='utf8'))
    errors=[name for name,value in prepared.items() if sha(ROOT/name)!=value]
    assert not errors, 'PREPARED_PACKAGE_CHANGED'
    before=json.loads((RUN/'preparation/preservation-before.json').read_text(encoding='utf8'))
    changed=[name for name,value in before.items() if sha(ROOT/name)!=value]
    assert not changed,'PREVIOUS_FILES_CHANGED'
    evidence=RUN/'operational-preflight'; evidence.mkdir()
    authorization=dict(responsavel='Lucas',date='2026-11-15',received_explicitly=True,
        run_id=RUN.name,run_path=str(RUN),items={f'R3-{n:02}':'AUTORIZADO' for n in range(2,6)},
        host=config.HOST,ports=[config.DB_PORT,config.RELAY_PORT],limits=config.LIMITS,
        stdout_limits=dict(relay_line_bytes=16384,worker_line_bytes=262144,process_total_bytes=4194304,queue_lines=64),
        authorization_source_sha256=sha(SOURCE))
    config.validate_authorization(authorization)
    with (RUN/'operational-authorization.json').open('x',encoding='utf8') as output:
        json.dump(authorization,output,ensure_ascii=False,indent=2)
    (evidence/'authorization-source.txt').write_bytes(SOURCE.read_bytes())
    config.require_operational(); config.verify_prepared_sources()
    for path in (RUN,*RUN.parents): assert not path.is_symlink() and not path.is_junction(),'REPARSE_POINT'
    assert RUN.resolve().parent==(ROOT/'temp/org03-e3a').resolve()
    assert shutil.disk_usage(RUN).free>=2*1024**3,'NO_FREE_SPACE'
    sid=execute(['whoami','/user','/fo','csv','/nh']).stdout.strip().split(',')[-1].strip('"')
    assert sid.startswith('S-1-')
    acl_script="$acl=Get-Acl -LiteralPath '"+str(RUN)+"'; [pscustomobject]@{ownerSid=$acl.GetOwner([System.Security.Principal.SecurityIdentifier]).Value;rules=@($acl.Access|ForEach-Object{[pscustomobject]@{sid=$_.IdentityReference.Translate([System.Security.Principal.SecurityIdentifier]).Value;rights=$_.FileSystemRights.ToString();type=$_.AccessControlType.ToString()}})}|ConvertTo-Json -Depth 6 -Compress"
    acl=json.loads(execute(['powershell','-NoProfile','-Command',acl_script]).stdout)
    assert acl['ownerSid']==sid,'RUN_OWNER_SID_MISMATCH'
    assert any(rule['sid']==sid and rule['type']=='Allow' and 'FullControl' in rule['rights'] for rule in acl['rules']),'RUN_WRITE_PERMISSION_MISSING'
    binaries={}
    old=json.loads((ROOT/'temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/preflight.json').read_text(encoding='utf8'))['binaries']
    for name in ('postgres','psql','initdb','pg_ctl'):
        path=Path('C:/Program Files/PostgreSQL/18/bin')/(name+'.exe')
        for parent in (path,*path.parents): assert not parent.is_symlink() and not parent.is_junction()
        version=execute([path,'--version'],timeout=5).stdout.strip()
        value=sha(path)
        assert '18.6' in version and value==old[name]['sha256'],'EXISTING_BINARY_CHANGED'
        binaries[name]=dict(version=version,sha256=value)
    script='@(Get-NetTCPConnection -State Listen -LocalPort 55435,55436 -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,OwningProcess) | ConvertTo-Json -Compress'
    value=execute(['powershell','-NoProfile','-Command',script])
    listeners=json.loads(value.stdout) if value.stdout.strip() else []
    assert not listeners,'APPROVED_PORT_CONFLICT_NO_FALLBACK'
    with ExitStack() as stack:
        for port in (55435,55436):
            handle=stack.enter_context(socket.socket())
            handle.setsockopt(socket.SOL_SOCKET,socket.SO_EXCLUSIVEADDRUSE,1)
            handle.bind(('127.0.0.1',port))
    result=dict(run_id=RUN.name,date_local=datetime.now(timezone(timedelta(hours=-3))).isoformat(),
        claim_consumed=False,cluster_created=False,sql_executed=False,
        prepared_artifacts=len(prepared),prepared_errors=errors,preserved_files=len(before),preservation_changes=changed,
        ports=[55435,55436],listeners_before=[],exclusive_bind_verified=True,free_bytes=shutil.disk_usage(RUN).free,
        sid=sid,run_acl=acl,secret_directory_absent=True,
        secret_acl_sequence='Runtime will create secrets, protect DACL and verify exclusive executor SID before writing secrets',
        binaries=binaries,pg_environment_names=[name for name in __import__('os').environ if name.upper().startswith('PG')],
        old_postgres_5432='NO_CONNECTION_OR_QUERY_OR_CONTROL',passed=True)
    (evidence/'preclaim-preflight.json').write_text(json.dumps(result,indent=2),encoding='utf8')
    print(json.dumps(result,ensure_ascii=False))


if __name__=='__main__': main()
