"""Teste de integracao explicito: python tools/test_visual_distribution.py ZIP REPORT.

Adiciona fixture sintetica SOMENTE em copia temporaria de um RC; recalcula todos
os inventarios e exige auditoria completa. A copia nunca e entregue como release.
"""
import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import zipfile
from auditar_rc import audit, AuditFailure
from pacote_visual import stage
import test_sprite_review


def run(source, report):
    fixture=test_sprite_review.SpriteReviewTest()
    fixture.setUp()
    try:
        root=fixture.root
        entry=copy.deepcopy(fixture.entry)
        entry['review']={'origin':'FIXTURE SINTETICA EXCLUSIVA DE TESTE',
            'approvedSha256':hashlib.sha256((root/'hero.png').read_bytes()).hexdigest()}
        manifest=root/'source.json'
        manifest.write_text(json.dumps(dict(packageVersion=1,assetId='synthetic.hero',assets=[entry])),encoding='utf-8')
        stage(root,manifest,root/'visual')
        with zipfile.ZipFile(source) as z:
            contents={i.filename.removeprefix('SpiritGate/'):z.read(i) for i in z.infolist() if not i.is_dir()}
        for p in (root/'visual').iterdir(): contents['visual/'+p.name]=p.read_bytes()
        config=json.loads(contents.pop('manifest.json').decode('utf-8-sig'))
        contents.pop('INVENTARIO-SHA256.txt')
        contents['INVENTARIO-SHA256.txt']=''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n' for name,data in sorted(contents.items())).encode()
        config['files']=[dict(path=name,sizeBytes=len(data),sha256=hashlib.sha256(data).hexdigest()) for name,data in sorted(contents.items())]
        contents['manifest.json']=json.dumps(config).encode()
        out=root/'synthetic-test-only.zip'
        def write():
            with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
                for name,data in contents.items():z.writestr('SpiritGate/'+name,data)
            Path(str(out)+'.sha256').write_text(hashlib.sha256(out.read_bytes()).hexdigest()+'  '+out.name+'\n')
            Path(str(out)+'.manifest.json').write_bytes(contents['manifest.json'])
        write()
        result={'checks':[]};audit(out,result)
        assert any(c['id']=='pacote_visual_aprovado_sha' for c in result['checks'])
        # Troca de hash revisado com ZIP/sidecar recalculado continua recusada.
        contents['visual/character.png']+=b'ALTERADO'
        write()
        try: audit(out,{'checks':[]})
        except (AuditFailure,ValueError): result['alteredVisualRejected']=True
        else:raise AssertionError('Auditor aceitou arte alterada')
        result.update(status='PASS',syntheticOnly=True,temporaryArchiveDestroyed=True)
        report.write_text(json.dumps(result,indent=2),encoding='utf-8')
    finally:fixture.doCleanups()


if __name__=='__main__':run(Path(sys.argv[1]),Path(sys.argv[2]))
