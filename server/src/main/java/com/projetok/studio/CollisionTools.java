package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/** Hitboxes editaveis em dados, com snap opcional e previa de raio 18. */
final class CollisionTools {
    final WorldStudio ui;
    CollisionTools(WorldStudio ui){this.ui=ui;ui.action("Mapa","Nova colisao",()->edit(null));ui.action("Mapa","Editar colisao",()->{var selected=ui.document.hitboxes().stream().filter(h->ui.canvas.selected.contains(h.id())).findFirst().orElse(null);if(selected==null){String id=(String)JOptionPane.showInputDialog(ui,"Colisao","Editar",JOptionPane.PLAIN_MESSAGE,null,ui.document.hitboxes().stream().map(WorldDocument.Hitbox::id).toArray(String[]::new),null);if(id==null)return;selected=ui.document.hitboxes().stream().filter(h->h.id().equals(id)).findFirst().orElseThrow();}edit(selected);});
        ui.action("Ver","Colisoes",()->{ui.canvas.collisions=!ui.canvas.collisions;ui.canvas.repaint();});ui.action("Ver","Raio do jogador",()->{ui.canvas.playerPreview=!ui.canvas.playerPreview;ui.canvas.repaint();});ui.action("Editar","Alternar snap",()->{ui.snap=!ui.snap;ui.status.setText("Snap: "+ui.snap);});
    }
    void drag(Point a,Point b){int x=Math.max(0,Math.min(a.x,b.x)),y=Math.max(0,Math.min(a.y,b.y));int w=Math.max(ui.document.grid(),Math.abs(a.x-b.x)),h=Math.max(ui.document.grid(),Math.abs(a.y-b.y));if(ui.snap){x=x/ui.document.grid()*ui.document.grid();y=y/ui.document.grid()*ui.document.grid();}add(new WorldDocument.Hitbox(EditorOperations.id(),x,y,w,h,true,layer()));}
    String layer(){return ui.document.layers().stream().filter(l->l.role()==WorldDocument.Role.COLLISION).findFirst().orElseThrow(()->new IllegalArgumentException("Crie camada COLLISION")).id();}
    void add(WorldDocument.Hitbox hitbox){EditorOperations.editable(ui.document,hitbox.layer());var list=new ArrayList<>(ui.document.hitboxes());list.removeIf(h->h.id().equals(hitbox.id()));list.add(hitbox);ui.change(ui.document.withHitboxes(list));ui.status.setText("Colisao atualizada | Sobreposicoes: "+StudioGeometry.overlaps(ui.document).size());}
    void edit(WorldDocument.Hitbox old){int[] defaults=old==null?new int[]{Math.max(0,ui.lastCursor.x),Math.max(0,ui.lastCursor.y),32,32}:new int[]{old.x(),old.y(),old.width(),old.height()};JPanel panel=new JPanel(new GridLayout(0,2));JSpinner[] fields=new JSpinner[4];String[] names={"X","Y","Largura","Altura"};for(int i=0;i<4;i++){panel.add(new JLabel(names[i]));fields[i]=new JSpinner(new SpinnerNumberModel(defaults[i],0,8192,1));panel.add(fields[i]);}JCheckBox solid=new JCheckBox("Solido",old==null||old.solid());panel.add(solid);if(JOptionPane.showConfirmDialog(ui,panel,"Colisao explicita",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;int[] n=Arrays.stream(fields).mapToInt(f->(Integer)f.getValue()).toArray();if(ui.snap){n[0]=n[0]/ui.document.grid()*ui.document.grid();n[1]=n[1]/ui.document.grid()*ui.document.grid();}add(new WorldDocument.Hitbox(old==null?EditorOperations.id():old.id(),n[0],n[1],n[2],n[3],solid.isSelected(),old==null?layer():old.layer()));}
}
