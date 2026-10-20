package com.projetok.studio;
import javax.swing.*;
import java.util.*;
final class EnvironmentTools {
    EnvironmentTools(WorldStudio ui){ui.action("Ambiente","Iluminacao e clima (previa)",()->{var mode=(EnvironmentPreview.Mode)JOptionPane.showInputDialog(ui,"Somente apresentacao; OFF desativa","Ambiente",JOptionPane.PLAIN_MESSAGE,null,EnvironmentPreview.Mode.values(),EnvironmentPreview.mode(ui.document));if(mode!=null){var m=new HashMap<>(ui.document.metadata());m.put("preview.environment",mode.name());ui.change(ui.document.withMetadata(m));}});}
}
