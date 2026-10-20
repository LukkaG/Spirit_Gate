package com.projetok.studio;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

/** Cache LRU de imagens, limitado a 64 MiB; a pintura usa peek sem I/O. */
public final class AssetImages {
    private final LinkedHashMap<String,BufferedImage> images=new LinkedHashMap<>(16,.75f,true);
    private long bytes;
    private long generation;
    private final Map<String,String> revisions=new HashMap<>();
    /** Carrega fora da EDT, conferindo a revisao indexada antes da decodificacao. */
    public BufferedImage load(AssetCatalog.Asset asset)throws IOException {
        long epoch;
        synchronized(this){BufferedImage cached=images.get(asset.id());if(cached!=null&&asset.sha().equals(revisions.get(asset.id())))return cached;epoch=generation;}
        byte[] data;try(var in=java.nio.file.Files.newInputStream(asset.file())){data=in.readNBytes(32_000_001);}if(data.length>32_000_000)throw new IOException("Imagem excessiva");
        try{if(!HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data)).equals(asset.sha()))throw new IOException("Asset alterado: reindexe e revise "+asset.path());}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        BufferedImage image;try(var input=new javax.imageio.stream.MemoryCacheImageInputStream(new java.io.ByteArrayInputStream(data))){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Imagem ilegivel");var reader=readers.next();try{reader.setInput(input,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);if(!reader.getFormatName().equalsIgnoreCase("png")||w<1||h<1||w>8192||h>8192||(long)w*h>8_000_000||w!=asset.width()||h!=asset.height())throw new IOException("Dimensoes/revisao recusadas antes da decodificacao");image=reader.read(0);}finally{reader.dispose();}}if(image==null)throw new IOException("Imagem recusada");
        long size=(long)image.getWidth()*image.getHeight()*4;
        synchronized(this) {
        if(epoch!=generation)return image;
        BufferedImage prior=images.remove(asset.id());if(prior!=null)bytes-=(long)prior.getWidth()*prior.getHeight()*4;
        while(bytes+size>64L*1024*1024&&!images.isEmpty()){var first=images.entrySet().iterator().next();bytes-=(long)first.getValue().getWidth()*first.getValue().getHeight()*4;images.remove(first.getKey());}
        images.put(asset.id(),image);revisions.put(asset.id(),asset.sha());bytes+=size;return image;
        }
    }
    /** Leitura em memoria, nunca acessa o disco durante paintComponent. */
    public synchronized BufferedImage peek(String id){return images.get(id);}
    /** Memoria aproximada dos pixels residentes. */
    public synchronized long bytes(){return bytes;}
    /** Invalida revisoes ao reindexar manualmente. */
    public synchronized void clear(){images.clear();revisions.clear();bytes=0;generation++;}
}
