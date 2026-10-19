package com.projetok.rpg;
import java.io.*;
import java.util.Properties;
/** XP cumulativa triangular; parametros RPG independentes da arena. */
public record RpgProgression(int cap,int step,int hpPerLevel,int attackPerLevel) {
    public RpgProgression { if(cap<2||cap>20||step<1||step>1000||hpPerLevel<0||hpPerLevel>100||attackPerLevel<0||attackPerLevel>20) throw new IllegalArgumentException("Progressao invalida"); }
    public static RpgProgression builtin() {
        try(InputStream in=RpgProgression.class.getResourceAsStream("/rpg/progression.properties")) {
            if(in==null) throw new IOException("Progressao ausente"); Properties p=new Properties();p.load(in);
            return new RpgProgression(Integer.parseInt(p.getProperty("cap")),Integer.parseInt(p.getProperty("step")),Integer.parseInt(p.getProperty("hpPerLevel")),Integer.parseInt(p.getProperty("attackPerLevel")));
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    public long threshold(int level) { if(level<1||level>cap) throw new IllegalArgumentException("Nivel invalido"); return (long)step*level*(level-1)/2; }
    public int level(long xp) { if(xp<0||xp>1_000_000) throw new IllegalArgumentException("XP invalida"); int level=1;while(level<cap&&xp>=threshold(level+1))level++;return level; }
    public long award(long xp,long amount) { level(xp);if(amount<0||amount>1_000_000)throw new IllegalArgumentException("Recompensa invalida");return Math.min(threshold(cap),xp+amount); }
}
