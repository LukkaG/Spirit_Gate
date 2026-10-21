package com.projetok.prototype.rpg;
import com.projetok.rpg.RpgControls;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
final class ControlsDialog {
    static void open(JFrame owner,RpgControls current,Path file,java.util.function.Consumer<RpgControls> apply){
        JPanel form=new JPanel(new GridLayout(0,2,6,6));Map<String,JTextField> fields=new TreeMap<>();
        for(String action:new TreeSet<>(current.keys().keySet())){form.add(new JLabel(RpgLauncher.actionLabel(action)));JTextField text=new JTextField(current.key(action),8);fields.put(action,text);form.add(text);}
        JButton defaults=new CharacterSelection.SpiritButton("Restaurar padrões",false);defaults.addActionListener(e->fields.forEach((id,field)->field.setText(RpgControls.DEFAULTS.get(id))));form.add(defaults);form.add(new JLabel("WASD reservado"));SpiritTheme.apply(form);
        if(JOptionPane.showConfirmDialog(owner,new JScrollPane(form),"Atalhos RPG (WASD reservado)",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        try{Map<String,String> map=new TreeMap<>();fields.forEach((id,field)->map.put(id,field.getText().strip().toUpperCase(Locale.ROOT)));RpgControls controls=new RpgControls(map);
            new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{controls.save(file);return null;}protected void done(){try{get();apply.accept(controls);}catch(Exception error){JOptionPane.showMessageDialog(owner,"Falha ao salvar atalhos: "+error.getMessage());}}}.execute();
        }catch(IllegalArgumentException error){JOptionPane.showMessageDialog(owner,error.getMessage());}
    }
}
