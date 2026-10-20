package com.projetok.studio;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import javax.imageio.*;

/** Indexa apenas raizes autorizadas e nao modifica imagens originais. */
public final class AssetCatalog {
    /** Referencia estavel por caminho; hash identifica a revisao, nao aprovacao artistica. */
    public record Asset(String id,String path,Path file,String sha,int width,int height,boolean alpha,
            String category,BufferedImage thumbnail,Instant indexedAt) {
        @Override public String toString(){return path;}
        /** Categoria escolhida manualmente, sem inferencia pelo nome. */
        public Asset category(String value){return new Asset(id,path,file,sha,width,height,alpha,value,thumbnail,indexedAt);}
    }
    /** Resultado parcial seguro, com avisos e diferencas desde a ultima indexacao. */
    public record Result(java.util.List<Asset> assets,java.util.List<String> warnings,java.util.List<String> changes) {
        public Result {assets=java.util.List.copyOf(assets);warnings=java.util.List.copyOf(warnings);changes=java.util.List.copyOf(changes);}
        /** Consulta pelo ID sem aceitar caminho arbitrario. */
        public Map<String,Asset> byId(){Map<String,Asset> result=new LinkedHashMap<>();assets.forEach(a->result.put(a.id(),a));return Map.copyOf(result);}
    }
    private AssetCatalog(){}
    /** Busca limitada a assets e recursos do modulo; chamador executa fora da EDT. */
    public static Result scan(Path project,Result previous)throws IOException {
        Path base=project.toRealPath();Map<String,Asset> old=previous==null?Map.of():previous.byId();
        java.util.List<Asset> found=new ArrayList<>();java.util.List<String> warnings=new ArrayList<>(),changes=new ArrayList<>();
        int attempted=0;for(String name:java.util.List.of("assets","server/src/main/resources")) {
            Path root=base.resolve(name);if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS))continue;
            Path realRoot=root.toRealPath();if(!realRoot.startsWith(base)){warnings.add("Raiz fora do projeto: "+name);continue;}
            try(var paths=Files.walk(root,32)) {
                var iterator=paths.filter(p->p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).iterator();
                while(iterator.hasNext()) {
                    Path file=iterator.next();if(attempted++>=4096){warnings.add("Limite de 4096 candidatos atingido, incluindo invalidos");break;}
                    try {
                        if(Files.isSymbolicLink(file)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||!file.toRealPath().startsWith(realRoot))throw new IOException("Link/caminho recusado");
                        String relative=base.relativize(file).toString().replace('\\','/');String id=hash(relative.getBytes(StandardCharsets.UTF_8));
                        String sha=hash(file);var info=inspect(file);Asset prior=old.get(id);
                        found.add(new Asset(id,relative,file,sha,info.width,info.height,info.alpha,prior==null?"Sem categoria":prior.category(),info.thumb,Instant.now()));
                        if(prior==null)changes.add("Novo: "+relative);else if(!prior.sha().equals(sha))changes.add("Alterado: "+relative);
                    }catch(IOException|IllegalArgumentException error){warnings.add(file.getFileName()+": "+error.getMessage());}
                }
            }
        }
        found.sort(Comparator.comparing(Asset::path));return new Result(found,warnings,changes);
    }
    private record Info(int width,int height,boolean alpha,BufferedImage thumb){}
    /** Confere uma copia exportada sem aceitar dimensoes declaradas como prova do conteudo. */
    public static Asset verified(Path file,String id,String expected)throws IOException {if(!id.matches("[a-f0-9]{64}")||!hash(file).equals(expected))throw new IOException("Revisao de imagem divergente");var info=inspect(file);return new Asset(id,file.getFileName().toString(),file,expected,info.width,info.height,info.alpha,"Exportado",info.thumb,Instant.now());}
    private static Info inspect(Path file)throws IOException {
        if(Files.size(file)>32_000_000)throw new IOException("Imagem acima de 32 MB");
        try(var input=ImageIO.createImageInputStream(file.toFile())) {
            if(input==null)throw new IOException("Imagem ilegivel");var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("PNG invalido");
            var reader=readers.next();try {
                reader.setInput(input,true,true);if(!reader.getFormatName().equalsIgnoreCase("png"))throw new IOException("Conteudo nao PNG");
                int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>8192||h>8192||(long)w*h>8_000_000)throw new IOException("Dimensoes excedem 8M pixels/8192 por eixo");
                var type=reader.getRawImageType(0);boolean alpha=type!=null&&type.getColorModel().hasAlpha();var param=reader.getDefaultReadParam();int sample=Math.max(1,(Math.max(w,h)+95)/96);param.setSourceSubsampling(sample,sample,0,0);
                BufferedImage small=reader.read(0,param);if(small==null)throw new IOException("Decodificacao falhou");
                BufferedImage thumb=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=thumb.createGraphics();try{g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);double scale=Math.min(64d/small.getWidth(),64d/small.getHeight());int dw=Math.max(1,(int)(small.getWidth()*scale)),dh=Math.max(1,(int)(small.getHeight()*scale));g.drawImage(small,(64-dw)/2,(64-dh)/2,dw,dh,null);}finally{g.dispose();}
                return new Info(w,h,alpha,thumb);
            }finally{reader.dispose();}
        }
    }
    /** SHA-256 limitado por tamanho; nenhum conteudo e executado. */
    public static String hash(Path file)throws IOException {
        if(Files.size(file)>32_000_000)throw new IOException("Arquivo excessivo");
        try(var in=Files.newInputStream(file)){MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int n;long total=0;while((n=in.read(buffer))!=-1){total+=n;if(total>32_000_000)throw new IOException("Arquivo cresceu acima do limite");digest.update(buffer,0,n);}return HexFormat.of().formatHex(digest.digest());}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    /** Busca por nome/caminho e categoria manual. */
    public static java.util.List<Asset> search(Result catalog,String query,String category){String term=query.toLowerCase(Locale.ROOT);return catalog.assets().stream().filter(a->a.path().toLowerCase(Locale.ROOT).contains(term)&&(category.equals("Todas")||a.category().equals(category))).toList();}
    /** Extracao virtual de tileset com margem, espaco e regiao explicitamente escolhidos. */
    public static java.util.List<WorldDocument.Crop> tiles(Asset asset,int tw,int th,int margin,int spacing,WorldDocument.Crop region) {
        if(tw<1||th<1||tw>1024||th>1024||margin<0||spacing<0||spacing>1024||(long)region.x()+region.width()>asset.width()||(long)region.y()+region.height()>asset.height())throw new IllegalArgumentException("Configuracao de tileset invalida");
        java.util.List<WorldDocument.Crop> result=new ArrayList<>();
        for(int y=region.y()+margin;y+th<=region.y()+region.height()-margin;y+=th+spacing)for(int x=region.x()+margin;x+tw<=region.x()+region.width()-margin;x+=tw+spacing){if(result.size()>=65536)throw new IllegalArgumentException("Tiles excessivos");result.add(new WorldDocument.Crop(x,y,tw,th));}
        if(result.isEmpty())throw new IllegalArgumentException("Nenhum tile cabe na regiao");return java.util.List.copyOf(result);
    }
}
