package com.projetok.studio;

import com.projetok.rpg.*;
import com.projetok.prototype.rpg.RpgRuntime;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Demonstra autoria visual, exportação e conclusão no runtime sem saves pessoais. */
public final class QuestDemoProbe {
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args[0]);Files.createDirectories(output);QuestBook[] book={null};
        SwingUtilities.invokeAndWait(()->{var builder=new QuestBuilder(output);try{builder.name.setText("Visita à Aurora");builder.race.setSelectedItem("reaper");book[0]=builder.book();}finally{builder.dispose();}});
        QuestDocuments.save(output.resolve("demo.sgquest"),book[0]);var reloaded=QuestDocuments.load(output.resolve("demo.sgquest"));var world=new RpgWorld(CharacterIdentity.create("Corujao","reaper","azure"),null,reloaded);JFrame[] frame={null};
        SwingUtilities.invokeAndWait(()->frame[0]=QuestPlaytest.open(world,reloaded));
        RpgRuntime runtime=(RpgRuntime)frame[0].getRootPane().getClientProperty("syntheticRuntime");String id=reloaded.quests().get(0).id();
        try {
            boolean accepted=runtime.action(w->w.acceptAuthoredQuest(id)).get(5,TimeUnit.SECONDS),completed=runtime.action(w->w.completeAuthoredQuest(id)).get(5,TimeUnit.SECONDS),duplicate=runtime.action(w->w.completeAuthoredQuest(id)).get(5,TimeUnit.SECONDS);
            if(!accepted||!completed||duplicate)throw new IllegalStateException("Demonstração não cumpriu ciclo único");
            Files.writeString(output.resolve("result.json"),"{\"accepted\":"+accepted+",\"completed\":"+completed+",\"duplicateReward\":"+duplicate+",\"personalSaveUsed\":false}");
            SwingUtilities.invokeAndWait(()->{var ui=frame[0];var image=new BufferedImage(ui.getWidth(),ui.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();ui.printAll(g);g.dispose();try{ImageIO.write(image,"png",output.resolve("runtime.png").toFile());}catch(Exception e){throw new IllegalStateException(e);}});
        }finally{SwingUtilities.invokeAndWait(()->frame[0].dispose());}
        System.exit(0);
    }
}
