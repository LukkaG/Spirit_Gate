package com.projetok.multiplayer;

import com.projetok.rpg.RpgSkillCatalog;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Seleção e mira das técnicas; o servidor decide alvo, custo, colisão e resultado. */
final class NetworkSkillControls {
    private final LocalClient client;
    private final GameHudStyle.Button[] buttons=new GameHudStyle.Button[5];
    private final JLabel[] descriptions=new JLabel[5];
    private final JButton[] detailButtons=new JButton[5];
    private final JLabel notice=new JLabel("Escolha um domínio no nível 15 para liberar suas técnicas.");
    private final JLabel activeState=new JLabel();
    private final JLabel recipient=new JLabel("TAB ou clique: selecionar alvo. Sem alvo, a cura é em você.");
    private final JPanel details=new JPanel(new BorderLayout(6,6));
    private String[] keys={"F5","F6","F7","F8","F9"};
    private List<RpgSkillCatalog.Definition> definitions=List.of();
    private RpgSkillCatalog.Definition pending;
    private Runnable startAiming=()->{};
    private Runnable finishAiming=()->{};
    private final Set<Integer> aimKeys=new HashSet<>();
    private long lastAimNanos;
    private float aimX=Float.NaN,aimY=Float.NaN;
    private String context="";
    private NetworkSkillPresentation presentation;
    private String assetError="";
    private com.projetok.domain.movement.CollisionService.Space geometry;
    private final NetworkSkillSubmission submission=new NetworkSkillSubmission();
    private boolean aimClamped;
    private int inspectedSlot;
    private final String[] availabilityLabels=new String[5];
    private String targetHint="",inputHint="TAB no mundo: alvos · SHIFT+TAB: navegar na HUD · ENTER: usar controle";

