package com.projetok.rpg;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Protótipo independente: documento online V1 envolve payload offline V2 sem alterá-lo.
 * Não conectado aos launchers, ao mundo online ou ao diretório de saves pessoais.
 */
public final class SyntheticOnlineRepository {
    public record Document(UUID id,long revision,RpgSave state) { }
    public static final class Grant {
        private final UUID id;
        private final Object authority;
        private Grant(UUID id,Object authority){this.id=id;this.authority=authority;}
    }
    private final Path root;
    private final Object authority=new Object();
    public SyntheticOnlineRepository(Path root)throws IOException{
        this.root=root.toAbsolutePath().normalize();
        if(!this.root.getFileName().toString().startsWith("online-synthetic-"))throw new IllegalArgumentException("Repositorio exige diretorio online-synthetic-* independente");
        for(Path p=this.root;p!=null;p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Links nao autorizados");
        Files.createDirectories(this.root);
    }
    /** Autoridade efêmera de teste. Não representa conta, login ou credencial durável. */
    public Grant create(String race)throws IOException{
        var identity=CharacterIdentity.create("Sintetico",race,"azure");var grant=new Grant(identity.id(),authority);
        write(grant,0,new RpgWorld(identity).save());return grant;
    }
    public Document load(UUID id)throws IOException{var doc=decode(read(path(id)));if(!doc.id().equals(id))throw new IOException("Identidade divergente");return doc;}
    public Document recoverBackup(UUID id)throws IOException{return decode(read(root.resolve(id+".online.bak")));}
    public void write(Grant grant,long expectedRevision,RpgSave state)throws IOException{
        if(grant==null||grant.authority!=authority||!grant.id.equals(state.identity().id()))throw new SecurityException("Gravacao sintetica nao autorizada");
        if(expectedRevision<0||expectedRevision==Long.MAX_VALUE)throw new IllegalArgumentException("Revisao invalida");
        RpgWorld.restore(state);Path target=path(grant.id),lock=root.resolve(grant.id+".lock");
        if(Files.isSymbolicLink(lock)||Files.isSymbolicLink(target)||Files.isSymbolicLink(root.resolve(grant.id+".online.bak")))throw new IOException("Links nao autorizados");
        try(var channel=FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE);var held=channel.lock()){
            long revision=Files.exists(target)?load(grant.id).revision():0;
            if(revision!=expectedRevision)throw new IOException("Conflito de revisao");
            byte[] bytes=encode(new Document(grant.id,revision+1,state));Path temp=Files.createTempFile(root,"snapshot-",".tmp");
            try{Files.write(temp,bytes);decode(read(temp));try(var output=FileChannel.open(temp,StandardOpenOption.WRITE)){output.force(true);}
                if(Files.exists(target))Files.copy(target,root.resolve(grant.id+".online.bak"),StandardCopyOption.REPLACE_EXISTING);
                Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            }finally{Files.deleteIfExists(temp);}
        }
    }
    private Path path(UUID id){return root.resolve(Objects.requireNonNull(id)+".online");}
    private static byte[] read(Path file)throws IOException{if(Files.isSymbolicLink(file)||Files.size(file)>263000)throw new IOException("Documento invalido");try(var in=Files.newInputStream(file)){return in.readNBytes(263001);}}
    private static byte[] digest(byte[] bytes){try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static byte[] encode(Document document)throws IOException{
        var buffer=new ByteArrayOutputStream();var out=new DataOutputStream(buffer);byte[] payload=LocalSaveStore.encode(document.state());
        out.writeInt(0x53474f31);out.writeInt(1);out.writeLong(document.id().getMostSignificantBits());out.writeLong(document.id().getLeastSignificantBits());out.writeLong(document.revision());out.writeInt(payload.length);out.write(payload);out.flush();out.write(digest(buffer.toByteArray()));return buffer.toByteArray();
    }
    private static Document decode(byte[] bytes)throws IOException{
        try{if(bytes.length<69||!MessageDigest.isEqual(Arrays.copyOfRange(bytes,bytes.length-32,bytes.length),digest(Arrays.copyOf(bytes,bytes.length-32))))throw new IOException("Integridade invalida");var in=new DataInputStream(new ByteArrayInputStream(Arrays.copyOf(bytes,bytes.length-32)));if(in.readInt()!=0x53474f31||in.readInt()!=1)throw new IOException("Schema online incompativel");
            var id=new UUID(in.readLong(),in.readLong());long revision=in.readLong();int size=in.readInt();if(revision<1||size<1||size>262144)throw new IOException("Documento invalido");
            byte[] payload=in.readNBytes(size);if(payload.length!=size||in.available()!=0)throw new IOException("Integridade invalida");
            var state=LocalSaveStore.decode(payload);RpgWorld.restore(state);if(!id.equals(state.identity().id()))throw new IOException("Identidade divergente");return new Document(id,revision,state);
        }catch(IllegalArgumentException e){throw new IOException("Snapshot invalido",e);}
    }
}
