#!/usr/bin/env python3
"""Empacota efeitos existentes em quarenta clips, sem alterar o desenho das fontes.

Os recortes usam um enquadramento comum por animação. Escalas preservam a proporção
e usam nearest neighbour; composição e rotação apenas apresentam os frames já
fornecidos. Nenhum arquivo em --source-dir é alterado. Requer Pillow instalado.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw


GENERATOR = "prepare_skill_assets.py:v1"
ROOT = Path(__file__).resolve().parents[1]


@dataclass(frozen=True)
class Source:
    """Define o grid verificado e os quadros ativos de uma folha fornecida."""

    number: int
    columns: int
    rows: int
    frames: tuple[int, ...]


SOURCES = {
    "water_cut": Source(251, 3, 3, tuple(range(9))),
    "water_prison": Source(187, 3, 3, tuple(range(9))),
    "water_vortex": Source(134, 3, 1, (0, 1, 2)),
    "wind_cut": Source(253, 3, 3, tuple(range(9))),
    "wind_push": Source(235, 3, 1, (0, 1, 2)),
    "wind_pull": Source(238, 1, 3, (0, 1, 2)),
    "lightning": Source(96, 6, 1, tuple(range(6))),
    "lightning_lance": Source(34, 2, 5, tuple(range(9))),
    "lightning_dash": Source(307, 9, 1, tuple(range(9))),
    "fire_cut": Source(252, 3, 3, tuple(range(9))),
    "fire_line": Source(343, 10, 1, tuple(range(9))),
    "fire_vortex": Source(249, 4, 4, tuple(range(4, 12))),
    "fire_eruption": Source(249, 4, 4, tuple(range(16))),
    "pool": Source(349, 4, 1, (0, 1, 2, 3)),
    "web": Source(175, 4, 3, tuple(range(12))),
    "beam": Source(109, 4, 4, (12, 13, 14, 15)),
    "prison": Source(64, 5, 4, tuple(range(20))),
    "quicksand": Source(102, 4, 4, tuple(range(15))),
    "arrow": Source(341, 16, 1, tuple(range(6))),
    "arrow_rain": Source(339, 16, 1, tuple(range(6))),
    "heal": Source(131, 16, 1, (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)),
    "heal_area": Source(399, 5, 4, tuple(range(20))),
    "punch": Source(41, 3, 1, (0, 1, 2)),
    "impact": Source(6, 3, 1, (0, 1, 2)),
    "cut": Source(5, 3, 1, (0, 1, 2)),
    "fissure": Source(53, 15, 1, tuple(range(15))),
    "earth": Source(256, 8, 2, tuple(range(16))),
}


@dataclass(frozen=True)
class Clip:
    """Relaciona uma habilidade ao efeito e à sua apresentação no mundo."""

    id: str
    source: str
    mode: str = "single"
    canvas: tuple[int, int] = (128, 128)
    draw: tuple[int, int] = (128, 128)
    millis: int = 80
    extra: str | None = None
    loop: bool = False


CLIPS = [
    Clip("water_1", "water_cut"),
    Clip("water_2", "water_cut", "triple"),
    Clip("water_3", "water_prison", millis=110, loop=True),
    Clip("water_4", "water_vortex", draw=(160, 160), millis=110, loop=True),
    Clip("water_5", "water_cut", "triple", millis=60),
    Clip("wind_1", "wind_cut"),
    Clip("wind_2", "wind_cut", "triple", millis=60),
    Clip("wind_3", "wind_cut", millis=55),
    Clip("wind_4", "wind_push", millis=110),
    Clip("wind_5", "wind_pull", millis=110),
    Clip("thunder_1", "lightning", "horizontal", canvas=(192, 64), draw=(144, 48)),
    Clip("thunder_2", "lightning_lance", canvas=(192, 64), draw=(192, 64), millis=65),
    Clip("thunder_3", "lightning", "triple", millis=90),
    Clip("thunder_4", "lightning_dash", millis=55),
    Clip("thunder_5", "lightning", "ring", draw=(192, 192), millis=65),
    Clip("fire_1", "fire_cut"),
    Clip("fire_2", "fire_line", "line", canvas=(192, 96), draw=(192, 96), millis=100),
    Clip("fire_3", "fire_vortex", draw=(160, 160), millis=80, loop=True),
    Clip("fire_4", "fire_line", "eruption", canvas=(192, 128), draw=(192, 128), extra="fire_eruption"),
    Clip("fire_5", "fire_cut", "triple", draw=(192, 192), millis=90),
    Clip("spirit_1", "pool", "pool_web", extra="web", millis=110, loop=True),
    Clip("spirit_2", "web", millis=80),
    Clip("spirit_3", "beam", millis=100),
    Clip("spirit_4", "prison", draw=(160, 160), millis=95, loop=True),
    Clip("spirit_5", "quicksand", draw=(160, 160), millis=120, loop=True),
    Clip("aetheri_1", "arrow", canvas=(192, 64), draw=(144, 48), millis=65),
    Clip("aetheri_2", "arrow", "triple", millis=45),
    Clip("aetheri_3", "arrow_rain", "rain", draw=(192, 192), millis=75),
    Clip("aetheri_4", "arrow", canvas=(192, 64), draw=(240, 80), millis=60),
    Clip("aetheri_5", "arrow", "cone", draw=(192, 192), millis=60),
    Clip("light_1", "heal", millis=85),
    Clip("light_2", "heal_area", draw=(192, 192), millis=95),
    Clip("light_3", "heal", "revive", extra="heal_area", draw=(160, 160), millis=110),
    Clip("light_4", "beam", millis=75),
    Clip("light_5", "heal", draw=(176, 176), millis=70),
    Clip("martial_1", "punch", millis=85),
    Clip("martial_2", "punch", "triple", millis=55),
    Clip("martial_3", "fissure", "ground", extra="earth", draw=(192, 192), millis=85),
    Clip("martial_4", "impact", "slam", extra="punch", millis=110),
    Clip("martial_5", "cut", "cone", draw=(176, 176), millis=60),
]


def sha256(path: Path) -> str:
    """Obtém o hash da fonte ou saída para permitir auditoria sem caminhos privados."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_source(key: str, source: Source, source_dir: Path, water_variant: Path | None, variants: dict) -> tuple[list[Image.Image], dict]:
    """Recorta apenas o grid declarado, mantendo a mesma transformação dos frames."""
    entry = variants.get(key)
    if source.number == 251 and water_variant is not None:
        entry = {**(entry or {}), "resolved_path": water_variant, "prompt": "water-slash-prompt.txt"}
    variant = entry is not None
    path = Path(entry["resolved_path"]) if variant else source_dir / f"{source.number}.png"
    columns = entry.get("columns", source.columns) if variant else source.columns
    rows = entry.get("rows", source.rows) if variant else source.rows
    indices = tuple(entry.get("frames", source.frames)) if variant else source.frames
    image = Image.open(path).convert("RGBA")
    if not variant and (image.width % columns or image.height % rows):
        raise ValueError(f"Grid não divisível: {path.name} {image.size} {columns}x{rows}")
    width, height = math.ceil(image.width / columns), math.ceil(image.height / rows)
    frames, rects = [], []
    for index in indices:
        column, row = index % columns, index // columns
        rect = (round(column * image.width / columns), round(row * image.height / rows), round((column + 1) * image.width / columns), round((row + 1) * image.height / rows))
        cropped = image.crop(rect)
        frame = Image.new("RGBA", (width, height))
        frame.paste(cropped, ((width - cropped.width) // 2, (height - cropped.height) // 2))
        if frame.getchannel("A").getbbox() is None:
            raise ValueError(f"Quadro vazio: {path.name}, índice {index}")
        frames.append(frame)
        rects.append(list(rect))
    return frames, {
        "file": path.name,
        "original_file": f"{source.number}.png",
        "sha256": sha256(path),
        "original_sha256": sha256(source_dir / f"{source.number}.png"),
        "art_source": "host_image" if variant else "existing",
        "source_size": list(image.size),
        "source_grid": [columns, rows],
        "source_rects": rects,
        "source_indices": list(indices),
        "original_grid": [source.columns, source.rows],
        "original_source_indices": list(source.frames),
        "generation": {"route": "image_gen", "model": None, "estimateUsd": None, "prompt": entry.get("prompt"), "notes": "Model and cost were not returned by the built-in tool."} if variant else None,
    }


def common_crop(frames: list[Image.Image]) -> tuple[list[Image.Image], tuple[int, int, int, int]]:
    """Enquadra a sequência inteira sem recentralizar cada pose individualmente."""
    mask = Image.new("L", frames[0].size)
    for frame in frames:
        mask = ImageChops.lighter(mask, frame.getchannel("A"))
    left, top, right, bottom = mask.getbbox()
    rect = (max(0, left - 2), max(0, top - 2), min(mask.width, right + 2), min(mask.height, bottom + 2))
    return [frame.crop(rect) for frame in frames], rect


def fit(frame: Image.Image, bounds: tuple[int, int]) -> Image.Image:
    """Escala uniformemente, sem interpolar os pixels dos sprites fornecidos."""
    scale = min(bounds[0] / frame.width, bounds[1] / frame.height)
    return frame.resize((max(1, round(frame.width * scale)), max(1, round(frame.height * scale))), Image.Resampling.NEAREST)


def paste_center(canvas: Image.Image, frame: Image.Image, center: tuple[int, int]) -> None:
    """Compõe um frame com alfa nativo, sem pintar fundos ou recolorir a arte."""
    canvas.alpha_composite(frame, (round(center[0] - frame.width / 2), round(center[1] - frame.height / 2)))


def compose(clip: Clip, frames: list[Image.Image], extra: list[Image.Image] | None) -> list[Image.Image]:
    """Apresenta os recortes conforme a leitura visual de cada habilidade."""
    count = max(len(frames), len(extra) if extra else 0)
    result = []
    width, height = clip.canvas
    for index in range(count):
        canvas = Image.new("RGBA", clip.canvas)
        frame = frames[index % len(frames)]
        secondary = extra[index % len(extra)] if extra else None
        if clip.mode == "single":
            paste_center(canvas, fit(frame, (width - 16, height - 16)), (width // 2, height // 2))
        elif clip.mode == "horizontal":
            rotated = frame.rotate(90, resample=Image.Resampling.NEAREST, expand=True)
            paste_center(canvas, fit(rotated, (width - 16, height - 16)), (width // 2, height // 2))
        elif clip.mode == "triple":
            for offset, center in enumerate(((40, 38), (88, 63), (52, 91))):
                image = frames[(index + offset * 2) % len(frames)]
                paste_center(canvas, fit(image, (72, 64)), center)
        elif clip.mode == "ring":
            for direction in range(4):
                image = fit(frames[(index + direction) % len(frames)], (30, 62))
                image = image.rotate(90 * direction, resample=Image.Resampling.NEAREST, expand=True)
                angle = math.pi / 2 * direction
                center = (64 + round(math.sin(angle) * 29), 64 - round(math.cos(angle) * 29))
                paste_center(canvas, image, center)
        elif clip.mode == "cone":
            for angle in (-28, 0, 28):
                image = fit(frame, (104, 30)).rotate(angle, resample=Image.Resampling.NEAREST, expand=True)
                paste_center(canvas, image, (64, 64 + round(math.sin(math.radians(angle)) * 28)))
        elif clip.mode == "rain":
            for column in range(5):
                image = frames[(index + column * 2) % len(frames)]
                paste_center(canvas, fit(image, (18, 112)), (24 + column * 20, 64))
        elif clip.mode in ("line", "eruption"):
            for column in range(3):
                paste_center(canvas, fit(frames[(index + column) % len(frames)], (52, 68)), (36 + column * 53, height - 39))
            if secondary:
                paste_center(canvas, fit(secondary, (96, 110)), (138, 61))
        elif clip.mode == "pool_web":
            paste_center(canvas, fit(frame, (112, 34)), (64, 94))
            paste_center(canvas, fit(secondary, (96, 96)), (64, 58))
        elif clip.mode == "revive":
            paste_center(canvas, fit(secondary, (116, 112)), (64, 66))
            paste_center(canvas, fit(frame, (68, 104)), (64, 59))
        elif clip.mode == "ground":
            for direction in range(4):
                image = fit(frame, (25, 76)).rotate(90 * direction, resample=Image.Resampling.NEAREST, expand=True)
                paste_center(canvas, image, (64, 64))
            paste_center(canvas, fit(secondary, (76, 80)), (64, 64))
        elif clip.mode == "slam":
            paste_center(canvas, fit(frame, (112, 112)), (64, 64))
            paste_center(canvas, fit(secondary, (72, 72)), (64, 66))
        else:
            raise ValueError(f"Apresentação desconhecida: {clip.mode}")
        if canvas.getchannel("A").getbbox() is None:
            raise ValueError(f"Saída vazia: {clip.id}, quadro {index}")
        result.append(canvas)
    return result


def atlas(frames: list[Image.Image]) -> Image.Image:
    """Organiza a sequência em uma faixa horizontal de células sem sobreposição."""
    width, height = frames[0].size
    sheet = Image.new("RGBA", (width * len(frames), height))
    for index, frame in enumerate(frames):
        sheet.paste(frame, (index * width, 0))
    return sheet


def build_preview(clip: Clip, frames: list[Image.Image], preview_dir: Path) -> None:
    """Gera GIF sobre fundo navy, mantendo PNG transparente como material runtime."""
    previews = []
    for frame in frames:
        preview = Image.new("RGB", frame.size, (20, 34, 48))
        preview.paste(frame, mask=frame.getchannel("A"))
        previews.append(preview)
    previews[0].save(preview_dir / f"{clip.id}.gif", save_all=True, append_images=previews[1:], duration=clip.millis, loop=0, disposal=2)


# Refinamento V2: pacote parcial separado; o catálogo e os PNGs V1 são preservados.
@dataclass(frozen=True)
class RefinedClip:
    """Define apresentação, pivô e fase sem transportar regras de dano ao cliente."""

    id: str
    source: str
    canvas: tuple[int, int]
    draw: tuple[int, int]
    millis: int = 85
    pivot: tuple[int, int] | None = None
    mode: str = "single"
    loop: bool = False


REFINED_CLIPS = [
    RefinedClip("water_3", "water_prison_v2", (160, 192), (100, 120), 100, loop=True),
    RefinedClip("water_4", "water_vortex_v2", (192, 256), (192, 256), 85, (96, 220), loop=True),
    RefinedClip("water_5", "water_dash_v2", (256, 96), (256, 96), 45, (232, 48), loop=True),
    RefinedClip("thunder_1", "thunder_bolt_v2", (192, 64), (192, 64), 70, mode="horizontal"),
    RefinedClip("thunder_2", "thunder_lance_v2", (256, 64), (256, 64), 65),
    RefinedClip("thunder_3", "thunder_bolt_v2", (192, 64), (192, 64), 85, mode="horizontal"),
    RefinedClip("thunder_4", "thunder_dash_v2", (256, 96), (256, 96), 45, (232, 48), loop=True),
    RefinedClip("thunder_5", "thunder_ultimate_v2", (256, 256), (240, 240), 115),
    RefinedClip("fire_2", "fire_fissure_v2", (320, 96), (260, 78), 100, (0, 48)),
    RefinedClip("fire_2.travel", "fire_cut_v1", (192, 128), (120, 80), 60),
    RefinedClip("fire_4", "fire_eruption_original", (192, 192), (192, 192), 80, (96, 180)),
    RefinedClip("fire_4.travel", "fire_cut_v1", (192, 128), (144, 96), 60),
    RefinedClip("fire_4.ground", "fire_fissure_v2", (320, 96), (280, 84), 100, (0, 48)),
    RefinedClip("fire_4.impact", "fire_eruption_original", (192, 192), (192, 192), 80, (96, 180)),
    RefinedClip("aetheri_4", "ice_arrow_v2", (256, 96), (240, 90), 65, loop=True),
    RefinedClip("aetheri_5", "ice_arrow_v2", (256, 192), (224, 168), 70, mode="volley"),
    RefinedClip("aetheri_5.travel", "ice_arrow_v2", (256, 96), (72, 27), 70, loop=True),
    RefinedClip("martial_3", "martial_crater_v2", (256, 256), (240, 240), 95),
]


def split_refined_source(entry: dict, source_dir: Path) -> tuple[list[Image.Image], dict]:
    """Recorta grids ou retângulos declarados e registra as fontes sem repintura."""
    path = Path(entry["path"])
    if entry["art_source"] == "existing":
        path = source_dir / entry["references"][0]
    elif not path.is_absolute():
        path = ROOT / path
    image = Image.open(path).convert("RGBA")
    cols, rows = entry["columns"], entry["rows"]
    rects = entry.get("rects") or [
        [round((index % cols) * image.width / cols), round((index // cols) * image.height / rows),
         round((index % cols + 1) * image.width / cols), round((index // cols + 1) * image.height / rows)]
        for index in entry["frames"]
    ]
    cell = entry.get("cellCanvas", [max(rect[2] - rect[0] for rect in rects), max(rect[3] - rect[1] for rect in rects)])
    frames, edges = [], []
    for index, rect in enumerate(rects):
        cropped = image.crop(rect)
        alpha = cropped.getchannel("A")
        if alpha.getbbox() is None:
            raise ValueError(f"Fonte V2 vazia: {path.name}, retângulo {rect}")
        edges.append(sum(alpha.getpixel((x, y)) > 128 for x, y in
                         [(0, y) for y in range(alpha.height)] + [(alpha.width - 1, y) for y in range(alpha.height)] +
                         [(x, 0) for x in range(alpha.width)] + [(x, alpha.height - 1) for x in range(alpha.width)]))
        frame = Image.new("RGBA", cell)
        if entry.get("anchors"):
            anchor = entry["anchors"][index]
            offset = (round(cell[0] / 2 - anchor[0]), round(cell[1] / 2 - anchor[1]))
        else:
            offset = ((cell[0] - cropped.width) // 2, (cell[1] - cropped.height) // 2)
        frame.paste(cropped, offset)
        frames.append(frame)
    # Projéteis ficam registrados pela ponta, sem recentralizar sua silhueta animada.
    registered_anchor = None
    if entry.get("registration") == "front_tip":
        stable = []
        target = (cell[0], cell[1])
        for frame in frames:
            alpha = frame.getchannel("A")
            solid = alpha.point(lambda value: 255 if value >= 128 else 0).getbbox()
            if solid is None:
                raise ValueError(f"Ponta de projetil sem leitura: {path.name}")
            right = solid[2]
            pixels = alpha.load()
            ys = [y for y in range(alpha.height) if any(pixels[x, y] >= 128 for x in range(max(0, right - 3), right))]
            anchor = (right - entry.get("front_inset", 0), (min(ys) + max(ys) + 1) / 2)
            padded = Image.new("RGBA", (cell[0] * 2, cell[1] * 2))
            padded.paste(frame, (round(target[0] - anchor[0]), round(target[1] - anchor[1])))
            stable.append(padded)
        frames = stable
        cell = list(frames[0].size)
        registered_anchor = target
    # O limiar enquadra a arte visível; os pixels dentro do recorte mantêm alfa nativo.
    union = Image.new("L", frames[0].size)
    for frame in frames:
        union = ImageChops.lighter(union, frame.getchannel("A"))
    bounds = union.point(lambda value: 255 if value >= 8 else 0).getbbox()
    left, top, right, bottom = bounds
    crop = (max(0, left - 4), max(0, top - 4), min(cell[0], right + 4), min(cell[1], bottom + 4))
    frames = [frame.crop(crop) for frame in frames]
    if registered_anchor:
        registered_anchor = [registered_anchor[0] - crop[0], registered_anchor[1] - crop[1]]
    provenance = {**entry, "sha256": sha256(path), "size": list(image.size), "source_rects": rects,
                  "common_crop": list(crop), "registered_anchor": registered_anchor, "framing_alpha_threshold": 8, "strong_alpha_boundary_pixels": edges,
                  "originals": [{"file": name, "sha256": sha256(source_dir / name)} for name in entry["references"]],
                  "generation": {"route": "image_gen", "model": None, "estimateUsd": None} if entry["art_source"] == "host_image" else None}
    return frames, provenance


def compose_refined(clip: RefinedClip, source: list[Image.Image]) -> list[Image.Image]:
    """Normaliza o enquadramento; o leque usa sete cópias da mesma flecha aprovada."""
    result = []
    width, height = clip.canvas
    for frame in source:
        canvas = Image.new("RGBA", clip.canvas)
        if clip.mode == "volley":
            arrow = fit(frame, (88, 32))
            for index in range(7):
                angle = math.radians(-20 + index * 40 / 6)
                rotated = arrow.rotate(-math.degrees(angle), resample=Image.Resampling.NEAREST, expand=True)
                paste_center(canvas, rotated, (16 + round(145 * math.cos(angle)), 96 + round(145 * math.sin(angle))))
        else:
            if clip.mode == "horizontal":
                frame = frame.rotate(90, resample=Image.Resampling.NEAREST, expand=True)
            paste_center(canvas, fit(frame, (width - 16, height - 16)), (width // 2, height // 2))
        if canvas.getchannel("A").getbbox() is None:
            raise ValueError(f"Clip V2 vazio: {clip.id}")
        result.append(canvas)
    return result


def build_refined_bundle(source_dir: Path, output_dir: Path, preview_dir: Path, replace: bool) -> None:
    """Publica um overlay parcial V2, gravando o manifesto por último e atomicamente."""
    previous_path = output_dir / "provenance.json"
    if previous_path.exists():
        previous = json.loads(previous_path.read_text(encoding="utf-8"))
        if not replace or previous.get("generator") != "prepare_skill_assets.py:refined-v2":
            raise ValueError("Bundle V2 existente; atualização exige --replace-generated.")
        for item in previous["clips"]:
            path = output_dir / item["image"]
            if path.exists() and sha256(path) != item["output_sha256"]:
                raise ValueError(f"PNG V2 alterado manualmente, preservado: {path.name}")
    entries = json.loads((ROOT / "assets/skills/refined-v2/art/manifest.json").read_text(encoding="utf-8"))
    loaded, sources = {}, {}
    for key, entry in entries.items():
        loaded[key], sources[key] = split_refined_source(entry, source_dir)
        if key.endswith("_v2") and any(sources[key]["strong_alpha_boundary_pixels"]):
            raise ValueError(f"Fonte nova cruza limite de recorte: {key}")
    generated = [(clip, compose_refined(clip, loaded[clip.source])) for clip in REFINED_CLIPS]
    output_dir.mkdir(parents=True, exist_ok=True)
    preview_dir.mkdir(parents=True, exist_ok=True)
    provenance = {"generator": "prepare_skill_assets.py:refined-v2", "art_source": "mixed", "sources": sources,
                  "base_manifest_sha256": sha256(ROOT / "assets/skills/manifest.properties"), "clips": [],
                  "packaging": "Explicit source rectangles/anchors, shared framing crop, uniform nearest scale, rotation/composition; no Python repaint, recolor or alpha rewrite."}
    qa = {"clip_count": len(generated), "frame_count": 0, "empty_frames": 0, "grid_failures": 0, "warnings": [], "clips": []}
    properties = ["# Overlay parcial V2. Carregar apos o manifesto V1.", "formatVersion=2", "skills=" + ",".join(clip.id for clip, _ in generated), ""]
    for clip, frames in generated:
        path = output_dir / f"{clip.id}.png"
        sheet = atlas(frames)
        sheet.save(path)
        build_preview(clip, frames, preview_dir)
        width, height = clip.canvas
        pivot = clip.pivot or (width // 2, height // 2)
        if sources[clip.source].get("registered_anchor") and clip.mode == "single":
            original = loaded[clip.source][0]
            normalized = fit(original, (width - 16, height - 16))
            anchor = sources[clip.source]["registered_anchor"]
            pivot = (round((width - normalized.width) / 2 + anchor[0] * normalized.width / original.width),
                     round((height - normalized.height) / 2 + anchor[1] * normalized.height / original.height))
        values = {"path": f"assets/skills/refined-v2/{path.name}", "image": f"assets/skills/refined-v2/{path.name}",
                  "frameWidth": width, "frameHeight": height, "columns": len(frames), "frames": len(frames), "frameMillis": clip.millis,
                  "drawWidth": clip.draw[0], "drawHeight": clip.draw[1], "pivotX": pivot[0], "pivotY": pivot[1], "loop": str(clip.loop).lower()}
        properties.extend(f"{clip.id}.{key}={value}" for key, value in values.items())
        properties.append("")
        edge_pixels = 0
        for frame in frames:
            alpha = frame.getchannel("A")
            edge_pixels += sum(alpha.getpixel((x, y)) > 0 for x, y in
                               [(0, y) for y in range(height)] + [(width - 1, y) for y in range(height)] +
                               [(x, 0) for x in range(width)] + [(x, height - 1) for x in range(width)])
        item = {"id": clip.id, "frames": len(frames), "nonempty_frames": len(frames), "edge_pixels": edge_pixels,
                "grid_valid": sheet.size == (width * len(frames), height), "alpha_extrema": list(sheet.getchannel("A").getextrema())}
        qa["clips"].append(item)
        qa["frame_count"] += len(frames)
        if edge_pixels:
            qa["warnings"].append({"id": "canvas.edge_pixels", "value": edge_pixels, "threshold": 0, "file": path.name})
        provenance["clips"].append({"id": clip.id, "image": path.name, "source": clip.source, "mode": clip.mode, "metadata": values, "output_sha256": sha256(path)})
    for page in range(math.ceil(len(generated) / 4)):
        contact = Image.new("RGB", (960, 480), (20, 34, 48))
        complete = Image.new("RGB", (1024, 400), (20, 34, 48))
        for column, (clip, frames) in enumerate(generated[page * 4:page * 4 + 4]):
            ImageDraw.Draw(contact).text((column * 240 + 8, 8), clip.id, fill="white")
            ImageDraw.Draw(complete).text((5, column * 100 + 2), clip.id, fill="white")
            for row, index in enumerate((0, len(frames) // 2, len(frames) - 1)):
                frame = fit(frames[index], (224, 140))
                contact.paste(frame, (column * 240 + (240 - frame.width) // 2, 30 + row * 148 + (140 - frame.height) // 2), frame.getchannel("A"))
            for index, frame in enumerate(frames):
                frame = fit(frame, (60, 72))
                complete.paste(frame, (index * 64 + (64 - frame.width) // 2, column * 100 + 23 + (72 - frame.height) // 2), frame.getchannel("A"))
        contact.save(preview_dir / f"contact-{page + 1}.png")
        complete.save(preview_dir / f"frames-{page + 1}.png")
    light = Image.new("RGB", (960, math.ceil(len(generated) / 4) * 150), (232, 236, 239))
    for index, (clip, frames) in enumerate(generated):
        x, y = index % 4 * 240, index // 4 * 150
        ImageDraw.Draw(light).text((x + 6, y + 4), clip.id, fill=(20, 34, 48))
        frame = fit(frames[len(frames) // 2], (224, 120))
        light.paste(frame, (x + (240 - frame.width) // 2, y + 25 + (120 - frame.height) // 2), frame.getchannel("A"))
    light.save(preview_dir / "alpha-light.png")
    (preview_dir / "qa.json").write_text(json.dumps(qa, indent=2) + "\n", encoding="utf-8")
    (output_dir / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n", encoding="utf-8")
    if qa["warnings"]:
        raise ValueError("QA V2 encontrou avisos; manifesto nao foi publicado.")
    staged = output_dir / "manifest.properties.pending"
    staged.write_text("\n".join(properties), encoding="ascii")
    staged.replace(output_dir / "manifest.properties")
    (preview_dir / "README.md").write_text(
        "# Refinamento visual V2\n\nOverlay parcial: 13 habilidades revisadas e cinco fases auxiliares, sem substituir o bundle V1. "
        "Dez folhas novas foram editadas pelo image_gen nativo a partir dos PNGs fornecidos; os prompts e versões ficam em `assets/skills/refined-v2/art/`. "
        "Fogo reutiliza também o corte aprovado e a erupção da fonte249.\n\n"
        "O empacotamento usa retângulos explícitos, registro declarado, enquadramento comum e escala uniforme nearest. "
        "Não há repintura, recoloração ou alteração de alfa pelo Python. A lança usa faixas de recorte com alturas diferentes para respeitar os intervalos transparentes da geração.\n\n"
        "Reproduzir: `python tools/prepare_skill_assets.py --source-dir 'C:\\Users\\lucas\\Downloads\\animations' --refined-v2`. "
        "Para regenerar este pacote, acrescentar `--replace-generated`; o script recusa PNGs alterados manualmente. "
        "O manifesto é publicado atomicamente após todos os PNGs e QA.\n\n"
        "Alvos, duração, dano, colisão e trajetórias permanecem definidos no servidor. Draw sizes são apresentação, não hitboxes. "
        "GIFs usam fundo navy apenas para inspeção. Conferir o resultado integrado no cliente e em rede.\n", encoding="utf-8")
    print(json.dumps({"output": str(output_dir), "clips": qa["clip_count"], "frames": qa["frame_count"], "warnings": qa["warnings"]}))


def main() -> None:
    """Valida fontes e publica somente arquivos gerados com proveniência registrada."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--water-variant", type=Path)
    parser.add_argument("--variant-map", type=Path, default=ROOT / "assets" / "skills" / "variants" / "variants.json")
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--preview-dir", type=Path)
    parser.add_argument("--refined-v2", action="store_true", help="Gera apenas o overlay visual V2 em diretorio separado.")
    parser.add_argument("--replace-generated", action="store_true", help="Atualiza somente o bundle criado por este script; recusa PNG alterado manualmente.")
    args = parser.parse_args()
    source_dir = args.source_dir.resolve()
    output_dir = (args.output_dir or ROOT / "assets" / "skills" / ("refined-v2" if args.refined_v2 else "")).resolve()
    preview_dir = (args.preview_dir or ROOT / "docs" / ("skills-preview-v2" if args.refined_v2 else "skills-preview")).resolve()
    if output_dir == source_dir or source_dir in output_dir.parents:
        raise ValueError("As saídas devem ficar fora do diretório de fontes.")
    if args.refined_v2:
        build_refined_bundle(source_dir, output_dir, preview_dir, args.replace_generated)
        return
    existing = output_dir / "provenance.json"
    if output_dir.exists() and any(path.name != "variants" for path in output_dir.iterdir()):
        if not args.replace_generated or not existing.exists():
            raise ValueError("Diretório de saída já contém arquivos; use um diretório novo.")
        previous = json.loads(existing.read_text(encoding="utf-8"))
        if previous.get("generator") != GENERATOR:
            raise ValueError("O bundle existente pertence a outro gerador.")
        for item in previous["clips"]:
            path = output_dir / item["image"]
            if path.exists() and sha256(path) != item["output_sha256"]:
                raise ValueError(f"Asset modificado manualmente, preservado: {path.name}")
    elif preview_dir.exists() and any(preview_dir.iterdir()):
        raise ValueError("Diretório de previews já contém arquivos; use um diretório novo.")

    variants = {}
    if args.variant_map.exists():
        variants = json.loads(args.variant_map.read_text(encoding="utf-8"))
        for entry in variants.values():
            entry["resolved_path"] = args.variant_map.parent / entry["path"]
    loaded, sources = {}, {}
    for key, source in SOURCES.items():
        raw, provenance = read_source(key, source, source_dir, args.water_variant, variants)
        cropped, rect = common_crop(raw)
        provenance["common_crop_in_cell"] = list(rect)
        loaded[key], sources[key] = cropped, provenance

    generated = [(clip, compose(clip, loaded[clip.source], loaded.get(clip.extra))) for clip in CLIPS]
    output_dir.mkdir(parents=True, exist_ok=True)
    preview_dir.mkdir(parents=True, exist_ok=True)
    properties = ["# Spirit Gate: 40 clips; fonte, recortes e hashes em provenance.json.", "# Frame order: horizontal; pivots em pixels da celula; sem dano/cooldown nos assets.", "formatVersion=1", "skills=" + ",".join(clip.id for clip in CLIPS), ""]
    provenance = {"generator": GENERATOR, "art_source": "mixed" if variants or args.water_variant else "existing", "art_sources": ["existing", "host_image"] if variants or args.water_variant else ["existing"], "transformations": "Python packaging: grid crop; one common crop per sequence; uniform nearest-neighbour scale; composition; rotation; no repaint or recolor. Art variants: image_gen edits documented per source.", "sources": sources, "clips": []}
    qa = {"clip_count": len(CLIPS), "frame_count": 0, "empty_frames": 0, "grid_failures": 0, "native_alpha": True, "warnings": [], "clips": []}
    for clip, frames in generated:
        image_path = output_dir / f"{clip.id}.png"
        sheet = atlas(frames)
        sheet.save(image_path)
        build_preview(clip, frames, preview_dir)
        width, height = clip.canvas
        values = {"path": f"assets/skills/{image_path.name}", "image": f"assets/skills/{image_path.name}", "frameWidth": width, "frameHeight": height, "columns": len(frames), "frames": len(frames), "frameMillis": clip.millis, "drawWidth": clip.draw[0], "drawHeight": clip.draw[1], "pivotX": width // 2, "pivotY": height // 2, "loop": str(clip.loop).lower()}
        properties.extend(f"{clip.id}.{key}={value}" for key, value in values.items())
        properties.append("")
        nonempty = sum(frame.getchannel("A").getbbox() is not None for frame in frames)
        edge_pixels = 0
        for frame in frames:
            alpha = frame.getchannel("A")
            edge_pixels += sum(alpha.getpixel((x, y)) > 0 for x, y in [(0, y) for y in range(height)] + [(width - 1, y) for y in range(height)] + [(x, 0) for x in range(width)] + [(x, height - 1) for x in range(width)])
        item = {"id": clip.id, "frames": len(frames), "nonempty_frames": nonempty, "edge_pixels": edge_pixels, "alpha_extrema": list(sheet.getchannel("A").getextrema()), "grid_valid": sheet.size == (width * len(frames), height)}
        qa["clips"].append(item)
        qa["frame_count"] += len(frames)
        qa["empty_frames"] += len(frames) - nonempty
        qa["grid_failures"] += not item["grid_valid"]
        if edge_pixels:
            qa["warnings"].append({"id": "canvas.edge_pixels", "value": edge_pixels, "threshold": 0, "file": image_path.name})
        provenance["clips"].append({"id": clip.id, "image": image_path.name, "source_keys": [clip.source] + ([clip.extra] if clip.extra else []), "mode": clip.mode, "output_sha256": sha256(image_path), "metadata": values})

    (output_dir / "manifest.properties").write_text("\n".join(properties), encoding="ascii")
    (output_dir / "provenance.json").write_text(json.dumps(provenance, indent=2) + "\n", encoding="utf-8")
    (preview_dir / "qa.json").write_text(json.dumps(qa, indent=2) + "\n", encoding="utf-8")
    for group in range(8):
        sheet = Image.new("RGB", (1000, 450), (20, 34, 48))
        draw = ImageDraw.Draw(sheet)
        for column, (clip, frames) in enumerate(generated[group * 5:group * 5 + 5]):
            x = column * 200
            draw.text((x + 10, 10), clip.id, fill=(220, 240, 255))
            for row, frame_index in enumerate((0, len(frames) // 2, len(frames) - 1)):
                frame = fit(frames[frame_index], (184, 122))
                image = Image.new("RGBA", (200, 136))
                paste_center(image, frame, (100, 68))
                sheet.paste(image, (x, 35 + row * 136), image.getchannel("A"))
        sheet.save(preview_dir / f"contact-{group + 1}.png")
        all_frames = Image.new("RGB", (1280, 430), (20, 34, 48))
        draw = ImageDraw.Draw(all_frames)
        for row, (clip, frames) in enumerate(generated[group * 5:group * 5 + 5]):
            draw.text((5, row * 86 + 2), clip.id, fill=(220, 240, 255))
            for column, frame in enumerate(frames):
                fitted = fit(frame, (60, 60))
                all_frames.paste(fitted, (column * 64 + (64 - fitted.width) // 2, row * 86 + 22 + (60 - fitted.height) // 2), fitted.getchannel("A"))
        all_frames.save(preview_dir / f"frames-{group + 1}.png")
    light_background = Image.new("RGB", (1000, 1120), (232, 236, 239))
    draw = ImageDraw.Draw(light_background)
    for index, (clip, frames) in enumerate(generated):
        x, y = (index % 5) * 200, (index // 5) * 140
        draw.text((x + 8, y + 4), clip.id, fill=(20, 34, 48))
        frame = fit(frames[len(frames) // 2], (184, 112))
        light_background.paste(frame, (x + (200 - frame.width) // 2, y + 24 + (112 - frame.height) // 2), frame.getchannel("A"))
    light_background.save(preview_dir / "alpha-light.png")
    (preview_dir / "README.md").write_text(
        "# Prévias das habilidades\n\n"
        "Quarenta GIFs, oito contact sheets, oito folhas com todos os frames e um teste sobre fundo claro mostram os mesmos frames usados pelos PNGs de runtime. "
        "Os GIFs usam fundo navy apenas para inspeção; os PNGs preservam alfa real.\n\n"
        "As fontes são imagens fornecidas pelo desenvolvedor, com uma variante editada pelo image_gen em cada família. "
        "Recorte, escala uniforme, rotação e composição adaptam sua apresentação. O script de empacotamento não redesenha nem recolore a arte. "
        "A proveniência e os retângulos de origem ficam em `assets/skills/provenance.json`.\n\n"
        "Reproduzir (PowerShell):\n\n```powershell\n"
        "python tools/prepare_skill_assets.py --source-dir 'C:\\Users\\lucas\\Downloads\\animations'\n```\n\n"
        "Para atualizar exclusivamente este bundle, acrescentar `--replace-generated`. "
        "As variantes aprovadas são lidas de `assets/skills/variants/variants.json` (opcionalmente `--variant-map`). "
        "Uma variante aprovada da folha de água pode ser fornecida por `--water-variant <arquivo.png>` "
        "com o mesmo grid 3×3. O gerador preserva arquivos de origem e recusa PNGs de saída alterados manualmente.\n\n"
        "Os tempos, composição e dimensões são valores iniciais de apresentação. "
        "Dano, colisão, alvos e duração dos status permanecem sob autoridade do servidor. "
        "A homologação visual dentro do cliente e em rede continua necessária.\n", encoding="utf-8")
    if qa["empty_frames"] or qa["grid_failures"]:
        raise ValueError("QA do bundle falhou: consultar qa.json")
    print(json.dumps({"output": str(output_dir), "clips": qa["clip_count"], "frames": qa["frame_count"], "warnings": qa["warnings"]}))


if __name__ == "__main__":
    main()
