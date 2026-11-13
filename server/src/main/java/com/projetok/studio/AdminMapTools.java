package com.projetok.studio;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;

/** Catalogo administrativo dos mapas ativos e rascunhos locais, sem publicacao implicita. */
final class AdminMapTools {
    private final WorldStudio ui;
    private final DefaultListModel<String> model=new DefaultListModel<>();
    private final JList<String> list=new JList<>(model);
    private final Map<String,Path> files=new HashMap<>();
    AdminMapTools(WorldStudio ui){
        this.ui=ui;var panel=new JPanel(new BorderLayout(4,4));panel.add(new JScrollPane(list));
        var buttons=new JPanel(new GridLayout(0,1,4,4));
        button(buttons,"Novo mapa",()->ui.fileTools.confirm(ui.fileTools::create));
        button(buttons,"Entrar no editor",this::open);button(buttons,"Atualizar lista",this::refresh);
        panel.add(buttons,BorderLayout.SOUTH);list.setVisibleRowCount(8);ui.inspectorTabs.addTab("Mapas",panel);
        ui.action("Admin","Mapas do jogo",()->{refresh();ui.inspectorTabs.setSelectedComponent(panel);});
        refresh();
    }
    private void button(JPanel panel,String label,Runnable action){var b=new JButton(label);b.addActionListener(e->{try{action.run();}catch(Exception error){ui.error(error);}});panel.add(b);}
    /** Lista mapas ativos, documentos ja abertos e arquivos locais; erros ficam visoveis. */
    void refresh(){
        ui.knownMaps.put(ui.document.id(),ui.document);model.clear();new TreeSet<>(ui.knownMaps.keySet()).forEach(model::addElement);
        new SwingWorker<Map<String,Path>,Void>(){
            final java.util.List<String> warnings=new ArrayList<>();
            protected Map<String,Path> doInBackground()throws Exception{
                var found=new TreeMap<String,Path>();Path folder=ui.project.resolve("studio-projects");if(!Files.isDirectory(folder))return found;
                try(var paths=Files.walk(folder,3)){for(var path:paths.filter(p->p.toString().endsWith(".sgmap")&&!p.toString().contains("live-revisions")&&!p.toString().contains("game-revisions-")).limit(256).toList()){
                    try{var map=DocumentStore.load(path);if(found.putIfAbsent(map.id(),path)!=null)warnings.add("ID duplicado: "+map.id());}
                    catch(Exception invalid){warnings.add(path.getFileName()+": "+invalid.getMessage());}
                }}return found;
            }
            protected void done(){try{files.clear();files.putAll(get());var ids=new TreeSet<>(ui.maps().keySet());ids.addAll(files.keySet());model.clear();ids.forEach(model::addElement);if(!warnings.isEmpty())ui.details.setText(String.join("\n",warnings));}catch(Exception error){ui.error(error);}}
        }.execute();
    }
    private void open(){
        String id=list.getSelectedValue();if(id==null)throw new IllegalArgumentException("Selecione um mapa");
        ui.fileTools.confirm(()->{
            ui.knownMaps.put(ui.document.id(),ui.document);
            var next=ui.knownMaps.get(id);
            if(next==null){ui.fileTools.load(files.get(id),false);return;}
            ui.openDocument(next,files.get(id));
        });
    }
}
