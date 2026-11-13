package com.projetok.multiplayer;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/** Acabamento Java2D da HUD, com a paleta e o emblema geométrico da entrada aprovada. */
final class GameHudStyle {
    static final Color NAVY=com.projetok.prototype.rpg.SpiritTheme.BACKGROUND,TEXT=new Color(227,248,255),MUTED=new Color(153,189,205),CYAN=com.projetok.prototype.rpg.SpiritTheme.ACCENT;
    enum Glyph { SWORD, ADVANCE, SHIELD, POTION, PROFILE, BAG, BOOK, MENU, SETTINGS, CRYSTAL, RELIC, ARMOR, SKILLS, SORT, COLLECT }
    static Icon icon(Glyph glyph,int size){return new Icon(){public int getIconWidth(){return size;}public int getIconHeight(){return size;}public void paintIcon(Component c,Graphics original,int x,int y){var g=graphics(original);try{g.setColor(c.isEnabled()?CYAN:MUTED);glyph(g,glyph,x,y,size);}finally{g.dispose();}}};}
    static Glyph itemGlyph(com.projetok.rpg.ItemCatalog.Item item){return switch(item.category()){case EQUIPMENT->item.slot().equals("weapon")?Glyph.SWORD:Glyph.ARMOR;case CONSUMABLE->Glyph.POTION;case MATERIAL->Glyph.CRYSTAL;case QUEST->Glyph.RELIC;};}
    static JButton iconButton(String name,String label,Glyph glyph,Runnable action){var button=new JButton(icon(glyph,20));button.setName(name);button.setToolTipText(label);button.getAccessibleContext().setAccessibleName(label);button.addActionListener(e->action.run());button.putClientProperty("spirit.compact",Boolean.TRUE);skin(button);return button;}
    static Graphics2D graphics(Graphics original){var g=(Graphics2D)original.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);return g;}
    static void panel(Graphics2D g,int width,int height,int radius) {
        g.setPaint(new GradientPaint(0,0,new Color(13,37,57,240),0,height,new Color(4,15,29,225)));
        g.fillRoundRect(2,2,width-5,height-5,radius,radius);
        g.setColor(new Color(43,108,139,100));g.setStroke(new BasicStroke(4));g.drawRoundRect(2,2,width-5,height-5,radius,radius);
        g.setStroke(new BasicStroke(1));g.setColor(new Color(70,153,180,185));g.drawRoundRect(2,2,width-5,height-5,radius,radius);
        g.setColor(new Color(112,225,244,180));g.drawLine(18,2,Math.min(width-18,65),2);
    }
    static void emblem(Graphics2D g,int x,int y,int size) {
        var copy=(Graphics2D)g.create();try{copy.translate(x,y);copy.scale(size/32.0,size/32.0);copy.setColor(CYAN);copy.setStroke(new BasicStroke(1.2f));
            copy.drawPolygon(new int[]{16,30,16,2},new int[]{0,16,32,16},4);copy.drawArc(9,7,14,16,0,180);copy.drawLine(9,15,9,24);copy.drawLine(23,15,23,24);copy.drawLine(9,24,23,24);
        }finally{copy.dispose();}
    }
    static void glyph(Graphics2D original,Glyph glyph,int x,int y,int size) {
        var g=(Graphics2D)original.create();try {
            g.translate(x,y);g.scale(size/32.0,size/32.0);g.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(glyph) {
                case SWORD->{g.drawPolygon(new int[]{10,25,28,13},new int[]{21,3,3,24},4);g.drawLine(6,18,17,27);g.drawLine(8,24,3,29);}
                case ADVANCE->{g.drawLine(3,9,17,9);g.drawLine(1,16,22,16);g.drawLine(5,23,17,23);g.drawLine(18,7,28,16);g.drawLine(28,16,18,25);}
                case SHIELD->{g.draw(new Path2D.Double(){{moveTo(16,3);lineTo(27,7);lineTo(25,19);quadTo(23,25,16,29);quadTo(9,25,7,19);lineTo(5,7);closePath();}});g.drawLine(16,8,16,23);}
                case POTION->{g.drawRoundRect(11,2,10,5,2,2);g.drawLine(12,7,12,12);g.drawLine(20,7,20,12);g.draw(new Path2D.Double(){{moveTo(12,12);curveTo(0,23,5,29,16,29);curveTo(27,29,32,23,20,12);}});g.drawLine(8,22,24,22);}
                case PROFILE->{g.drawOval(11,3,10,11);g.drawArc(5,17,22,20,0,180);g.drawLine(5,27,27,27);}
                case BAG->{g.drawRoundRect(5,10,22,19,4,4);g.drawArc(10,2,12,16,0,180);g.drawLine(5,17,27,17);g.drawLine(14,16,18,16);}
                case BOOK->{g.draw(new Path2D.Double(){{moveTo(16,8);quadTo(9,3,3,6);lineTo(3,26);quadTo(9,23,16,28);quadTo(23,23,29,26);lineTo(29,6);quadTo(23,3,16,8);lineTo(16,28);}});}
                case MENU->{for(int i=0;i<3;i++){g.drawLine(5,8+i*8,27,8+i*8);g.fillOval(2,6+i*8,4,4);}}
                case SETTINGS->{g.drawOval(7,7,18,18);g.drawOval(12,12,8,8);for(int i=0;i<8;i++){double a=i*Math.PI/4;g.draw(new Line2D.Double(16+10*Math.cos(a),16+10*Math.sin(a),16+14*Math.cos(a),16+14*Math.sin(a)));}}
                case CRYSTAL->{g.drawPolygon(new int[]{16,25,21,11,7},new int[]{2,12,28,28,12},5);g.drawLine(16,2,12,15);g.drawLine(12,15,16,28);g.drawLine(12,15,25,12);}
                case RELIC->{g.drawOval(8,3,16,16);g.drawLine(12,19,9,29);g.drawLine(20,19,23,29);g.drawLine(16,7,16,15);g.drawLine(12,11,20,11);}
                case ARMOR->{g.drawPolygon(new int[]{10,4,1,7,10,10,22,22,25,31,28,22,20,12},new int[]{3,5,13,16,11,29,29,11,16,13,5,3,8,8},14);g.drawLine(13,16,19,16);}
                case SKILLS->{g.drawPolygon(new int[]{17,7,14,11,26,19,23},new int[]{1,17,17,31,12,12,1},7);}
                case SORT->{g.drawLine(5,6,5,27);g.drawLine(1,22,5,27);g.drawLine(9,22,5,27);g.drawLine(13,7,29,7);g.drawLine(13,15,25,15);g.drawLine(13,23,21,23);}
                case COLLECT->{g.drawRoundRect(4,18,24,11,3,3);g.drawLine(16,2,16,21);g.drawLine(10,14,16,21);g.drawLine(22,14,16,21);}
            }
        } finally {g.dispose();}
    }
    static final class Button extends JButton {
        private final Glyph glyph;private String badge="";private boolean selected;private float remaining;
        Button(String name,String tooltip,Glyph glyph,Runnable action) {
            this.glyph=glyph;setName(name);setToolTipText(tooltip);getAccessibleContext().setAccessibleName(tooltip);
            setRolloverEnabled(true);
            setOpaque(false);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(36,36));addActionListener(e->action.run());
        }
        void visual(boolean selected,String badge,float remaining){this.selected=selected;this.badge=badge;this.remaining=Math.max(0,Math.min(1,remaining));repaint();}
        @Override protected void paintComponent(Graphics original){var g=graphics(original);try{
            int w=getWidth(),h=getHeight();boolean hover=getModel().isRollover()||hasFocus(),pressed=getModel().isPressed();boolean selected=this.selected||Boolean.TRUE.equals(getClientProperty("spirit.active"));
            if(hover||pressed){g.setColor(new Color(65,207,236,pressed?35:16));g.fillRoundRect(1,1,w-2,h-2,6,6);}
            if(hasFocus()){g.setColor(CYAN);g.setStroke(new BasicStroke(1));g.drawRoundRect(1,1,w-3,h-3,6,6);}
            if(selected){g.setColor(CYAN);g.fillRoundRect(w/2-7,h-3,14,2,2,2);}
            boolean hasBadge=!badge.isEmpty();g.setColor(isEnabled()?(hover?CYAN:TEXT):new Color(99,129,147));int size=Math.min(24,Math.min(w-6,h-(hasBadge?13:6)));
            if(getClientProperty("spirit.skillIcon") instanceof Icon icon){var image=(Graphics2D)g.create();try{if(!isEnabled())image.setComposite(AlphaComposite.SrcOver.derive(.4f));image.translate((w-size)/2.0,hasBadge?2:(h-size)/2.0);image.scale(size/(double)icon.getIconWidth(),size/(double)icon.getIconHeight());icon.paintIcon(this,image,0,0);}finally{image.dispose();}}
            else glyph(g,glyph,(w-size)/2,hasBadge?2:(h-size)/2,size);
            if(remaining>0){g.setColor(CYAN);g.setStroke(new BasicStroke(2));g.drawArc(2,1,w-5,h-10,90,-Math.round(360*remaining));}
            g.setFont(new Font("SansSerif",Font.BOLD,8));g.setColor(isEnabled()?CYAN:MUTED);int tw=g.getFontMetrics().stringWidth(badge);g.drawString(badge,(w-tw)/2,h-5);
        }finally{g.dispose();}}
    }
    /** Os conteúdos existentes são preservados e apenas transferidos para acesso pela HUD. */
    static JDialog window(JFrame owner,String title,JComponent contents,int width,int height) {
        var dialog=new JDialog(owner,title,false);dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setContentPane(contents);dialog.setSize(width,height);dialog.setLocationRelativeTo(owner);
        dialog.getRootPane().registerKeyboardAction(e->dialog.setVisible(false),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);
        return dialog;
    }
    static void toggle(JDialog dialog,JComponent world){dialog.setVisible(!dialog.isVisible());if(!dialog.isVisible())world.requestFocusInWindow();}
    /** Estilo local por componente, sem alterar UIManager ou interfaces externas. */
    static void skin(Component component){
        component.setForeground(TEXT);component.setFont(new Font("SansSerif",Font.PLAIN,12));
        if(component instanceof JPanel panel)panel.setOpaque(false);
        if(component instanceof JLabel label)label.setOpaque(false);
        if(component instanceof javax.swing.text.JTextComponent text){text.setBackground(new Color(9,27,43));text.setCaretColor(CYAN);if(!text.isEditable())text.setOpaque(false);}
        if(component instanceof JButton button&&!(button instanceof Button)){
            button.setUI(new javax.swing.plaf.basic.BasicButtonUI(){@Override protected void paintText(Graphics g,JComponent c,Rectangle rect,String text){if(button.isEnabled())super.paintText(g,c,rect,text);else{g.setColor(MUTED);g.drawString(text,rect.x,rect.y+g.getFontMetrics().getAscent());}}@Override public void paint(Graphics original,JComponent c){var g=graphics(original);try{
                boolean active=button.getModel().isRollover()||button.hasFocus()||Boolean.TRUE.equals(button.getClientProperty("spirit.active"));g.setColor(new Color(12,39,58,235));g.fillRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,8,8);
                if(active){g.setColor(new Color(91,212,239,35));g.fillRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,8,8);}
                if(button.getModel().isPressed()){g.setColor(new Color(91,212,239,65));g.fillRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,8,8);}
                g.setColor(active?CYAN:new Color(49,110,139));g.drawRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,8,8);
            }finally{g.dispose();}super.paint(original,c);}});
            button.setOpaque(false);button.setContentAreaFilled(false);button.setBorder(Boolean.TRUE.equals(button.getClientProperty("spirit.compact"))?BorderFactory.createEmptyBorder(4,6,4,6):BorderFactory.createEmptyBorder(8,12,8,12));button.setRolloverEnabled(true);
        }
        if(component instanceof JCheckBox box){box.setOpaque(false);box.setIcon(new Icon(){public int getIconWidth(){return 18;}public int getIconHeight(){return 18;}public void paintIcon(Component c,Graphics original,int x,int y){var g=graphics(original);try{
            g.setColor(new Color(12,38,56));g.fillRoundRect(x,y,16,16,4,4);g.setColor(box.hasFocus()?Color.WHITE:CYAN);g.drawRoundRect(x,y,16,16,4,4);
            if(box.isSelected()){g.setStroke(new BasicStroke(2));g.drawLine(x+3,y+8,x+7,y+12);g.drawLine(x+7,y+12,x+13,y+4);}
        }finally{g.dispose();}}});}
        if(component instanceof JSlider slider){slider.setOpaque(false);slider.setUI(new javax.swing.plaf.basic.BasicSliderUI(slider){
            @Override public void paintTrack(Graphics original){var g=graphics(original);try{int y=trackRect.y+trackRect.height/2;g.setColor(new Color(26,61,79));g.fillRoundRect(trackRect.x,y-3,trackRect.width,6,6,6);g.setColor(CYAN);g.fillRoundRect(trackRect.x,y-3,Math.max(0,thumbRect.x-trackRect.x+thumbRect.width/2),6,6,6);}finally{g.dispose();}}
            @Override public void paintThumb(Graphics original){var g=graphics(original);try{g.setColor(new Color(112,225,244,45));g.fillOval(thumbRect.x-3,thumbRect.y-3,thumbRect.width+6,thumbRect.height+6);g.setColor(CYAN);g.fillOval(thumbRect.x,thumbRect.y,thumbRect.width,thumbRect.height);}finally{g.dispose();}}
            @Override public void paintFocus(Graphics g){if(slider.hasFocus()){g.setColor(CYAN);g.drawRect(0,0,slider.getWidth()-1,slider.getHeight()-1);}}
        });if(slider.getLabelTable()!=null){var labels=slider.getLabelTable().elements();while(labels.hasMoreElements()){var label=labels.nextElement();if(label instanceof JComponent c){c.setForeground(TEXT);c.setFont(new Font("SansSerif",Font.PLAIN,10));c.setOpaque(false);}}}}
        if(component instanceof JComboBox<?> combo){combo.setBackground(new Color(12,35,52));combo.setRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> l,Object value,int index,boolean selected,boolean focus){var label=(JLabel)super.getListCellRendererComponent(l,value,index,selected,focus);if(combo.getClientProperty("spirit.labels") instanceof java.util.Map<?,?> labels&&labels.get(value)!=null)label.setText(labels.get(value).toString());label.setBackground(selected?new Color(26,73,94):new Color(12,35,52));label.setForeground(TEXT);label.setBorder(BorderFactory.createEmptyBorder(4,6,4,6));return label;}});combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){@Override public void paintCurrentValueBackground(Graphics g,Rectangle bounds,boolean focus){g.setColor(new Color(12,35,52));g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);}@Override protected JButton createArrowButton(){var b=new JButton(){@Override protected void paintComponent(Graphics original){var g=graphics(original);try{g.setColor(CYAN);int x=getWidth()/2,y=getHeight()/2;g.fillPolygon(new int[]{x-4,x+4,x},new int[]{y-2,y-2,y+3},3);}finally{g.dispose();}}};b.setOpaque(false);b.setContentAreaFilled(false);b.setBorder(BorderFactory.createEmptyBorder());return b;}});}
        if(component instanceof JList<?> list){list.setOpaque(false);list.setSelectionBackground(new Color(26,73,94));list.setSelectionForeground(TEXT);if(!Boolean.TRUE.equals(list.getClientProperty("spirit.customRenderer")))list.setCellRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean selected,boolean focus){var c=(JLabel)super.getListCellRendererComponent(l,v,i,selected,focus);c.setOpaque(selected);c.setForeground(TEXT);c.setBorder(BorderFactory.createEmptyBorder(7,9,7,9));return c;}});}
        if(component instanceof JScrollPane scroll){scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.setBorder(BorderFactory.createLineBorder(new Color(42,88,110)));scroll.getVerticalScrollBar().setUnitIncrement(18);}
        if(component instanceof JProgressBar bar){bar.setOpaque(false);bar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI(){@Override protected void paintDeterminate(Graphics original,JComponent c){var g=graphics(original);try{int w=c.getWidth(),h=c.getHeight();g.setColor(new Color(5,22,36,235));g.fillRoundRect(0,0,w-1,h-1,6,6);g.setColor(new Color(32,127,155));g.fillRoundRect(2,2,(int)((w-4)*bar.getPercentComplete()),Math.max(0,h-4),5,5);g.setColor(CYAN);g.drawRoundRect(0,0,w-1,h-1,6,6);if(bar.isStringPainted()){g.setFont(bar.getFont());String text=bar.getString();g.setColor(TEXT);g.drawString(text,(w-g.getFontMetrics().stringWidth(text))/2,(h-g.getFontMetrics().getHeight())/2+g.getFontMetrics().getAscent());}}finally{g.dispose();}}});}
        if(component instanceof JScrollBar bar){bar.setOpaque(false);bar.setPreferredSize(new Dimension(10,10));bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            @Override protected JButton createDecreaseButton(int orientation){var b=new JButton();b.setPreferredSize(new Dimension());return b;}
            @Override protected JButton createIncreaseButton(int orientation){return createDecreaseButton(orientation);}
            @Override protected void paintTrack(Graphics g,JComponent c,Rectangle r){g.setColor(new Color(6,21,34,160));g.fillRect(r.x,r.y,r.width,r.height);}
            @Override protected void paintThumb(Graphics g,JComponent c,Rectangle r){g.setColor(new Color(75,166,190));g.fillRoundRect(r.x+2,r.y+2,Math.max(2,r.width-4),Math.max(2,r.height-4),6,6);}
        });}
        if(component instanceof Container container)for(var child:container.getComponents())skin(child);
    }
    private GameHudStyle() { }
}
