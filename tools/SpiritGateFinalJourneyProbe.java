package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
/** Percurso real com identidade criada pela UI e fixtures explicitamente sintéticas. */
public final class SpiritGateFinalJourneyProbe {
    static Path output,data;static JFrame game;
    static JFrame menu(){return SpiritGateCorujao3Probe.frame("Spirit Gate | Inicio");}
    static void waitFor(SpiritGateWindowsProbe.Check check)throws Exception{SpiritGateWindowsProbe.waitFor(check);}
    static void click(JFrame frame,String text)throws Exception{SpiritGateCorujao3Probe.click(frame,text);}
    static void capture(Window window,String name)throws Exception{SpiritGateWindowsProbe.capture(window,name);}
    static void openGame()throws Exception{click(menu(),"Jogar");waitFor(()->SpiritGateCorujao3Probe.frame("Spirit Gate | RPG offline")!=null);game=SpiritGateCorujao3Probe.frame("Spirit Gate | RPG offline");}
    static void saveMenu()throws Exception{click(game,"Salvar e voltar ao menu");waitFor(()->menu()!=null);waitFor(()->SpiritGateWindowsProbe.find(menu(),JButton.class,"Jogar").isEnabled());}
    static void reloadMenu()throws Exception{SwingUtilities.invokeAndWait(()->{menu().dispose();RpgMenu.open();});waitFor(()->menu()!=null&&SpiritGateWindowsProbe.find(menu(),JButton.class,"Jogar").isEnabled());}
    static void key(String key)throws Exception{SwingUtilities.invokeAndWait(()->{var panel=SpiritGateWindowsProbe.find(game,com.projetok.prototype.DebugPanel.class,null);panel.getActionMap().get(key+"true").actionPerformed(new java.awt.event.ActionEvent(panel,0,"probe"));});if(key.equals("W"))Thread.sleep(200);SwingUtilities.invokeAndWait(()->{var panel=SpiritGateWindowsProbe.find(game,com.projetok.prototype.DebugPanel.class,null);panel.getActionMap().get(key+"false").actionPerformed(new java.awt.event.ActionEvent(panel,0,"probe"));});}
    static java.util.List<JList<?>> lists(Container parent){var result=new java.util.ArrayList<JList<?>>();for(Component child:parent.getComponents()){if(child instanceof JList<?> list)result.add(list);else if(child instanceof Container container)result.addAll(lists(container));}return result;}
    static boolean label(Container root,String fragment){for(Component child:root.getComponents()){if(child instanceof JLabel l&&l.getText().contains(fragment))return true;if(child instanceof Container nested&&label(nested,fragment))return true;}return false;}
    /** Nenhum save pessoal; fixtures só reposicionam cenas de teste no mesmo personagem. */
    public static void main(String[] args){try{run(args);System.exit(0);}catch(Throwable error){error.printStackTrace();System.exit(1);}}
    static void run(String[] args)throws Exception{
        output=Path.of(args[0]);Files.createDirectories(output);SpiritGateWindowsProbe.output=output;data=Files.createTempDirectory("corujao3-final-");System.setProperty("spiritgate.rpg.dataDir",data.toString());
        SwingUtilities.invokeLater(SpiritGateEntry::open);waitFor(()->SpiritGateCorujao3Probe.frame("Spirit Gate | Portal de entrada")!=null);
        JFrame entry=SpiritGateCorujao3Probe.frame("Spirit Gate | Portal de entrada");SwingUtilities.invokeAndWait(()->SpiritGateWindowsProbe.find(entry,JTextField.class,null).setText("Perfil sintético"));click(entry,"Entrar");waitFor(()->menu()!=null);waitFor(()->SpiritGateWindowsProbe.find(menu(),JButton.class,"Criar personagem").isEnabled());
        click(menu(),"Criar personagem");waitFor(()->SpiritGateWindowsProbe.dialog("Criar personagem local")!=null);JDialog creation=SpiritGateWindowsProbe.dialog("Criar personagem local");
        SwingUtilities.invokeAndWait(()->{var form=(CharacterCreation)creation.getContentPane();form.name.setText("Jornada sintética");form.guardian.doClick();form.create.doClick();});
        waitFor(()->SpiritGateCorujao3Probe.frame("Spirit Gate | RPG offline")!=null);game=SpiritGateCorujao3Probe.frame("Spirit Gate | RPG offline");waitFor(()->SpiritGateWindowsProbe.dialog("Primeiros passos")!=null);SpiritGateCorujao3Probe.closeHelp();
        key("W");click(game,"Minimapa");click(game,"Minimapa");click(game,"Mapa [M]");waitFor(()->SpiritGateWindowsProbe.dialog("Diario e exploracao")!=null);JDialog diary=SpiritGateWindowsProbe.dialog("Diario e exploracao");capture(diary,"01-mapa");SwingUtilities.invokeAndWait(()->diary.setVisible(false));
        saveMenu();var repository=new CharacterSaves(data.resolve("saves/characters"));var slot=repository.list().get(0);var store=new LocalSaveStore(slot.file());var state=store.load();if(state.y()>=320)throw new AssertionError("Movimento não confirmado no save");
        // Fixture de comércio: mesma identidade criada na UI, saldo e posição explícitos.
        var fixture=RpgWorld.restore(new RpgSave(state.identity(),"village",230,400,state.hp(),state.xp(),100,state.inventory(),state.equipment(),state.quests(),state.flags(),state.ticks()));fixture.inventory().add("sword",1);store.save(fixture.save());reloadMenu();openGame();key("F");waitFor(()->SpiritGateWindowsProbe.find(game,JButton.class,"Comércio")!=null);click(game,"Comércio");waitFor(()->SpiritGateWindowsProbe.dialog("Comércio")!=null);JDialog shop=SpiritGateWindowsProbe.dialog("Comércio");
        SwingUtilities.invokeAndWait(()->{lists(shop).get(0).setSelectedIndex(0);SpiritGateWindowsProbe.find(shop,JButton.class,"Comprar").doClick();});waitFor(()->label(shop,"Saldo: 88 "));
        SwingUtilities.invokeAndWait(()->{lists(shop).get(1).setSelectedIndex(0);SpiritGateWindowsProbe.find(shop,JButton.class,"Vender").doClick();});waitFor(()->label(shop,"Saldo: 94 "));capture(shop,"02-comercio");SwingUtilities.invokeAndWait(()->SpiritGateWindowsProbe.find(shop,JButton.class,"Sair").doClick());key("ESCAPE");
        click(game,"Inventario [I]");waitFor(()->SpiritGateWindowsProbe.dialog("Inventario e equipamento")!=null);JDialog inventory=SpiritGateWindowsProbe.dialog("Inventario e equipamento");SwingUtilities.invokeAndWait(()->{lists(inventory).get(0).setSelectedIndex(1);SpiritGateWindowsProbe.find(inventory,JButton.class,"Equipar").doClick();});
        waitFor(()->{var model=lists(inventory).get(1).getModel();for(int i=0;i<model.getSize();i++)if(model.getElementAt(i).toString().startsWith("weapon: Espada"))return true;return false;});capture(inventory,"03-equipamento");SwingUtilities.invokeAndWait(()->inventory.setVisible(false));
        click(game,"Diario [J]");capture(diary,"04-diario");SwingUtilities.invokeAndWait(()->diary.setVisible(false));saveMenu();state=store.load();
        // Fixture de combate: só muda região/posição, mantendo inventário e moedas verificados.
        store.save(new RpgSave(state.identity(),"forest",240,360,state.hp(),state.xp(),state.coins(),state.inventory(),state.equipment(),state.quests(),state.flags(),state.ticks()));reloadMenu();openGame();
        for(int i=0;i<6;i++){click(game,"Golpe [SPACE]");Thread.sleep(450);}click(game,"Habilidade [Q]");capture(game,"05-combate");
        click(game,"Opções");waitFor(()->SpiritGateWindowsProbe.dialog("Opções do Spirit Gate")!=null);JDialog options=SpiritGateWindowsProbe.dialog("Opções do Spirit Gate");SwingUtilities.invokeAndWait(()->{SpiritGateWindowsProbe.find(options,JSlider.class,null).setValue(55);SpiritGateWindowsProbe.find(options,JButton.class,"Aplicar").doClick();});waitFor(()->!options.isDisplayable());saveMenu();
        var saved=store.load();if(saved.xp()<=0||!saved.equipment().containsValue("sword")||saved.coins()<94)throw new AssertionError("Combate/equipamento/moedas não persistiram");if(RpgPreferences.load(data.resolve("saves/preferences.properties")).volume()!=55)throw new AssertionError("Opções não persistiram");
        openGame();capture(game,"06-continuar");saveMenu();var continued=store.load();if(!continued.identity().equals(saved.identity())||continued.xp()!=saved.xp()||!continued.equipment().equals(saved.equipment()))throw new AssertionError("Continuação alterou progresso");
        Files.writeString(output.resolve("probe.txt"),"PASS: entrada/perfil, criação Guardian, ajuda dispensada, ação de movimento, mapa/minimapa, NPC comerciante, compra/venda, inventário/equipamento, missão consultada, inimigo derrotado com XP, habilidade, opções volume 55, salvar/seleção/continuar íntegros. Mesmo personagem criado pela UI. Duas fixtures explícitas reposicionam comércio e combate e fornecem saldo/espada sintéticos; não constituem teste de progressão natural da campanha. Ações Swing, não teclado físico. Dados: "+data+"\n");SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())window.dispose();});
    }
}
