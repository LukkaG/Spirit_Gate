package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
import com.projetok.studio.WorldContent;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Composição da TASK-213: mundo livre, minimapa superior e controles inferiores sobrepostos. */
public final class NetworkGameHud extends JLayeredPane {
    private final DebugPanel world;
    private final NetworkMinimap minimap=new NetworkMinimap();
    private final JPanel dock;
    private final JPanel location;
    private final JTextArea diagnostics=new JTextArea();
    private final JLabel player=new JLabel("Conectando…"),notice=new JLabel("Sincronizando o mundo");
    private boolean mapVisible=true;
    private GamePanels panels;
    private JPanel leftTools;
    private JLabel skillNotice;
    /** Faixa informativa somente da aplicação de skills; não substitui as nove ações da hotbar. */
    public void skillFeedback(String text,String context){
        if(skillNotice==null){skillNotice=new JLabel(){@Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{GameHudStyle.panel(g,getWidth(),getHeight(),8);}finally{g.dispose();}super.paintComponent(original);}};
            skillNotice.setName("hudSkillFeedback");skillNotice.setForeground(GameHudStyle.TEXT);skillNotice.setFont(new Font("SansSerif",Font.PLAIN,11));skillNotice.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));add(skillNotice,PALETTE_LAYER);revalidate();}
        skillNotice.setText("<html>"+escape(text)+"<br><font color='#91c9d6'>"+escape(context)+"</font></html>");
        skillNotice.setToolTipText(text+" | "+context);skillNotice.getAccessibleContext().setAccessibleDescription(text+". "+context);
    }
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    public void skillTargetHint(String hint){if(targetPanel!=null)targetPanel.skillHint(hint);}
    public void skills(Runnable open){leftTools.add(new GameHudStyle.Button("hudSkills","Habilidades e domínio",GameHudStyle.Glyph.SKILLS,open));leftTools.revalidate();}
    private NetworkTargetPanel targetPanel;
    public void targeting(Runnable clear){targetPanel=new NetworkTargetPanel(clear);targetPanel.setVisible(false);add(targetPanel,PALETTE_LAYER);revalidate();}
    public void target(Protocol.State snapshot,int self,int target,int range){if(targetPanel!=null){targetPanel.refresh(snapshot,self,target,range);targetPanel.setVisible(snapshot!=null&&target!=0);}}
    void attachPanels(GamePanels panels){this.panels=panels;}
    public NetworkGameHud(DebugPanel world,NetworkCombatHud bars,NetworkHotbar hotbar,Runnable profile,Runnable inventory,Runnable journal,Runnable menu,Runnable settings) {
        this.world=world;setName("networkGameHud");setOpaque(true);setBackground(GameHudStyle.NAVY);
        add(world,DEFAULT_LAYER);add(minimap,PALETTE_LAYER);
        location=new JPanel(new GridLayout(2,1,0,6)){
            @Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{GameHudStyle.panel(g,getWidth(),getHeight(),12);}finally{g.dispose();}}
        };
        location.setOpaque(false);location.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
        player.setFont(new Font("Serif",Font.BOLD,17));player.setForeground(GameHudStyle.TEXT);notice.setFont(new Font("SansSerif",Font.PLAIN,11));notice.setForeground(GameHudStyle.MUTED);
        location.add(player);location.add(notice);add(location,PALETTE_LAYER);
        bars.overlayStyle();
        leftTools=utilityGroup("hudLeftTools",
            new GameHudStyle.Button("hudProfile","Personagem e equipamentos",GameHudStyle.Glyph.PROFILE,profile),
            new GameHudStyle.Button("hudInventory","Inventário — I",GameHudStyle.Glyph.BAG,inventory));
        var rightTools=utilityGroup("hudRightTools",
            new GameHudStyle.Button("hudJournal","Missões — J",GameHudStyle.Glyph.BOOK,journal),
            new GameHudStyle.Button("hudMenu","Menu da partida — ESC",GameHudStyle.Glyph.MENU,menu),
            new GameHudStyle.Button("hudSettings","Configurações",GameHudStyle.Glyph.SETTINGS,settings));
        bars.setName("hudResources");
        dock=new JPanel(null){@Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{
            int start=Math.max(8,(getWidth()-584)/2);g.setPaint(new GradientPaint(0,0,new Color(3,12,25,0),0,getHeight(),new Color(3,12,25,160)));g.fillRoundRect(start,0,getWidth()-start*2,getHeight(),12,12);
        }finally{g.dispose();}}@Override public void doLayout(){
            int w=getWidth(),side=108,gap=8,centerWidth=Math.min(320,w-2*(12+side+gap)),centerX=(w-centerWidth)/2;
            bars.setBounds(centerX,0,centerWidth,42);hotbar.setBounds(centerX,48,centerWidth,44);
            leftTools.setBounds(centerX-gap-side,48,side,44);rightTools.setBounds(centerX+centerWidth+gap,48,side,44);
            leftTools.putClientProperty("buttonSize",32);rightTools.putClientProperty("buttonSize",32);
            leftTools.doLayout();rightTools.doLayout();hotbar.doLayout();
        }};
        dock.setOpaque(false);dock.add(bars);dock.add(hotbar);dock.add(leftTools);dock.add(rightTools);add(dock,PALETTE_LAYER);
        diagnostics.setEditable(false);diagnostics.setBackground(new Color(3,12,25,235));diagnostics.setForeground(GameHudStyle.TEXT);diagnostics.setFont(new Font("Monospaced",Font.PLAIN,10));diagnostics.setBorder(BorderFactory.createEmptyBorder(8,10,8,10));diagnostics.setVisible(false);add(diagnostics,MODAL_LAYER);
    }
    private static JPanel utilityGroup(String name,GameHudStyle.Button... buttons){
        var group=new JPanel(null){@Override public void doLayout(){int size=getClientProperty("buttonSize") instanceof Integer value?value:32,gap=4,count=getComponentCount();
                int x=(getWidth()-count*size-(count-1)*gap)/2;
                for(var button:getComponents()){button.setBounds(x,(getHeight()-size)/2,size,size);x+=size+gap;}}};
        group.setName(name);group.setOpaque(false);for(var button:buttons)group.add(button);return group;
    }
    @Override public void doLayout(){int w=getWidth(),h=getHeight(),margin=w<760?12:24;
        world.setBounds(0,0,w,h);int mw=w<760?170:218,mh=w<760?142:184;
        minimap.setBounds(Math.max(margin,w-mw-margin),margin,mw,mh);
        if(targetPanel!=null)targetPanel.setBounds(w>=1000?(w-300)/2:margin,w>=1000?margin:100,300,skillNotice==null?100:136);
        location.setBounds(margin,margin,Math.max(180,Math.min(290,w-mw-3*margin)),74);
        dock.setBounds(0,Math.max(0,h-104),w,104);dock.doLayout();if(skillNotice!=null){int width=Math.min(660,w-2*margin);skillNotice.setBounds((w-width)/2,Math.max(margin,h-160),width,50);}diagnostics.setBounds(margin,margin+88,Math.min(410,w-2*margin),Math.max(0,Math.min(410,h-265)));if(panels!=null)panels.layoutWithin();
    }
    public void toggleMinimap(){mapVisible=!mapVisible;minimap.setVisible(mapVisible);}
    public void shortcuts(java.util.Map<String,Integer> keys){updateShortcut(this,"hudInventory","Inventário — "+java.awt.event.KeyEvent.getKeyText(keys.get("inventory")));updateShortcut(this,"hudJournal","Missões — "+java.awt.event.KeyEvent.getKeyText(keys.get("hud-J")));minimap.setToolTipText("Mapa da região atual — "+java.awt.event.KeyEvent.getKeyText(keys.get("hud-M"))+" para mostrar ou ocultar");}
    private static void activeTools(Container root,String active){for(var child:root.getComponents()){if(child instanceof GameHudStyle.Button button&&button.getName()!=null){String id=switch(button.getName()){case "hudInventory"->"inventory";case "hudProfile"->"profile";case "hudSkills"->"skills";case "hudJournal"->"journal";case "hudMenu"->"menu";case "hudSettings"->"settings";default->"";};if(!id.isEmpty())button.putClientProperty("spirit.active",("panel-"+id).equals(active));}if(child instanceof Container container)activeTools(container,active);}}
    private static void updateShortcut(Container root,String name,String text){for(var child:root.getComponents()){if(name.equals(child.getName())&&child instanceof JButton button){button.setToolTipText(text);button.getAccessibleContext().setAccessibleName(text);}if(child instanceof Container container)updateShortcut(container,name,text);}}
    public void refresh(Protocol.State snapshot,int self,String region,WorldContent.Prepared content,String identity,String status,List<String> debug,boolean showDebug) {
        minimap.refresh(snapshot,self,region,content,world.visibleWorld());player.setText(identity);player.setToolTipText(identity);notice.setText(status);notice.setToolTipText(status);activeTools(dock,panels==null||panels.active()==null?"":panels.active().getName());
        diagnostics.setVisible(showDebug);if(showDebug)diagnostics.setText(String.join("\n",debug));repaint();
    }
}
