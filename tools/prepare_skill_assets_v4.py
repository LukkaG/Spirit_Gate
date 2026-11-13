"""Empacota as fontes V4 sem repintura: registro, recorte, composição e redução sem ampliar pixels."""
from pathlib import Path
import hashlib
import json
import math
import sys
from PIL import Image, ImageDraw
import prepare_skill_assets as prior

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/"assets/skills/refined-v4/art"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def reduce_frame(image,box):
    """Reduz uniformemente quando necessário; nunca inventa resolução por ampliação."""
    factor=min(1,box[0]/image.width,box[1]/image.height)
    return image.resize((max(1,round(image.width*factor)),max(1,round(image.height*factor))),Image.Resampling.NEAREST)


def source(name,filename,rows=3,front=False,margin=0,ground=False):
    """Registra bases/pontas usando retângulos explícitos e conserva os pixels das fontes."""
    path=ART/filename;im=Image.open(path).convert("RGBA")
    entry=dict(path=path.relative_to(ROOT).as_posix(),columns=3,rows=rows,frames=list(range(rows*3)),references=[],art_source="host_image")
    yb=[round(i*im.height/rows) for i in range(rows+1)]
    if name=="water-vortex":yb=[0,410,780,1145]
    rects=[[round(i%3*im.width/3)+margin,yb[i//3]+margin,round((i%3+1)*im.width/3)-margin,yb[i//3+1]-margin] for i in range(rows*3)]
    entry["rects"]=rects
    if front:entry["registration"]="front_tip"
    if name=="impact":
        entry["anchors"]=[[256,288]]*3+[[256,256]]*3;entry["cellCanvas"]=[768,768]
    elif ground:
        anchors=[]
        for rect in rects:
            frame=im.crop(rect);a=frame.getchannel("A")
            candidates=[y for y in range(frame.height) if sum(a.getpixel((x,y))>=128 for x in range(frame.width))>=frame.width*.2]
            if not candidates:raise ValueError(f"Base não encontrada: {name}")
            y=max(candidates);xs=[x for x in range(frame.width) if a.getpixel((x,y))>=128]
            anchors.append([(min(xs)+max(xs))/2,y])
        entry["anchors"]=anchors;entry["cellCanvas"]=[768,1024]
    frames,meta=prior.split_refined_source(entry,ROOT)
    if any(meta["strong_alpha_boundary_pixels"]):raise ValueError(f"Fonte cruza seu retângulo: {name}")
    if meta.get("registered_anchor"):root=meta["registered_anchor"]
    elif entry.get("anchors"):root=[entry["cellCanvas"][0]/2-meta["common_crop"][0],entry["cellCanvas"][1]/2-meta["common_crop"][1]]
    else:
        cell=entry.get("cellCanvas",[max(r[2]-r[0] for r in rects),max(r[3]-r[1] for r in rects)])
        root=[cell[0]/2-meta["common_crop"][0],cell[1]*(.62 if name=="seismic" else .5)-meta["common_crop"][1]]
        if name=="fire-fissure":root[1]=im.height/3*.64-margin-meta["common_crop"][1]
    return frames,meta,root


def build(destination,preview):
    """Publica o manifesto após QA e recusa substituir PNGs V4 editados manualmente."""
    destination.mkdir(parents=True,exist_ok=True);preview.mkdir(parents=True,exist_ok=True)
    old=destination/"provenance.json"
    if old.exists():
        for item in json.loads(old.read_text(encoding="utf-8"))["clips"]:
            path=destination/item["file"]
            if path.exists() and sha(path)!=item["sha256"]:raise ValueError(f"PNG editado manualmente: {path}")
    specs={
        "impact":("impact.png",2,False,0,False),
        "fire-fissure":("fire-fissure.png",3,False,12,False),
        "seismic":("seismic-padded.png",3,False,0,False),
        "martial-punch":("martial-punch-padded.png",2,True,0,False),
        "water-cut":("water-cut.png",3,True,0,False),
        "water-prison":("water-prison.png",3,False,0,True),
        "water-vortex":("water-vortex-padded.png",3,False,0,True),
        "water-dash":("water-dash.png",3,True,0,False),
    }
    sources={name:source(name,*args) for name,args in specs.items()}
    # id, fonte, canvas, dimensões de desenho, duração/frame, composição opcional.
    clips=[
        ("impact","impact",(128,128),(64,64),50,"single"),
        ("fire_2","fire-fissure",(384,160),(288,120),80,"ground"),
        ("martial_3","seismic",(384,384),(320,320),85,"single"),
        ("martial_1","martial-punch",(192,128),(96,64),40,"single"),
        ("water_1","water-cut",(256,160),(128,80),65,"single"),
        ("water_2","water-cut",(256,256),(200,200),65,"spin"),
        ("water_3","water-prison",(256,256),(128,128),100,"single"),
        ("water_4","water-vortex",(256,320),(192,240),85,"single"),
        ("water_5","water-dash",(320,128),(240,96),45,"single"),
    ]
    properties={};records=[];qa=dict(clips=0,frames=0,empty_frames=0,edge_pixels=0,warnings=[],upscaled_frames=0)
    contact=Image.new("RGB",(1200,900),(20,34,48))
    for index,(name,key,canvas,draw,millis,mode) in enumerate(clips):
        originals,meta,root=sources[key];frames=[];pivot=None
        for original in originals:
            frame=Image.new("RGBA",canvas)
            if mode=="spin":
                cut=reduce_frame(original,(108,64))
                for angle in (0,120,240):
                    turned=cut.rotate(angle,resample=Image.Resampling.NEAREST,expand=True);prior.paste_center(frame,turned,(128+round(math.cos(math.radians(angle))*42),128+round(math.sin(math.radians(angle))*42)))
                pivot=(128,128)
            else:
                fitted=reduce_frame(original,(canvas[0]-16,canvas[1]-16));x=(canvas[0]-fitted.width)//2;y=(canvas[1]-fitted.height)//2
                frame.alpha_composite(fitted,(x,y));pivot=(round(x+root[0]*fitted.width/original.width),round(y+root[1]*fitted.height/original.height))
                if mode=="ground":pivot=(8,pivot[1])
            a=frame.getchannel("A")
            if not a.getbbox():raise ValueError(f"Frame vazio: {name}")
            edges=[a.crop((0,0,frame.width,1)),a.crop((0,frame.height-1,frame.width,frame.height)),a.crop((0,0,1,frame.height)),a.crop((frame.width-1,0,frame.width,frame.height))]
            if any(part.getextrema()[1]>0 for part in edges):raise ValueError(f"Pixels na borda: {name}")
            frames.append(frame)
        path=destination/(name+".png");prior.atlas(frames).save(path)
        values=dict(path=f"assets/skills/refined-v4/{path.name}",frameWidth=canvas[0],frameHeight=canvas[1],columns=len(frames),frames=len(frames),frameMillis=millis,drawWidth=draw[0],drawHeight=draw[1],pivotX=pivot[0],pivotY=pivot[1])
        properties[name]=values;records.append(dict(id=name,file=path.name,source=key,sha256=sha(path),metadata=values,mode=mode))
        preview_clip=prior.RefinedClip(name,key,canvas,draw,millis,pivot)
        prior.build_preview(preview_clip,frames,preview)
        x=index%3*400;y=index//3*300;ImageDraw.Draw(contact).text((x+8,y+8),name,fill="white")
        sample=reduce_frame(frames[len(frames)//2],(380,260));contact.paste(sample,(x+(400-sample.width)//2,y+35+(260-sample.height)//2),sample)
        qa["clips"]+=1;qa["frames"]+=len(frames)
    # Metadados compartilhados evitam duplicar os atlas de impacto e de golpes.
    aliases={"martial_2":"martial_1","fire_2.ground":"fire_2","fire_4.ground":"fire_2"}
    for target,origin in aliases.items():properties[target]=dict(properties[origin])
    impact_ids=["water_1","wind_1","wind_2","thunder_1","thunder_2","fire_1","spirit_2","spirit_3","aetheri_1","aetheri_2","aetheri_3","aetheri_4","aetheri_5"]
    for name in impact_ids:properties[name+".impact"]=dict(properties["impact"])
    properties.pop("impact")
    legacy={}
    for line in (ROOT/"assets/skills/manifest.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):k,v=line.split("=",1);legacy[k]=v
    wind={k[len("wind_1."):]:v for k,v in legacy.items() if k.startswith("wind_1.")}
    properties["wind_2"]=dict(wind);properties["wind_2.travel"]=dict(wind)
    # Chuva usa a flecha de energia já aprovada; posição/queda são calculadas na apresentação.
    v3={}
    for line in (ROOT/"assets/skills/refined-v3/manifest.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):k,v=line.split("=",1);v3[k]=v
    arrow={k[len("aetheri_1."):]:v for k,v in v3.items() if k.startswith("aetheri_1.")}
    arrow.update(drawWidth=64,drawHeight=24);properties["aetheri_3.travel"]=arrow
    contact.save(preview/"contact.png")
    (preview/"qa.json").write_text(json.dumps(qa,indent=2)+"\n",encoding="utf-8")
    (destination/"provenance.json").write_text(json.dumps(dict(route="image_gen",model=None,estimateUsd=None,sources={key:meta for key,(_,meta,_) in sources.items()},clips=records,aliases=properties,packaging="Recorte e registro explícitos; redução nearest sem ampliação, sem repintura/recoloração por código."),indent=2)+"\n",encoding="utf-8")
    lines=["# Sobreposicao visual V4; carregar depois de V1/V2/V3.","formatVersion=4"]
    for name,values in properties.items():lines.extend(f"{name}.{key}={value}" for key,value in values.items())
    pending=destination/"manifest.properties.pending";pending.write_text("\n".join(lines)+"\n",encoding="ascii");pending.replace(destination/"manifest.properties")
    print(json.dumps(qa))


if __name__=="__main__":
    build(Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/"assets/skills/refined-v4",Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/"docs/skills-preview-v4")
