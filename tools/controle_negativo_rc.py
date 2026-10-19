"""Adultera somente uma copia nova e exige recusa do auditor oficial."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import zipfile
from auditar_rc import audit, AuditFailure

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('report', type=Path)
    args = parser.parse_args()
    if args.output.exists():
        raise FileExistsError(args.output)
    with zipfile.ZipFile(args.source) as src, zipfile.ZipFile(args.output, 'x', zipfile.ZIP_DEFLATED) as dst:
        for info in src.infolist():
            data = src.read(info)
            if info.filename.endswith('spirit-gate-arena.jar'):
                data += b'ADULTERADO'
            dst.writestr(info, data)
    digest = hashlib.sha256(args.output.read_bytes()).hexdigest()
    Path(str(args.output) + '.sha256').write_text(digest + '  ' + args.output.name + '\n')
    shutil.copyfile(str(args.source) + '.manifest.json', str(args.output) + '.manifest.json')
    result = {'checks': [], 'expected': 'FAIL'}
    try:
        audit(args.output, result)
    except AuditFailure as error:
        result.update(status='FAIL', negativeControl='PASS', error=str(error))
    else:
        raise RuntimeError('Auditor aceitou pacote adulterado')
    args.report.write_text(json.dumps(result, indent=2), encoding='utf-8')

if __name__ == '__main__':
    main()
