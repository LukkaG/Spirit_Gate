"""Empacota recolorações nativas de raio e as duas fases marciais; não repinta pixels."""
from pathlib import Path
import hashlib
import json
import sys
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/"assets/skills/refined-v8/art"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build(destination,preview):
    """Registra centros/bases e mantém uma escala por sequência, sem ampliar as fontes."""
    destination.mkdir(parents=True,exist_ok=True);preview.mkdir(parents=True,exist_ok=True)
    previous=destination/"provenance.json"
    if previous.exists():
        for record in json.loads(previous.read_text(encoding="utf-8"))["clips"]:
            path=destination/record["file"]
            if path.exists() and sha(path)!=record["sha256"]:raise ValueError(f"Atlas editado manualmente: {path}")
    specs=[
        ("thunder_1","thunder-cut.png",3,2,list(range(6)),(192,64),(192,64),70,"center"),
        ("thunder_2","thunder-spear.png",3,3,list(range(9)),(256,64),(224,56),70,"center"),
        ("thunder_4","thunder-dash-alpha.png",3,3,list(range(9)),(256,96),(256,96),45,"center"),
        ("thunder_5","thunder-circuit-padded.png",3,3,list(range(9)),(256,256),(240,240),115,"center"),
        ("thunder_1.impact","thunder-impact.png",3,2,list(range(6)),(128,128),(64,64),50,"center"),
        ("martial_4","martial-slam.png",4,4,list(range(8)),(192,192),(96,96),25,"center"),
        ("martial_4.impact","martial-slam.png",4,4,list(range(8,16)),(192,192),(128,128),45,"ground"),
        ("water_5.travel","water-trail.png",3,2,list(range(6)),(128,64),(96,48),45,"center"),
        ("wind_3.travel","wind-trail.png",3,2,list(range(6)),(128,64),(96,48),45,"center"),
    ]
    records=[];properties={};qa=dict(clips=9,frames=67,empty_frames=0,edge_pixels=0,upscaled_frames=0,warnings=[],source_files=8)
    contact=Image.new("RGB",(1200,900),(20,34,48))
    for row,(name,file,cols,rows,indices,canvas,draw,millis,registration) in enumerate(specs):
        source=ART/file;bitmap=Image.open(source).convert("RGBA");rects=[(round(i%cols*bitmap.width/cols),round(i//cols*bitmap.height/rows),round((i%cols+1)*bitmap.width/cols),round((i//cols+1)*bitmap.height/rows)) for i in indices]
        # A primeira faixa da lança ultrapassa a divisão aritmética em dois pixels.
        if file=="thunder-spear.png":
            ys=[0,245,483,725];rects=[(r[0],ys[i//cols],r[2],ys[i//cols+1]) for i,r in zip(indices,rects)]
        originals=[bitmap.crop(rect) for rect in rects];boxes=[];anchors=[]
        for index,frame in zip(indices,originals):
            a=frame.getchannel("A");box=a.point(lambda v:255 if v>=16 else 0).getbbox()
            if box is None:raise ValueError(f"Estágio sem arte: {name}/{index}")
            edges=[a.crop((0,0,frame.width,1)),a.crop((0,frame.height-1,frame.width,frame.height)),a.crop((0,0,1,frame.height)),a.crop((frame.width-1,0,frame.width,frame.height))]
            if any(edge.getextrema()[1]>=16 for edge in edges):raise ValueError(f"Fonte atravessa célula: {name}/{index}")
            boxes.append(box)
            anchors.append(((box[0]+box[2])/2,(box[1]+box[3])/2) if registration=="center" else (192,211 if index<12 else 203))
        common=(int(min(b[0]-p[0] for b,p in zip(boxes,anchors)))-1,int(min(b[1]-p[1] for b,p in zip(boxes,anchors)))-1,int(max(b[2]-p[0] for b,p in zip(boxes,anchors)))+1,int(max(b[3]-p[1] for b,p in zip(boxes,anchors)))+1)
        width,height=common[2]-common[0],common[3]-common[1];scale=min(1,(canvas[0]-16)/width,(canvas[1]-16)/height)
        size=(round(width*scale),round(height*scale));offset=((canvas[0]-size[0])//2,(canvas[1]-size[1])//2);pivot=(round(offset[0]-common[0]*size[0]/width),round(offset[1]-common[1]*size[1]/height))
        frames=[]
        for frame,anchor in zip(originals,anchors):
            registered=Image.new("RGBA",(width,height));registered.alpha_composite(frame,(round(-anchor[0]-common[0]),round(-anchor[1]-common[1])))
            fitted=Image.new("RGBA",canvas);fitted.alpha_composite(registered.resize(size,Image.Resampling.NEAREST),offset)
            box=fitted.getchannel("A").getbbox()
            if box is None:raise ValueError(f"Frame vazio: {name}")
            if box[0]<=0 or box[1]<=0 or box[2]>=canvas[0] or box[3]>=canvas[1]:raise ValueError(f"Frame na borda: {name}")
            frames.append(fitted)
        atlas=Image.new("RGBA",(canvas[0]*4,canvas[1]*((len(frames)+3)//4)))
        for i,frame in enumerate(frames):atlas.alpha_composite(frame,(i%4*canvas[0],i//4*canvas[1]))
        path=destination/(name+".png");atlas.save(path)
        values=dict(path="assets/skills/refined-v8/"+path.name,frameWidth=canvas[0],frameHeight=canvas[1],columns=4,frames=len(frames),frameMillis=millis,drawWidth=draw[0],drawHeight=draw[1],pivotX=pivot[0],pivotY=pivot[1]);properties[name]=values
        records.append(dict(id=name,file=path.name,sha256=sha(path),source=source.relative_to(ROOT).as_posix(),source_sha256=sha(source),source_size=list(bitmap.size),rects=rects,anchors=anchors,registration=registration,common_crop=common,scale=scale,metadata=values))
        preview_frames=[]
        for frame in frames:
            background=Image.new("RGBA",draw,(20,34,48,255));background.alpha_composite(frame.resize(draw,Image.Resampling.NEAREST));preview_frames.append(background.convert("RGB"))
        preview_frames[0].save(preview/(name+".gif"),save_all=True,append_images=preview_frames[1:],duration=millis,loop=0,disposal=2)
        shot=preview_frames[len(frames)//2];x=row%3*400;y=row//3*300;contact.paste(shot,(x+(400-shot.width)//2,y+35+(240-shot.height)//2));ImageDraw.Draw(contact).text((x+10,y+10),name,fill="white")
    properties["thunder_3"]=dict(properties["thunder_1"],frameMillis=85)
    for name in ["thunder_1","thunder_2","thunder_3"]:properties[name+".impact"]=dict(properties["thunder_1.impact"])
    for name in ["thunder_4","thunder_5"]:properties[name+".travel"]=dict(properties["thunder_4"])
    properties["martial_4.travel"]=dict(properties["martial_4"])
    contact.save(preview/"contact.png");(preview/"qa.json").write_text(json.dumps(qa,indent=2)+"\n",encoding="utf-8")
    (destination/"provenance.json").write_text(json.dumps(dict(route="image_gen",model=None,estimateUsd=None,clips=records,aliases=properties,packaging="Recorte, registro e redução nearest; sem ampliação/recoloração/repintura por código."),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    lines=["# Overlay V8: raio dourado e agarrão/body slam.","formatVersion=8"]
    for name,values in properties.items():lines.extend(f"{name}.{key}={value}" for key,value in values.items())
    pending=destination/"manifest.properties.pending";pending.write_text("\n".join(lines)+"\n",encoding="utf-8");pending.replace(destination/"manifest.properties");print(json.dumps(qa))


if __name__=="__main__":
    build(Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/"assets/skills/refined-v8",Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/"docs/skills-preview-v8")
