package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.movement.*;
import com.projetok.multiplayer.Protocol;
import com.projetok.prototype.DebugSnapshot;
import com.projetok.prototype.rpg.MobSprites;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/** Arena local descartavel com a mesma IA dos mobs do jogo; nao acessa saves ou publicacao. */
public final class MobPlaytest extends JPanel {
    private final CollisionService.Space space=new CollisionService.Space(960,640,java.util.List.of(new CollisionService.Obstacle(380,260,160,60)),java.util.List.of());
    private final EnemyService enemies;
    private final MobSprites sprites=new MobSprites();
    private final Set<Integer> keys=new HashSet<>();
    private Position player=new Position(480,570);
    private final GameEntity playerBody=new GameEntity(1,"Teste",player){};
    private final MovementService movement=new MovementService(new CollisionService());
    private long tick,lastPunch=-30;
    private int damage;
    private final javax.swing.Timer timer=new javax.swing.Timer(33,e->step());

    /** Distribui os seis tipos em pontos livres, usando exclusivamente os assets e a IA importados. */
    public MobPlaytest() {
        var spawns=new TreeMap<String,String>();
        for(int i=0;i<MobSprites.TYPES.size();i++)spawns.put("preview_"+i,"mob_"+MobSprites.TYPES.get(i)+"|forest|"+(180+i%3*300)+"|"+(160+i/3*240));
        enemies=new EnemyService(RpgContent.builtin(),space,spawns,r->space);
        setPreferredSize(new Dimension(960,640));setFocusable(true);
        addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){keys.add(e.getKeyCode());}public void keyReleased(KeyEvent e){keys.remove(e.getKeyCode());}});
        addFocusListener(new FocusAdapter(){public void focusLost(FocusEvent e){keys.clear();}});
    }
    @Override public void addNotify(){super.addNotify();timer.start();requestFocusInWindow();}
    @Override public void removeNotify(){timer.stop();super.removeNotify();}
    /** Executa movimento, carga, alcance e dano em ticks; o jogador de teste e imortal. */
    private void step() {
        tick++;
        float dx=(keys.contains(KeyEvent.VK_D)||keys.contains(KeyEvent.VK_RIGHT)?1:0)-(keys.contains(KeyEvent.VK_A)||keys.contains(KeyEvent.VK_LEFT)?1:0);
        float dy=(keys.contains(KeyEvent.VK_S)||keys.contains(KeyEvent.VK_DOWN)?1:0)-(keys.contains(KeyEvent.VK_W)||keys.contains(KeyEvent.VK_UP)?1:0);
        movePlayer(dx,dy);
        if(keys.contains(KeyEvent.VK_R)){
            // Reposicionar tambem respeita mobs e paredes, sem criar sobreposicao.
            var occupied=occupied(false);
            for(float x:new float[]{480,440,520,400,560})if(new CollisionService().isFree(playerBody,x,570,12,occupied)){
                player=new Position(x,570);playerBody.setPosition(player);damage=0;break;
            }
        }
        if(keys.contains(KeyEvent.VK_SPACE)&&tick-lastPunch>=15){
            lastPunch=tick;
            enemies.current("forest").stream().filter(e->e.hp>0&&Math.hypot(e.getPosition().x()-player.x(),e.getPosition().y()-player.y())<=65)
                .min(Comparator.comparingDouble(e->Math.hypot(e.getPosition().x()-player.x(),e.getPosition().y()-player.y())))
                .ifPresent(e->{e.hp=Math.max(0,e.hp-25);if(e.hp==0)e.readyTick=tick+90;});
        }
        enemies.respawn(tick,"forest",occupied(true));
        enemies.update("forest",tick,player,true,movement,occupied(true),n->damage+=n,q->RpgSight.clear(player,q,space),true);
        var actors=new ArrayList<Protocol.Actor>();actors.add(new Protocol.Actor(1,"Teste",player.x(),player.y(),100,100,0,0,false));
        for(var e:enemies.views("forest"))actors.add(new Protocol.Actor(1_000_000+e.id(),e.name(),e.x(),e.y(),e.hp(),e.maxHp(),0,0,true,1,0,e.state().equals("PREPARE")||e.state().equals("ATTACK")?1:0));
        sprites.update(new Protocol.State(tick,actors));repaint();
    }
    /** Usa varredura continua e corpos reais para impedir atravessar mobs, paredes e bordas. */
    void movePlayer(float dx,float dy) {
        playerBody.setPosition(player);
        var direction=movement.direction(dx,dy);
        movement.moveContinuous(playerBody,direction.x()*4,direction.y()*4,12,occupied(false));
        player=playerBody.getPosition();
    }
    /** Compartilha corpos vivos entre movimento e respawn; mortos deixam de bloquear. */
    private CollisionService.Space occupied(boolean includePlayer) {
        var bodies=new ArrayList<CollisionService.Body>();
        if(includePlayer)bodies.add(new CollisionService.Body(playerBody,12));
        for(var e:enemies.current("forest"))if(e.hp>0)bodies.add(new CollisionService.Body(e,19));
        return new CollisionService.Space(space.width(),space.height(),space.obstacles(),bodies);
    }
    @Override protected void paintComponent(Graphics original) {
        super.paintComponent(original);var g=(Graphics2D)original.create();
        try {
            g.scale(getWidth()/960.0,getHeight()/640.0);g.setColor(new Color(23,42,48));g.fillRect(0,0,960,640);
            g.setColor(new Color(37,62,67));for(int x=0;x<960;x+=32)g.drawLine(x,0,x,640);for(int y=0;y<640;y+=32)g.drawLine(0,y,960,y);
            g.setColor(new Color(78,98,111));for(var wall:space.obstacles())g.fillRect((int)wall.x(),(int)wall.y(),(int)wall.width(),(int)wall.height());
            for(var e:enemies.views("forest")) {
                sprites.draw(g,new DebugSnapshot.ActorView(1_000_000+e.id(),e.name(),e.x(),e.y(),19,e.hp(),e.maxHp(),false,e.hp()>0));
                g.setColor(Color.CYAN);g.fillRect((int)e.x()-24,(int)e.y()+8,e.hp()*48/e.maxHp(),3);
            }
            g.setColor(Color.ORANGE);g.fillOval((int)player.x()-12,(int)player.y()-28,24,28);
            sprites.effects(g);g.setColor(Color.WHITE);
            g.drawString("MOBS / CERO — WASD ou setas: mover | ESPACO: golpe | R: reposicionar | jogador imortal",20,25);
            g.drawString("Dano recebido: "+damage+" | Mobs: velocidade 80 | Colisao com jogador, mobs e obstaculo central.",20,45);
        }finally{g.dispose();}
    }
    /** Abre apenas o playtest solicitado; nenhum servidor ou banco externo e iniciado. */
    public static void main(String[] args) {
        com.projetok.diagnostics.ErrorReports.install("playtest-mobs");
        SwingUtilities.invokeLater(()->{var frame=new JFrame("Spirit Gate | Teste dos seis mobs e Cero");frame.setContentPane(new MobPlaytest());frame.pack();frame.setLocationRelativeTo(null);frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);frame.setVisible(true);});
    }
}
