"""Empacota a revisão V3 sem repintar os PNGs: recorte, registro e composição nearest."""
from pathlib import Path
import hashlib
import json
import math
import sys
from PIL import Image, ImageDraw
import prepare_skill_assets as previous

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(output, preview):
    """Publica o manifesto por último e registra fontes, pivôs e QA reproduzíveis."""
    output.mkdir(parents=True, exist_ok=True)
    preview.mkdir(parents=True, exist_ok=True)
    prior=output/"provenance.json"
    if prior.exists():
        for record in json.loads(prior.read_text(encoding="utf-8"))["clips"]:
            path=output/(record["id"]+".png")
            if path.exists() and digest(path)!=record["sha256"]:
                raise ValueError(f"PNG V3 editado manualmente; preservado: {path}")
    source_meta = {}
    sources = {}
    for key, filename in (("energy", "energy-arrow-padded.png"), ("combo", "martial-combo-alpha.png")):
        entry = dict(path=f"assets/skills/refined-v3/art/{filename}", columns=3, rows=3,
                     frames=list(range(9)), references=[], art_source="host_image")
        if key == "energy":
            entry["registration"] = "front_tip"
        sources[key], source_meta[key] = previous.split_refined_source(entry, ROOT)
        if any(source_meta[key]["strong_alpha_boundary_pixels"]):
            raise ValueError(f"Fonte cruza limite de frame: {key}")
    specs = [
        ("aetheri_1", "energy", (256,96), (96,36), "single"),
        ("aetheri_2", "energy", (256,96), (96,36), "single"),
        ("aetheri_3", "energy", (256,256), (220,220), "rain"),
        ("aetheri_4", "energy", (256,96), (240,90), "single"),
        ("aetheri_5", "energy", (256,192), (224,168), "volley"),
        ("aetheri_5.travel", "energy", (256,96), (72,27), "single"),
        ("martial_5", "combo", (320,192), (220,180), "combo"),
    ]
    manifest = ["# Sobreposicao V3; carregar apos V1 e V2.", "formatVersion=3"]
    records = []
    contact = Image.new("RGB", (960, 600), (20,34,48))
    qa = dict(clips=0, frames=0, warnings=[], empty_frames=0, grid_failures=0)
    for index, (name, key, canvas, draw, mode) in enumerate(specs):
        clip = previous.RefinedClip(name, key, canvas, draw, 70, mode="volley" if mode=="volley" else "single")
        if mode == "rain":
            frames=[]
            for phase, original in enumerate(sources[key]):
                frame=Image.new("RGBA", canvas)
                arrow=previous.fit(original,(64,24)).transpose(Image.Transpose.ROTATE_270)
                for column in range(5):
                    y=42+(phase*12+column*37)%165
                    frame.alpha_composite(arrow,(24+column*44,y-arrow.height//2))
                frames.append(frame)
            pivot=(128,128)
        else:
            frames=previous.compose_refined(clip,sources[key])
            pivot=(32,canvas[1]//2) if mode=="combo" else (canvas[0]//2,canvas[1]//2)
            anchor=source_meta[key].get("registered_anchor")
            if anchor and mode=="single":
                original=sources[key][0]; fitted=previous.fit(original,(canvas[0]-16,canvas[1]-16))
                pivot=(round((canvas[0]-fitted.width)/2+anchor[0]*fitted.width/original.width),
                       round((canvas[1]-fitted.height)/2+anchor[1]*fitted.height/original.height))
        sheet=previous.atlas(frames); path=output/f"{name}.png"; sheet.save(path)
        for frame in frames:
            alpha=frame.getchannel("A")
            if not alpha.getbbox(): raise ValueError(f"Frame vazio: {name}")
            if max(alpha.crop((0,0,frame.width,1)).getextrema()+alpha.crop((0,frame.height-1,frame.width,frame.height)).getextrema())>0:
                raise ValueError(f"Frame toca borda vertical: {name}")
            if max(alpha.crop((0,0,1,frame.height)).getextrema()+alpha.crop((frame.width-1,0,frame.width,frame.height)).getextrema())>0:
                raise ValueError(f"Frame toca borda horizontal: {name}")
        values=dict(path=f"assets/skills/refined-v3/{name}.png", frameWidth=canvas[0],frameHeight=canvas[1],
                    columns=9,frames=9,frameMillis=70,drawWidth=draw[0],drawHeight=draw[1],pivotX=pivot[0],pivotY=pivot[1])
        manifest += [f"{name}.{field}={value}" for field,value in values.items()]
        records.append(dict(id=name,source=key,mode=mode,metadata=values,sha256=digest(path)))
        previous.build_preview(clip,frames,preview)
        x,y=(index%4)*240,(index//4)*300
        ImageDraw.Draw(contact).text((x+8,y+8),name,fill="white")
        sample=previous.fit(frames[4],(224,240));contact.paste(sample,(x+(240-sample.width)//2,y+30+(240-sample.height)//2),sample.getchannel("A"))
        qa["clips"]+=1;qa["frames"]+=len(frames)
    # Somente apresentação do impacto sísmico cresce; o círculo de dano continua explícito no catálogo.
    manifest += ["martial_3.drawWidth=320", "martial_3.drawHeight=320"]
    contact.save(preview/"contact.png")
    (preview/"qa.json").write_text(json.dumps(qa,indent=2)+"\n",encoding="utf-8")
    (output/"provenance.json").write_text(json.dumps(dict(art_source="host_image",route="image_gen",sources=source_meta,
        clips=records,packaging="Recorte, registro de ponta, composicao e escala nearest; sem repintura ou recoloracao.",
        inherited_seismic="assets/skills/refined-v2/martial_3.png; somente draw 320x320"),indent=2)+"\n",encoding="utf-8")
    pending=output/"manifest.properties.pending";pending.write_text("\n".join(manifest)+"\n",encoding="ascii");pending.replace(output/"manifest.properties")
    print(json.dumps(qa))


if __name__ == "__main__":
    destination=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/"assets/skills/refined-v3"
    previews=Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/"docs/skills-preview-v3"
    build(destination,previews)
