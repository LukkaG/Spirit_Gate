package com.projetok.prototype;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;

/** Carrega uma vez a exportacao VALIDADO da TASK-013; sem IO por frame. */
public final class ValidatedSpriteAtlas {
    private final BufferedImage image;
    private final Map<String,int[]> frames;
    private final int pivotX, pivotY, offsetX, offsetY;
    private ValidatedSpriteAtlas(BufferedImage image, Map<String,int[]> frames, Properties p) {
        this.image=image; this.frames=Map.copyOf(frames);
        pivotX=Integer.parseInt(p.getProperty("pivotX")); pivotY=Integer.parseInt(p.getProperty("pivotY"));
        offsetX=Integer.parseInt(p.getProperty("offsetX")); offsetY=Integer.parseInt(p.getProperty("offsetY"));
        if (pivotX<0 || pivotX>64 || pivotY<0 || pivotY>64) throw new IllegalArgumentException("Pivot invalido");
        if(Math.abs((long)offsetX)>4096 || Math.abs((long)offsetY)>4096) throw new IllegalArgumentException("Offset excessivo");
    }
    /** Recusa status, hash, tamanho ou coordenadas inconsistentes antes de ativar o recurso. */
    public static ValidatedSpriteAtlas load(Path manifest) throws IOException {
        try { return loadChecked(manifest); }
        catch (IllegalArgumentException | NullPointerException error) { throw new IOException("Manifest visual invalido", error); }
    }
    /** Ausencia ou corrupcao opcional conserva o fallback, sem IO durante render. */
    public static ValidatedSpriteAtlas loadOptional(Path manifest) {
        if (!Files.exists(manifest)) return null;
        try { return load(manifest); }
        catch (IOException error) {
            System.err.println("[Spirit Gate] Pacote visual recusado; usando fallback: " + error.getMessage());
            return null;
        }
    }
    private static ValidatedSpriteAtlas loadChecked(Path manifest) throws IOException {
        if(Files.size(manifest)>1_000_000) throw new IOException("Manifest excessivo");
        Properties p=new Properties() {
            @Override public synchronized Object put(Object key,Object value) {
                if(containsKey(key)) throw new IllegalArgumentException("Chave visual duplicada");
                return super.put(key,value);
            }
        };
        try (var reader=Files.newBufferedReader(manifest)) { p.load(reader); }
        if (!"VALIDADO".equals(p.getProperty("status"))) throw new IOException("Asset sem validacao");
        boolean versioned=p.getProperty("packageVersion")!=null;
        if(versioned && (!"1".equals(p.getProperty("packageVersion")) ||
                !p.getProperty("assetId", "").matches("[a-z][a-z0-9._-]{0,63}") ||
                !"64".equals(p.getProperty("frameSize")) || !"32".equals(p.getProperty("tileSize")) ||
                p.getProperty("reviewOrigin", "").isBlank() ||
                !p.getProperty("sha256", "").equals(p.getProperty("approvedSha256"))))
            throw new IOException("Contrato versionado ou revisao invalida");
        Path png=manifest.toAbsolutePath().getParent().resolve(p.getProperty("image")).normalize();
        if(versioned && (!png.startsWith(manifest.toAbsolutePath().getParent()) || Files.isSymbolicLink(png)))
            throw new IOException("Caminho visual inseguro");
        if(!png.toRealPath().startsWith(manifest.toAbsolutePath().getParent().toRealPath()))
            throw new IOException("Caminho visual externo");
        if(Files.size(png)>16_000_000) throw new IOException("PNG excessivo");
        byte[] data=Files.readAllBytes(png);
        try {
            String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
            if (!sha.equals(p.getProperty("sha256"))) throw new IOException("Hash de asset divergente");
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
        // Inspeciona dimensões antes de alocar pixels, inclusive em PNG com hash correto.
        try(var input=ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(data))) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw new IOException("Imagem ilegivel");
            var reader=readers.next();
            try {reader.setInput(input);
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w>4096 || h>4096 || w<=0 || h<=0) throw new IOException("Dimensoes visuais excessivas");
            } finally {reader.dispose();}
        }
        BufferedImage image=ImageIO.read(new java.io.ByteArrayInputStream(data));
        if (image==null || image.getWidth()%64!=0 || image.getHeight()%64!=0) throw new IOException("Grade invalida");
        Map<String,int[]> frames=new HashMap<>();
        for (var state:DirectionalAnimation.State.values()) for (var direction:DirectionalAnimation.Direction.values()) {
            if (state==DirectionalAnimation.State.FAINT && direction!=DirectionalAnimation.Direction.FRONT) continue;
            for (int i=0;i<DirectionalAnimation.count(state);i++) {
                String key=key(state,direction,i), value=p.getProperty(key);
                if (value==null) {
                    if(versioned) throw new IOException("Pacote versionado incompleto");
                    continue; // contrato legado admite fallback por frame.
                }
                String[] parts=value.split(",");
                if(parts.length!=4) throw new IOException("Recorte invalido");
                int[] r=new int[4];
                try { for(int j=0;j<4;j++) r[j]=Integer.parseInt(parts[j]); }
                catch(NumberFormatException error) { throw new IOException("Coordenada invalida",error); }
                if(r[0]<0 || r[1]<0 || r[0]%64!=0 || r[1]%64!=0 || r[2]!=64 || r[3]!=64 || r[0]+64>image.getWidth() || r[1]+64>image.getHeight()) throw new IOException("Recorte fora da grade");
                for (int[] other:frames.values()) if(r[0]==other[0] && r[1]==other[1]) throw new IOException("Frames sobrepostos");
                frames.put(key,r);
            }
        }
        try { return new ValidatedSpriteAtlas(image,frames,p); }
        catch (IllegalArgumentException error) { throw new IOException("Metadados invalidos",error); }
    }
    private static String key(DirectionalAnimation.State state, DirectionalAnimation.Direction direction, int frame) {
        String action=switch(state) { case ATTACK -> "PUNCH"; case FAINT -> "KO"; default -> state.name(); };
        return action+"."+direction+"."+frame;
    }
    /** Retorna falso para preservar o desenho anterior quando falta qualquer frame. */
    public boolean draw(Graphics2D original, DebugSnapshot.ActorView actor, DirectionalAnimation.View view) {
        int[] r=frames.get(key(view.state(),view.direction(),view.frame()));
        if(r==null) return false;
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int x=Math.round(actor.x())-pivotX+offsetX, y=Math.round(actor.y())-pivotY+offsetY;
            g.drawImage(image,x,y,x+64,y+64,r[0],r[1],r[0]+64,r[1]+64,null);
        } finally {g.dispose();}
        return true;
    }
}
