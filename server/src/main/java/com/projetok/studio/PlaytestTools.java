package com.projetok.studio;
import javax.swing.*;
import java.nio.file.*;
import java.util.*;

/** Exportacao e playtest como operacoes reais em worker. */
final class PlaytestTools {
    final WorldStudio ui;
    PlaytestTools(WorldStudio ui){this.ui=ui;ui.action("Arquivo","Exportar para RPG",()->export(false));ui.action("Mapa","Testar mapa no RPG",()->export(true));}
    void export(boolean play){JFileChooser chooser=new JFileChooser(ui.project.resolve("studio-projects").toFile());chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(!play&&chooser.showSaveDialog(ui)!=JFileChooser.APPROVE_OPTION)return;Path base=play?ui.project.resolve("studio-projects/playtests"):chooser.getSelectedFile().toPath();var maps=ui.maps();var doc=ui.document;var catalog=ui.catalog;ui.status.setText("Validando e exportando... (fora da interface)");new SwingWorker<Path,Void>(){protected Path doInBackground()throws Exception{Files.createDirectories(base);Path path=StudioExport.write(base.resolve(doc.id()+"-"+UUID.randomUUID()),doc.id(),maps,catalog);if(play)StudioPlaytest.launch(path,ui.project);return path;}protected void done(){try{ui.status.setText((play?"Playtest isolado: ":"Exportado: ")+get());}catch(Exception error){ui.error(error);}}}.execute();}
}
