package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
/** Menu local. IO em SwingWorker, sem login ou rede. */
final class RpgMenu {
    static Path home(){String exe=System.getProperty("jpackage.app-path");return exe==null?Path.of("").toAbsolutePath():Path.of(exe).toAbsolutePath().getParent();}
    /** Updater stores user data outside disposable application versions. */
    static Path dataHome(){String data=System.getProperty("spiritgate.rpg.dataDir");return data==null?home():Path.of(data).toAbsolutePath();}
    static void open() {
        JFrame frame=new JFrame("Spirit Gate | Inicio");frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(16,16));root.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        JLabel title=new JLabel("SPIRIT GATE - RPG OFFLINE");title.setFont(title.getFont().deriveFont(Font.BOLD,26));root.add(title,BorderLayout.NORTH);
        DefaultListModel<CharacterSaves.Slot> model=new DefaultListModel<>();JList<CharacterSaves.Slot> list=new JList<>(model);root.add(new JScrollPane(list),BorderLayout.CENTER);
        JLabel status=new JLabel("Lendo personagens locais...");JPanel south=new JPanel(new BorderLayout());south.add(status,BorderLayout.NORTH);JPanel buttons=new JPanel(new GridLayout(2,2,8,8));south.add(buttons,BorderLayout.CENTER);root.add(south,BorderLayout.SOUTH);
        CharacterSaves repository=new CharacterSaves(dataHome().resolve("saves/characters"));
        JButton create=new JButton("Criar personagem");JButton resume=new JButton("Continuar selecionado");resume.setEnabled(false);JButton arena=new JButton("Arena historica");JButton exit=new JButton("Sair");
        buttons.add(create);buttons.add(resume);buttons.add(arena);buttons.add(exit);
        list.addListSelectionListener(e->resume.setEnabled(list.getSelectedValue()!=null&&list.getSelectedValue().state()!=null));
        create.addActionListener(e->{
            JTextField name=new JTextField(20);JComboBox<String> archetype=new JComboBox<>(new String[]{"guardian","mystic"});JComboBox<String> appearance=new JComboBox<>(new String[]{"azure","amber"});
            JPanel form=new JPanel(new GridLayout(0,2,8,8));form.add(new JLabel("Nome (3-24 caracteres)"));form.add(name);form.add(new JLabel("Guardian: investida | Mystic: cura"));form.add(archetype);form.add(new JLabel("Aparencia geometrica"));form.add(appearance);
            if(JOptionPane.showConfirmDialog(frame,form,"Criar personagem local",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
            try{var identity=CharacterIdentity.create(name.getText(),(String)archetype.getSelectedItem(),(String)appearance.getSelectedItem());var world=new RpgWorld(identity);launch(frame,world,new LocalSaveStore(repository.newFile(identity)),true);}catch(IllegalArgumentException error){JOptionPane.showMessageDialog(frame,error.getMessage());}
        });
        resume.addActionListener(e->{var slot=list.getSelectedValue();if(slot==null||slot.state()==null)return;Path file=slot.backup()?repository.recoveryFile(slot.state().identity()):slot.file();launch(frame,RpgWorld.restore(slot.state()),new LocalSaveStore(file),slot.backup());});
        arena.addActionListener(e->com.projetok.prototype.DebugLauncher.main(new String[0]));exit.addActionListener(e->frame.dispose());
        new SwingWorker<java.util.List<CharacterSaves.Slot>,Void>() {
            protected java.util.List<CharacterSaves.Slot> doInBackground() throws Exception{return repository.list();}
            protected void done(){try{var slots=get();slots.forEach(model::addElement);status.setText(slots.stream().anyMatch(s->s.state()!=null)?"Selecione um personagem. Entradas invalidas sao preservadas.":"Nenhum save valido. Crie um personagem para comecar.");if(!slots.isEmpty())list.setSelectedIndex(0);}catch(Exception error){status.setText("Nao foi possivel ler saves: "+error.getMessage());}}
        }.execute();
        frame.setContentPane(root);frame.setSize(760,450);frame.setLocationRelativeTo(null);frame.setVisible(true);
    }
    private static void launch(JFrame menu,RpgWorld world,LocalSaveStore store,boolean initialSave) {
        menu.setEnabled(false);
        new SwingWorker<Void,Void>() {
            protected Void doInBackground() throws Exception{if(initialSave)store.save(world.save());return null;}
            protected void done(){try{get();menu.dispose();RpgLauncher.play(world,store);}catch(Exception error){menu.setEnabled(true);JOptionPane.showMessageDialog(menu,"Falha ao abrir personagem: "+error.getMessage());}}
        }.execute();
    }
}
