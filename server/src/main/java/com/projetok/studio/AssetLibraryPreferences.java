package com.projetok.studio;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Preferências do editor separadas de mapas, imagens originais e saves de personagem. */
public record AssetLibraryPreferences(Set<String> favorites,List<String> recent,int thumbnailSize) {
    public AssetLibraryPreferences {
        favorites=Set.copyOf(favorites);recent=List.copyOf(recent);
        if(favorites.size()>4096||recent.size()>32||new HashSet<>(recent).size()!=recent.size()
            ||thumbnailSize<32||thumbnailSize>96)throw new IllegalArgumentException("Preferências fora dos limites");
        for(String id:favorites)check(id);for(String id:recent)check(id);
    }
    private static void check(String id){if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("ID de recurso inválido");}
    public static AssetLibraryPreferences empty(){return new AssetLibraryPreferences(Set.of(),List.of(),64);}
    public AssetLibraryPreferences toggle(String id){check(id);var next=new HashSet<>(favorites);if(!next.remove(id))next.add(id);return new AssetLibraryPreferences(next,recent,thumbnailSize);}
    public AssetLibraryPreferences used(String id){check(id);var next=new ArrayList<>(recent);next.remove(id);next.add(0,id);return new AssetLibraryPreferences(favorites,next.subList(0,Math.min(32,next.size())),thumbnailSize);}
    public AssetLibraryPreferences size(int value){return new AssetLibraryPreferences(favorites,recent,value);}
    private static Path path(Path project)throws IOException {
        Path root=project.toRealPath(),folder=root.resolve("studio-projects");
        if(Files.isSymbolicLink(folder))throw new IOException("Pasta de preferências não pode ser link");
        Files.createDirectories(folder);Path file=folder.resolve("library.properties");
        if(Files.isSymbolicLink(file)||!folder.toRealPath().startsWith(root))throw new IOException("Preferências fora do projeto");
        return file;
    }
    /** Leitura limitada; chamar fora da EDT. */
    public static AssetLibraryPreferences load(Path project)throws IOException {
        Path file=path(project);if(!Files.exists(file))return empty();
        Properties p=new Properties(){@Override public synchronized Object put(Object k,Object v){if(containsKey(k))throw new IllegalArgumentException("Preferência duplicada");return super.put(k,v);}};
        try(var in=Files.newInputStream(file)){byte[] bytes=in.readNBytes(400001);if(bytes.length>400000)throw new IOException("Preferências excessivas");p.load(new ByteArrayInputStream(bytes));}
        try {
            if(!"1".equals(p.remove("schema")))throw new IllegalArgumentException("Versão de preferências inválida");
            String f=(String)p.remove("favorites"),r=(String)p.remove("recent");int size=Integer.parseInt((String)p.remove("size"));
            if(!p.isEmpty()||f==null||r==null)throw new IllegalArgumentException("Preferências desconhecidas/incompletas");
            return new AssetLibraryPreferences(f.isEmpty()?Set.of():new HashSet<>(List.of(f.split(","))),r.isEmpty()?List.of():List.of(r.split(",")),size);
        }catch(RuntimeException e){throw new IOException("Preferências inválidas",e);}
    }
    /** Gravação atômica com temporário local, sem alterar PNGs. */
    public void save(Path project)throws IOException {
        Path file=path(project),temp=Files.createTempFile(file.getParent(),"library-",".tmp");
        try {
            Properties p=new Properties();p.setProperty("schema","1");p.setProperty("favorites",String.join(",",new TreeSet<>(favorites)));p.setProperty("recent",String.join(",",recent));p.setProperty("size",""+thumbnailSize);
            try(var out=Files.newOutputStream(temp)){p.store(out,"Spirit Gate - preferencias da biblioteca");}
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
}
