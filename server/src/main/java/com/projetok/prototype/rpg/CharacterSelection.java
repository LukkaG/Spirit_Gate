package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import com.projetok.prototype.DebugSnapshot;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Apresentacao do catalogo existente; nao possui simulacao ou formato de save. */
final class CharacterSelection extends JPanel {
    static final Color INK=SpiritTheme.BACKGROUND, CYAN=SpiritTheme.ACCENT;
    final DefaultListModel<CharacterSaves.Slot> model=new DefaultListModel<>();
    final JList<CharacterSaves.Slot> list=new JList<>(model){@Override public boolean getScrollableTracksViewportWidth(){return true;}};
    final JButton play=new SpiritButton("Jogar",true), create=new SpiritButton("Criar personagem",false), back=new SpiritButton("Voltar",false), delete=new SpiritButton("Arquivar personagem",false);
    final JButton techniques=new SpiritButton("Conhecer os caminhos",false);
    final JLabel status=new JLabel("Carregando personagens locais...");
    final Preview preview=new Preview();
    final JLabel name=label("Seu próximo destino",22), details=label("Selecione um personagem",13);
    final JLabel resources=label(" ",13), style=label(" ",12);
    final JLabel empty=label("Nenhum personagem local",14);
    private volatile BufferedImage backdrop;

    /** Chamado pelo worker do catalogo; arte opcional nunca bloqueia os saves. */
    void loadBackdrop(){
        try{SpiritTheme.reducedMotion=!RpgPreferences.load(RpgMenu.dataHome().resolve("saves/preferences.properties")).environment();}catch(java.io.IOException|IllegalArgumentException ignored){/* Preferências inválidas continuam preservadas. */}
        try(var in=getClass().getResourceAsStream(SpiritGateEntry.ART)){if(in!=null)backdrop=ImageIO.read(in);}catch(java.io.IOException ignored){/* Composicao vetorial permanece disponivel. */}
    }

