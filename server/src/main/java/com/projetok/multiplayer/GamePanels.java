package com.projetok.multiplayer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import javax.swing.*;

/** Janelas internas da partida: sem peers nativos e sem transparência dependente do SO. */
public final class GamePanels implements AutoCloseable {
    private final NetworkGameHud root;
    private final JComponent world;
    private final Map<String,Panel> panels=new LinkedHashMap<>();
    private final JPanel shield=new JPanel(){@Override protected void paintComponent(Graphics g){g.setColor(new Color(0,7,17,65));g.fillRect(0,0,getWidth(),getHeight());}};
    private Panel active;
    private Runnable contextChanged=()->{};
    private boolean closed;
    public GamePanels(NetworkGameHud root,JComponent world){this.root=root;this.world=world;shield.setOpaque(false);shield.setVisible(false);shield.addMouseListener(new MouseAdapter(){});root.add(shield,JLayeredPane.POPUP_LAYER);root.attachPanels(this);}
    public void contextChanged(Runnable callback){contextChanged=callback;}
    public Panel register(String id,String title,JComponent content,int width,int height,boolean modal){var panel=new Panel(id,title,content,width,height,modal);panels.put(id,panel);root.add(panel,JLayeredPane.DRAG_LAYER);panel.setVisible(false);return panel;}
    public boolean modal(){return active!=null&&active.modal;}
    public boolean anyOpen(){return active!=null;}
    public Panel active(){return active;}
    /** Remove uma ferramenta temporária sem manter componentes ou callbacks de uma sessão encerrada. */
    public void unregister(String id){var panel=panels.remove(id);if(panel==null)return;panel.setVisible(false);if(active==panel){active=null;shield.setVisible(false);contextChanged.run();}root.remove(panel);root.repaint();}
    public void escape(){if(active!=null&&!active.dismissOverlay.getAsBoolean())active.hidePanel();}
    public void layoutWithin(){shield.setBounds(0,0,root.getWidth(),root.getHeight());for(var panel:panels.values())panel.fit();}
    public final class Panel extends JPanel {
        private final int preferredWidth,preferredHeight;
        private final boolean modal;
        private final JButton close=new JButton("×");
        private final String id;
        private boolean positioned;
        private Component previousFocus;
        private Runnable onOpen=()->{},onClose=()->{};
        private BooleanSupplier canClose=()->true;
        private BooleanSupplier dismissOverlay=()->false;
        public void dismissOverlay(BooleanSupplier action){dismissOverlay=action;}
        Panel(String id,String title,JComponent content,int width,int height,boolean modal){
            super(new BorderLayout(10,12));this.id=id;this.modal=modal;preferredWidth=width;preferredHeight=height;
            setName("panel-"+id);setOpaque(false);setBorder(BorderFactory.createEmptyBorder(14,18,18,18));setFocusCycleRoot(true);setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
            var heading=new JPanel(new BorderLayout(8,0));var label=new JLabel(title);label.setFont(new Font("Serif",Font.BOLD,22));heading.add(label);heading.add(close,BorderLayout.EAST);close.setName("close-"+id);close.getAccessibleContext().setAccessibleName("Fechar "+title);close.addActionListener(e->hidePanel());
            add(heading,BorderLayout.NORTH);add(content);GameHudStyle.skin(this);label.setFont(new Font("Serif",Font.BOLD,22));label.setForeground(GameHudStyle.CYAN);
            var drag=new MouseAdapter(){private Point start;public void mousePressed(MouseEvent e){start=SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),root);}public void mouseDragged(MouseEvent e){if(start==null)return;Point next=SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),root);setLocation(clamp(getX()+next.x-start.x,8,root.getWidth()-getWidth()-8),clamp(getY()+next.y-start.y,8,root.getHeight()-getHeight()-8));start=next;}};
            heading.addMouseListener(drag);heading.addMouseMotionListener(drag);label.addMouseListener(drag);label.addMouseMotionListener(drag);
            registerKeyboardAction(e->escape(),KeyStroke.getKeyStroke("ESCAPE"),WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        }
        public void onOpen(Runnable action){onOpen=action;}
        public void onClose(Runnable action){onClose=action;}
        public void canClose(BooleanSupplier value){canClose=value;}
        public void toggle(){if(isVisible())hidePanel();else showPanel();}
        public void showPanel(){
            if(closed)return;if(active!=null&&active!=this){if(!active.canClose.getAsBoolean())return;active.hidePanel();}
            previousFocus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();contextChanged.run();active=this;if(modal)positioned=false;fit();setVisible(true);shield.setVisible(modal);root.moveToFront(this);onOpen.run();close.requestFocusInWindow();root.repaint();
        }
        public void hidePanel(){if(!isVisible()||!canClose.getAsBoolean())return;onClose.run();setVisible(false);if(active==this)active=null;shield.setVisible(false);contextChanged.run();
            if(previousFocus!=null&&previousFocus.isShowing()&&SwingUtilities.isDescendingFrom(previousFocus,root))previousFocus.requestFocusInWindow();else world.requestFocusInWindow();root.repaint();}
        private void fit(){int w=Math.max(1,Math.min(preferredWidth,root.getWidth()-24)),h=Math.max(1,Math.min(preferredHeight,root.getHeight()-24));
            if(!positioned&&root.getWidth()>24&&root.getHeight()>24){setBounds((root.getWidth()-w)/2,(root.getHeight()-h)/2,w,h);positioned=true;}
            else setBounds(clamp(getX(),8,root.getWidth()-w-8),clamp(getY(),8,root.getHeight()-h-8),w,h);
        }
        @Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{
            g.setPaint(new GradientPaint(0,0,new Color(8,27,44,238),0,getHeight(),new Color(4,16,30,226)));g.fillRoundRect(2,2,getWidth()-5,getHeight()-5,16,16);
            g.setColor(new Color(84,205,233,45));g.setStroke(new BasicStroke(5));g.drawRoundRect(3,3,getWidth()-7,getHeight()-7,16,16);
            g.setColor(new Color(112,225,244,185));g.setStroke(new BasicStroke(1));g.drawRoundRect(2,2,getWidth()-5,getHeight()-5,16,16);
        }finally{g.dispose();}}
    }
    private static int clamp(int value,int min,int max){return Math.max(min,Math.min(Math.max(min,max),value));}
    @Override public void close(){if(closed)return;closed=true;for(var p:panels.values()){p.onClose.run();root.remove(p);}root.remove(shield);panels.clear();active=null;root.attachPanels(null);}
}
