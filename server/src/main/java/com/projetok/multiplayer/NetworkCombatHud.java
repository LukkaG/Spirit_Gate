package com.projetok.multiplayer;

import java.awt.*;
import javax.swing.*;

/** Barras de recursos reais publicadas pelo servidor; atualização somente na EDT. */
public final class NetworkCombatHud extends JPanel {
    private final JProgressBar hp=new JProgressBar(),stamina=new JProgressBar(0,1000);
    private final JLabel state=new JLabel("Aguardando estado do servidor");
    private boolean overlayStyle,initialized,reducedMotion;
    private double shownHp,shownStamina;
    private GameplayProtocol.Energy energyProfile;
    private boolean energyPending;
    public void energy(GameplayProtocol.Energy profile,boolean pending){energyProfile=profile;energyPending=pending;}
    /** A apresentação nova usa os mesmos modelos de recursos, sem substituir seus valores. */
    public void overlayStyle(){overlayStyle=true;removeAll();setOpaque(false);setBorder(null);}
    public void reducedMotion(boolean value){reducedMotion=value;}
    public NetworkCombatHud() {
        super(new GridLayout(1,3,12,0));
        hp.setStringPainted(true);stamina.setStringPainted(true);
        hp.setForeground(new Color(70,180,120));stamina.setForeground(new Color(65,155,225));
        add(hp);add(stamina);add(state);setBorder(BorderFactory.createEmptyBorder(5,8,5,8));
    }
    public void update(Protocol.State snapshot,int self,NetworkCombatPresentation presentation) {
        var actor=snapshot==null?null:snapshot.actors().stream().filter(a->a.id()==self).findFirst().orElse(null);
        if(actor==null){hp.setValue(0);hp.setString("HP: aguardando");stamina.setValue(0);stamina.setString("Energia: aguardando");state.setText("Sem personagem nesta região");initialized=false;shownHp=shownStamina=0;repaint();return;}
        hp.setMaximum(actor.maxHp());hp.setValue(actor.hp());hp.setString("HP "+actor.hp()+" / "+actor.maxHp());
        int maximum=energyProfile==null?1000:energyProfile.maximum(),current=energyProfile==null?Math.round(actor.stamina()):energyProfile.currentFor(snapshot,self);
        stamina.setMaximum(maximum);stamina.setValue(current);stamina.setString(energyPending?"Energia: sincronizando":"Energia "+Math.round(current/10f)+" / "+maximum/10);
        state.setText(actor.hp()==0?"Derrotado — R para retornar":
            ((actor.flags()&1)!=0?"Ataque em recuperação":"Ataque disponível")+
            (presentation.target()==0?" | Sem alvo":" | Alvo "+presentation.target()));
        setToolTipText(state.getText());getAccessibleContext().setAccessibleDescription(hp.getString()+"; "+stamina.getString()+"; "+state.getText());
        double targetHp=actor.hp()/(double)actor.maxHp(),targetStamina=current/(double)maximum;
        if(!initialized||reducedMotion){shownHp=targetHp;shownStamina=targetStamina;initialized=true;}
        else {shownHp+=(targetHp-shownHp)*.18;shownStamina+=(targetStamina-shownStamina)*.18;}
        repaint();
    }
    @Override protected void paintComponent(Graphics original){
        if(!overlayStyle){super.paintComponent(original);return;}
        var g=GameHudStyle.graphics(original);try {
            int gap=20,left=4,bar=Math.max(20,(getWidth()-8-gap)/2),right=left+bar+gap;
            resource(g,left,22,bar,hp.getString(),shownHp,new Color(187,60,91),new Color(250,134,158));
            resource(g,right,22,bar,stamina.getString(),shownStamina,new Color(24,129,167),GameHudStyle.CYAN);
        }finally{g.dispose();}
    }
    private static void resource(Graphics2D g,int x,int y,int width,String text,double value,Color dark,Color light){
        g.setColor(new Color(3,13,24,230));g.fillRoundRect(x,y,width,16,6,6);
        int fill=Math.max(0,Math.min(width-4,(int)Math.round((width-4)*value)));
        g.setPaint(new GradientPaint(x,y,dark,x+width,y,light));g.fillRoundRect(x+2,y+2,fill,12,4,4);
        g.setColor(new Color(100,186,207,180));g.drawRoundRect(x,y,width,16,6,6);
        g.setFont(new Font("SansSerif",Font.BOLD,11));g.setColor(GameHudStyle.TEXT);g.drawString(text,x+3,y-6);
        if(fill>6){g.setColor(new Color(240,255,255,90));g.drawLine(x+5,y+3,x+fill-1,y+3);}
    }
    public String healthText(){return hp.getString();}
    public String energyText(){return stamina.getString();}
    public String stateText(){return state.getText();}
}
