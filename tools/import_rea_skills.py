"""Importa somente VFX finais do REA, validando frames contra os metadados de origem."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
DIRECTIONS = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW']


def digest(data):
    return hashlib.sha256(data).hexdigest()


def import_pack(source, destination):
    """Escolhe a maior revisão numérica e preserva pixels, pivôs e tempos de cada fase."""
    selected = {}
    for path in (source / 'Skill_Collection_v1').glob('*/*/manifest.json'):
        match = re.fullmatch(r'([A-Z]+_[1-5])_v(\d+)', path.parent.name)
        if match and int(match[2]) > selected.get(match[1], (0, None))[0]:
            selected[match[1]] = (int(match[2]), path)
    water = source / 'WATER_Pack_v1/manifest.json'
    for slot in range(1, 6):
        selected[f'WATER_{slot}'] = (1, water)
    assert len(selected) == 40, f'Esperadas 40 habilidades, encontradas {len(selected)}'
    records, skipped, warnings, lines = [], [], [], ['# VFX REA: pixels originais, oito direções, tempos por frame.']
    destination.mkdir(parents=True, exist_ok=True)
    contact = Image.new('RGB', (1000, 8 * 150), '#142230')
    for skill_index, (ability, (version, manifest)) in enumerate(sorted(selected.items())):
        bundle = json.loads(manifest.read_text(encoding='utf-8'))
        clips = [c for c in bundle['clips'] if c['ability'] == ability]
        preview = None
        for clip in clips:
            phase = clip['phase']
            # Poses contêm personagem/cabelo: não são uma camada de habilidade.
            if clip.get('art_role') == 'character' or phase in {'body_action', 'body_combo', 'body_stomp', 'attacker', 'target'}:
                skipped.append(f'{ability}:{phase}')
                continue
            folder = manifest.parent / ability
            sheet = folder / clip['sheet']
            metadata = json.loads(sheet.with_suffix('.json').read_text(encoding='utf-8'))
            aseprite = json.loads(sheet.with_suffix('.aseprite.json').read_text(encoding='utf-8'))
            bitmap = Image.open(sheet)
            assert bitmap.mode == 'RGBA', sheet
            cell, count, rows = metadata['cell'], metadata['frames'], metadata['rows']
            assert rows in [DIRECTIONS, ['RADIAL']], (sheet, rows)
            assert bitmap.size == (cell * count, cell * len(rows)), (sheet, bitmap.size)
            times = metadata['durations_ms']
            assert len(times) == count and all(0 < ms <= 2000 for ms in times), sheet
            native = list(aseprite['frames'].items())
            assert len(native) == count * len(rows), sheet
            info = metadata['frames_info']
            assert len(info) == len(native), sheet
            for index, (name, frame) in enumerate(native):
                row, column = divmod(index, count)
                rect = frame['frame']
                assert rect == dict(x=column*cell, y=row*cell, w=cell, h=cell), (sheet, name)
                assert frame['duration'] == times[column], (sheet, name, 'tempo')
                assert not frame.get('rotated') and not frame.get('trimmed'), (sheet, name)
                expected = info[index]
                assert expected['direction'] == rows[row] and expected['index'] == column, (sheet, name, 'ordem')
                crop = bitmap.crop((column*cell, row*cell, (column+1)*cell, (row+1)*cell))
                reference = expected.get('sha256_rgba', clip['frames_info'][index].get('sha256_rgba'))
                if reference:
                    assert digest(crop.tobytes()) == reference, (sheet, name, 'pixels')
                individual = folder / 'Frames' / phase / name
                with Image.open(individual) as original:
                    assert original.convert('RGBA').tobytes() == crop.tobytes(), (sheet, name, 'PNG individual')
                if crop.getchannel('A').getbbox() is None:
                    warnings.append(f'{ability}:{phase}:{rows[row]}:{column}:frame transparente na origem')
            assert bitmap.getchannel('A').getextrema()[0] == 0, sheet
            target = destination / ability.lower() / sheet.name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(sheet, target)
            shutil.copyfile(sheet.with_suffix('.json'), target.with_suffix('.json'))
            prefix = ability.lower() + '.rea.' + phase + '.'
            values = dict(path=target.relative_to(ROOT).as_posix(), cell=cell, frames=count,
                          rows=len(rows), durations=','.join(map(str, times)),
                          pivotX=metadata['pivot_px'][0], pivotY=metadata['pivot_px'][1],
                          loop=str(metadata['loop']).lower(), attachment=metadata.get('attachment', 'engine_effect_origin'))
            lines.extend(prefix + key + '=' + str(value) for key, value in values.items())
            records.append(dict(skill=ability.lower(), version=version, phase=phase,
                                source=str(sheet.relative_to(source)), sha256=digest(sheet.read_bytes()),
                                frames=count*len(rows), metadata=values))
            if preview is None and not phase.startswith(('air_', 'weapon_')):
                row = 2 if len(rows) == 8 else 0
                preview = bitmap.crop((cell*(count//2), row*cell, cell*(count//2+1), (row+1)*cell))
        if preview is not None:
            preview.thumbnail((180, 110), Image.Resampling.NEAREST)
            x, y = skill_index % 5 * 200, skill_index // 5 * 150
            contact.paste(preview, (x+(200-preview.width)//2, y+30), preview)
            ImageDraw.Draw(contact).text((x+8, y+8), f'{ability} v{version}', fill='white')
    (destination/'manifest.properties').write_text('\n'.join(lines)+'\n', encoding='utf-8')
    report = dict(skills={k.lower(): v[0] for k,v in sorted(selected.items())},
                  clips=records, skipped_character_layers=skipped, warnings=warnings,
                  verified_frames=sum(c['frames'] for c in records))
    (destination/'provenance.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    contact.save(destination/'contact.png')
    print(json.dumps(dict(skills=len(selected), clips=len(records), frames=report['verified_frames'],
                          skipped=len(skipped), warnings=len(warnings))))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    args = parser.parse_args()
    import_pack(args.source, ROOT/'assets/skills/rea-v1')
