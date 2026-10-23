package com.projetok.studio;

import java.nio.file.*;
import java.util.*;

/** Mede primeiro salvamento e repetições, isolando inicialização do temporário. */
public final class StudioSaveProbe {
    static double ms(long start){return (System.nanoTime()-start)/1_000_000d;}
    public static void main(String[] args)throws Exception {
        Path out=Path.of(args[0]),root=Path.of(args[1]);Files.createDirectory(out);long start=System.nanoTime();var catalog=AssetCatalog.scan(root,null);double index=ms(start);
        var asset=catalog.assets().stream().filter(a->a.width()>=32&&a.height()>=32).findFirst().orElseThrow();var empty=WorldDocument.empty("benchmark",4096,4096);var elements=new ArrayList<WorldDocument.Element>();
        for(int i=0;i<12000;i++)elements.add(new WorldDocument.Element("tile_"+i,WorldDocument.Kind.TILE,asset.id(),asset.sha(),new WorldDocument.Crop(0,0,32,32),i%128*32,i/128*32,32,32,empty.layers().get(i%4).id(),true,false,"","FRONT",Map.of()));var document=empty.withElements(elements);
        start=System.nanoTime();byte[] bytes=DocumentStore.encode(document);double encode=ms(start);start=System.nanoTime();DocumentStore.decode(bytes);double decode=ms(start);
        StringJoiner samples=new StringJoiner(",");
        for(int i=0;i<5;i++){Path file=out.resolve("map-"+i+".sgmap");start=System.nanoTime();DocumentStore.save(file,document);double first=ms(start);start=System.nanoTime();DocumentStore.save(file,document);double backup=ms(start);if(!DocumentStore.load(file).equals(document)||!DocumentStore.load(Path.of(file+".bak")).equals(document))throw new IllegalStateException("Roundtrip/backup divergente");samples.add(String.format(Locale.ROOT,"{\"firstSaveMs\":%.4f,\"backupSaveMs\":%.4f}",first,backup));}
        start=System.nanoTime();Path temporary=Files.createTempFile(out,"jdk-probe-",".tmp");double temp=ms(start);Files.delete(temporary);
        String json=String.format(Locale.ROOT,"{\"java\":\"%s\",\"assets\":%d,\"elements\":12000,\"indexMs\":%.4f,\"encodeMs\":%.4f,\"decodeMs\":%.4f,\"warmedJdkTempMs\":%.4f,\"samples\":[%s],\"filesystemCacheControlled\":false,\"coldProcess\":true}",System.getProperty("java.version"),catalog.assets().size(),index,encode,decode,temp,samples);Files.writeString(out.resolve("metrics.json"),json);System.out.println(json);
    }
}
