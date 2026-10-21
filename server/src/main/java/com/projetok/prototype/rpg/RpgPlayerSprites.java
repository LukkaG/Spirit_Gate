package com.projetok.prototype.rpg;

import com.projetok.prototype.DebugPanel;
import com.projetok.prototype.DebugSnapshot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Separate experimental contract; does not relax or use the official 59-frame atlas. */
public final class RpgPlayerSprites implements DebugPanel.ActorRenderer {
    public static final String ENABLED="spiritgate.rpg.experimentalSprites";
    public static final String FRAME_MILLIS="spiritgate.rpg.spriteFrameMillis";
    public static final String SCALE="spiritgate.rpg.spriteScale";
    public static final int DEFAULT_FRAME_MILLIS=250;
    public static final double DEFAULT_SCALE=0.5;
    private record Frame(BufferedImage image,int pivotX,int pivotY) { }
    private final Map<String,Frame> frames;
    private final RpgPlayerAnimation animation;
    private final double scale;
    private final RpgVisualState visual=new RpgVisualState();
    private boolean spirit;
    private RpgPlayerSprites(Map<String,Frame> frames,int frameMillis,double scale) {
        this.frames=Map.copyOf(frames);animation=new RpgPlayerAnimation(frameMillis);this.scale=scale;
    }
    public static Path defaultFolder() { return RpgMenu.home().resolve("assets/dev/player-base"); }
    public static RpgPlayerSprites loadOptional(Path folder,int frameMillis) {
        return loadOptional(folder,frameMillis,DEFAULT_SCALE);
    }
    public static RpgPlayerSprites loadOptional(Path folder,int frameMillis,double scale) {
        try { return load(folder,frameMillis,scale); }
        catch(IOException|IllegalArgumentException error) {
            System.err.println("[Spirit Gate] Sprites RPG experimentais recusadas; fallback geometrico: "+error.getMessage());
            return null;
        }
    }
    public static RpgPlayerSprites configured() {
        if(!Boolean.getBoolean(ENABLED)) return null;
        try { return loadOptional(defaultFolder(),Integer.parseInt(System.getProperty(FRAME_MILLIS,""+DEFAULT_FRAME_MILLIS)),
                Double.parseDouble(System.getProperty(SCALE,""+DEFAULT_SCALE))); }
        catch(IllegalArgumentException error) {
            System.err.println("[Spirit Gate] Configuracao experimental invalida; fallback geometrico: "+error.getMessage());return null;
        }
    }
    public static RpgPlayerSprites load(Path folder,int frameMillis) throws IOException {
        return load(folder,frameMillis,DEFAULT_SCALE);
    }
    public static RpgPlayerSprites load(Path folder,int frameMillis,double scale) throws IOException {
        if(!Double.isFinite(scale)||scale<0.1||scale>1.0) throw new IllegalArgumentException("spriteScale: 0.1..1.0");
        Properties metadata=new Properties();
        try(var input=RpgPlayerSprites.class.getResourceAsStream("/rpg/experimental-player.properties")) {
            if(input==null) throw new IOException("Metadados experimentais ausentes");metadata.load(input);
        }
        Set<String> actual=new HashSet<>();
        try(var files=Files.list(folder)) { files.forEach(p->actual.add(p.getFileName().toString())); }
        Map<String,Frame> frames=new HashMap<>();
        for(var direction:com.projetok.prototype.DirectionalAnimation.Direction.values()) for(int index=0;index<5;index++) {
            String key=direction+"."+index;
            String[] fields=metadata.getProperty(key,"").split(",");
            if(fields.length!=6) throw new IOException("Metadados incompletos: "+key);
            String name=fields[0];
            if(!actual.contains(name)) throw new IOException("Arquivo ausente (nome exato): "+name);
            Path file=folder.resolve(name);
            if(Files.isSymbolicLink(file)||!Files.isRegularFile(file)||Files.size(file)>1_000_000) throw new IOException("Arquivo invalido: "+name);
            byte[] bytes=Files.readAllBytes(file);
            try {
                if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(fields[1]))
                    throw new IOException("SHA-256 divergente: "+name);
            } catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
            // Exact known PNG bytes are checked before ImageIO decodes or allocates pixels.
            BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes));
            int width=Integer.parseInt(fields[2]),height=Integer.parseInt(fields[3]);
            if(image==null || image.getWidth()!=width || image.getHeight()!=height || !image.getColorModel().hasAlpha())
                throw new IOException("Dimensoes/alfa invalidos: "+name);
            frames.put(key,new Frame(image,Integer.parseInt(fields[4]),Integer.parseInt(fields[5])));
        }
        return new RpgPlayerSprites(frames,frameMillis,scale);
    }
    public double scale() { return scale; }
    public RpgPlayerAnimation.Pose update(com.projetok.rpg.RpgWorld.View view) { spirit=view.identity().race().equals("spirit");var pose=animation.update(view);visual.update(view,pose.walking());return pose; }
    /** Estado visual sem influencia nas regras do mundo. */
    public RpgVisualState.State visualState(){return visual.state();}
    public RpgPlayerAnimation.Pose pose() { return animation.pose(); }
    public String frameKey() {
        var pose=animation.pose();return pose.direction()+"."+(pose.walking()?pose.frame()+1:0);
    }
    /** Desenha Idle já carregado, sem alterar animação, orientação ou regras da sessão. */
    public boolean drawPreview(Graphics2D original, com.projetok.prototype.DirectionalAnimation.Direction direction, int x, int y) {
        Frame frame=frames.get(direction+".0");if(frame==null)return false;
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.translate(Math.round(x-frame.pivotX()*scale),Math.round(y-frame.pivotY()*scale));
            g.scale(scale,scale);g.drawImage(frame.image(),0,0,null);return true;
        } finally {g.dispose();}
    }
    @Override public boolean draw(Graphics2D original,DebugSnapshot.ActorView actor) {
        if(!actor.player() || actor.id()!=1 || !actor.alive()) return false;
        Frame frame=frames.get(frameKey());
        if(frame==null) return false;
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // Keep complete originals and align their recorded foot pivots at any configured scale.
            g.translate(Math.round(actor.x()-frame.pivotX()*scale),Math.round(actor.y()-frame.pivotY()*scale));
            g.scale(scale,scale);g.drawImage(frame.image(),0,0,null);
        } finally { g.dispose(); }
        // Sem arte de ataque/dano: sinais geometricos sobre o PNG original, sem trava logica.
        Graphics2D effect=(Graphics2D)original.create();
        try {
            int x=Math.round(actor.x()),y=Math.round(actor.y());
            if(visual.state()==RpgVisualState.State.ATTACK) {
                effect.setColor(spirit?new Color(165,125,255):new Color(255,220,110));effect.setStroke(new BasicStroke(3));
                int angle=switch(animation.pose().direction()){case FRONT->210;case BACK->30;case LEFT->120;case RIGHT->300;};
                if(spirit)effect.drawOval(x-32,y-45,64,64);else effect.drawArc(x-32,y-45,64,64,angle,120);
            } else if(visual.state()==RpgVisualState.State.HIT) {
                effect.setColor(new Color(255,100,100,190));effect.setStroke(new BasicStroke(2));
                effect.drawOval(x-24,y-24,48,30);
            }
        } finally {effect.dispose();}
        return true;
    }
}
