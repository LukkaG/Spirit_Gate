package com.projetok.studio;
import javax.swing.*;
/** Ajustes de ergonomia e acesso seguro a exclusao de portais. */
final class FinalTools {
    FinalTools(WorldStudio ui){ui.action("Editar","Configurar grade",()->{String value=JOptionPane.showInputDialog(ui,"Grade em pixels (1..256); nao reposiciona conteudo existente",ui.document.grid());if(value==null)return;var d=ui.document;ui.change(new WorldDocument(1,d.id(),d.name(),d.region(),d.width(),d.height(),Integer.parseInt(value),d.layers(),d.elements(),d.hitboxes(),d.portals(),d.triggers(),d.metadata()));});ui.action("Mapa","Remover portal",()->{String id=(String)JOptionPane.showInputDialog(ui,"Portal","Remover",JOptionPane.PLAIN_MESSAGE,null,ui.document.portals().stream().map(WorldDocument.Portal::id).toArray(String[]::new),null);if(id!=null){var next=ui.document.withPortals(ui.document.portals().stream().filter(p->!p.id().equals(id)).toList());StudioEvents.validate(next);ui.change(next);}});
        for(String key:new String[]{"control S","control shift S","control O","control N"}){ui.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),key);ui.getRootPane().getActionMap().put(key,new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){switch(key){case "control S"->ui.fileTools.save(false,()->{});case "control shift S"->ui.fileTools.save(true,()->{});case "control O"->ui.fileTools.confirm(()->ui.fileTools.open(false));default->ui.fileTools.confirm(ui.fileTools::create);}}});}
    }
}
