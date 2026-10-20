package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/** Aviso pontilhado provisorio com oclusao real; cache evita recalcular por repaint. */
public final class RpgBossOverlay {
    private RpgBossThreat threat;
    private Path2D.Float dots=new Path2D.Float();
    private String key="";
    /** Atualiza o tempo a cada snapshot e a geometria apenas quando necessario. */
    public void update(RpgWorld.View view) {
        threat=view.enemies().stream().filter(e->e.boss()&&e.hp()>0).map(EnemyService.View::threat).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if(threat==null){key="";dots=new Path2D.Float();return;}
        String next=threat.x()+":"+threat.y()+":"+threat.radius();if(key.equals(next))return;key=next;dots=new Path2D.Float();
        int radius=threat.radius(),cx=Math.round(threat.x()),cy=Math.round(threat.y());
        for(int y=cy-radius;y<=cy+radius;y+=6)for(int x=cx-radius;x<=cx+radius;x+=6)
            if(threat.contains(x,y)&&threat.contains(x+1,y)&&threat.contains(x,y+1)&&threat.contains(x+1,y+1))dots.append(new Rectangle2D.Float(x,y,2,2),false);
    }
    /** Mostra preparacao e alcance ocluido, sem conceder dano nem alterar IA. */
    public void draw(Graphics2D original) {
        if(threat==null)return;Graphics2D g=(Graphics2D)original.create();
        try {g.setColor(new Color(255,180,65,180));g.fill(dots);g.setColor(new Color(255,210,100));
            g.setFont(new Font("SansSerif",Font.BOLD,13));g.drawString("Preparando "+RpgCombatStatus.cooldown(threat.remainingTicks()),threat.x()-60,threat.y()-threat.radius()-10);
        } finally {g.dispose();}
    }
}
