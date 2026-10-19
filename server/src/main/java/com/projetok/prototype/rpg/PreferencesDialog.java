package com.projetok.prototype.rpg;
import com.projetok.rpg.RpgPreferences;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
final class PreferencesDialog {
    static void open(JFrame owner,RpgPreferences current,Path file,java.util.function.Consumer<RpgPreferences> apply){
        JSlider volume=new JSlider(0,100,current.volume());volume.setPaintTicks(true);volume.setMajorTickSpacing(25);volume.setPaintLabels(true);JCheckBox muted=new JCheckBox("Mudo",current.muted()),environment=new JCheckBox("Ciclo de iluminacao",current.environment());JPanel form=new JPanel(new GridLayout(0,1));form.add(new JLabel("Volume dos tons sintetizados"));form.add(volume);form.add(muted);form.add(environment);
        if(JOptionPane.showConfirmDialog(owner,form,"Audio e ambiente",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        var value=new RpgPreferences(volume.getValue(),muted.isSelected(),environment.isSelected());new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{value.save(file);return null;}protected void done(){try{get();apply.accept(value);}catch(Exception error){JOptionPane.showMessageDialog(owner,"Falha nas preferencias: "+error.getMessage());}}}.execute();
    }
}
