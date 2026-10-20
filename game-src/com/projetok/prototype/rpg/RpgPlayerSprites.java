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
    public static final int DEFAULT_FRAME_MILLIS=220;
    public static final double DEFAULT_SCALE=0.4;
    private record Frame(BufferedImage image,int pivotX,int pivotY) { }
    private final Map<String,Frame> frames;
    private final RpgPlayerAnimation animation;
    private final double scale;
    private RpgPlayerSprites(Map<String,Frame> frames,int frameMillis,double scale) {
        this.frames=Map.copyOf(frames);animation=new RpgPlayerAnimation(frameMillis);this.scale=scale;
    }
    public static Path defaultFolder() {
        String browserFolder=System.getProperty("spiritgate.rpg.spriteFolder");
        return browserFolder==null?RpgMenu.home().resolve("assets/dev/player-base"):Path.of(browserFolder);
    }
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
        if(folder.toString().startsWith("/app/") || folder.toString().equals("/str") || folder.toString().startsWith("/str/")) {
            for(Object value:metadata.values()) actual.add(value.toString().split(",")[0]);
        } else {
            try(var files=Files.list(folder)) { files.forEach(p->actual.add(p.getFileName().toString())); }
        }
        Map<String,Frame> frames=new HashMap<>();
        for(var direction:com.projetok.prototype.DirectionalAnimation.Direction.values()) for(int index=0;index<3;index++) {
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
    public RpgPlayerAnimation.Pose update(com.projetok.rpg.RpgWorld.View view) { return animation.update(view); }
    public RpgPlayerAnimation.Pose pose() { return animation.pose(); }
    public String frameKey() {
        var pose=animation.pose();return pose.direction()+"."+(pose.walking()?pose.frame()+1:0);
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
        return true;
    }
}
