package com.projetok.prototype.rpg;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Perfil sintético V1 independente de personagens, equipamentos e schema de save V2. */
public record CosmeticProfile(String syntheticId,CosmeticCatalog.Selection selection) {
    public CosmeticProfile{if(!syntheticId.matches("[a-z0-9_-]{1,40}"))throw new IllegalArgumentException("Perfil sintético inválido");Objects.requireNonNull(selection);}
    public void save(Path file,CosmeticCatalog catalog)throws IOException {
        catalog.validate(selection);Path path=file.toAbsolutePath();Files.createDirectories(path.getParent());if(Files.isSymbolicLink(path))throw new IOException("Perfil não pode ser link");if(Files.exists(path))load(path,catalog);Properties p=new Properties();p.setProperty("schema","1");p.setProperty("synthetic",syntheticId);p.setProperty("hair",selection.hair());p.setProperty("clothing",selection.clothing());Path temp=Files.createTempFile(path.getParent(),"cosmetic-",".tmp");try{try(var out=Files.newOutputStream(temp)){p.store(out,"TEMPORARIO - perfil cosmetico sintetico");}load(temp,catalog);try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
    public static CosmeticProfile load(Path file,CosmeticCatalog catalog)throws IOException {
        Properties p=new Properties(){@Override public synchronized Object put(Object k,Object v){if(containsKey(k))throw new IllegalArgumentException("Campo duplicado");return super.put(k,v);}};
        try(var in=Files.newInputStream(file)){byte[] bytes=in.readNBytes(4097);if(bytes.length>4096)throw new IOException("Perfil excessivo");p.load(new ByteArrayInputStream(bytes));}
        try{if(!"1".equals(p.remove("schema")))throw new IllegalArgumentException("Perfil incompatível");var result=new CosmeticProfile((String)p.remove("synthetic"),new CosmeticCatalog.Selection((String)p.remove("hair"),(String)p.remove("clothing")));catalog.validate(result.selection());if(!p.isEmpty())throw new IllegalArgumentException("Campos desconhecidos");return result;}catch(RuntimeException e){throw new IOException("Perfil cosmético inválido",e);}
    }
}
