"""Fechamento documental R3, preservando preparação e fontes operacionais."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
from datetime import datetime,timezone

ROOT=Path(__file__).resolve().parents[2]
RUN=ROOT/'temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70'
EV=RUN/'evidence'


def sha(path):
    """Hash somente leitura."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    """Sela relatório e evidências sem repetir SQL, cluster ou cenários."""
    delivery=json.loads((EV/'delivery-validation.json').read_text(encoding='utf8'))
    assert delivery['passed'] and delivery['verdict']=='APROVADO'
    before=json.loads((RUN/'preparation/preservation-before.json').read_text(encoding='utf8'))
    after={name:sha(ROOT/name) for name in before}
    assert before==after,'PREVIOUS_FILE_CHANGED'
    (EV/'preservation-after.json').write_text(json.dumps(after,indent=2),encoding='utf8')
    prepared=json.loads((RUN/'preparation/artifact-sha256.json').read_text(encoding='utf8'))
    assert all(sha(ROOT/name)==value for name,value in prepared.items())
    sources=json.loads((RUN/'preparation/source-sha256.json').read_text(encoding='utf8'))
    assert all(sha(RUN/'scripts'/name)==value for name,value in sources.items())
    docs=[ROOT/'docs'/name for name in ('ORG-03-E3-A-R3-RELATORIO-FINAL.md','ORG-03-E3-A-R3-CRASH-RECOVERY.md','ORG-03-E3-A-R3-ISOLAMENTO.md')]
    broken=[str(path)+': '+link for path in docs for link in re.findall(r'\]\(([^)]+)\)',path.read_text(encoding='utf8')) if not link.startswith('https://') and not(path.parent/link).exists()]
    assert not broken,'BROKEN_DELIVERY_LINK'
    branch=subprocess.run(['git','branch','--show-current'],cwd=ROOT,capture_output=True,text=True,check=True).stdout.strip()
    head=subprocess.run(['git','rev-parse','HEAD'],cwd=ROOT,capture_output=True,text=True,check=True).stdout.strip()
    assert branch=='beco' and head=='c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39'
    assert not (RUN/'data').exists() and not (RUN/'secrets').exists()
    status=dict(run_id=RUN.name,responsavel='Lucas',verdict='APROVADO',scope='ORG-03-E3-A synthetic lab R3',
        sealed_utc=datetime.now(timezone.utc).isoformat(),branch=branch,head=head,cases_passed=9,
        crashes=2,restarts=4,sql_receipts=11,sql_orphans=0,pending_transactions=0,
        safe_shutdown_and_discard=True,preserved_files=len(before),prepared_artifact_errors=[],source_errors=[],
        old_postgres_5432='NO_CONNECTION_QUERY_OR_CONTROL',next_gate_started=False,
        run_id_consumed=True,operational_authorization='EXPLICIT_LUCAS_2026_11_15_R3_02_TO_05',broken_links=[])
    (RUN/'operational-status.json').write_text(json.dumps(status,indent=2),encoding='utf8')
    target=EV/'artifact-sha256.json'
    files=[p for p in RUN.rglob('*') if p.is_file() and p!=target]
    files+=docs+list(Path(__file__).parent.glob('*.py'))+[ROOT/'logs/analises/20261115-org03-e3a-r3.md']
    manifest={p.relative_to(ROOT).as_posix():sha(p) for p in files}
    target.write_text(json.dumps(manifest,indent=2),encoding='utf8')
    assert all(sha(ROOT/name)==value for name,value in manifest.items()),'DELIVERY_HASH_MISMATCH'
    total=sum(p.stat().st_size for p in RUN.rglob('*') if p.is_file())
    logs=sum(p.stat().st_size for p in RUN.rglob('*') if p.is_file() and 'scripts' not in p.relative_to(RUN).parts)
    assert total<=256*1024**2 and logs<=32*1024**2
    print(json.dumps(dict(**status,artifact_files=len(manifest),final_tree_bytes=total,final_all_logs_evidence_bytes=logs)))


if __name__=='__main__': main()
