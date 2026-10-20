package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/** Gestos agrupados em um comando Undo/Redo; instalados na janela principal. */
final class PaintTools {
    final WorldStudio ui;String tool="Selecionar";Point start;int changes;boolean tile;
    java.util.List<WorldDocument.Element> clipboard=java.util.List.of();
    PaintTools(WorldStudio ui) {
        this.ui=ui;JComboBox<String> tools=new JComboBox<>(new String[]{"Selecionar","Lapis","Borracha","Retangulo","Balde","Conta-gotas","Colisao"});tools.addActionListener(e->tool=(String)tools.getSelectedItem());ui.toolbar.add(tools);
        JComboBox<String> mode=new JComboBox<>(new String[]{"Objeto real","Tile na grade"});mode.addActionListener(e->tile=mode.getSelectedIndex()==1);ui.toolbar.add(mode);
        JButton crop=new JButton("Recortar tileset");crop.addActionListener(e->{if(ui.brush!=null)TilePicker.open(ui,ui.brush,ui.images.peek(ui.brush.id()),(next,all)->{ui.brushCrop=next;ui.tilesetCrops=all;mode.setSelectedIndex(1);ui.details.setText("Recorte "+next);});});ui.toolbar.add(crop);
        add("Desfazer","control Z",()->ui.change(ui.history.undo()));add("Refazer","control Y",()->ui.change(ui.history.redo()));add("Copiar","control C",()->clipboard=EditorOperations.copy(ui.document,ui.canvas.selected));add("Colar","control V",()->ui.change(EditorOperations.paste(ui.document,clipboard,ui.lastCursor.x,ui.lastCursor.y)));
        ui.canvas.pressed=e->{start=ui.canvas.worldPoint(e);changes=0;try{apply(start);}catch(Exception error){ui.error(error);start=null;}};
        ui.canvas.dragged=e->{if(start!=null&&(tool.equals("Lapis")||tool.equals("Borracha")))try{apply(ui.canvas.worldPoint(e));}catch(Exception error){ui.document=ui.history.current();ui.error(error);start=null;}};
        ui.canvas.released=e->{if(start==null)return;Point end=ui.canvas.worldPoint(e);try{
            if(tool.equals("Retangulo")){if(!tile)throw new IllegalArgumentException("Retangulo requer Tile na grade");ui.document=EditorOperations.rectangle(ui.document,brush(),start,end);}
            if(tool.equals("Colisao"))ui.collisionTools.drag(start,end);
            if(tool.equals("Selecionar")){Rectangle box=new Rectangle(Math.min(start.x,end.x),Math.min(start.y,end.y),Math.max(1,Math.abs(end.x-start.x)),Math.max(1,Math.abs(end.y-start.y)));if(!e.isShiftDown())ui.canvas.selected.clear();ui.document.elements().stream().filter(a->a.layer().equals(ui.canvas.activeLayer)&&box.intersects(a.x(),a.y(),a.width(),a.height())).forEach(a->ui.canvas.selected.add(a.id()));ui.inspectSelection();}
            if(tool.equals("Selecionar"))ui.document.hitboxes().stream().filter(h->new Rectangle(h.x(),h.y(),h.width(),h.height()).contains(end)).findFirst().ifPresent(h->ui.canvas.selected.add(h.id()));
            ui.change(ui.document);
        }catch(Exception error){ui.document=ui.history.current();ui.error(error);}finally{start=null;ui.canvas.repaint();}};
    }
    void add(String name,String key,Runnable action){JButton b=new JButton(name);b.addActionListener(e->{try{action.run();}catch(Exception error){ui.error(error);}});ui.toolbar.add(b);ui.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),name);ui.getRootPane().getActionMap().put(name,new AbstractAction(){public void actionPerformed(ActionEvent e){b.doClick();}});}
    WorldDocument.Element brush(){if(tile&&!ui.activeTerrain.isEmpty())return Autotiling.brush(ui.document,ui.activeTerrain,ui.canvas.activeLayer);if(ui.brush==null)throw new IllegalArgumentException("Selecione uma imagem na biblioteca");return new WorldDocument.Element("brush",WorldDocument.Kind.OBJECT,ui.brush.id(),ui.brush.sha(),ui.brushCrop,0,0,ui.brushCrop.width(),ui.brushCrop.height(),ui.canvas.activeLayer,true,false,"","FRONT",Map.of());}
    void apply(Point p){if(++changes>4096)throw new IllegalArgumentException("Gesto excede 4096 pontos");switch(tool){case "Lapis"->ui.document=EditorOperations.pencil(ui.document,brush(),p.x,p.y,tile);case "Borracha"->ui.document=EditorOperations.erase(ui.document,ui.canvas.activeLayer,p.x,p.y);case "Balde"->{if(!tile)throw new IllegalArgumentException("Balde requer Tile na grade");ui.document=EditorOperations.flood(ui.document,brush(),p.x,p.y);}case "Conta-gotas"->ui.document.elements().stream().filter(e->e.layer().equals(ui.canvas.activeLayer)&&new Rectangle(e.x(),e.y(),e.width(),e.height()).contains(p)).reduce((a,b)->b).ifPresent(e->{ui.brush=ui.catalog.byId().get(e.asset());ui.brushCrop=e.crop();});default->{}}ui.canvas.repaint();}
}
