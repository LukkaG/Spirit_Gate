package com.projetok.studio;

import java.nio.channels.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Local synthetic target only. No implicit binding to Radmin or global game files.
 * Production V3 lacks the map/revision contract: the unbound path is a hard block. */
public final class LivePublishing {
    public enum Fault { NONE,AFTER_STAGE,AFTER_ACTIVATION,BEFORE_POINTER }
    public record Result(boolean active,String hash,String message,Path revision,long nanos){}
    private final Path root;
    public LivePublishing(Path root){this.root=root.toAbsolutePath();}
    public Path root(){return root;}
    public String current()throws Exception{var file=root.resolve("active.txt");if(!Files.exists(file))return "";String hash=Files.readString(file).strip();if(!hash.matches("[a-f0-9]{64}"))throw new IllegalStateException("Ponteiro publicado invalido");
        boolean valid=false;for(var version:history()){var p=metadata(version);verifyStoredAssets(version,p);if(!hash.equals(p.getProperty("hash"))||!"CONFIRMED".equals(p.getProperty("activation")))continue;var digest=java.security.MessageDigest.getInstance("SHA-256");var docs=new TreeMap<String,WorldDocument>();try(var files=Files.list(version)){for(var path:files.filter(f->f.toString().endsWith(".sgmap")).toList()){var d=DocumentStore.load(path);docs.put(d.id(),d);}}for(var d:docs.values()){digest.update(d.id().getBytes(StandardCharsets.UTF_8));digest.update((byte)0);digest.update(LiveRevision.canonical(d));}if(hash.equals(HexFormat.of().formatHex(digest.digest())))valid=true;}
        if(!valid)throw new IllegalStateException("Revisao publicada ausente ou alterada");return hash;
    }
    public LiveRevision active(long number,AssetCatalog.Result catalog)throws Exception{String hash=current();if(hash.isEmpty())throw new IllegalStateException("Nenhuma revisao publicada");for(var version:history())if(hash.equals(metadata(version).getProperty("hash")))return restore(version,number,catalog);throw new IllegalStateException("Revisao ausente");}
    private static void verifyStoredAssets(Path version,Properties metadata)throws Exception{if(!"true".equals(metadata.getProperty("assetsStored")))return;for(String key:metadata.stringPropertyNames())if(key.startsWith("asset.")){String hash=metadata.getProperty(key);if(!hash.matches("[a-f0-9]{64}"))throw new IllegalStateException("Hash de asset invalido");var image=version.resolve("images/"+hash+".png");if(!Files.isRegularFile(image)||Files.size(image)>32_000_000||!AssetCatalog.hash(image).equals(hash))throw new IllegalStateException("Asset arquivado ausente/alterado");}}
    private static Properties metadata(Path version)throws Exception{var p=new Properties();try(var in=Files.newInputStream(version.resolve("revision.properties"))){p.load(in);}return p;}
    static void atomic(Path path,byte[] data)throws Exception{Path temporary=Files.createTempFile(path.getParent(),"live-",".tmp");try{try(var c=FileChannel.open(temporary,StandardOpenOption.WRITE)){var bytes=java.nio.ByteBuffer.wrap(data);while(bytes.hasRemaining())c.write(bytes);c.force(true);}Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}}
    public Result publish(LiveRevision revision,String expectedDisk,String expectedGame,PublicationTarget target,Fault fault)throws Exception{
        return publish(revision,expectedDisk,expectedGame,target,fault,()->false);
    }
    private static void checkCancellation(java.util.function.BooleanSupplier cancelled){if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException("Publicacao cancelada antes do commit");}
    public Result publish(LiveRevision revision,String expectedDisk,String expectedGame,PublicationTarget target,Fault fault,java.util.function.BooleanSupplier cancelled)throws Exception{
        checkCancellation(cancelled);
        long started=System.nanoTime();if(target==null)return new Result(false,expectedDisk,"Bloqueado: servidor RPG V3 nao transmite mapas/revisoes; nenhum arquivo publicado",null,System.nanoTime()-started);
        var validated=LiveRevision.prepare(revision.number(),revision.bundle().start(),revision.bundle().maps(),revision.bundle().assets());if(!validated.hash().equals(revision.hash()))throw new IllegalArgumentException("Revisao mudou durante a validacao");
        checkCancellation(cancelled);Files.createDirectories(root);try(var channel=FileChannel.open(root.resolve("publication.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock()){
            if(lock==null)throw new IllegalStateException("Outra publicacao em andamento");if(history().size()>=200)throw new IllegalStateException("Historico limitado a 200 revisoes; arquive copias antes de nova publicacao");if(!current().equals(expectedDisk))return new Result(false,current(),"Revisao publicada mudou; recarregue antes de Salvar",null,System.nanoTime()-started);
            if(Files.exists(root.resolve("pending.properties")))throw new IllegalStateException("Transacao interrompida: recupere o ponteiro anterior antes de salvar");
            checkCancellation(cancelled);var reservation=target.reserve(expectedGame,revision).get(40,TimeUnit.SECONDS);if(!reservation.accepted())return new Result(false,current(),reservation.message(),null,System.nanoTime()-started);
            Path stage=null,version=null;boolean pointerChanged=false;
            try{
                checkCancellation(cancelled);var journal=new Properties();journal.setProperty("before",expectedDisk);journal.setProperty("after",revision.hash());journal.setProperty("state","PREPARED");atomic(root.resolve("pending.properties"),properties(journal));
                if(expectedDisk.isEmpty()&&target instanceof com.projetok.multiplayer.RevisionCoordinator managed)archiveBaseline(managed.revision());
                Path revisions=root.resolve("revisions");Files.createDirectories(revisions);stage=Files.createTempDirectory(revisions,"staging-");
                for(var d:revision.bundle().maps().values())Files.write(stage.resolve(d.id()+".sgmap"),LiveRevision.canonical(d),StandardOpenOption.CREATE_NEW);
                if(!revision.bundle().assets().assets().isEmpty()){Files.createDirectory(stage.resolve("images"));for(var asset:revision.bundle().assets().assets()){Path copy=stage.resolve("images/"+asset.sha()+".png");if(!Files.exists(copy))Files.copy(asset.file(),copy);if(!AssetCatalog.hash(copy).equals(asset.sha()))throw new IllegalStateException("Asset mudou durante staging");}}
                var meta=new Properties();meta.setProperty("assetsStored","true");meta.setProperty("hash",revision.hash());meta.setProperty("source",revision.bundle().start());meta.setProperty("number",""+revision.number());meta.setProperty("time",Instant.now().toString());meta.setProperty("compatibility",target.compatibility());meta.setProperty("validation","runtime+geometry+assets validated; activation pending");meta.setProperty("profile",revision.profile().toString());meta.setProperty("previous",expectedDisk);
                for(var a:revision.bundle().assets().assets())if(revision.bundle().maps().values().stream().flatMap(d->d.elements().stream()).anyMatch(e->e.asset().equals(a.id())))meta.setProperty("asset."+a.id(),a.sha());
                Files.write(stage.resolve("revision.properties"),properties(meta));version=revisions.resolve(revision.hash()+"-"+revision.number()+"-"+UUID.randomUUID());Files.move(stage,version,StandardCopyOption.ATOMIC_MOVE);stage=null;
                if(fault==Fault.AFTER_STAGE)throw new IllegalStateException("Falha injetada apos staging");
                checkCancellation(cancelled);var activated=target.activateReserved().get(40,TimeUnit.SECONDS);if(!activated.accepted()||!activated.hash().equals(revision.hash()))throw new IllegalStateException("Ativacao nao confirmada");
                if(fault==Fault.AFTER_ACTIVATION||fault==Fault.BEFORE_POINTER)throw new IllegalStateException("Falha injetada antes do ponteiro");
                checkCancellation(cancelled);if(!expectedDisk.isEmpty())atomic(root.resolve("previous.txt"),expectedDisk.getBytes(StandardCharsets.UTF_8));
                atomic(root.resolve("active.txt"),revision.hash().getBytes(StandardCharsets.UTF_8));pointerChanged=true;
                meta.setProperty("activation","CONFIRMED");atomic(version.resolve("revision.properties"),properties(meta));Files.delete(root.resolve("pending.properties"));
                checkCancellation(cancelled);var confirmed=target.confirmReserved().get(40,TimeUnit.SECONDS);if(!confirmed.accepted()||!confirmed.hash().equals(revision.hash()))throw new IllegalStateException("Destino recusou a confirmacao, desconectou ou reverteu a ativacao");
                return new Result(true,revision.hash(),"Aplicado e confirmado: "+target.compatibility(),version,System.nanoTime()-started);
            }catch(Exception error){
                try{var rollback=target.abortReserved().get(40,TimeUnit.SECONDS);if(pointerChanged&&!rollback.accepted()&&rollback.hash().equals(revision.hash()))return new Result(true,revision.hash(),"Commit confirmado pelo destino apos espera",version,System.nanoTime()-started);}catch(Exception rollback){error.addSuppressed(rollback);}
                if(pointerChanged){if(expectedDisk.isEmpty())Files.deleteIfExists(root.resolve("active.txt"));else atomic(root.resolve("active.txt"),expectedDisk.getBytes(StandardCharsets.UTF_8));}
                if(version!=null){var failed=new Properties();failed.setProperty("activation","ABORTED");failed.setProperty("hash",revision.hash());Files.write(version.resolve("aborted.properties"),properties(failed));}
                Files.deleteIfExists(root.resolve("pending.properties"));throw error;
            }finally{if(stage!=null)try(var paths=Files.walk(stage)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
        }
    }
    private void archiveBaseline(LiveRevision revision)throws Exception{
        Path revisions=root.resolve("revisions");Files.createDirectories(revisions);Path destination=revisions.resolve("baseline-"+revision.hash());if(Files.exists(destination))return;
        Path stage=Files.createTempDirectory(revisions,"baseline-staging-");for(var doc:revision.bundle().maps().values())Files.write(stage.resolve(doc.id()+".sgmap"),LiveRevision.canonical(doc));
        var meta=new Properties();meta.setProperty("hash",revision.hash());meta.setProperty("source",revision.bundle().start());meta.setProperty("number",""+revision.number());meta.setProperty("time",Instant.now().toString());meta.setProperty("activation","CONFIRMED");meta.setProperty("compatibility","rpg-content-baseline");meta.setProperty("validation","Actual server baseline before first explicit Save");meta.setProperty("profile",revision.profile().toString());Files.write(stage.resolve("revision.properties"),properties(meta));Files.move(stage,destination,StandardCopyOption.ATOMIC_MOVE);
    }
    static byte[] properties(Properties p)throws Exception{var out=new java.io.ByteArrayOutputStream();p.store(out,"Studio Live revision");return out.toByteArray();}
    public List<Path> history()throws Exception{if(!Files.isDirectory(root.resolve("revisions")))return List.of();try(var paths=Files.list(root.resolve("revisions"))){return paths.filter(p->Files.isRegularFile(p.resolve("revision.properties"))&&!Files.exists(p.resolve("aborted.properties"))).sorted().toList();}}
    public LiveRevision restore(Path version,long number,AssetCatalog.Result catalog)throws Exception{
        if(!history().contains(version.toAbsolutePath()))throw new IllegalArgumentException("Revisao fora do historico");var meta=new Properties();try(var in=Files.newInputStream(version.resolve("revision.properties"))){meta.load(in);}verifyStoredAssets(version,meta);var maps=new TreeMap<String,WorldDocument>();try(var files=Files.list(version)){for(var file:files.filter(f->f.toString().endsWith(".sgmap")).toList()){var d=DocumentStore.load(file);maps.put(d.id(),d);}}
        var revision=LiveRevision.prepare(number,meta.getProperty("source"),maps,catalog);if(!revision.hash().equals(meta.getProperty("hash")))throw new IllegalArgumentException("Integridade da revisao alterada");return revision;
    }
    /** Explicit recovery never activates content; records remain for audit. */
    public void recoverPreviousPointer()throws Exception{var file=root.resolve("pending.properties");if(!Files.exists(file))return;var p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}String before=p.getProperty("before","");if(!before.isEmpty()&&!before.matches("[a-f0-9]{64}"))throw new IllegalStateException("Journal invalido");if(before.isEmpty())Files.deleteIfExists(root.resolve("active.txt"));else atomic(root.resolve("active.txt"),before.getBytes(StandardCharsets.UTF_8));Files.move(file,root.resolve("recovered-"+UUID.randomUUID()+".properties"));}
}
