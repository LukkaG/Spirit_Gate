package com.projetok.studio;
import javax.swing.*;
import java.awt.*;
import java.util.*;

/** Comandos de grupo e selecao explicita, inclusive de objetos bloqueados. */
final class GroupTools {
    GroupTools(WorldStudio ui){
        ui.action("Selecao","Selecionar por ID",()->{var model=new DefaultListModel<String>();ui.document.elements().forEach(e->model.addElement(e.id()+" | "+e.kind()+" | "+e.layer()+(e.locked()?" [bloqueado]":"")));ui.document.hitboxes().forEach(b->model.addElement(b.id()+" | colisao"));JList<String> list=new JList<>(model);if(JOptionPane.showConfirmDialog(ui,new JScrollPane(list),"Selecao multipla",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){ui.canvas.selected.clear();list.getSelectedValuesList().forEach(s->ui.canvas.selected.add(s.split(" ")[0]));ui.canvas.repaint();}});
        ui.action("Selecao","Mover grupo",()->{String value=JOptionPane.showInputDialog(ui,"Deslocamento dx,dy (pixels)","32,0");if(value==null)return;String[] xy=value.split(",");if(xy.length!=2)throw new IllegalArgumentException("Informe dx,dy");ui.change(GroupEditing.move(ui.document,ui.canvas.selected,Integer.parseInt(xy[0].trim()),Integer.parseInt(xy[1].trim())));});
        ui.action("Selecao","Duplicar",()->ui.change(EditorOperations.paste(ui.document,EditorOperations.copy(ui.document,ui.canvas.selected),ui.lastCursor.x,ui.lastCursor.y)));
        ui.action("Selecao","Excluir",()->ui.change(GroupEditing.delete(ui.document,ui.canvas.selected)));
        ui.action("Selecao","Bloquear",()->ui.change(GroupEditing.lock(ui.document,ui.canvas.selected,true)));
        ui.action("Selecao","Desbloquear",()->ui.change(GroupEditing.lock(ui.document,ui.canvas.selected,false)));
        ui.action("Selecao","Trazer para frente",()->ui.change(GroupEditing.order(ui.document,ui.canvas.selected,true)));
        ui.action("Selecao","Enviar para tras",()->ui.change(GroupEditing.order(ui.document,ui.canvas.selected,false)));
        for(boolean horizontal:new boolean[]{true,false})for(boolean distribute:new boolean[]{false,true})ui.action("Selecao",(distribute?"Distribuir ":"Alinhar ")+(horizontal?"horizontal":"vertical"),()->ui.change(GroupEditing.arrange(ui.document,ui.canvas.selected,horizontal,distribute)));
    }
}
