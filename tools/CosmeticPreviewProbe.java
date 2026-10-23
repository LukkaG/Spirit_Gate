package com.projetok.studio;

import com.projetok.prototype.rpg.*;
import com.projetok.prototype.DirectionalAnimation.Direction;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Captura combinações Idle/Walk e janela de seleção, sem gravar perfis pessoais. */
public final class CosmeticPreviewProbe {
    public static void main(String[] args)throws Exception {
        Path project=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);var catalog=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));var body=RpgPlayerSprites.load(project.resolve("assets/dev/player-base"),250,.5);
        var image=new BufferedImage(900,880,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(24,30,40));g.fillRect(0,0,900,880);int row=0;
        for(boolean walking:new boolean[]{false,true})for(var direction:Direction.values()){int column=0;for(var hair:catalog.all("hair"))for(var clothing:catalog.all("clothing")){int x=column++*100,y=row*110;g.setColor(Color.LIGHT_GRAY);g.setFont(new Font(Font.DIALOG,Font.PLAIN,9));g.drawString(direction+" "+(walking?"Walk":"Idle"),x+2,y+12);CosmeticRenderer.draw(g,body,catalog,new CosmeticCatalog.Selection(hair.id(),clothing.id()),direction,1,walking,x+50,y+104);}row++;}g.dispose();ImageIO.write(image,"png",out.resolve("combinations.png").toFile());
        SwingUtilities.invokeAndWait(()->{var frame=AppearancePreview.open(project,body,catalog);try{int found=0;for(Component top:frame.getContentPane().getComponents())if(top instanceof JPanel panel)for(Component c:panel.getComponents())if(c instanceof JComboBox<?> combo&&combo.getItemCount()>0&&"none".equals(combo.getItemAt(0))){combo.setSelectedIndex(++found);}
            frame.validate();var shot=new BufferedImage(frame.getWidth(),frame.getHeight(),2);var graphics=shot.createGraphics();frame.printAll(graphics);graphics.dispose();try{ImageIO.write(shot,"png",out.resolve("appearance-window.png").toFile());}catch(Exception e){throw new IllegalStateException(e);}
        }finally{frame.dispose();}});System.exit(0);
    }
}
