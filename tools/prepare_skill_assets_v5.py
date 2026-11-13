"""Registra e empacota três ciclos V5, sem repintura ou ampliação das fontes geradas."""
from pathlib import Path
import hashlib
import json
import sys
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "assets/skills/refined-v5/art"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(destination, preview):
    """Usa recortes e apoios explícitos; uma única escala por sequência conserva o crescimento."""
    destination.mkdir(parents=True, exist_ok=True)
    preview.mkdir(parents=True, exist_ok=True)
    old = destination / "provenance.json"
    if old.exists():
        for clip in json.loads(old.read_text(encoding="utf-8"))["clips"]:
            path = destination / clip["file"]
            if path.exists() and sha(path) != clip["sha256"]:
                raise ValueError(f"Atlas editado manualmente: {path}")
    specs = [
        ("fire_4", "fire-eruption", [0, 360, 800, 1086],
         [(189, 302), (198, 304), (200, 305), (200, 305), (190, 331), (199, 331), (196, 331), (200, 331), (192, 224), (200, 224), (200, 224), (193, 224)],
         (384, 448), (240, 280), 100),
        ("fire_3", "fire-vortex", [0, 360, 800, 1086],
         [(180, 316), (180, 316), (186, 316), (184, 316), (184, 327), (184, 327), (187, 327), (187, 327), (185, 204), (184, 204), (182, 200), (192, 200)],
         (320, 400), (192, 240), 110),
        ("water_4", "water-vortex", [0, 375, 815, 1086],
         [(184, 342), (190, 345), (190, 346), (190, 348), (190, 373), (193, 373), (190, 373), (190, 373), (190, 207), (190, 207), (185, 200), (190, 190)],
         (320, 400), (192, 240), 110),
    ]
    records = []
    properties = []
    qa = {"clips": 3, "frames": 36, "empty_frames": 0, "edge_pixels": 0, "upscaled_frames": 0, "warnings": [], "lifecycles": {}}
    contact = Image.new("RGB", (1200, 900), (20, 34, 48))
    for row, (name, source, ys, anchors, canvas, draw, millis) in enumerate(specs):
        path = ART / (source + ".png")
        bitmap = Image.open(path).convert("RGBA")
        if bitmap.size != (1448, 1086):
            raise ValueError(f"Fonte mudou de geometria: {path}")
        rects = [(i % 4 * 362, ys[i // 4], (i % 4 + 1) * 362, ys[i // 4 + 1]) for i in range(12)]
        originals = [bitmap.crop(rect) for rect in rects]
        # O recorte comum é relativo ao apoio fixo, não à caixa individual de cada estágio.
        boxes = [frame.getbbox() for frame in originals]
        if any(box is None for box in boxes):
            raise ValueError(f"Frame vazio: {name}")
        left = min(box[0] - anchor[0] for box, anchor in zip(boxes, anchors))
        top = min(box[1] - anchor[1] for box, anchor in zip(boxes, anchors))
        right = max(box[2] - anchor[0] for box, anchor in zip(boxes, anchors))
        bottom = max(box[3] - anchor[1] for box, anchor in zip(boxes, anchors))
        width, height = right - left, bottom - top
        scale = min(1, (canvas[0] - 16) / width, (canvas[1] - 16) / height)
        size = (round(width * scale), round(height * scale))
        offset = ((canvas[0] - size[0]) // 2, (canvas[1] - size[1]) // 2)
        pivot = (round(offset[0] - left * size[0] / width), round(offset[1] - top * size[1] / height))
        frames, areas = [], []
        for index, (original, anchor) in enumerate(zip(originals, anchors)):
            alpha = original.getchannel("A")
            boundaries = [alpha.crop((0, 0, original.width, 1)), alpha.crop((0, original.height - 1, original.width, original.height)), alpha.crop((0, 0, 1, original.height)), alpha.crop((original.width - 1, 0, original.width, original.height))]
            if any(part.getextrema()[1] >= 128 for part in boundaries):
                raise ValueError(f"Arte cruza o recorte: {name}/{index}")
            registered = Image.new("RGBA", (width, height))
            registered.alpha_composite(original, (-anchor[0] - left, -anchor[1] - top))
            frame = Image.new("RGBA", canvas)
            frame.alpha_composite(registered.resize(size, Image.Resampling.NEAREST), offset)
            a = frame.getchannel("A")
            areas.append(sum(value >= 128 for value in a.get_flattened_data()))
            if not a.getbbox() or a.getbbox()[0] == 0 or a.getbbox()[1] == 0 or a.getbbox()[2] == canvas[0] or a.getbbox()[3] == canvas[1]:
                raise ValueError(f"Frame vazio ou na borda: {name}/{index}")
            frames.append(frame)
        peak = max(areas)
        if areas[0] > peak * .1 or areas[-1] > peak * .1 or areas.index(peak) not in (4, 5, 6):
            raise ValueError(f"Envelope de nascimento/dissipação inválido: {name}")
        qa["lifecycles"][name] = dict(opaque_pixels=areas, first_peak_ratio=round(areas[0] / peak, 4), last_peak_ratio=round(areas[-1] / peak, 4))
        atlas = Image.new("RGBA", (canvas[0] * 4, canvas[1] * 3))
        for index, frame in enumerate(frames):
            atlas.alpha_composite(frame, (index % 4 * canvas[0], index // 4 * canvas[1]))
        target = destination / (name + ".png")
        atlas.save(target)
        values = dict(path=f"assets/skills/refined-v5/{target.name}", frameWidth=canvas[0], frameHeight=canvas[1], columns=4, frames=12, frameMillis=millis, drawWidth=draw[0], drawHeight=draw[1], pivotX=pivot[0], pivotY=pivot[1])
        for alias in ([name, name + ".impact"] if name == "fire_4" else [name]):
            properties.extend(f"{alias}.{key}={value}" for key, value in values.items())
        records.append(dict(id=name, file=target.name, sha256=sha(target), source=path.relative_to(ROOT).as_posix(), source_sha256=sha(path), rects=rects, anchors=anchors, common_box=[left, top, right, bottom], scale=scale, metadata=values))
        previews = []
        for frame in frames:
            background = Image.new("RGBA", draw, (20, 34, 48, 255))
            background.alpha_composite(frame.resize(draw, Image.Resampling.NEAREST))
            previews.append(background.convert("RGB"))
        previews[0].save(preview / (name + ".gif"), save_all=True, append_images=previews[1:], duration=millis, loop=0, disposal=2)
        for column, index in enumerate([0, 3, 5, 11]):
            shot = previews[index]
            contact.paste(shot, (column * 300 + (300 - shot.width) // 2, row * 300 + 18))
            ImageDraw.Draw(contact).text((column * 300 + 10, row * 300 + 285), f"{name} frame {index + 1}", fill="white")
    contact.save(preview / "contact.png")
    (preview / "qa.json").write_text(json.dumps(qa, indent=2) + "\n", encoding="utf-8")
    (destination / "provenance.json").write_text(json.dumps(dict(route="image_gen", model=None, estimateUsd=None, clips=records, packaging="Recorte, registro explícito e redução nearest; sem ampliação, repintura ou recoloração."), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    pending = destination / "manifest.properties.pending"
    pending.write_text("# Overlay V5: ciclos completos, apos V4.\nformatVersion=5\n" + "\n".join(properties) + "\n", encoding="ascii")
    pending.replace(destination / "manifest.properties")
    print(json.dumps(qa))


if __name__ == "__main__":
    build(Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "assets/skills/refined-v5", Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "docs/skills-preview-v5")
