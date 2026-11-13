package com.projetok.multiplayer;

import com.projetok.rpg.*;
import com.projetok.prototype.rpg.LauncherSession;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task214To220UiTest {
    @TempDir Path root;
    @Test void categoriesSelectionAndOrganizationKeepConfirmedQuantities()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var panel=new NetworkInventoryPanel(null);GameHudStyle.skin(panel);
            var inventory=Map.of("0:potion",3,"1:small_sword",1,"2:robe",1,"3:crystal",5,"4:relic",1);
            var sheet=new GameplayProtocol.Sheet(inventory,Map.of());panel.update(sheet);
            var grid=(JList<?>)GamePanelsIntegrationTest.find(panel,"inventoryGrid");assertEquals(JList.HORIZONTAL_WRAP,grid.getLayoutOrientation());assertEquals(5,grid.getModel().getSize());
            var expected=Map.of("ARMOR",1,"WEAPON",1,"DROP",3,"ALL",5);
            expected.forEach((category,count)->{((JButton)GamePanelsIntegrationTest.find(panel,"inventoryCategory"+category)).doClick(0);assertEquals(count.intValue(),grid.getModel().getSize());assertTrue(Boolean.TRUE.equals(((JButton)GamePanelsIntegrationTest.find(panel,"inventoryCategory"+category)).getClientProperty("spirit.active")));});
            ((JButton)GamePanelsIntegrationTest.find(panel,"inventoryCategoryALL")).doClick(0);String selected=grid.getSelectedValue().toString();((JButton)GamePanelsIntegrationTest.find(panel,"inventoryOrganize")).doClick(0);assertEquals(selected,grid.getSelectedValue().toString());assertEquals(inventory,sheet.inventory());
            var search=(JTextField)GamePanelsIntegrationTest.find(panel,"inventorySearch");search.setText("nada-existente");assertEquals(0,grid.getModel().getSize());assertFalse(GamePanelsIntegrationTest.find(panel,"inventoryEquip").isEnabled());assertFalse(GamePanelsIntegrationTest.find(panel,"inventoryUse").isEnabled());
            search.setText("");assertEquals(5,grid.getModel().getSize());panel.update((GameplayProtocol.Sheet)null);assertEquals(0,grid.getModel().getSize());
        });
    }
    @Test void questSelectionReflectsEveryServerStateWithoutLosingProgress()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var panel=new NetworkQuestPanel(null);var list=(JList<?>)GamePanelsIntegrationTest.find(panel,"questList");
            for(String state:List.of("AVAILABLE","ACTIVE","READY","COMPLETED")){
                var q=new Protocol.QuestProgress("coop_slime",state,state.equals("READY")||state.equals("COMPLETED")?1:0,1);panel.refresh(List.of(q));assertEquals(1,list.getModel().getSize());assertEquals(q,list.getSelectedValue());
                assertTrue(((JTextArea)GamePanelsIntegrationTest.find(panel,"questDescription")).getText().contains("slime"));
            }
            panel.refresh(List.of(new Protocol.QuestProgress("coop_slime","UNAVAILABLE",0,1)));assertEquals(0,list.getModel().getSize());
        });
    }
    @Test void gridEquipUsesServerAndKeepsTheOtherInventoryPrivate()throws Exception {
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,SessionCharacterSelectionTest.catalog(),root);
            var a=Task207InventoryTest.connect(server,"Grid Reaper","reaper");var b=Task207InventoryTest.connect(server,"Grid Human","human")){
            Task201CharacterSessionTest.ready(a,b);Task201CharacterSessionTest.waitFor(()->a.playerSheet()!=null&&b.playerSheet()!=null,a,b);var other=b.playerSheet();
            a.gameplay(GameplayProtocol.Operation.UNEQUIP,"weapon");Task201CharacterSessionTest.waitFor(()->a.playerSheet().equipment().isEmpty(),a,b);
            SwingUtilities.invokeAndWait(()->{var panel=new NetworkInventoryPanel(a);panel.update(a.playerSheet());((JButton)GamePanelsIntegrationTest.find(panel,"inventoryCategoryWEAPON")).doClick(0);var grid=(JList<?>)GamePanelsIntegrationTest.find(panel,"inventoryGrid");assertEquals(1,grid.getModel().getSize());((JButton)GamePanelsIntegrationTest.find(panel,"inventoryEquip")).doClick(0);});
            Task201CharacterSessionTest.waitFor(()->"small_sword".equals(a.playerSheet().equipment().get("weapon")),a,b);assertEquals(0,Task207InventoryTest.count(a.playerSheet(),"small_sword"));assertEquals(other,b.playerSheet());assertNull(server.failure());
            SwingUtilities.invokeAndWait(()->{
                var hotbar=new NetworkHotbar(a);hotbar.overlayStyle(()->{});hotbar.resources(new Protocol.State(1,List.of(new Protocol.Actor(1,"Teste",100,100,100,100,0,0,false))),1);
                var progress=new GameplayProtocol.Progress(0,20,20000,18,0,"none");hotbar.refresh(new GameplayProtocol.Sheet(Map.of(),Map.of(),progress));
                assertTrue(GamePanelsIntegrationTest.find(hotbar,"hudSkill0").isEnabled(),"Ataque básico não deve exigir energia");assertFalse(GamePanelsIntegrationTest.find(hotbar,"hudSkill1").isEnabled());assertFalse(GamePanelsIntegrationTest.find(hotbar,"hudSkill3").isEnabled());
                hotbar.resources(new Protocol.State(2,List.of(new Protocol.Actor(1,"Teste",100,100,100,100,1000,0,false))),1);
                assertTrue(a.cycleTarget());
                hotbar.refresh(new GameplayProtocol.Sheet(Map.of("0:potion",2),Map.of(),progress,new GameplayProtocol.Abilities(6,0,0,60),""));assertFalse(GamePanelsIntegrationTest.find(hotbar,"hudSkill0").isEnabled());assertTrue(GamePanelsIntegrationTest.find(hotbar,"hudSkill1").isEnabled());assertTrue(GamePanelsIntegrationTest.find(hotbar,"hudSkill3").isEnabled());assertTrue(((JButton)GamePanelsIntegrationTest.find(hotbar,"hudSkill0")).getToolTipText().contains("0.2s"));
            });
        }
    }
}
