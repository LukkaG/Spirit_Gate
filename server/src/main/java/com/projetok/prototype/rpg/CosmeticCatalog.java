package com.projetok.prototype.rpg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;

/** Catálogo cosmético único, versionado, limitado e sem atributos de gameplay. */
public final class CosmeticCatalog {
    public record Definition(String id,String type,String name){@Override public String toString(){return name;}}
    public record Selection(String hair,String clothing){public Selection{if(hair==null||clothing==null||!hair.matches("[a-z][a-z0-9_]{0,30}")||!clothing.matches("[a-z][a-z0-9_]{0,30}"))throw new IllegalArgumentException("ID cosmético inválido");}public static Selection none(){return new Selection("none","none");}}
    private record Frame(BufferedImage image,int pivotX,int pivotY) { }
    private final Map<String,Definition> definitions;
    private final Map<String,Frame> frames;
    private final java.util.List<String> warnings;
    private CosmeticCatalog(Map<String,Definition> definitions,Map<String,Frame> frames,java.util.List<String> warnings){this.definitions=Map.copyOf(definitions);this.frames=Map.copyOf(frames);this.warnings=java.util.List.copyOf(warnings);}
    public java.util.List<Definition> all(String type){return definitions.values().stream().filter(d->d.type().equals(type)).sorted(Comparator.comparing(Definition::id)).toList();}
    public java.util.List<String> warnings(){return warnings;}
    public void validate(Selection selection){validateId(selection.hair(),"hair");validateId(selection.clothing(),"clothing");}
    private void validateId(String id,String type){if(id.equals("none"))return;var d=definitions.get(id);if(d==null||!d.type().equals(type))throw new IllegalArgumentException("Recurso cosmético indisponível: "+id);}
    public static CosmeticCatalog load(Path root)throws IOException{return read(root,true);}
    public static CosmeticCatalog metadata(Path root)throws IOException{return read(root,false);}
    private static CosmeticCatalog read(Path root,boolean images)throws IOException {
        Path base=root.toRealPath();Properties p=new Properties(){@Override public synchronized Object put(Object k,Object v){if(containsKey(k))throw new IllegalArgumentException("Campo cosmético duplicado");return super.put(k,v);}};
        try(var in=Files.newInputStream(base.resolve("manifest.properties"))){byte[] bytes=in.readNBytes(1_000_001);if(bytes.length>1_000_000)throw new IOException("Manifesto excessivo");p.load(new ByteArrayInputStream(bytes));}
        try{
            if(!"1".equals(p.remove("schema"))||!"true".equals(p.remove("temporary")))throw new IllegalArgumentException("Versão/marcador cosmético inválido");int count=Integer.parseInt((String)p.remove("count"));if(count<1||count>1024)throw new IllegalArgumentException("Quantidade cosmética inválida");
            Map<String,Definition> defs=new TreeMap<>();for(String key:new HashSet<>(p.stringPropertyNames()))if(key.startsWith("cosmetic.")){String id=key.substring(9);String[] v=((String)p.remove(key)).split("\\|",-1);if(!id.matches("[a-z][a-z0-9_]{0,30}")||v.length!=3||!Set.of("hair","clothing").contains(v[0])||!v[2].equals("TEMPORARIO")||v[1].length()>80)throw new IllegalArgumentException("Definição cosmética inválida");defs.put(id,new Definition(id,v[0],v[1]));}
            if(defs.size()>32||defs.isEmpty())throw new IllegalArgumentException("Catálogo cosmético vazio/excessivo");Properties original=new Properties();try(var in=CosmeticCatalog.class.getResourceAsStream("/rpg/experimental-player.properties")){original.load(in);}
            Map<String,Frame> frames=new HashMap<>();Set<String> seen=new HashSet<>();java.util.List<String> warnings=new ArrayList<>();long resident=0;
            for(int i=0;i<count;i++){
                String[] v=((String)p.remove("frame."+i)).split("\\|",-1);if(v.length!=10||!defs.containsKey(v[0])||!Set.of("hair_back","hair_front","clothing").contains(v[2])||!v[9].equals("TEMPORARIO")||!v[4].matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Frame cosmético inválido");
                if(!defs.get(v[0]).type().equals(v[2].equals("clothing")?"clothing":"hair"))throw new IllegalArgumentException("Camada incompatível");String[] expected=Objects.requireNonNull(original.getProperty(v[1]),"Pose ausente").split(",");int w=Integer.parseInt(v[5]),h=Integer.parseInt(v[6]),px=Integer.parseInt(v[7]),py=Integer.parseInt(v[8]);if(w!=Integer.parseInt(expected[2])||h!=Integer.parseInt(expected[3])||px!=Integer.parseInt(expected[4])||py!=Integer.parseInt(expected[5]))throw new IllegalArgumentException("Dimensões/pivô incompatíveis");
                String key=v[0]+"/"+v[1]+"/"+v[2];if(!seen.add(key))throw new IllegalArgumentException("Frame repetido");Path relative=Path.of(v[3]);if(relative.isAbsolute()||v[3].contains("\\")||!v[3].endsWith(".png")||!base.resolve(relative).normalize().startsWith(base))throw new IllegalArgumentException("Caminho cosmético inválido");
                if(images)try{Path file=base.resolve(relative);if(Files.isSymbolicLink(file)||!file.toRealPath().startsWith(base)||Files.size(file)>1_000_000)throw new IOException("Imagem fora do catálogo/limite");byte[] bytes=Files.readAllBytes(file);if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(v[4]))throw new IOException("Revisão divergente");BufferedImage image;try(var input=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("PNG ilegivel");var reader=readers.next();try{reader.setInput(input,true,true);if(!reader.getFormatName().equalsIgnoreCase("png")||reader.getWidth(0)!=w||reader.getHeight(0)!=h)throw new IOException("Dimensoes incompativeis antes de decodificar");image=reader.read(0);}finally{reader.dispose();}}if(image==null||!image.getColorModel().hasAlpha())throw new IOException("PNG sem transparencia");long size=(long)w*h*4;if(resident+size>64L*1024*1024)throw new IOException("Cache cosmético limitado a 64 MiB");frames.put(key,new Frame(image,px,py));resident+=size;}catch(Exception error){warnings.add(key+": "+error.getMessage());}
            }
            if(!p.isEmpty())throw new IllegalArgumentException("Campos cosméticos desconhecidos");return new CosmeticCatalog(defs,frames,warnings);
        }catch(RuntimeException e){throw new IOException("Catálogo cosmético inválido: "+e.getMessage(),e);}
    }
    /** Desenho apenas em memória, sempre no pivô da pose base correspondente. */
    public boolean draw(Graphics2D original,String id,String pose,String layer,float x,float y,double scale){var frame=frames.get(id+"/"+pose+"/"+layer);if(frame==null)return false;var g=(Graphics2D)original.create();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.translate(Math.round(x-frame.pivotX()*scale),Math.round(y-frame.pivotY()*scale));g.scale(scale,scale);g.drawImage(frame.image(),0,0,null);return true;}finally{g.dispose();}}
}
