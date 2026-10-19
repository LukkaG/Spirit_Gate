package com.projetok.multiplayer;
import java.net.InetAddress;
import java.time.Duration;
/** Executado como processo-filho nos testes; nao entra em distribuicoes. */
public final class TwoProcessProbe {
    public static void main(String[] args)throws Exception{
        try(var client=new LocalClient(InetAddress.getLoopbackAddress(),Integer.parseInt(args[0]),args[1])){
            var first=client.await(s->s.actors().stream().filter(a->!a.boss()).count()==2,Duration.ofSeconds(5));client.send(Protocol.Action.MOVE,1,0);
            var moved=client.await(s->s.tick()>=first.tick()+20,Duration.ofSeconds(5));boolean moving=moved.actors().stream().anyMatch(a->a.id()==client.id()&&a.x()>140);if(!moving)throw new IllegalStateException("Nao moveu");
            System.out.println("{\"session\":"+client.id()+",\"players\":2,\"moved\":true,\"tick\":"+moved.tick()+"}");
        }
    }
}
