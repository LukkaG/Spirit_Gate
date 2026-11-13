import com.projetok.multiplayer.*;
import com.projetok.prototype.rpg.*;
import com.projetok.studio.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

/** Prova loopback com o JAR e runtime de cada pacote, sem alterar seus arquivos. */
public final class CorujaoSevenPackageProbe {
    public static void main(String[] args)throws Exception {
        var root=Path.of(args[1]);var evidence=Path.of(args[2]);Files.createDirectories(evidence);
        if(args[0].equals("server")) {
            System.setProperty("spiritgate.rpg.dataDir",evidence.resolve("isolated-data").toString());
            try(var server=LauncherSession.startExpanded(InetAddress.getLoopbackAddress(),0,CosmeticCatalog.load(root.resolve("assets/dev/generated/corujao-iv-v1")),root)) {
                Files.writeString(evidence.resolve("port.txt"),""+server.port());
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(35);boolean observed=false;
                while(System.nanoTime()<end){if(server.failure()!=null)throw new AssertionError(server.failure());if(server.players()==2)observed=true;if(observed&&server.players()==0)break;Thread.sleep(20);}
                if(!observed)throw new AssertionError("Dois processos clientes não observados");
                Files.writeString(evidence.resolve("server.json"),"{\"twoClients\":true,\"loopbackOnly\":true,\"failure\":null}");
            }
        } else {
            boolean guest=args[3].equals("human");System.setProperty("spiritgate.guest",Boolean.toString(guest));
            var assets=AssetCatalog.scan(root,null);
            int port=Integer.parseInt(Files.readString(evidence.resolve("port.txt")));
            try(var client=LocalClient.gameplay(InetAddress.getLoopbackAddress(),port,new SessionCharacterSelection(guest?"Pacote Human":"Pacote Reaper",args[3],"none","none"),false,false,null,assets)) {
                client.revisions().renderer(p->CompletableFuture.completedFuture(null));
                long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
                while(System.nanoTime()<end&&(client.snapshot()==null||client.playerSheet()==null||client.appearances().size()!=2)){client.measureLatency();Thread.sleep(20);}
                if(client.snapshot()==null||client.playerSheet()==null||client.appearances().size()!=2)throw new AssertionError(client.problem());
                var ids=client.appearances().values().stream().map(Protocol.Appearance::race).collect(java.util.stream.Collectors.toSet());
                if(!ids.equals(java.util.Set.of("reaper","human")))throw new AssertionError(ids);
                if(guest)try{LauncherSession.startExpanded(InetAddress.getLoopbackAddress(),0,null,root);throw new AssertionError("Convidado hospedou");}catch(IllegalStateException expected){}
                Files.writeString(evidence.resolve(args[3]+".json"),"{\"session\":"+client.id()+",\"gameplay\":true,\"appearances\":2,\"level\":"+client.playerSheet().progress().level()+",\"guest\":"+guest+"}");
                Thread.sleep(400);
            }
        }
    }
}
