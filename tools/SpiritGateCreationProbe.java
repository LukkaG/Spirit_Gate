package com.projetok.prototype.rpg;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Captura o diálogo real sem acessar os saves pessoais. */
public final class SpiritGateCreationProbe {
    /** Executa captura, validação, cancelamento e criação pelo diálogo modal real. */
    public static void main(String[] args) throws Exception {
        try { run(args); } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
        System.exit(0);
    }
    private static void run(String[] args) throws Exception {
        Path output = Path.of(args[0]); Files.createDirectories(output);
        var accepted = new java.util.concurrent.atomic.AtomicReference<com.projetok.rpg.CharacterIdentity>();
        SwingUtilities.invokeLater(() -> {
            JFrame owner = new JFrame("Probe de criação"); owner.setSize(1000,620); owner.setVisible(true);
            CharacterCreation.open(owner,null,accepted::set);
            owner.dispose();
        });
        JDialog dialog = null;
        for(int i=0;i<400&&dialog==null;i++) {
            Thread.sleep(50);
            for(Window window:Window.getWindows()) if(window instanceof JDialog d&&d.isShowing())dialog=d;
        }
        if(dialog==null)throw new AssertionError("Diálogo não abriu");
        final JDialog target=dialog;
        SwingUtilities.invokeAndWait(() -> {
            var form=(CharacterCreation)target.getContentPane();
            form.name.setText("!");form.create.doClick();
            if(form.error.getText().isBlank()||accepted.get()!=null)throw new AssertionError("Validação");
            form.mystic.doClick();form.appearance.setSelectedItem("amber");
        });
        for(var size:new int[][]{{820,540},{800,600},{640,480}}) {
            SwingUtilities.invokeAndWait(() -> {
                target.setSize(size[0],size[1]);target.validate();
                var image=new java.awt.image.BufferedImage(size[0],size[1],2);
                var g=image.createGraphics();target.printAll(g);g.dispose();
                try{ImageIO.write(image,"png",output.resolve("criacao-"+size[0]+"x"+size[1]+".png").toFile());}catch(Exception error){throw new RuntimeException(error);}
            });
        }
        SwingUtilities.invokeAndWait(() -> {
            var form=(CharacterCreation)target.getContentPane();form.name.setText("Heroi sintético");form.create.doClick();
        });
        if(accepted.get()==null||!accepted.get().archetype().equals("mystic"))throw new AssertionError("Criação");
        Files.writeString(output.resolve("probe.txt"),"PASS: diálogo modal real, nome inválido preserva formulário, Mystic/amber, fallback e criação válida. Capturas printAll; não homologam monitor físico. Nenhum save escrito.\n");
        SwingUtilities.invokeAndWait(() -> {for(Window window:Window.getWindows())window.dispose();});
    }
}
