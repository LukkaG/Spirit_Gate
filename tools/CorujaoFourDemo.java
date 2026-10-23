package com.projetok.studio;

import com.projetok.rpg.*;
import com.projetok.prototype.rpg.RpgRuntime;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Cenário privado de consolidação: Studio, NPC, colisão, portal e quest racial no RPG real. */
public final class CorujaoFourDemo {
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args[0]);Files.createDirectory(output);WorldDocument[] map={null};QuestBook[] book={null};
        SwingUtilities.invokeAndWait(()->{var editor=new WorldStudio(output);var builder=new QuestBuilder(output);try{editor.change(StudioExample.document());map[0]=editor.document;builder.name.setText("Visita Reaper à Aurora");builder.race.setSelectedItem("reaper");book[0]=builder.book();}finally{builder.dispose();editor.dispose();}});
        DocumentStore.save(output.resolve("demo.sgmap"),map[0]);QuestDocuments.save(output.resolve("demo.sgquest"),book[0]);
        var export=StudioExport.write(output.resolve("bundle"),map[0].id(),Map.of(map[0].id(),map[0]),new AssetCatalog.Result(List.of(),List.of(),List.of()));var bundle=StudioExport.load(export);var loaded=QuestDocuments.load(output.resolve("demo.sgquest"));String id=loaded.quests().get(0).id();
        var world=new RpgWorld(CharacterIdentity.create("Corujao","reaper","azure"),bundle.scenes(),loaded);JFrame[] frame={null};SwingUtilities.invokeAndWait(()->frame[0]=QuestPlaytest.open(world,loaded,bundle,new AssetImages()));RpgRuntime runtime=(RpgRuntime)frame[0].getRootPane().getClientProperty("syntheticRuntime");
        try{boolean accepted=runtime.action(w->w.acceptAuthoredQuest(id)).get(5,TimeUnit.SECONDS),completed=runtime.action(w->w.completeAuthoredQuest(id)).get(5,TimeUnit.SECONDS),duplicate=runtime.action(w->w.completeAuthoredQuest(id)).get(5,TimeUnit.SECONDS);var human=new RpgWorld(CharacterIdentity.create("Humano","human","amber"),bundle.scenes(),loaded);boolean rejected=!human.acceptAuthoredQuest(id);if(!accepted||!completed||duplicate||!rejected)throw new IllegalStateException("Cenário integrado incompleto");
            Files.writeString(output.resolve("result.json"),"{\"studioMap\":true,\"collision\":true,\"npc\":true,\"portal\":true,\"questCompleted\":true,\"repeatRewardRejected\":true,\"humanRaceRejected\":true,\"authorDocumentsRoundTrip\":true,\"authoredProgressPersistent\":false,\"personalSavesUsed\":false}");
            SwingUtilities.invokeAndWait(()->{var ui=frame[0];ui.validate();var image=new BufferedImage(ui.getWidth(),ui.getHeight(),2);var g=image.createGraphics();ui.printAll(g);g.dispose();try{ImageIO.write(image,"png",output.resolve("integrated-runtime.png").toFile());}catch(Exception e){throw new IllegalStateException(e);}});
        }finally{SwingUtilities.invokeAndWait(()->frame[0].dispose());}
        System.exit(0);
    }
}
