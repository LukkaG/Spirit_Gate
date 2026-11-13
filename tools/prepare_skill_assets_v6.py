"""Empacota os cortes V6 com registro fixo e redução, preservando os atlas anteriores."""
from pathlib import Path
import hashlib
import json
import sys
from PIL import Image, ImageDraw, ImageOps

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/"assets/skills/refined-v6/art"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(destination,preview):
    """Mede alfa significativo para o recorte; preserva o alfa nativo dentro da região registrada."""
    destination.mkdir(parents=True,exist_ok=True);preview.mkdir(parents=True,exist_ok=True)
    previous=destination/"provenance.json"
    if previous.exists():
        for item in json.loads(previous.read_text(encoding="utf-8"))["clips"]:
            path=destination/item["file"]
            if path.exists() and sha(path)!=item["sha256"]:raise ValueError(f"Atlas modificado manualmente: {path}")
    specs=[("fire_5","fire-cross.png",(192,192),(96,96),28,False),
           ("water_2","water-spin-isolated.png",(320,320),(200,200),50,False),
           ("martial_3","seismic-transparent.png",(320,240),(240,180),75,False),
           ("wind_4","wind-triple.png",(320,224),(160,112),45,False)]
    properties=[];records=[];packaged={};qa=dict(clips=5,frames=54,empty_frames=0,edge_pixels=0,upscaled_frames=0,warnings=[],lifecycles={})
    contact=Image.new("RGB",(1200,1200),(20,34,48))
    for row,(name,filename,canvas,draw,millis,flip) in enumerate(specs):
        path=ART/filename;bitmap=Image.open(path).convert("RGBA")
        if bitmap.size!=(1448,1086):raise ValueError(f"Geometria da fonte mudou: {path}")
        rects=[(i%4*362,i//4*362,(i%4+1)*362,(i//4+1)*362) for i in range(12)]
        originals=[bitmap.crop(rect) for rect in rects]
        if flip:originals=[ImageOps.mirror(frame) for frame in originals]
        # As fontes têm resíduos de alfa 1/2 nos gutters. O limiar só mede o recorte, sem repintar.
        boxes=[frame.getchannel("A").point(lambda a:255 if a>=8 else 0).getbbox() for frame in originals]
        if any(box is None for box in boxes):raise ValueError(f"Estágio sem pixels visíveis: {name}")
        box=(min(b[0] for b in boxes),min(b[1] for b in boxes),max(b[2] for b in boxes),max(b[3] for b in boxes))
        width,height=box[2]-box[0],box[3]-box[1]
        scale=min(1,(canvas[0]-16)/width,(canvas[1]-16)/height)
        size=(round(width*scale),round(height*scale));offset=((canvas[0]-size[0])//2,(canvas[1]-size[1])//2)
        pivot=(round(offset[0]+(181-box[0])*size[0]/width),round(offset[1]+(181-box[1])*size[1]/height))
        frames=[];areas=[]
        for index,original in enumerate(originals):
            alpha=original.getchannel("A")
            edges=[alpha.crop((0,0,362,1)),alpha.crop((0,361,362,362)),alpha.crop((0,0,1,362)),alpha.crop((361,0,362,362))]
            if any(edge.getextrema()[1]>=8 for edge in edges):raise ValueError(f"Arte atravessa a célula: {name}/{index}")
            frame=Image.new("RGBA",canvas);frame.alpha_composite(original.crop(box).resize(size,Image.Resampling.NEAREST),offset)
            a=frame.getchannel("A");areas.append(sum(v>=16 for v in a.get_flattened_data()))
            if not a.getbbox():raise ValueError(f"Frame vazio: {name}/{index}")
            if a.getbbox()[0]<=0 or a.getbbox()[1]<=0 or a.getbbox()[2]>=canvas[0] or a.getbbox()[3]>=canvas[1]:raise ValueError(f"Frame na borda: {name}/{index}")
            frames.append(frame)
        peak=max(areas)
        if areas[0]>peak*.15 or areas[-1]>peak*.1:raise ValueError(f"Ciclo não começa/termina pequeno: {name}")
        qa["lifecycles"][name]=dict(visible_alpha_threshold=16,area=areas,first_peak_ratio=round(areas[0]/peak,4),last_peak_ratio=round(areas[-1]/peak,4))
        packaged[name]=(frames,canvas,draw,millis,pivot)
        records.append(dict(id=name,file=name+".png",source=path.relative_to(ROOT).as_posix(),source_sha256=sha(path),rects=rects,common_crop=box,pivot=pivot,scale=scale,mirror_horizontal=flip))
        previews=[]
        for frame in frames:
            background=Image.new("RGBA",draw,(20,34,48,255));background.alpha_composite(frame.resize(draw,Image.Resampling.NEAREST));previews.append(background.convert("RGB"))
        previews[0].save(preview/(name+".gif"),save_all=True,append_images=previews[1:],duration=millis,loop=0,disposal=2)
        for column,index in enumerate([0,3,5,11]):
            image=previews[index];contact.paste(image,(column*300+(300-image.width)//2,row*300+30+(240-image.height)//2));ImageDraw.Draw(contact).text((column*300+10,row*300+285),f"{name} frame {index+1}",fill="white")
    frames,canvas,draw,millis,pivot=packaged["wind_4"]
    packaged["wind_4.impact"]=(frames[6:],canvas,draw,50,pivot)
    records.append(dict(id="wind_4.impact",file="wind_4.impact.png",source_frames=[6,7,8,9,10,11]))
    for name,(frames,canvas,draw,millis,pivot) in packaged.items():
        atlas=Image.new("RGBA",(canvas[0]*4,canvas[1]*((len(frames)+3)//4)))
        for i,frame in enumerate(frames):atlas.alpha_composite(frame,(i%4*canvas[0],i//4*canvas[1]))
        path=destination/(name+".png");atlas.save(path)
        values=dict(path="assets/skills/refined-v6/"+path.name,frameWidth=canvas[0],frameHeight=canvas[1],columns=4,frames=len(frames),frameMillis=millis,drawWidth=draw[0],drawHeight=draw[1],pivotX=pivot[0],pivotY=pivot[1])
        aliases=[name,"wind_4.travel","wind_5","wind_5.travel"] if name=="wind_4" else [name]
        for alias in aliases:properties.extend(f"{alias}.{key}={value}" for key,value in values.items())
        record=next(r for r in records if r["id"]==name);record.update(sha256=sha(path),metadata=values)
    contact.save(preview/"contact.png")
    (preview/"qa.json").write_text(json.dumps(qa,indent=2)+"\n",encoding="utf-8")
    (destination/"provenance.json").write_text(json.dumps(dict(route="image_gen",model=None,estimateUsd=None,clips=records,packaging="Recorte, registro, espelhamento de direção e redução nearest; sem ampliação/recoloração/repintura."),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    pending=destination/"manifest.properties.pending";pending.write_text("# Overlay V6 visual, apos V5; nao e versao de protocolo.\nformatVersion=6\n"+"\n".join(properties)+"\n",encoding="ascii");pending.replace(destination/"manifest.properties")
    print(json.dumps(qa))


if __name__=="__main__":
    build(Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/"assets/skills/refined-v6",Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/"docs/skills-preview-v6")
