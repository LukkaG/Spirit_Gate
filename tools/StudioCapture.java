package com.projetok.studio;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Captura janelas Swing reais em EDT, sem abrir nem gravar saves de personagem. */
public final class StudioCapture {
    /** Argumentos: diretório de saída novo, raiz do projeto, prefixo da captura. */
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]); Files.createDirectories(output);
        SwingUtilities.invokeAndWait(()->{
            JFrame ui=args[2].startsWith("quest")?new QuestBuilder(Path.of(args[1])):new WorldStudio(Path.of(args[1]));
            try {
                ui.setVisible(true);
                for(int[] size:new int[][]{{1400,900},{1000,700}}) {
                    ui.setSize(size[0],size[1]); ui.validate();
                    var image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);
                    var g=image.createGraphics(); ui.printAll(g);g.dispose();
                    try { ImageIO.write(image,"png",output.resolve(args[2]+"-"+size[0]+".png").toFile()); }
                    catch(Exception e){throw new IllegalStateException(e);}
                }
            } finally {ui.dispose();}
        });
        System.exit(0);
    }
}