    /** Prepara os cinco slots sem criar outro sistema de janelas. */
    NetworkSkillControls(LocalClient client) {
        this.client=client;
        var rows=new JPanel(new GridLayout(0,1,3,6));
        for(int index=0;index<5;index++){
            final int slot=index+1;
            buttons[index]=new GameHudStyle.Button("hudDomainSkill"+slot,"Técnica "+slot,GameHudStyle.Glyph.SKILLS,()->activate(slot));
            buttons[index].setEnabled(false);buttons[index].visual(false,keys[index],0);
            buttons[index].addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseEntered(java.awt.event.MouseEvent e){inspectedSlot=slot;}});
            buttons[index].addFocusListener(new java.awt.event.FocusAdapter(){@Override public void focusGained(java.awt.event.FocusEvent e){inspectedSlot=slot;}});
            descriptions[index]=new JLabel("<html>"+keys[index]+" · Técnica "+slot+" — escolha um domínio</html>");
            var row=new JPanel(new BorderLayout(6,0));row.add(descriptions[index]);
            var activate=new JButton("Usar");detailButtons[index]=activate;activate.setEnabled(false);activate.setName("domainSkillUse"+slot);activate.addActionListener(e->activate(slot));row.add(activate,BorderLayout.EAST);rows.add(row);
        }
        details.add(rows);
        var targeting=new JPanel(new GridLayout(0,1,3,3));targeting.add(recipient);
        targeting.add(new JLabel("Linha/cone: lançamento frontal. Área: mouse/WASD/setas; ENTER/mesma tecla confirma."));
        targeting.add(new JLabel("ESC ou botão direito cancela a seleção de área."));targeting.add(activeState);targeting.add(notice);details.add(targeting,BorderLayout.SOUTH);
    }
    JComponent details(){return details;}
    GameHudStyle.Button[] buttons(){return buttons.clone();}
    void onAim(Runnable action){startAiming=Objects.requireNonNull(action);}
    void onAimFinish(Runnable action){finishAiming=Objects.requireNonNull(action);}
    void presentation(NetworkSkillPresentation value){presentation=value;}
    /** Geometria recebida da revisão ativa; null significa que a prévia não conhece o cenário. */
    void geometry(com.projetok.domain.movement.CollisionService.Space value){geometry=value;}
    String feedbackText(){return notice.getText();}
    String targetHint(){return targetHint;}
    String inputHint(){return inputHint;}
    /** Expõe falhas de carregamento também na janela do jogador, além do diagnóstico técnico. */
    void assetError(String value){assetError=value;notice.setText("Sprites indisponíveis: "+value);}
    void shortcuts(Map<String,Integer> values){for(int index=0;index<5;index++){Integer code=values.get("domain-skill-"+(index+1));if(code!=null)keys[index]=java.awt.event.KeyEvent.getKeyText(code);}}
    boolean aiming(){return pending!=null;}
    /** Cancela apenas a mira local; nunca desfaz uma habilidade aceita. */
    boolean cancel(){if(pending==null)return false;pending=null;aimKeys.clear();finishAiming.run();notice.setText("Mira cancelada.");return true;}
    /** Movimento real do mouse substitui a mira atual; pintar a tela nunca a reposiciona. */
    void pointer(float x,float y){if(pending!=null&&Float.isFinite(x)&&Float.isFinite(y))position(x,y);}
    void aimKey(int code,boolean pressed){if(pressed)aimKeys.add(code);else aimKeys.remove(code);}
    /** Avança o retículo em unidades do mundo, sem enviar movimento de personagem. */
    void advanceAim(long now){
        double dt=lastAimNanos==0?0:Math.min(.05,Math.max(0,(now-lastAimNanos)/1_000_000_000.0));lastAimNanos=now;
        if(pending==null||aimKeys.isEmpty())return;
        float x=(key(java.awt.event.KeyEvent.VK_D,java.awt.event.KeyEvent.VK_RIGHT)?1:0)-(key(java.awt.event.KeyEvent.VK_A,java.awt.event.KeyEvent.VK_LEFT)?1:0);
        float y=(key(java.awt.event.KeyEvent.VK_S,java.awt.event.KeyEvent.VK_DOWN)?1:0)-(key(java.awt.event.KeyEvent.VK_W,java.awt.event.KeyEvent.VK_UP)?1:0);
        double length=Math.hypot(x,y);if(length>0)position((float)(aimX+x/length*220*dt),(float)(aimY+y/length*220*dt));
    }
    private boolean key(int a,int b){return aimKeys.contains(a)||aimKeys.contains(b);}
    private Protocol.Actor self(){var snapshot=client.snapshot();return snapshot==null?null:snapshot.actors().stream().filter(actor->actor.id()==client.id()).findFirst().orElse(null);}
    /** Limita o ponto ao alcance e ao apoio válido do mapa. */
    private void position(float x,float y){
        var actor=self();if(actor==null||pending==null)return;double dx=x-actor.x(),dy=y-actor.y(),distance=Math.hypot(dx,dy),scale=distance>pending.range()?pending.range()/distance:1;
        float width=geometry==null?960:geometry.width(),height=geometry==null?640:geometry.height();
        aimX=Math.max(18,Math.min(width-18,(float)(actor.x()+dx*scale)));aimY=Math.max(18,Math.min(height-18,(float)(actor.y()+dy*scale)));
        aimClamped=distance>pending.range()||Math.abs(aimX-x)>.01||Math.abs(aimY-y)>.01;
    }
    float aimX(){return aimX;}
    float aimY(){return aimY;}

    /** Atualiza recargas e destinatários exclusivamente de snapshots recebidos. */
    void refresh(GameplayProtocol.Sheet sheet,float energy,boolean alive) {
        var state=client.skills();
        if(sheet==null||state==null||!client.connected()||!alive){cancel();submission.clear();notice.setText(!client.connected()?"Desconectado: preparação cancelada; resultado anterior indisponível.":!alive?"Personagem derrotado: conjuração indisponível.":"Aguardando sincronização das habilidades.");targetHint="Alvo indisponível neste contexto.";for(var button:buttons){button.setEnabled(false);button.visual(false,"—",0);}for(var button:detailButtons)button.setEnabled(false);activeState.setText("");return;}
        String nextContext=state.hash()+":"+client.region()+":"+sheet.progress().domain();
        if(!nextContext.equals(context)){cancel();submission.clear();context=nextContext;notice.setText("Prévia local: envio não garante aceite. O servidor valida alcance, obstáculos e estado.");}
        submission.refresh(state,System.nanoTime());
        if(!aiming()&&!submission.message().isEmpty())notice.setText(submission.message());
        definitions=RpgSkillCatalog.forDomain(sheet.progress().domain());
        int selected=client.selectedTarget();var snapshot=client.snapshot();var target=snapshot==null?null:snapshot.actors().stream().filter(actor->actor.id()==selected).findFirst().orElse(null);
        var self=self();
        recipient.setText(target==null?"Sem alvo: curas e limpeza são em você.":"Alvo: "+target.name()+" · HP "+target.hp()+"/"+target.maxHp()+(selected<=1_000_000?(target.hp()==0?" · aliado derrotado (revive)":" · aliado"):" · inimigo"));
        targetHint=target==null?"TAB no viewport ou clique: selecionar alvo.":NetworkSkillPreview.sight(self,target.x(),target.y(),geometry)==NetworkSkillPreview.Sight.BLOCKED?"Possível obstáculo na visão (prévia).":"Alcance depende da skill; passe o mouse por um slot.";
        activeState.setText(state.sentryShots()>0?"Sentinela: "+state.sentryShots()+"s · SPACE dispara · conjurar novamente cancela":state.windCheckpoint()?"Vendaval: retorno disponível":"");
        for(int index=0;index<5;index++){
            var definition=index<definitions.size()?definitions.get(index):null;var button=buttons[index];
            if(definition==null){button.setEnabled(false);detailButtons[index].setEnabled(false);button.setToolTipText(null);detailButtons[index].setToolTipText(null);descriptions[index].setToolTipText(null);button.visual(false,"NV15",0);descriptions[index].setText("<html>"+keys[index]+" · Técnica "+(index+1)+" — escolha um domínio</html>");continue;}
            int cooldown=state.cooldowns().get(index);
            boolean checkpoint=definition.id().equals("wind_5")&&!state.windCheckpoint();
            boolean low=energy<definition.energyCost()*10;
            boolean locked=sheet.progress().level()<15;
            var hint=NetworkSkillPreview.target(definition,self,target,selected,geometry);
            boolean targetMissing=!hint.targetValid();
            boolean sentryActive=definition.mechanics().pattern()==RpgSkillCatalog.Pattern.SENTRY&&state.sentryShots()>0;
            boolean ready=sentryActive||!locked&&!low&&!checkpoint&&!targetMissing&&cooldown==0;
            button.setEnabled(ready);detailButtons[index].setEnabled(ready);
            String availability=locked?"Requer nível "+com.projetok.rpg.DomainProgression.INITIATION_LEVEL:checkpoint?"Use Vendaval primeiro":low?"Energia insuficiente":targetMissing?hint.text():cooldown>0?"Recarga: "+seconds(cooldown):hint.text();
            if(sentryActive)availability="Cancelar Sentinela";
            availabilityLabels[index]=availability;
            String tooltip=tooltip(definition,keys[index],availability);
            button.setToolTipText(tooltip);detailButtons[index].setToolTipText(tooltip);
            button.getAccessibleContext().setAccessibleName(definition.name()+". "+definition.description()+" "+availability);
            boolean awaiting=submission.waitingSlot()==index+1;
            String badge=sentryActive?"CANCELAR":locked?"NV15":low?"EN":targetMissing?"ALVO":cooldown>0?seconds(cooldown):checkpoint?"COMBO":awaiting?"ENVIO":hint.warning()?hint.badge():keys[index];
            button.visual(sentryActive||pending!=null&&pending.slot()==index+1,sentryActive?"CANCELAR":badge,cooldown/(float)definition.cooldownTicks());
            if(inspectedSlot==index+1)targetHint=keys[index]+" · "+definition.name()+": "+availability;
            if(presentation!=null){var clip=presentation.clip(definition.id());if(clip!=null)button.putClientProperty("spirit.skillIcon",clip.icon());}
            descriptions[index].setText("<html><b>"+keys[index]+" · "+definition.name()+"</b> — "+definition.energyCost()+" EN · "+seconds(definition.cooldownTicks())+"<br>"+definition.description()+"</html>");
            descriptions[index].setToolTipText(tooltip);
        }
        if(aiming())notice.setText(areaHint()+" ENTER/"+keys[pending.slot()-1]+": enviar · ESC: cancelar.");
    }
    /** Ajuda completa também durante recarga, sem depender da disponibilidade para conjurar. */
    static String tooltip(RpgSkillCatalog.Definition skill,String key,String availability){
        String effects=com.projetok.rpg.RpgSkillDescriptions.effects(skill);
        return "<html><table width='350'><tr><td><b>"+html(key+" · "+skill.name())+"</b><br>"
                +html(skill.description())+"<br><br>"+html(com.projetok.rpg.RpgSkillDescriptions.activation(skill))
                +(effects.isEmpty()?"":"<br>"+html(effects))+"<br><br>"
                +skill.energyCost()+" energia · Recarga: "+com.projetok.rpg.RpgSkillDescriptions.seconds(skill.cooldownTicks())
                +"<br>Alcance: "+skill.range()+" · Raio: "+skill.radius()+"<br><b>"+html(availability)+"</b></td></tr></table></html>";
    }
    /** Impede que nomes de atalhos ou mensagens sejam interpretados como marcação Swing. */
    private static String html(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    /** Arma a mira ou envia uma única intenção; resultados nunca são antecipados na UI. */
    void activate(int slot) {
        if(pending!=null&&pending.slot()==slot){confirm();return;}
        if(slot<1||slot>5||definitions.size()<slot)return;
        if(!buttons[slot-1].isEnabled()){submission.clear();notice.setText(definitions.get(slot-1).name()+": "+availabilityLabels[slot-1]);return;}
        var definition=definitions.get(slot-1);cancel();
        switch(definition.targeting()){
            case GROUND->{submission.clear();startAiming.run();pending=definition;aimKeys.clear();lastAimNanos=System.nanoTime();var actor=self();if(actor!=null){var point=facingPoint(actor,Math.min(120,definition.range()*.55f));position(point.x,point.y);}notice.setText(definition.name()+": ajuste a área; ENTER ou a mesma tecla confirma.");}
            case LINE,CONE->{var actor=self();if(actor!=null){var point=facingPoint(actor,definition.range());send(definition,0,point.x,point.y);}}
            case SELF->send(definition,0,0,0);
            case ALLY,TARGET->send(definition,client.selectedTarget(),0,0);
        }
    }
    /** Smartcast preserva a direção confirmada pelo servidor e encurta o raio somente nas bordas. */
    static Point2D.Float facingPoint(Protocol.Actor actor,float range){
        var heading=com.projetok.rpg.RpgSkillEngine.eightDirections(new com.projetok.domain.entity.Position(actor.facingX(),actor.facingY()));
        float fx=heading.x(),fy=heading.y();
        float distance=range;if(fx>0)distance=Math.min(distance,(942-actor.x())/fx);else if(fx<0)distance=Math.min(distance,(18-actor.x())/fx);
        if(fy>0)distance=Math.min(distance,(622-actor.y())/fy);else if(fy<0)distance=Math.min(distance,(18-actor.y())/fy);
        return new Point2D.Float(actor.x()+fx*Math.max(0,distance),actor.y()+fy*Math.max(0,distance));
    }
    private void send(RpgSkillCatalog.Definition definition,int target,float x,float y){
        var before=client.skills();
        boolean sent=client.castSkill(definition.slot(),target,0,x,y);
        submission.sent(definition,before,client.id(),System.nanoTime(),sent);
        notice.setText(submission.message()+(assetError.isEmpty()?"":" Sprites locais indisponíveis."));
    }
    /** Confirma no ponto atual da câmera; bordas/obstáculos continuam sujeitos à validação do servidor. */
    boolean click(float x,float y){
        if(pending==null)return false;
        if(!Float.isFinite(x)||!Float.isFinite(y))return true;
        position(x,y);confirm();return true;
    }
    /** Confirma somente a área preparada e libera as teclas mantidas sem repeti-las no personagem. */
    void confirm(){if(pending==null)return;if(!client.connected()||client.skills()==null||self()==null||self().hp()<=0){cancel();notice.setText("Preparação cancelada: sessão ou personagem indisponível.");return;}var definition=pending;pending=null;aimKeys.clear();finishAiming.run();send(definition,0,aimX,aimY);}
    /** Texto do retículo é uma estimativa local, mesmo quando o cenário foi recebido integralmente. */
    private String areaHint(){var hint=NetworkSkillPreview.ground(self(),aimX,aimY,pending.range(),geometry);return (aimClamped?"Área limitada por alcance/bordas. ":"")+hint.text();}
    /** Telegraph local de alcance e área, independente da animação de acerto. */
    void draw(Graphics2D original){
        if(pending==null||!Float.isFinite(aimX)||!Float.isFinite(aimY))return;
        var snapshot=client.snapshot();var self=snapshot==null?null:snapshot.actors().stream().filter(a->a.id()==client.id()).findFirst().orElse(null);if(self==null)return;
        var g=(Graphics2D)original.create();
        try {
            double dx=aimX-self.x(),dy=aimY-self.y(),distance=Math.hypot(dx,dy),scale=distance>pending.range()&&distance>0?pending.range()/distance:1;
            float x=pending.targeting()==RpgSkillCatalog.Targeting.GROUND?aimX:(float)(self.x()+dx*scale),y=pending.targeting()==RpgSkillCatalog.Targeting.GROUND?aimY:(float)(self.y()+dy*scale);
            var hint=NetworkSkillPreview.ground(self,aimX,aimY,pending.range(),geometry);
            g.setColor(hint.warning()?new Color(255,196,104,190):new Color(112,225,244,155));g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Float(self.x()-pending.range(),self.y()-pending.range(),pending.range()*2,pending.range()*2));
            g.draw(new Rectangle2D.Float(18,18,(geometry==null?960:geometry.width())-36,(geometry==null?640:geometry.height())-36));
            if(pending.targeting()==RpgSkillCatalog.Targeting.GROUND){int radius=pending.radius();g.draw(new Ellipse2D.Float(x-radius,y-radius,radius*2,radius*2));g.drawLine((int)x-5,(int)y,(int)x+5,(int)y);g.drawLine((int)x,(int)y-5,(int)x,(int)y+5);}
            else if(pending.targeting()==RpgSkillCatalog.Targeting.CONE){double angle=Math.atan2(dy,dx),radius=pending.range();var cone=new Path2D.Double();cone.moveTo(self.x(),self.y());for(int i=0;i<=12;i++){double a=angle-Math.PI/6+i*Math.PI/36;cone.lineTo(self.x()+Math.cos(a)*radius,self.y()+Math.sin(a)*radius);}cone.closePath();g.draw(cone);}
            else {g.draw(new Line2D.Float(self.x(),self.y(),x,y));g.draw(new Ellipse2D.Float(x-5,y-5,10,10));}
        } finally {g.dispose();}
    }
    private static String seconds(int ticks){return String.format(Locale.ROOT,"%.1fs",ticks/30.0);}
}
