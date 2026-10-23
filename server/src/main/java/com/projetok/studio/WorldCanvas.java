package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.function.*;

/** Canvas de autoria; paintComponent consulta apenas documento e imagens residentes. */
public final class WorldCanvas extends JPanel {
    final Supplier<WorldDocument> source;
    final AssetImages images;
    StudioViewport viewport=new StudioViewport(.75,20,20);
    boolean grid=true,collisions=true;
    boolean playerPreview;
    Rectangle selectionBox;
    Point cursor=new Point();
    String activeLayer="ground";
    final Set<String> selected=new LinkedHashSet<>();
    Consumer<Point> pointer=p->{};
    Consumer<MouseEvent> pressed=e->{},dragged=e->{},released=e->{};
    Supplier<WorldDocument.Element> brushPreview=()->null;
    Point panStart;double startX,startY;
    final long animationStart=System.nanoTime();
    WorldDocument indexedDocument;CanvasIndex index;com.projetok.domain.movement.CollisionService.Space physics;
    final javax.swing.Timer animationTimer=new javax.swing.Timer(50,e->animate());
    private void animate(){var d=source.get();if(d.elements().stream().anyMatch(a->a.properties().containsKey("animation.frames"))||EnvironmentPreview.mode(d)==EnvironmentPreview.Mode.RAIN||EnvironmentPreview.mode(d)==EnvironmentPreview.Mode.CYCLE)repaint();}
    @Override public void addNotify(){super.addNotify();animationTimer.start();}
    @Override public void removeNotify(){animationTimer.stop();super.removeNotify();}
    /** Canvas independente de arquivo e de regras de combate. */
    public WorldCanvas(Supplier<WorldDocument> source,AssetImages images) {
        this.source=source;this.images=images;setBackground(new Color(27,31,40));setFocusable(true);
        var mouse=new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){requestFocusInWindow();if(SwingUtilities.isMiddleMouseButton(e)){panStart=e.getPoint();startX=viewport.panX();startY=viewport.panY();}else pressed.accept(e);}
            @Override public void mouseDragged(MouseEvent e){if(panStart!=null){viewport=new StudioViewport(viewport.zoom(),startX+e.getX()-panStart.x,startY+e.getY()-panStart.y);repaint();}else dragged.accept(e);mouseMoved(e);}
            @Override public void mouseReleased(MouseEvent e){if(panStart!=null)panStart=null;else released.accept(e);}
            @Override public void mouseMoved(MouseEvent e){cursor=worldPoint(e);pointer.accept(cursor);repaint();}
            @Override public void mouseWheelMoved(MouseWheelEvent e){viewport=viewport.zoomAt(viewport.zoom()*Math.pow(1.12,-e.getWheelRotation()),e.getX(),e.getY());repaint();}
        };addMouseListener(mouse);addMouseMotionListener(mouse);addMouseWheelListener(mouse);
    }
    /** Conversao utilizada por todas as ferramentas. */
    public Point worldPoint(MouseEvent event){var p=viewport.world(event.getX(),event.getY());return new Point((int)Math.floor(p.x),(int)Math.floor(p.y));}
    /** Zoom explicito conserva o centro da area de trabalho. */
    public void zoom(double value){viewport=viewport.zoomAt(value,getWidth()/2d,getHeight()/2d);repaint();}
    @Override protected void paintComponent(Graphics original) {
        super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            var d=source.get();g.translate(viewport.panX(),viewport.panY());g.scale(viewport.zoom(),viewport.zoom());g.clipRect(0,0,d.width(),d.height());
            g.setColor(new Color(43,70,59));g.fillRect(0,0,d.width(),d.height());Rectangle visible=g.getClipBounds();
            if(indexedDocument!=d){index=new CanvasIndex(d);physics=StudioGeometry.space(d);indexedDocument=d;}var visibleElements=index.query(visible);
            for(var layer:d.orderedLayers())if(layer.visible())for(var e:visibleElements.getOrDefault(layer.id(),java.util.List.of())) {
                Optional<EnvironmentAnimation.Sequence> sequence;try{sequence=EnvironmentAnimation.sequence(e);}catch(RuntimeException invalid){sequence=Optional.empty();}var image=images.peek(sequence.map(s->s.frames().get(s.index(System.nanoTime()-animationStart))).orElse(e.asset()));var c=sequence.isPresent()&&image!=null?new WorldDocument.Crop(0,0,image.getWidth(),image.getHeight()):e.crop();
                if(image!=null&&c.x()+c.width()<=image.getWidth()&&c.y()+c.height()<=image.getHeight())g.drawImage(image,e.x(),e.y(),e.x()+e.width(),e.y()+e.height(),c.x(),c.y(),c.x()+c.width(),c.y()+c.height(),null);
                else {g.setColor(e.kind()==WorldDocument.Kind.PLAYER?Color.CYAN:new Color(130,170,175));g.drawRect(e.x(),e.y(),e.width(),e.height());g.setFont(new Font("SansSerif",0,11));g.drawString(e.reference().isEmpty()?e.kind().name():e.reference(),e.x(),e.y()+12);}
                if(selected.contains(e.id())){g.setColor(Color.YELLOW);g.setStroke(new BasicStroke((float)(2/viewport.zoom())));g.drawRect(e.x(),e.y(),e.width(),e.height());}
                if(e.kind()==WorldDocument.Kind.ENEMY&&e.properties().containsKey("spawn.quantity")){g.setColor(new Color(255,140,60,60));g.fillRect(e.x(),e.y(),e.width(),e.height());g.setColor(Color.ORANGE);g.drawString("Spawn x"+e.properties().get("spawn.quantity"),e.x(),e.y()+12);}
            }
            if(grid&&d.grid()*viewport.zoom()>=8){g.setColor(new Color(255,255,255,30));g.setStroke(new BasicStroke((float)(1/viewport.zoom())));int cell=d.grid();for(int x=Math.max(0,visible.x/cell*cell);x<=Math.min(d.width(),visible.x+visible.width);x+=cell)g.drawLine(x,visible.y,x,visible.y+visible.height);for(int y=Math.max(0,visible.y/cell*cell);y<=Math.min(d.height(),visible.y+visible.height);y+=cell)g.drawLine(visible.x,y,visible.x+visible.width,y);}
            if(collisions)for(var h:d.hitboxes()) {g.setColor(h.solid()?new Color(255,85,70,80):new Color(65,220,160,60));g.fillRect(h.x(),h.y(),h.width(),h.height());g.setColor(selected.contains(h.id())?Color.YELLOW:h.solid()?Color.RED:Color.GREEN);g.drawRect(h.x(),h.y(),h.width(),h.height());}
            if(playerPreview){g.setColor(StudioGeometry.free(physics,cursor.x,cursor.y,18)?Color.GREEN:Color.RED);g.drawOval(cursor.x-18,cursor.y-18,36,36);}
            var ghost=brushPreview.get();
            if(ghost!=null){var resident=images.peek(ghost.asset());var crop=ghost.crop();var composite=g.getComposite();g.setComposite(AlphaComposite.SrcOver.derive(.45f));
                if(resident!=null&&crop.x()+crop.width()<=resident.getWidth()&&crop.y()+crop.height()<=resident.getHeight())g.drawImage(resident,ghost.x(),ghost.y(),ghost.x()+ghost.width(),ghost.y()+ghost.height(),crop.x(),crop.y(),crop.x()+crop.width(),crop.y()+crop.height(),null);
                g.setComposite(composite);g.setColor(new Color(88,204,200));g.drawRect(ghost.x(),ghost.y(),ghost.width(),ghost.height());}
            for(var p:d.portals()){g.setColor(Color.MAGENTA);g.drawOval(p.x()-12,p.y()-12,24,24);g.drawString(p.destination(),p.x()+14,p.y());}
            if(selectionBox!=null){g.setColor(new Color(80,190,255,50));g.fill(selectionBox);g.setColor(Color.CYAN);g.setStroke(new BasicStroke((float)(1/viewport.zoom())));g.draw(selectionBox);}
            EnvironmentPreview.draw(g,visible,EnvironmentPreview.mode(d),System.nanoTime()-animationStart);
        }finally{g.dispose();}
    }
}
