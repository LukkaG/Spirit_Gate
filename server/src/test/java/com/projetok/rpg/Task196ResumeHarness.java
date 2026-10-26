package com.projetok.rpg;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Harness sintético de dois processos. Nunca chamado pelo launcher. */
public final class Task196ResumeHarness {
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static String contentFingerprint()throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        digest.update("TASK196:builtin-village:960x640:radius18:obstacle380,180,120,120:v1".getBytes(StandardCharsets.UTF_8));
        for(String name:List.of("content","items","quests","loot","enemies","spawns","npcs","objects","portals","progression","dialogues")){
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            try(var in=RpgWorld.class.getResourceAsStream("/rpg/"+name+".properties")){
                if(in==null)throw new IOException("Catalogo ausente: "+name);digest.update(in.readAllBytes());
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    static RpgWorld restoreConfirmed(RpgSave state,String fingerprint)throws Exception {
        if(!contentFingerprint().equals(fingerprint))throw new IOException("Conteudo confirmado incompativel com harness");
        if(!state.region().equals("village"))throw new IOException("Regiao fora do contrato deste harness");
        return RpgWorld.restore(state);
    }
    static RpgSave positioned(RpgSave s,float x,float y){return new RpgSave(s.identity(),s.region(),x,y,s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),s.quests(),s.flags(),s.ticks());}
    static RpgSave rewardedFixture(RpgSave seed)throws Exception {
        var world=RpgWorld.restore(seed);world.interact();require(world.acceptQuest("first_steps"),"Aceitar quest existente");
        var s=world.save();var progress=new TreeMap<>(s.quests());progress.put("first_steps",2);
        var flags=new TreeSet<>(s.flags());flags.add("qo:first_steps:0:2");
        // Fixture explícita de objetivo pronto; recompensa usa o motor real, sem simular kills inexistentes.
        world=RpgWorld.restore(new RpgSave(s.identity(),s.region(),s.x(),s.y(),s.hp(),s.xp(),s.coins(),s.inventory(),s.equipment(),progress,flags,s.ticks()));
        world.interact();require(world.completeQuest("first_steps"),"Aplicar recompensa real");
        world=RpgWorld.restore(positioned(world.save(),330,440));require(world.harvest("starter_chest"),"Coletar objeto real uma vez");
        return world.save();
    }
    static void assertNoReplay(RpgSave snapshot)throws Exception {
        var chest=RpgWorld.restore(snapshot);require(!chest.harvest("starter_chest"),"Objeto persistido nao duplica loot");
        require(chest.save().equals(snapshot),"Replay de loot preserva snapshot inteiro");
        var quest=RpgWorld.restore(positioned(snapshot,140,320));quest.interact();var before=quest.save();
        require(!quest.completeQuest("first_steps"),"Quest concluida nao repete recompensa");require(before.equals(quest.save()),"Replay de quest preserva recursos");
        require(snapshot.xp()==80&&snapshot.coins()==45&&chest.inventory().count("potion")==5&&chest.inventory().count("sword")==1,"XP moedas e itens exatos");
    }
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[1]);String fingerprint=contentFingerprint();
        if(args[0].equals("seed")){
            try(var lab=new SyntheticSessionLaboratory(root)){
                var handle=lab.create("spirit");var first=lab.snapshot(handle);var rewarded=rewardedFixture(first.state());
                var confirmed=lab.save(handle,1,rewarded);require(confirmed.revision()==2,"Commit revision 2");assertNoReplay(rewarded);
                Files.writeString(root.resolve("harness-id.txt"),first.id().toString(),StandardOpenOption.CREATE_NEW);
                Files.writeString(root.resolve("harness-content.sha256"),fingerprint,StandardOpenOption.CREATE_NEW);
                lab.disconnect(handle);
            }
        }else if(args[0].equals("resume")){
            UUID id=UUID.fromString(Files.readString(root.resolve("harness-id.txt")));
            try(var lab=new SyntheticSessionLaboratory(root)){
                var handle=lab.resume(id);var confirmed=lab.snapshot(handle);
                var restored=restoreConfirmed(confirmed.state(),Files.readString(root.resolve("harness-content.sha256")));
                require(restored.save().equals(confirmed.state()),"Snapshot exato apos restart");assertNoReplay(restored.save());
                byte[] primary=Files.readAllBytes(root.resolve(id+".online"));
                for(int i=0;i<20;i++)require(lab.save(handle,1,confirmed.state()).equals(confirmed),"Retry sem novo commit");
                require(Arrays.equals(primary,Files.readAllBytes(root.resolve(id+".online"))),"Bytes preservados apos retry");
                require(confirmed.revision()==2,"Revisao continua 2");lab.disconnect(handle);
            }
        }else throw new IllegalArgumentException("Modo desconhecido");
        System.out.println("TASK196 phase="+args[0]+" contentSha256="+fingerprint+" revision=2 xp=80 coins=45 potions=5 swords=1 questRewardReplay=false chestReplay=false launcherIntegration=false");
    }
}
