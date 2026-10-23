package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/** Gestos agrupados em um comando Undo/Redo; instalados na janela principal. */
final class PaintTools {
    final WorldStudio ui;String tool="Selecionar";Point start;int changes;boolean tile,rightPainting;Point lastPaint;boolean moving,copyFromPalette;WorldDocument gestureBase;
    record BrushSelection(AssetCatalog.Asset asset,WorldDocument.Crop crop,boolean tile) {}
    BrushSelection leftBrush,rightBrush;
    final JLabel assignments=new JLabel("Pinceis: selecione os recortes");
    java.util.List<WorldDocument.Element> clipboard=java.util.List.of();
    PaintTools(WorldStudio ui) {
        this.ui=ui;JComboBox<String> tools=new JComboBox<>(new String[]{"Selecionar","Mover","Lapis","Borracha","Retangulo","Balde","Conta-gotas","Colisao"});tools.addActionListener(e->tool=(String)tools.getSelectedItem());ui.toolbar.add(tools);
        JComboBox<String> mode=new JComboBox<>(new String[]{"Objeto real","Tile na grade"});mode.addActionListener(e->tile=mode.getSelectedIndex()==1);ui.toolbar.add(mode);
        ui.toolbar.add(assignments);
        ui.canvas.brushPreview=()->{
            if(!Set.of("Lapis","Retangulo","Balde").contains(tool))return null;
            try{var b=brush();int grid=ui.document.grid(),x=ui.canvas.cursor.x,y=ui.canvas.cursor.y;
                if(brushTile()){x=Math.floorDiv(x,grid)*grid;y=Math.floorDiv(y,grid)*grid;}
                return new WorldDocument.Element("preview",b.kind(),b.asset(),b.sha(),b.crop(),x,y,b.width(),b.height(),b.layer(),true,false,b.reference(),b.direction(),b.properties());
            }catch(IllegalArgumentException e){return null;}
        };
        ui.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),"Cancelar gesto");
        ui.getRootPane().getActionMap().put("Cancelar gesto",new AbstractAction(){public void actionPerformed(ActionEvent e){cancelGesture();}});
        ui.addWindowFocusListener(new WindowAdapter(){@Override public void windowLostFocus(WindowEvent e){cancelGesture();}});
        ui.tilePalette.choose=(c,right)->{
            var asset=ui.tilePalette.asset;if(asset==null)return;copyFromPalette=true;
            ui.brushCrop=c;assign(asset,c,c.width()<=(Integer)ui.tilePalette.tileWidth.getValue()&&c.height()<=(Integer)ui.tilePalette.tileHeight.getValue(),right);
            if(!right)tools.setSelectedItem("Lapis");ui.tilePalette.select(c);
        };
        add("Desfazer","control Z",()->ui.change(ui.history.undo()));add("Refazer","control Y",()->ui.change(ui.history.redo()));add("Copiar","control C",this::copySelection);add("Colar","control V",this::pasteSelection);
        ui.canvas.pressed=e->{copyFromPalette=false;start=null;rightPainting=false;lastPaint=null;changes=0;moving=false;gestureBase=ui.document;ui.canvas.selectionBox=null;
            if(!SwingUtilities.isLeftMouseButton(e)&&!SwingUtilities.isRightMouseButton(e))return;
            start=ui.canvas.worldPoint(e);rightPainting=SwingUtilities.isRightMouseButton(e);
            if(!rightPainting&&(tool.equals("Selecionar")||tool.equals("Mover"))&&!e.isShiftDown()){
                var hit=ui.document.elements().stream().filter(a->a.layer().equals(ui.canvas.activeLayer)&&new Rectangle(a.x(),a.y(),a.width(),a.height()).contains(start)).reduce((a,b)->b);
                if(hit.isPresent()&&(tool.equals("Mover")||ui.canvas.selected.contains(hit.get().id()))){
                    if(!ui.canvas.selected.contains(hit.get().id())){ui.canvas.selected.clear();ui.canvas.selected.add(hit.get().id());}moving=true;
                }
            }
            try{if(rightPainting)paintRight(start);else apply(start);}catch(Exception error){ui.document=ui.history.current();ui.error(error);start=null;rightPainting=false;}
        };
        ui.canvas.dragged=e->{if(start!=null)try{if(rightPainting)paintRight(ui.canvas.worldPoint(e));else if(moving)moveSelection(ui.canvas.worldPoint(e));else if(tool.equals("Selecionar")||tool.equals("Mover")){ui.canvas.selectionBox=selectionBox(start,ui.canvas.worldPoint(e));ui.canvas.repaint();}else if(tool.equals("Lapis")||tool.equals("Borracha"))apply(ui.canvas.worldPoint(e));}catch(Exception error){ui.document=ui.history.current();ui.canvas.repaint();ui.error(error);start=null;rightPainting=false;}};
        ui.canvas.released=e->{if(start==null)return;Point end=ui.canvas.worldPoint(e);try{
            if(rightPainting){paintRight(end);ui.change(ui.document);return;}
            if(moving){moveSelection(end);ui.change(ui.document);return;}
            if(tool.equals("Retangulo")){if(!brushTile())throw new IllegalArgumentException("Retangulo requer Tile na grade");ui.document=EditorOperations.rectangle(ui.document,brush(),start,end);}
            if(tool.equals("Colisao"))ui.collisionTools.drag(start,end);
            if(tool.equals("Selecionar")||tool.equals("Mover")){Rectangle box=selectionBox(start,end);if(!e.isShiftDown())ui.canvas.selected.clear();ui.document.elements().stream().filter(a->a.layer().equals(ui.canvas.activeLayer)&&box.intersects(a.x(),a.y(),a.width(),a.height())).forEach(a->ui.canvas.selected.add(a.id()));ui.inspectSelection();}
            if(tool.equals("Selecionar"))ui.document.hitboxes().stream().filter(h->new Rectangle(h.x(),h.y(),h.width(),h.height()).contains(end)).findFirst().ifPresent(h->ui.canvas.selected.add(h.id()));
            ui.change(ui.document);
        }catch(Exception error){ui.document=ui.history.current();ui.error(error);}finally{ui.canvas.selectionBox=null;moving=false;gestureBase=null;start=null;rightPainting=false;lastPaint=null;ui.canvas.repaint();}};
    }
    static Rectangle selectionBox(Point a,Point b){return new Rectangle(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.abs(a.x-b.x)+1,Math.abs(a.y-b.y)+1);}
    /** Reverte apenas o gesto ainda não confirmado; não acrescenta uma entrada ao histórico. */
    void cancelGesture(){if(start!=null)ui.document=ui.history.current();start=null;gestureBase=null;moving=false;rightPainting=false;lastPaint=null;ui.canvas.selectionBox=null;ui.canvas.repaint();}
    void moveSelection(Point end){
        int dx=end.x-start.x,dy=end.y-start.y;
        if(ui.snap){int grid=ui.document.grid();dx=Math.round((float)dx/grid)*grid;dy=Math.round((float)dy/grid)*grid;}
        ui.document=GroupEditing.move(gestureBase,ui.canvas.selected,dx,dy);ui.canvas.repaint();
    }
    void copySelection(){
        if(copyFromPalette||ui.tilePalette.sheet.isFocusOwner()||ui.canvas.selected.isEmpty()){
            var palette=ui.tilePalette;if(palette.asset==null||palette.selected==null){ui.status.setText("Selecione tiles no mapa ou na aba Tiles para copiar.");return;}
            var c=palette.selected;clipboard=java.util.List.of(new WorldDocument.Element("clipboard",WorldDocument.Kind.OBJECT,palette.asset.id(),palette.asset.sha(),c,0,0,c.width(),c.height(),ui.canvas.activeLayer,true,false,"","FRONT",Map.of()));
        }else clipboard=EditorOperations.copy(ui.document,ui.canvas.selected);
        ui.status.setText(clipboard.size()+" elemento(s) copiado(s). Posicione o cursor no mapa e use Colar.");
    }
    void pasteSelection(){
        if(clipboard.isEmpty())return;
        int x=ui.lastCursor.x,y=ui.lastCursor.y;if(ui.snap){x=Math.floorDiv(x,ui.document.grid())*ui.document.grid();y=Math.floorDiv(y,ui.document.grid())*ui.document.grid();}
        var before=ui.document;var next=EditorOperations.paste(before,clipboard,x,y);var old=new HashSet<String>();before.elements().forEach(e->old.add(e.id()));
        ui.change(next);ui.canvas.selected.clear();next.elements().stream().filter(e->!old.contains(e.id())).forEach(e->ui.canvas.selected.add(e.id()));ui.inspectSelection();ui.canvas.repaint();
    }
    void paintRight(Point p){
        int step=brushTile()?ui.document.grid():Math.max(1,Math.min(brush().width(),brush().height()));
        if(p.equals(lastPaint))return;
        Point from=lastPaint==null?p:lastPaint;
        int count=Math.max(1,(int)Math.ceil(Math.max(Math.abs(p.x-from.x),Math.abs(p.y-from.y))/(double)step));
        for(int i=1;i<=count;i++){
            Point at=new Point(from.x+(p.x-from.x)*i/count,from.y+(p.y-from.y)*i/count);
            if(brushTile()&&lastPaint!=null&&Math.floorDiv(at.x,step)==Math.floorDiv(lastPaint.x,step)&&Math.floorDiv(at.y,step)==Math.floorDiv(lastPaint.y,step))continue;
            if(++changes>4096)throw new IllegalArgumentException("Gesto excede 4096 pontos");
            ui.document=EditorOperations.pencil(ui.document,brush(),at.x,at.y,brushTile());lastPaint=at;
        }
        ui.canvas.repaint();
    }
    void add(String name,String key,Runnable action){JButton b=new JButton(name);b.addActionListener(e->{try{action.run();}catch(Exception error){ui.error(error);}});ui.toolbar.add(b);ui.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),name);ui.getRootPane().getActionMap().put(name,new AbstractAction(){public void actionPerformed(ActionEvent e){b.doClick();}});}
    void assign(AssetCatalog.Asset asset,WorldDocument.Crop crop,boolean tile,boolean right){
        var selection=new BrushSelection(asset,crop,tile);if(right)rightBrush=selection;else leftBrush=selection;
        ui.activeTerrain="";
        assignments.setText("Esq: "+describe(leftBrush)+" | Dir: "+describe(rightBrush));
        assignments.setToolTipText(assignments.getText());
    }
    String describe(BrushSelection b){return b==null?"imagem atual":b.asset().file().getFileName()+" ["+b.crop().x()+","+b.crop().y()+"]";}
    BrushSelection activeBrush(){return rightPainting?rightBrush:leftBrush;}
    boolean brushTile(){var b=activeBrush();return b==null?tile:b.tile();}
    WorldDocument.Element brush(){
        if(brushTile()&&!ui.activeTerrain.isEmpty())return Autotiling.brush(ui.document,ui.activeTerrain,ui.canvas.activeLayer);
        var b=activeBrush();var asset=b==null?ui.brush:b.asset();var crop=b==null?ui.brushCrop:b.crop();
        if(asset==null)throw new IllegalArgumentException("Selecione uma imagem na biblioteca");
        return new WorldDocument.Element("brush",WorldDocument.Kind.OBJECT,asset.id(),asset.sha(),crop,0,0,crop.width(),crop.height(),ui.canvas.activeLayer,true,false,"","FRONT",Map.of());
    }
    void apply(Point p){if(!tool.equals("Lapis")&&++changes>4096)throw new IllegalArgumentException("Gesto excede 4096 pontos");switch(tool){case "Lapis"->paintRight(p);case "Borracha"->ui.document=EditorOperations.erase(ui.document,ui.canvas.activeLayer,p.x,p.y);case "Balde"->{if(!brushTile())throw new IllegalArgumentException("Balde requer Tile na grade");ui.document=EditorOperations.flood(ui.document,brush(),p.x,p.y);}case "Conta-gotas"->ui.document.elements().stream().filter(e->e.layer().equals(ui.canvas.activeLayer)&&new Rectangle(e.x(),e.y(),e.width(),e.height()).contains(p)).reduce((a,b)->b).ifPresent(e->{ui.brush=ui.catalog.byId().get(e.asset());ui.brushCrop=e.crop();if(ui.brush!=null)assign(ui.brush,e.crop(),e.kind()==WorldDocument.Kind.TILE,false);});default->{}}ui.canvas.repaint();}
}
