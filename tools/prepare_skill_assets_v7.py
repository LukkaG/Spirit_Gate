"""Registra o sísmico V7 sem repintura: uma escala, pivô fixo e redução nearest."""
from pathlib import Path
import hashlib
import json
import sys
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"assets/skills/refined-v7/art/seismic-readable.png"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(destination,preview):
    """Conserva as posições relativas das lascas e a abertura das fissuras nos 16 estágios."""
    destination.mkdir(parents=True,exist_ok=True);preview.mkdir(parents=True,exist_ok=True)
    previous=destination/"provenance.json"
    if previous.exists():
        record=json.loads(previous.read_text(encoding="utf-8"))["clip"]
        path=destination/record["file"]
        if path.exists() and sha(path)!=record["sha256"]:raise ValueError(f"Atlas editado manualmente: {path}")
    bitmap=Image.open(SOURCE).convert("RGBA")
    if bitmap.size!=(1448,1086):raise ValueError("A fonte mudou de geometria")
    # As quatro faixas desenhadas têm alturas diferentes: não cortar as partículas entre elas.
    rows=[0,350,650,850,1086];roots=[200,190,100,100]
    rects=[(i%4*362,rows[i//4],(i%4+1)*362,rows[i//4+1]) for i in range(16)]
    anchors=[(190,roots[i//4]) for i in range(16)]
    originals=[bitmap.crop(rect) for rect in rects]
    boxes=[frame.getchannel("A").point(lambda a:255 if a>=8 else 0).getbbox() for frame in originals]
    if any(box is None for box in boxes):raise ValueError("Estágio sem arte visível")
    common=(min(b[0]-p[0] for b,p in zip(boxes,anchors)),min(b[1]-p[1] for b,p in zip(boxes,anchors)),max(b[2]-p[0] for b,p in zip(boxes,anchors)),max(b[3]-p[1] for b,p in zip(boxes,anchors)))
    width,height=common[2]-common[0],common[3]-common[1];canvas=(320,240);draw=(240,180)
    scale=min(1,(canvas[0]-16)/width,(canvas[1]-16)/height)
    size=(round(width*scale),round(height*scale));offset=((canvas[0]-size[0])//2,(canvas[1]-size[1])//2)
    pivot=(round(offset[0]-common[0]*size[0]/width),round(offset[1]-common[1]*size[1]/height))
    frames=[];areas=[]
    for index,(frame,anchor) in enumerate(zip(originals,anchors)):
        alpha=frame.getchannel("A");w,h=frame.size
        edges=[alpha.crop((0,0,w,1)),alpha.crop((0,h-1,w,h)),alpha.crop((0,0,1,h)),alpha.crop((w-1,0,w,h))]
        if any(edge.getextrema()[1]>=8 for edge in edges):raise ValueError(f"Arte cruza o recorte: {index}")
        registered=Image.new("RGBA",(width,height));registered.alpha_composite(frame,(-anchor[0]-common[0],-anchor[1]-common[1]))
        fitted=Image.new("RGBA",canvas);fitted.alpha_composite(registered.resize(size,Image.Resampling.NEAREST),offset)
        a=fitted.getchannel("A");box=a.getbbox()
        if box is None:raise ValueError(f"Frame vazio: {index}")
        if box[0]<=0 or box[1]<=0 or box[2]>=canvas[0] or box[3]>=canvas[1]:raise ValueError(f"Frame na borda: {index}")
        areas.append(sum(v>=16 for v in a.get_flattened_data()));frames.append(fitted)
    peak=max(areas)
    if areas[0]>peak*.1 or areas[-1]>peak*.1:raise ValueError("Entrada/saída não dissipam até poucos pixels")
    atlas=Image.new("RGBA",(canvas[0]*4,canvas[1]*4))
    for index,frame in enumerate(frames):atlas.alpha_composite(frame,(index%4*canvas[0],index//4*canvas[1]))
    path=destination/"martial_3.png";atlas.save(path)
    values=dict(path="assets/skills/refined-v7/martial_3.png",frameWidth=320,frameHeight=240,columns=4,frames=16,frameMillis=56,drawWidth=240,drawHeight=180,pivotX=pivot[0],pivotY=pivot[1])
    record=dict(file=path.name,sha256=sha(path),source=SOURCE.relative_to(ROOT).as_posix(),source_sha256=sha(SOURCE),rects=rects,anchors=anchors,common_crop=common,scale=scale,metadata=values)
    (destination/"provenance.json").write_text(json.dumps(dict(route="image_gen",model=None,estimateUsd=None,clip=record,packaging="Recorte, registro e redução nearest. Sem ampliação/recoloração/repintura."),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    qa=dict(clips=1,frames=16,empty_frames=0,edge_pixels=0,upscaled_frames=0,warnings=[],visible_alpha_threshold=16,area=areas,first_peak_ratio=round(areas[0]/peak,4),last_peak_ratio=round(areas[-1]/peak,4),display=list(draw),duration_millis=896)
    (preview/"qa.json").write_text(json.dumps(qa,indent=2)+"\n",encoding="utf-8")
    previews=[];contact=Image.new("RGB",(1000,800),(43,68,57))
    for index,frame in enumerate(frames):
        background=Image.new("RGBA",draw,(43,68,57,255));background.alpha_composite(frame.resize(draw,Image.Resampling.NEAREST));shot=background.convert("RGB");previews.append(shot)
        x=index%4*250;y=index//4*200;contact.paste(shot,(x,y));ImageDraw.Draw(contact).text((x+6,y+184),f"frame {index+1}",fill="white")
    previews[0].save(preview/"martial_3.gif",save_all=True,append_images=previews[1:],duration=56,loop=0,disposal=2);contact.save(preview/"contact.png")
    pending=destination/"manifest.properties.pending";pending.write_text("# Overlay visual V7: somente impacto sismico.\nformatVersion=7\n"+"\n".join(f"martial_3.{k}={v}" for k,v in values.items())+"\n",encoding="ascii");pending.replace(destination/"manifest.properties")
    print(json.dumps(qa))


if __name__=="__main__":
    build(Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/"assets/skills/refined-v7",Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/"docs/skills-preview-v7")
