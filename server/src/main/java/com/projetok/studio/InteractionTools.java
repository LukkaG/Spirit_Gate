package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/** Formularios para colocar entidades registradas sem criar classes ou scripts. */
final class InteractionTools {
    final WorldStudio ui;
    InteractionTools(WorldStudio ui){this.ui=ui;for(var kind:new WorldDocument.Kind[]{WorldDocument.Kind.NPC,WorldDocument.Kind.CHEST,WorldDocument.Kind.RESOURCE,WorldDocument.Kind.ENEMY,WorldDocument.Kind.PLAYER,WorldDocument.Kind.POINT})ui.action("Entidades","Inserir "+kind,()->add(kind));}
    void add(WorldDocument.Kind kind){JPanel form=new JPanel(new GridLayout(0,2));JComboBox<String> refs=new JComboBox<>(StudioContent.references(kind).toArray(String[]::new));form.add(new JLabel("ID do catalogo"));form.add(refs);JSpinner x=new JSpinner(new SpinnerNumberModel(Math.max(18,ui.lastCursor.x),0,8192,1)),y=new JSpinner(new SpinnerNumberModel(Math.max(18,ui.lastCursor.y),0,8192,1));form.add(new JLabel("X"));form.add(x);form.add(new JLabel("Y"));form.add(y);JComboBox<String> direction=new JComboBox<>(new String[]{"FRONT","BACK","LEFT","RIGHT"});form.add(new JLabel("Orientacao"));form.add(direction);JCheckBox sprite=new JCheckBox("Usar imagem selecionada (opcional)");form.add(sprite);
        if(JOptionPane.showConfirmDialog(ui,form,"Inserir "+kind,JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        boolean image=sprite.isSelected()&&ui.brush!=null;String layer=ui.document.layers().stream().filter(l->l.role()==WorldDocument.Role.INTERACTION).findFirst().orElseThrow().id();EditorOperations.editable(ui.document,layer);
        var element=new WorldDocument.Element(EditorOperations.id(),kind,image?ui.brush.id():"",image?ui.brush.sha():"",image?ui.brushCrop:new WorldDocument.Crop(0,0,32,32),(Integer)x.getValue(),(Integer)y.getValue(),32,32,layer,true,false,(String)refs.getSelectedItem(),(String)direction.getSelectedItem(),Map.of());StudioContent.validate(element,ui.document.region());
        if(kind==WorldDocument.Kind.PLAYER&&ui.document.elements().stream().anyMatch(e->e.kind()==kind))throw new IllegalArgumentException("O mapa ja tem spawn do jogador");
        var next=new ArrayList<>(ui.document.elements());next.add(element);ui.change(ui.document.withElements(next));ui.warmImages();
    }
}
