import com.projetok.multiplayer.*;
import com.projetok.prototype.rpg.*;
import com.projetok.studio.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Abre o JAR distribuído com os assets dos dois pacotes e captura os painéis reais. */
public final class CorujaoEightUiProbe {
    static Component find(Container root,String name){if(name.equals(root.getName()))return root;for(var c:root.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container nested){var found=find(nested,name);if(found!=null)return found;}}return null;}
    static void capture(JFrame frame,Path path)throws Exception{SwingUtilities.invokeAndWait(()->{try{var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{frame.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",path.toFile());}catch(Exception e){throw new RuntimeException(e);}});}
    public static void main(String[] args)throws Exception{
        var host=Path.of(args[0]);var guest=Path.of(args[1]);var evidence=Path.of(args[2]);Files.createDirectories(evidence);
        if(!NetworkInventoryPanel.class.getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"))throw new AssertionError("Produção deve vir do JAR");
        System.setProperty("spiritgate.rpg.dataDir",evidence.resolve("isolated-data").toString());
        var catalog=CosmeticCatalog.load(host.resolve("assets/dev/generated/corujao-iv-v1"));var frames=new JFrame[2];
        try(var server=LauncherSession.startExpanded(InetAddress.getLoopbackAddress(),0,catalog,host);
            var a=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Pacote Reaper","reaper","none","none"),false,false,null,AssetCatalog.scan(host,null));
            var b=LocalClient.gameplay(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Pacote Human","human","none","none"),false,false,null,AssetCatalog.scan(guest,null))){
            var clients=new LocalClient[]{a,b};var roots=new Path[]{host,guest};
            for(int i=0;i<2;i++){final int index=i;var sprites=RpgPlayerSprites.load(roots[i].resolve("assets/dev/player-base"),250,.5);var visuals=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(roots[i].resolve("assets/dev/reaper"),sprites),catalog.onLogicalCanvas(64,64,32,60,.4));
                SwingUtilities.invokeAndWait(()->frames[index]=RpgNetworkClientLauncher.open(clients[index],sprites,"TCP do pacote privado",catalog,CosmeticCatalog.Selection.none(),f->{},visuals));}
            long end=System.nanoTime()+30_000_000_000L;
            while(System.nanoTime()<end&&(a.playerSheet()==null||b.playerSheet()==null||a.appearances().size()!=2||b.appearances().size()!=2||a.revisions().blocked()||b.revisions().blocked())){if(server.failure()!=null)throw new AssertionError(server.failure());Thread.sleep(30);}
            if(a.playerSheet()==null||b.playerSheet()==null||a.appearances().size()!=2||b.appearances().size()!=2||a.revisions().blocked()||b.revisions().blocked())throw new AssertionError("Sincronização incompleta");
            Thread.sleep(200);
            for(int i=0;i<2;i++){capture(frames[i],evidence.resolve("package-client-"+i+".png"));if(frames[i].getOwnedWindows().length!=0)throw new AssertionError("Janela nativa secundária");}
            if(args.length>3&&args[3].equals("target")){
                if(find(frames[0],"hudTarget")==null||!a.cycleTarget()||a.selectedTarget()==0)throw new AssertionError("Seleção do alvo no pacote");
                Thread.sleep(100);capture(frames[0],evidence.resolve("package-target.png"));
                var before=a.snapshot().actors().stream().filter(actor->actor.id()==a.id()).findFirst().orElseThrow();a.send(Protocol.Action.ATTACK,0,0);Thread.sleep(150);
                var after=a.snapshot().actors().stream().filter(actor->actor.id()==a.id()).findFirst().orElseThrow();if(Math.abs(before.x()-after.x())>.01||Math.abs(before.y()-after.y())>.01)throw new AssertionError("Ataque alvo deslocou jogador");
                Files.writeString(evidence.resolve("target-result.json"),"{\"targetPanel\":true,\"selection\":true,\"targetedBasicNoWalking\":true,\"productionFromJar\":true}");
                a.selectTarget(0);
            }
            for(String id:new String[]{"Inventory","Journal","Settings"}){
                SwingUtilities.invokeAndWait(()->((JButton)find(frames[0],"hud"+id)).doClick(0));Thread.sleep(150);
                if(find(frames[0],"panel-"+switch(id){case "Inventory"->"inventory";case "Journal"->"journal";default->"settings";})==null)throw new AssertionError(id);
                capture(frames[0],evidence.resolve("package-"+id.toLowerCase()+".png"));SwingUtilities.invokeAndWait(()->((GamePanels)frames[0].getRootPane().getClientProperty("networkPanels")).escape());
            }
            if(args.length>3&&(args[3].equals("compact")||args[3].equals("attributes")||args[3].equals("energy"))){
                if(args[3].equals("energy")){
                    if(com.projetok.domain.combat.EnergyRules.maximum(2)!=1040||Math.abs(com.projetok.domain.combat.EnergyRules.regenerationPerSecond(2)-15.3)>.0001)throw new AssertionError("Energy do JAR");
                    a.requestEnergy();long energyDeadline=System.nanoTime()+3_000_000_000L;while(a.energy()==null&&System.nanoTime()<energyDeadline)Thread.sleep(20);if(a.energy()==null||a.energy().maximum()!=1000)throw new AssertionError("Energy privado do pacote");
                    Files.writeString(evidence.resolve("energy-result.json"),"{\"maximumAtTwo\":1040,\"regenPerSecondAtTwo\":15.3,\"privateEnergyFrame\":true,\"productionFromJar\":true}");
                }
                if(args[3].equals("attributes")){
                    if(com.projetok.domain.combat.AttributeCombatRules.basicSpeedMultiplier(100)!=2||com.projetok.domain.combat.AttributeCombatRules.basicDodgeChance(100)!=.25||com.projetok.domain.combat.AttributeCombatRules.damageBonus(com.projetok.domain.combat.AttributeCombatRules.DamageKind.DOMAIN_SKILL,0,10)!=5)throw new AssertionError("Fórmulas de atributos do JAR");
                    Files.writeString(evidence.resolve("attribute-rules-result.json"),"{\"speedCap\":2,\"dodgeCap\":0.25,\"intDomainBonus\":5,\"productionFromJar\":true}");
                }
                a.requestAttributes();long attributesDeadline=System.nanoTime()+3_000_000_000L;while(a.attributes()==null&&System.nanoTime()<attributesDeadline)Thread.sleep(20);if(a.attributes()==null)throw new AssertionError("Atributos privados do pacote");
                for(String id:new String[]{"Profile","Skills"}){SwingUtilities.invokeAndWait(()->((JButton)find(frames[0],"hud"+id)).doClick(0));Thread.sleep(100);capture(frames[0],evidence.resolve("package-"+id.toLowerCase()+".png"));SwingUtilities.invokeAndWait(()->((GamePanels)frames[0].getRootPane().getClientProperty("networkPanels")).escape());}
                SwingUtilities.invokeAndWait(()->((JButton)find(frames[0],"hudInventory")).doClick(0));Thread.sleep(80);
                SwingUtilities.invokeAndWait(()->{var list=(JList<?>)find(frames[0],"inventoryGrid");list.dispatchEvent(new java.awt.event.MouseEvent(list,java.awt.event.MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,20,20,1,false));});
                if(!find(frames[0],"inventoryItemPopup").isVisible())throw new AssertionError("Detalhes do item no pacote");capture(frames[0],evidence.resolve("package-item-popup.png"));
                Files.writeString(evidence.resolve("compact-result.json"),"{\"privateAttributes\":true,\"separateSkills\":true,\"itemPopover\":true,\"productionFromJar\":true}");
            }
            if(server.failure()!=null)throw new AssertionError(server.failure());
            Files.writeString(evidence.resolve("result.json"),"{\"productionFromJar\":true,\"twoClients\":true,\"integratedPanels\":true,\"nativeSecondaryWindows\":0,\"physicalKeyboardTested\":false,\"nativeExeTested\":false}");
            System.out.println("PACKAGED_GUI_PASS");
        }finally{SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});}
    }
}
