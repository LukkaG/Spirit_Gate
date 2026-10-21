package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
/** Verifica compra única e fechamento antes da confirmação com runtime real. */
public final class SpiritGateTradeProbe {
    /** Executa em memória e save temporário, sem diretórios pessoais. */
    public static void main(String[] args){try{run(args);System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
    static void run(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectories(output);SpiritGateWindowsProbe.output=output;
        var state=new RpgWorld().save();var world=RpgWorld.restore(new RpgSave(state.identity(),"village",230,400,state.hp(),0,100,state.inventory(),state.equipment(),state.quests(),state.flags(),0));world.interact();
        try(var runtime=new RpgRuntime(world)){
            JFrame[] owner={null};TradeWindow[] shop={null};
            SwingUtilities.invokeAndWait(()->{owner[0]=new JFrame("Comerciante sintético");owner[0].setSize(800,600);owner[0].setVisible(true);shop[0]=new TradeWindow(owner[0],runtime);shop[0].setVisible(true);SpiritGateWindowsProbe.find(shop[0],JList.class,null).setSelectedIndex(0);});
            SpiritGateWindowsProbe.capture(shop[0],"comercio-antes");
            SwingUtilities.invokeAndWait(()->{var buy=SpiritGateWindowsProbe.find(shop[0],JButton.class,"Comprar");buy.doClick();buy.doClick();shop[0].setVisible(false);});
            runtime.advanceOneTick();SwingUtilities.invokeAndWait(()->shop[0].setVisible(true));
            if(runtime.snapshot().state().coins()!=88)throw new AssertionError("Compra duplicada ou não confirmada");
            SpiritGateWindowsProbe.waitFor(()->hasBalance(shop[0]));
            SpiritGateWindowsProbe.capture(shop[0],"comercio-depois");
            var store=new LocalSaveStore(Files.createTempDirectory("trade-probe-").resolve("character.properties"));store.save(runtime.snapshot().state());if(!store.load().equals(runtime.snapshot().state()))throw new AssertionError("Persistência");
            SwingUtilities.invokeAndWait(()->owner[0].dispose());
        }
        Files.writeString(output.resolve("probe.txt"),"PASS: comércio real, compra única apesar de dois cliques, fechamento durante operação, saldo atualizado e save temporário íntegro. Capturas printAll; sem homologação física.\n");
    }
    static boolean hasBalance(Container root){for(Component child:root.getComponents()){if(child instanceof JLabel label&&label.getText().startsWith("Saldo: 88 "))return true;if(child instanceof Container nested&&hasBalance(nested))return true;}return false;}
}