    CharacterSelection() {
        super(new BorderLayout(12,12));setBackground(INK);setBorder(BorderFactory.createEmptyBorder(14,18,14,18));
        JPanel heading=new JPanel();heading.setLayout(new BoxLayout(heading,BoxLayout.Y_AXIS));heading.setOpaque(false);
        heading.add(label("SPIRIT GATE | PERSONAGENS LOCAIS",22));
        heading.add(label("O LIMIAR ENTRE DOIS MUNDOS  /  JORNADA OFFLINE",10));add(heading,BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.setBackground(INK);list.setForeground(Color.WHITE);
        list.setFixedCellHeight(80);list.setCellRenderer(new CharacterCard());list.setVisibleRowCount(3);
        list.getAccessibleContext().setAccessibleName("Personagens locais");
        JScrollPane scroll=new JScrollPane(list);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.setMinimumSize(new Dimension(0,0));
        scroll.getViewport().setBackground(INK);scroll.getVerticalScrollBar().setUnitIncrement(24);
        JPanel roster=panel(new BorderLayout(0,8));roster.add(label("SEUS PERSONAGENS",12),BorderLayout.NORTH);
        JPanel listArea=new JPanel(new BorderLayout());listArea.setOpaque(false);listArea.add(scroll);empty.setHorizontalAlignment(SwingConstants.CENTER);listArea.add(empty,BorderLayout.SOUTH);roster.add(listArea);
        JPanel right=panel(new BorderLayout(0,4));right.add(preview);right.add(preview.orientation(),BorderLayout.NORTH);
        JPanel summary=new JPanel();summary.setLayout(new BoxLayout(summary,BoxLayout.Y_AXIS));summary.setOpaque(false);
        for(JLabel line:new JLabel[]{name,details,resources,style}){summary.add(line);summary.add(Box.createVerticalStrut(4));}right.add(summary,BorderLayout.SOUTH);
        JPanel center=new JPanel(new GridLayout(1,2,12,0));center.setOpaque(false);center.add(roster);center.add(right);add(center);
        JPanel south=new JPanel(new BorderLayout(0,8));south.setOpaque(false);
        JPanel buttons=new JPanel(new GridLayout(0,2,8,6));buttons.setOpaque(false);
        for(JButton button:new JButton[]{play,create,delete,techniques,back})buttons.add(button);
        south.add(buttons);status.setForeground(CYAN);status.setFont(new Font("SansSerif",Font.PLAIN,11));south.add(status,BorderLayout.SOUTH);add(south,BorderLayout.SOUTH);
        play.setMnemonic('J');create.setMnemonic('C');back.setMnemonic('V');delete.setMnemonic('A');techniques.setMnemonic('T');
        play.setEnabled(false);delete.setEnabled(false);create.setEnabled(false);techniques.setEnabled(false);
        list.addListSelectionListener(e->{var slot=list.getSelectedValue();play.setEnabled(slot!=null&&slot.state()!=null);techniques.setEnabled(slot!=null&&slot.state()!=null);delete.setEnabled(slot!=null&&!slot.backup());preview.slot=slot;updateSummary(slot);preview.repaint();});
        techniques.addActionListener(e->{var slot=list.getSelectedValue();if(slot!=null&&slot.state()!=null){var state=slot.state();DomainPreview.open(SwingUtilities.getWindowAncestor(this),state.identity().race(),RpgWorld.restore(state).level(),DomainProgression.read(state.flags()).domain(),null);}});
        list.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"play");list.getActionMap().put("play",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){play.doClick();}});
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"),"back");getActionMap().put("back",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){back.doClick();}});
    }
    static JLabel label(String text,int size){JLabel l=new JLabel(text);l.setFont(new Font(size>=20?"Serif":"SansSerif",size>=20?Font.BOLD:Font.PLAIN,size));l.setForeground(size>=20?new Color(227,248,255):CYAN);return l;}
    static JPanel panel(LayoutManager layout){JPanel p=new JPanel(layout);p.setBackground(new Color(5,19,33));p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(40,103,125)),BorderFactory.createEmptyBorder(10,10,10,10)));return p;}
    static String raceName(RpgSave s){return CharacterRaces.get(s.identity().race()).name();}
    static String describe(CharacterSaves.Slot slot){if(slot.state()==null)return "Save inválido | "+slot.file().getFileName();var s=slot.state();return s.identity().name()+" | "+raceName(s)+" | Nível "+RpgWorld.restore(s).level()+(slot.backup()?" | Backup recuperável":"");}
    void updateSummary(CharacterSaves.Slot slot){
        name.setText(slot==null?"Seu próximo destino":slot.state()==null?"Arquivo preservado":slot.state().identity().name());
        details.setText(slot==null?"Selecione um personagem":slot.state()==null?"Save inválido":raceName(slot.state())+"  •  Nível "+RpgWorld.restore(slot.state()).level());
        resources.setText(" ");style.setText(" ");
        if(slot!=null&&slot.state()!=null){
            var view=RpgWorld.restore(slot.state()).snapshot();var combat=RpgCombatStatus.from(view);
            resources.setText("HP "+combat.hp()+" / "+combat.maxHp()+"   •   Vigor "+combat.stamina()/10+" / 100");
            var domain=DomainProgression.read(slot.state().flags());
            String mastery=domain.domain().equals("none")?"Aprendiz • Domínio no nível 15":Domains.get(domain.domain()).name()+"<br>XP de domínio "+DomainPreview.formatXp(domain.xpHalfUnits());
            style.setText("<html>XP "+view.state().xp()+" • Ataque "+view.attack()+" • Defesa "+view.defense()+"<br>"+mastery+"</html>");
        }
        for(JLabel line:new JLabel[]{name,details,resources,style})line.setToolTipText(line.getText());
    }
    void loaded(java.util.List<CharacterSaves.Slot> slots){model.clear();slots.forEach(model::addElement);empty.setVisible(slots.isEmpty());create.setEnabled(true);status.setText(slots.isEmpty()?"Nenhum personagem local. Crie seu primeiro personagem.":"Selecione um personagem. Saves inválidos e backups são preservados.");status.setToolTipText(status.getText());if(!slots.isEmpty())list.setSelectedIndex(0);}
    @Override protected void paintComponent(Graphics original){super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();try{
        g.setPaint(new GradientPaint(0,0,new Color(5,12,25),getWidth(),getHeight(),new Color(12,49,65)));g.fillRect(0,0,getWidth(),getHeight());
        if(backdrop!=null){double scale=Math.max((double)getWidth()/backdrop.getWidth(),(double)getHeight()/backdrop.getHeight());int w=(int)(backdrop.getWidth()*scale),h=(int)(backdrop.getHeight()*scale);g.drawImage(backdrop,(getWidth()-w)/2,(getHeight()-h)/2,w,h,null);g.setColor(new Color(2,10,23,195));g.fillRect(0,0,getWidth(),getHeight());}
        g.setColor(new Color(45,111,133,55));for(int i=0;i<8;i++){int x=i*getWidth()/7;g.fillRect(x,getHeight()/5,12,getHeight());g.drawLine(x-20,getHeight()/5,x+32,getHeight()/5);}
        g.setColor(new Color(96,217,241,100));g.drawLine(18,7,getWidth()-18,7);
    }finally{g.dispose();}}

    /** Card reutiliza o mesmo contrato de sprite frontal da previa. */
    final class CharacterCard extends JPanel implements ListCellRenderer<CharacterSaves.Slot> {
        final JLabel title=label("",15), subtitle=label("",12), note=label("",11);
        final JComponent portrait=new JComponent(){protected void paintComponent(Graphics original){Graphics2D g=(Graphics2D)original.create();try{g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(16,49,65));g.fillOval(2,2,48,58);if(slot!=null&&slot.state()!=null){g.translate(26,53);g.scale(.40,.40);Preview.drawActor(g,slot,preview.sprites,0,0);}else{g.setColor(CYAN);g.drawString("?",23,32);}}finally{g.dispose();}}};
        CharacterSaves.Slot slot;
        CharacterCard(){super(new BorderLayout(8,0));portrait.setPreferredSize(new Dimension(54,62));add(portrait,BorderLayout.WEST);JPanel text=new JPanel(new GridLayout(0,1));text.setOpaque(false);text.add(title);text.add(subtitle);text.add(note);add(text);}
        public Component getListCellRendererComponent(JList<? extends CharacterSaves.Slot> l,CharacterSaves.Slot value,int index,boolean selected,boolean focus){
            slot=value;setBackground(selected?new Color(18,66,85):new Color(8,25,40));
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(focus?Color.WHITE:selected?CYAN:new Color(26,60,78),focus?2:1),BorderFactory.createEmptyBorder(5,6,5,6)));
            title.setText(value.state()==null?"Save inválido":value.state().identity().name());
            subtitle.setText(value.state()==null?value.file().getFileName().toString():raceName(value.state())+" • Nível "+RpgWorld.restore(value.state()).level());
            note.setText(value.backup()?"Backup recuperável":value.state()==null?"Arquivo preservado":selected?"◆ Selecionado":"Personagem local");
            setToolTipText(value.problem().isEmpty()?describe(value):value.problem());return this;
        }
    }
    static final class SpiritButton extends JButton {
        final boolean primary;
        SpiritButton(String text,boolean primary){super(text);this.primary=primary;setContentAreaFilled(false);setFocusPainted(false);setForeground(new Color(231,249,255));setFont(new Font("SansSerif",primary?Font.BOLD:Font.PLAIN,13));setBorder(BorderFactory.createEmptyBorder(8,8,8,8));setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}
        @Override protected void paintComponent(Graphics original){Graphics2D g=(Graphics2D)original.create();try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            if(!isEnabled())g.setComposite(AlphaComposite.SrcOver.derive(.4f));
            g.setPaint(new GradientPaint(0,0,primary?new Color(27,119,147):new Color(17,48,65),0,getHeight(),INK));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
            g.setColor(primary||getModel().isRollover()?CYAN:new Color(48,111,133));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);
            if(getModel().isPressed()){g.setColor(new Color(0,0,0,70));g.fillRoundRect(1,1,getWidth()-2,getHeight()-2,8,8);}
            if(hasFocus()){g.setColor(Color.WHITE);g.setStroke(new BasicStroke(2));g.drawRoundRect(3,3,getWidth()-7,getHeight()-7,6,6);}
        }finally{g.dispose();}super.paintComponent(original);}
    }
    static final class Preview extends JPanel {
        CharacterSaves.Slot slot;RpgPlayerSprites sprites;
        com.projetok.prototype.DirectionalAnimation.Direction direction=com.projetok.prototype.DirectionalAnimation.Direction.FRONT;
        JButton orientation(){JButton turn=new SpiritButton("Alternar orientação",false);turn.addActionListener(e->{var directions=com.projetok.prototype.DirectionalAnimation.Direction.values();direction=directions[(direction.ordinal()+1)%directions.length];turn.setToolTipText(direction.toString());repaint();});return turn;}
        final Timer idle=new Timer(40,e->{if(!SpiritTheme.reducedMotion)repaint();});
        Preview(){setOpaque(false);setPreferredSize(new Dimension(260,260));setMinimumSize(new Dimension(0,0));}
        @Override public void addNotify(){super.addNotify();idle.start();}
        @Override public void removeNotify(){idle.stop();super.removeNotify();}
        @Override protected void paintComponent(Graphics original){super.paintComponent(original);Graphics2D g=(Graphics2D)original.create();try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),x=w/2,y=(int)(h*.82);float radius=Math.max(1,Math.min(w,h)*.7f);
            g.setPaint(new RadialGradientPaint(x,h*.5f,radius,new float[]{0,1},new Color[]{new Color(18,83,109),new Color(5,19,33)}));g.fillRect(0,0,w,h);
            int rw=Math.min(w-20,170),rh=Math.max(20,(int)(h*.78));
            for(int i=8;i>0;i--){g.setColor(new Color(56,201,235,12));g.setStroke(new BasicStroke(i*2));g.drawOval(x-rw/2,8,rw,rh);}
            g.setColor(new Color(83,193,216,130));g.setStroke(new BasicStroke(1));g.drawOval(x-rw/2,8,rw,rh);
            int base=Math.min(170,w-16);g.setColor(new Color(49,182,212,40));g.fillOval(x-base/2,y-9,base,26);g.setColor(CYAN);g.drawOval(x-base/2,y-9,base,26);g.drawOval(x-base/2+9,y-5,base-18,18);
            if(slot==null||slot.state()==null){g.setColor(CYAN);String text=slot==null?"Sua jornada aguarda":"Arquivo preservado";g.drawString(text,Math.max(4,(w-g.getFontMetrics().stringWidth(text))/2),Math.max(35,h/2));return;}
            int breath=SpiritTheme.reducedMotion?0:(int)Math.round(Math.sin(System.nanoTime()/650000000.0)*2);
            // A escala adapta apenas a apresentacao, sem modificar os assets.
            double scale=Math.max(.35,Math.min(1.7,(h-25)/140.0));g.translate(x,y+breath);g.scale(scale,scale);
            if(sprites==null||!sprites.drawPreview(g,direction,0,0))drawActor(g,slot,null,0,0);
        }finally{g.dispose();}}
        static void drawActor(Graphics2D g,CharacterSaves.Slot slot,RpgPlayerSprites sprites,int x,int y){
            if(sprites!=null&&sprites.drawPreview(g,com.projetok.prototype.DirectionalAnimation.Direction.FRONT,x,y))return;
            Color tint=slot.state().identity().appearance().equals("amber")?new Color(230,177,65):CYAN;
            g.setColor(new Color(5,16,30));g.fillPolygon(new int[]{x,x-30,x-22,x+22,x+30},new int[]{y-68,y-6,y+2,y+2,y-6},5);
            g.setColor(tint);g.fillOval(x-13,y-94,26,28);g.fillPolygon(new int[]{x,x-24,x+24},new int[]{y-64,y-8,y-8},3);
            g.setColor(new Color(223,247,255));g.drawLine(x,y-58,x,y-15);g.drawOval(x-15,y-96,30,32);
        }
    }
}
