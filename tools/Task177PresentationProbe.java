import com.projetok.multiplayer.*;
import java.util.*;
/** Deterministic presentation comparison, identical 10 Hz path, no VPN claims. */
public class Task177PresentationProbe {
    static Protocol.State state(long tick){float x=100+tick*2;return new Protocol.State(tick,List.of(new Protocol.Actor(1,"Local",100,300,100,100,100,0,false),new Protocol.Actor(2,"Remote",x,200,100,100,100,0,false)));}
    public static void main(String[] args){
        var d=new NetworkDiagnostics();var t=new RemoteTimeline(d);double oldSum=0,newSum=0;int count=0;
        long origin=1_000_000_000L;var previous=state(0);t.offer(previous,origin,"forest");
        for(int i=1;i<=100;i++){
            long receipt=origin+i*100_000_000L;var current=state(i*3);d.received(receipt);t.offer(current,receipt,"forest");var old=new RemotePresentation(previous,current);
            for(int frame=0;frame<6;frame++){
                double offset=frame*100d/6;long now=receipt+(long)(offset*1e6);
                double truth=100+(i*3+offset*.03)*2;
                double baseline=old.sample(offset/100,1).actors().get(1).x();double updated=t.sample(now,1).actors().get(1).x();
                if(i>5){oldSum+=(truth-baseline)/.06;newSum+=(truth-updated)/.06;count++;}
            }
            previous=current;
        }
        System.out.printf(Locale.ROOT,"Controlled regular 10Hz, 60FPS, constant 60 units/s: mean presentation lag baseline=%.3fms updated=%.3fms samples=%d buffer=%.2fms%n",oldSum/count,newSum/count,count,t.bufferMillis());
    }
}
