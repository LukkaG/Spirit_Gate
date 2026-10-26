package com.projetok.studio;
import com.projetok.rpg.*;
import com.projetok.prototype.*;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import java.awt.*;
import java.util.*;
import java.util.function.Supplier;

/** Camadas do bundle sobre o cliente real; imagens ja carregadas, sem I/O no desenho. */
public final class StudioMapRenderer implements DebugPanel.ActorRenderer {
    final StudioExport.Bundle bundle;final AssetImages images;final Supplier<RpgWorld.View> source;final RpgPlayerSprites player;
    private java.util.function.Supplier<String> region;
    public StudioMapRenderer(StudioExport.Bundle bundle,AssetImages images,java.util.function.Supplier<String> region){this(bundle,images,()->null,null);this.region=region;}
    private WorldDocument indexed;private CanvasIndex index;
    StudioMapRenderer(StudioExport.Bundle bundle,AssetImages images,Supplier<RpgWorld.View> source,RpgPlayerSprites player){this.bundle=bundle;this.images=images;this.source=source;this.player=player;}
    public WorldDocument document(){String current=region==null?source.get().state().region():region.get();return bundle.maps().values().stream().filter(d->d.region().equals(current)).findFirst().orElseThrow();}
    public void layers(Graphics2D original,boolean above){Graphics2D g=(Graphics2D)original.create();try{var d=document();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);if(!above){g.setColor(new Color(43,70,59));g.fillRect(0,0,d.width(),d.height());}if(indexed!=d){index=new CanvasIndex(d);indexed=d;}Rectangle visible=g.getClipBounds();if(visible==null)visible=new Rectangle(0,0,d.width(),d.height());var elements=index.query(visible);for(var l:d.orderedLayers())if(l.visible()&&(l.role()==WorldDocument.Role.ABOVE||l.role()==WorldDocument.Role.ROOF)==above)for(var e:elements.getOrDefault(l.id(),java.util.List.of()))if(e.visible()&&e.layer().equals(l.id())&&(e.kind()==WorldDocument.Kind.TILE||e.kind()==WorldDocument.Kind.OBJECT))drawImage(g,e,e.x(),e.y());}finally{g.dispose();}}
    boolean drawImage(Graphics2D g,WorldDocument.Element e,int x,int y){var image=images.peek(e.asset());if(image==null)return false;var c=e.crop();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(image,x,y,x+e.width(),y+e.height(),c.x(),c.y(),c.x()+c.width(),c.y()+c.height(),null);return true;}
    public boolean drawNetwork(Graphics2D g,DebugSnapshot.ActorView actor){
        if(!actor.alive()||actor.id()<1_000_000)return false;String reference=null;
        if(actor.id()<2_000_000){int index=actor.id()-1_000_100;var keys=new java.util.ArrayList<>(new java.util.TreeSet<>(bundle.scenes().enemies().keySet()));if(index>=0&&index<keys.size())reference=keys.get(index);}
        else{int index=actor.id()-2_000_001;var keys=new java.util.TreeSet<String>();bundle.scenes().npcs().forEach((id,value)->{if(value.split("\\|")[0].equals(document().region()))keys.add(id);});if(index>=0&&index<keys.size())reference=new java.util.ArrayList<>(keys).get(index);}
        if(reference==null)return false;for(var e:document().elements())if(!e.asset().isEmpty()){
            boolean matches=e.kind()==WorldDocument.Kind.NPC&&e.reference().equals(reference);
            if(e.kind()==WorldDocument.Kind.ENEMY)for(int i=0;i<SpawnAreas.quantity(e);i++)matches|=SpawnAreas.key(document(),e,i).equals(reference);
            if(matches)return drawImage(g,e,Math.round(actor.x()-e.width()/2f),Math.round(actor.y()-e.height()));
        }return false;
    }
    @Override public boolean draw(Graphics2D original,DebugSnapshot.ActorView actor){if(actor.id()==1)return player!=null&&player.draw(original,actor);if(!actor.alive())return false;var view=source.get();WorldDocument.Element found=null;
        if(view==null)return false;
        for(var e:document().elements())if(!e.asset().isEmpty()) {
            if(e.kind()==WorldDocument.Kind.NPC){int i=actor.id()-500;if(i>=0&&i<view.npcs().size()&&view.npcs().get(i).id().equals(e.reference()))found=e;}
            if(e.kind()==WorldDocument.Kind.ENEMY&&view.enemies().stream().anyMatch(n->n.id()==actor.id()&&java.util.stream.IntStream.range(0,SpawnAreas.quantity(e)).anyMatch(i->n.spawn().equals(SpawnAreas.key(document(),e,i)))))found=e;
        }
        if(found==null)return false;Graphics2D g=(Graphics2D)original.create();try{if(!drawImage(g,found,Math.round(actor.x()-found.width()/2f),Math.round(actor.y()-found.height())))return false;g.setColor(Color.WHITE);g.drawString(actor.name(),actor.x()-found.width()/2f,actor.y()-found.height()-4);if(!actor.player()&&actor.maxHp()>1){g.setColor(Color.RED);g.fillRect(Math.round(actor.x()-16),Math.round(actor.y()+3),32*actor.hp()/actor.maxHp(),3);}return true;}finally{g.dispose();}
    }
}
