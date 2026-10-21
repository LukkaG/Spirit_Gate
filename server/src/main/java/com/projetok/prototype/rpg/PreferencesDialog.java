package com.projetok.prototype.rpg;
import com.projetok.rpg.RpgPreferences;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
/** Opções reais, sem alterar o formato existente de preferências. */
final class PreferencesDialog {
    static void open(JFrame owner,RpgPreferences current,Path file,java.util.function.Consumer<RpgPreferences> apply){
        JDialog dialog=new JDialog(owner,"Opções do Spirit Gate",true);dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JSlider volume=new JSlider(0,100,current.volume());volume.setPaintTicks(true);volume.setMajorTickSpacing(25);volume.setPaintLabels(true);volume.getAccessibleContext().setAccessibleName("Volume geral");
        JCheckBox muted=new JCheckBox("Silenciar áudio",current.muted()),reduced=new JCheckBox("Reduzir iluminação e animações decorativas",!current.environment());
        java.util.LinkedHashSet<String> sizes=new java.util.LinkedHashSet<>();sizes.add(owner.getWidth()+"x"+owner.getHeight());
        var screen=Toolkit.getDefaultToolkit().getScreenSize();for(var size:new int[][]{{1260,900},{1000,620},{800,600},{640,480}})if(size[0]<=screen.width-40&&size[1]<=screen.height-60)sizes.add(size[0]+"x"+size[1]);
        JComboBox<String> resolution=new JComboBox<>(sizes.toArray(String[]::new));resolution.getAccessibleContext().setAccessibleName("Resolução nesta sessão");
        JPanel form=new JPanel(new GridLayout(0,1,4,8));form.add(new JLabel("Áudio — volume geral"));form.add(volume);form.add(muted);form.add(new JLabel("Acessibilidade e iluminação"));form.add(reduced);form.add(new JLabel("Resolução da janela (nesta sessão)"));form.add(resolution);
        JButton defaults=new CharacterSelection.SpiritButton("Restaurar padrões",false);defaults.addActionListener(e->{var value=RpgPreferences.defaults();volume.setValue(value.volume());muted.setSelected(value.muted());reduced.setSelected(!value.environment());});form.add(defaults);
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));root.add(new JScrollPane(form));
        JLabel status=new JLabel("Minimapa e atalhos: barra do jogo.");JButton save=new CharacterSelection.SpiritButton("Aplicar",true),cancel=new CharacterSelection.SpiritButton("Cancelar",false);
        JPanel footer=new JPanel(new BorderLayout());JPanel actions=new JPanel(new FlowLayout());actions.add(save);actions.add(cancel);footer.add(actions);footer.add(status,BorderLayout.SOUTH);root.add(footer,BorderLayout.SOUTH);
        cancel.addActionListener(e->dialog.dispose());save.addActionListener(e->{
            var value=new RpgPreferences(volume.getValue(),muted.isSelected(),!reduced.isSelected());String[] size=((String)resolution.getSelectedItem()).split("x");int width=Integer.parseInt(size[0]),height=Integer.parseInt(size[1]);
            save.setEnabled(false);cancel.setEnabled(false);
            new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{value.save(file);return null;}protected void done(){try{get();apply.accept(value);owner.setSize(width,height);dialog.dispose();}catch(Exception error){status.setText("Falha ao salvar preferências");save.setEnabled(true);cancel.setEnabled(true);}}}.execute();
        });
        SpiritTheme.dialog(dialog,root,save);dialog.setSize(560,500);dialog.setLocationRelativeTo(owner);dialog.setVisible(true);
    }
}
