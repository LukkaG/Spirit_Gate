package com.projetok.studio;

import com.projetok.prototype.rpg.*;
import com.projetok.prototype.DirectionalAnimation.Direction;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import javax.swing.*;

/** Preview de aparência em perfil sintético separado; não modifica personagem ou combate. */
public final class AppearancePreview {
    private AppearancePreview() { }
    public static void launch(Path project){new SwingWorker<Object[],Void>(){protected Object[] doInBackground()throws Exception{return new Object[]{RpgPlayerSprites.load(project.resolve("assets/dev/player-base"),250,.5),CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"))};}protected void done(){try{var loaded=get();open(project,(RpgPlayerSprites)loaded[0],(CosmeticCatalog)loaded[1]);}catch(Exception e){JOptionPane.showMessageDialog(null,"Aparência indisponível: "+e.getMessage());}}}.execute();}
    static JFrame open(Path project,RpgPlayerSprites base,CosmeticCatalog catalog){
        JFrame frame=new JFrame("Spirit Gate | APARÊNCIA TEMPORÁRIA | perfil sintético");frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);frame.setSize(700,600);var hair=new JComboBox<String>();var clothing=new JComboBox<String>();hair.addItem("none");clothing.addItem("none");catalog.all("hair").forEach(d->hair.addItem(d.id()));catalog.all("clothing").forEach(d->clothing.addItem(d.id()));var direction=new JComboBox<>(Direction.values());var walking=new JCheckBox("Walk — 250 ms");long started=System.nanoTime();
        JPanel preview=new JPanel(){protected void paintComponent(Graphics original){super.paintComponent(original);var g=(Graphics2D)original.create();try{g.scale(3,3);var selection=new CosmeticCatalog.Selection((String)hair.getSelectedItem(),(String)clothing.getSelectedItem());int index=(int)((System.nanoTime()-started)/250_000_000%4);CosmeticRenderer.draw(g,base,catalog,selection,(Direction)direction.getSelectedItem(),index,walking.isSelected(),getWidth()/6f,getHeight()/3f-10);}finally{g.dispose();}}};
        JPanel controls=new JPanel();controls.add(hair);controls.add(clothing);controls.add(direction);controls.add(walking);JButton save=new JButton("Salvar perfil sintético"),load=new JButton("Abrir perfil sintético");controls.add(save);controls.add(load);JLabel status=new JLabel("TEMPORÁRIO | preview ampliado 3x | sem atributos | "+catalog.warnings().size()+" avisos");Path file=project.resolve("studio-projects/appearance-preview.properties");
        save.addActionListener(e->{var profile=new CosmeticProfile("preview",new CosmeticCatalog.Selection((String)hair.getSelectedItem(),(String)clothing.getSelectedItem()));new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{profile.save(file,catalog);return null;}protected void done(){try{get();status.setText("Perfil sintético salvo; personagem V2 preservado.");}catch(Exception error){status.setText("Falha ao salvar: "+error.getMessage());}}}.execute();});
        load.addActionListener(e->new SwingWorker<CosmeticProfile,Void>(){protected CosmeticProfile doInBackground()throws Exception{return CosmeticProfile.load(file,catalog);}protected void done(){try{var selected=get().selection();hair.setSelectedItem(selected.hair());clothing.setSelectedItem(selected.clothing());status.setText("Perfil cosmético aberto.");}catch(Exception error){status.setText("Falha ao abrir: "+error.getMessage());}}}.execute());
        frame.add(controls,BorderLayout.NORTH);frame.add(preview);frame.add(status,BorderLayout.SOUTH);StudioTheme.apply(frame.getContentPane());var timer=new javax.swing.Timer(50,e->preview.repaint());frame.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();}});timer.start();frame.setVisible(true);return frame;
    }
}
