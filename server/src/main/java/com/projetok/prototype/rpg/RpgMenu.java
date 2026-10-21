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
        CharacterSelection root=new CharacterSelection();
        var list=root.list;var status=root.status;
        CharacterSaves repository=new CharacterSaves(dataHome().resolve("saves/characters"));
        JButton create=root.create,resume=root.play;
        root.arena.addActionListener(e->com.projetok.prototype.DebugLauncher.main(new String[0]));
        root.back.addActionListener(e->{frame.dispose();SpiritGateEntry.open();});
        root.delete.addActionListener(e->{
            var slot=list.getSelectedValue();if(slot==null||slot.backup())return;
            String confirmation=JOptionPane.showInputDialog(frame,"Arquivar este save? O arquivo ser\u00e1 movido para archived; backups .bak ser\u00e3o mantidos.\nDigite ARQUIVAR para confirmar.","Confirmar arquivamento",JOptionPane.WARNING_MESSAGE);
            if(!"ARQUIVAR".equals(confirmation))return;
            root.delete.setEnabled(false);resume.setEnabled(false);
            new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{repository.archive(slot,confirmation);return null;}protected void done(){try{get();frame.dispose();open();}catch(Exception error){root.delete.setEnabled(true);resume.setEnabled(slot.state()!=null);JOptionPane.showMessageDialog(frame,"Falha ao arquivar: "+error.getMessage());}}}.execute();
        });
        create.addActionListener(e->{
            CharacterCreation.open(frame,root.preview.sprites,identity->launch(frame,new RpgWorld(identity),new LocalSaveStore(repository.newFile(identity)),true,true));
        });
        resume.addActionListener(e->{var slot=list.getSelectedValue();if(slot==null||slot.state()==null)return;Path file=slot.backup()?repository.recoveryFile(slot.state().identity()):slot.file();launch(frame,RpgWorld.restore(slot.state()),new LocalSaveStore(file),slot.backup());});
        new SwingWorker<java.util.List<CharacterSaves.Slot>,Void>() {
            protected java.util.List<CharacterSaves.Slot> doInBackground() throws Exception{root.loadBackdrop();root.preview.sprites=RpgPlayerSprites.loadOptional(RpgPlayerSprites.defaultFolder(),250);return repository.list();}
            protected void done(){try{root.loaded(get());}catch(Exception error){create.setEnabled(true);status.setText("N\u00e3o foi poss\u00edvel ler saves: "+error.getMessage());}}

        }.execute();
        frame.setContentPane(root);frame.getRootPane().setDefaultButton(resume);frame.setMinimumSize(new Dimension(640,480));var screen=Toolkit.getDefaultToolkit().getScreenSize();frame.setSize(Math.min(1000,screen.width-40),Math.min(620,screen.height-60));frame.setLocationRelativeTo(null);frame.setVisible(true);
    }
    private static void launch(JFrame menu,RpgWorld world,LocalSaveStore store,boolean initialSave) {
        launch(menu,world,store,initialSave,false);
    }
    private static void launch(JFrame menu,RpgWorld world,LocalSaveStore store,boolean initialSave,boolean introduction) {
        menu.setEnabled(false);
        new SwingWorker<Void,Void>() {
            protected Void doInBackground() throws Exception{if(initialSave)store.save(world.save());return null;}
            protected void done(){try{get();menu.dispose();RpgLauncher.play(world,store,introduction);}catch(Exception error){menu.setEnabled(true);JOptionPane.showMessageDialog(menu,"Falha ao abrir personagem: "+error.getMessage());}}
        }.execute();
    }
}
