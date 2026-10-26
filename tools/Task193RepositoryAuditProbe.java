package com.projetok.rpg;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Caracterização do protótipo existente; não implementa o ADR ainda não aprovado. */
public final class Task193RepositoryAuditProbe {
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static UUID find(Path root,UUID except) throws Exception {
        try(var files=Files.list(root)) {
            return files.filter(p->p.toString().endsWith(".online")).map(p->UUID.fromString(p.getFileName().toString().replace(".online","")))
                .filter(id->!id.equals(except)).findFirst().orElseThrow();
        }
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]).toAbsolutePath().normalize();Files.createDirectory(out);
        Path source=out.resolve("online-synthetic-source"),copy=out.resolve("online-synthetic-copy");
        var repository=new SyntheticOnlineRepository(source);var grant=repository.create("spirit");UUID id=find(source,null);
        var first=repository.load(id);require(first.revision()==1,"Revision inicial");
        repository.write(grant,1,first.state());var second=repository.load(id);require(second.revision()==2,"CAS aplicado");
        require(repository.recoverBackup(id).revision()==1,"Backup anterior");
        boolean staleRejected=false;try{repository.write(grant,1,first.state());}catch(IOException expected){staleRejected=true;}
        require(staleRejected,"Replay de snapshot com expectedRevision antigo atualmente e conflito");
        var restarted=new SyntheticOnlineRepository(source);require(restarted.load(id).state().equals(first.state()),"Leitura apos restart");
        boolean grantRejected=false;try{restarted.write(grant,2,second.state());}catch(SecurityException expected){grantRejected=true;}
        require(grantRejected,"Grant de instancia anterior e efemero");
        byte[] beforeCopy=Files.readAllBytes(source.resolve(id+".online"));new SyntheticOnlineRepository(copy);
        Files.copy(source.resolve(id+".online"),copy.resolve(id+".online"));
        require(new SyntheticOnlineRepository(copy).load(id).state().equals(second.state()),"Copia apenas sintetica legivel");
        require(Arrays.equals(beforeCopy,Files.readAllBytes(source.resolve(id+".online"))),"Origem preservada pela copia");
        repository.create("human");UUID other=find(source,id);
        Files.copy(source.resolve(other+".online"),source.resolve(id+".online.bak"),StandardCopyOption.REPLACE_EXISTING);
        var mismatched=repository.recoverBackup(id);boolean backupMatchesRequestedId=mismatched.id().equals(id);
        boolean wrongWriteRejected=false;try{repository.write(grant,2,mismatched.state());}catch(SecurityException expected){wrongWriteRejected=true;}
        require(wrongWriteRejected,"Grant impede gravar personagem diferente mesmo com backup trocado");
        AtomicInteger committed=new AtomicInteger(),conflicts=new AtomicInteger(),overlaps=new AtomicInteger();
        var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var workers=Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures=new ArrayList<>();
            for(int i=0;i<2;i++)futures.add(workers.submit(()->{
                ready.countDown();try{start.await();repository.write(grant,2,second.state());committed.incrementAndGet();}
                catch(java.nio.channels.OverlappingFileLockException expected){overlaps.incrementAndGet();}
                catch(IOException expected){conflicts.incrementAndGet();}
                catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new RuntimeException(interrupted);}
            }));
            require(ready.await(3,TimeUnit.SECONDS),"Workers limitados iniciados");start.countDown();
            for(var future:futures)future.get(5,TimeUnit.SECONDS);
        } finally {workers.shutdownNow();require(workers.awaitTermination(3,TimeUnit.SECONDS),"Workers encerrados");}
        require(committed.get()==1&&conflicts.get()+overlaps.get()==1,"Exatamente uma revisao concorrente confirmada");
        require(repository.load(id).revision()==3,"Documento final valido sem escrita perdida");
        String json="{\"audit_status\":\"PASS_CHARACTERIZATION\",\"production_changed\":false,\"roundtrip\":true,\"cas_conflict\":true,\"ephemeral_grant\":true,\"synthetic_copy\":true,\"concurrent_commits\":"+committed.get()
            +",\"io_conflicts\":"+conflicts.get()+",\"same_jvm_lock_overlap\":"+overlaps.get()+",\"backup_matches_requested_id\":"+backupMatchesRequestedId
            +",\"wrong_character_write_rejected\":"+wrongWriteRejected+",\"idempotent_replay_supported\":false,\"adr_approved\":false}";
        Files.writeString(out.resolve("characterization.json"),json);System.out.println(json);
        System.out.println("TASK193 characterizationOnly=true personalSavesTouched=false launcherIntegration=false");
    }
}
