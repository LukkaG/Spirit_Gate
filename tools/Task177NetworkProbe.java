import com.projetok.multiplayer.*;
import java.net.*;
import java.time.*;
import java.util.*;
/** Same two-client workload before/after; no cross-machine clock arithmetic. */
public class Task177NetworkProbe {
    public static void main(String[] args)throws Exception {
        var rtt=new ArrayList<Double>();var gaps=new ArrayList<Double>();var ticks=new ArrayList<Double>();
        var bean=java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        long cpu0=bean instanceof com.sun.management.OperatingSystemMXBean b?b.getProcessCpuTime():0;
        long started=System.nanoTime();
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(List.of("reaper","human"),null,true,20))){
            server.start();
            try(var a=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Probe A",false);var b=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Probe B",false)){
                a.await(s->s.actors().size()>=2,Duration.ofSeconds(3));
                long lastTick=-1,lastReceipt=0;int peakQueue=0;
                for(int i=0;i<100;i++){
                    a.measureLatency();double previous=a.latencyMillis();long wait=System.nanoTime();
                    while(System.nanoTime()-wait<200_000_000L){
                        var state=a.snapshot();long now=System.nanoTime();
                        if(state.tick()!=lastTick){if(lastReceipt!=0)gaps.add((now-lastReceipt)/1e6);lastReceipt=now;lastTick=state.tick();}
                        peakQueue=Math.max(peakQueue,server.pendingCommands());Thread.sleep(1);
                    }
                    if(Double.isFinite(a.latencyMillis()))rtt.add(a.latencyMillis());
                    a.send(Protocol.Action.MOVE,i%20<10?1:-1,0);b.send(Protocol.Action.MOVE,0,i%20<10?-1:1);
                }
                System.out.printf(Locale.ROOT,"rttMedianMs=%.3f rttP95Ms=%.3f snapshotMedianMs=%.3f snapshotP95Ms=%.3f peakCommands=%d ticks=%d%n",p(rtt,.5),p(rtt,.95),p(gaps,.5),p(gaps,.95),peakQueue,server.snapshot().tick());
                if(Boolean.getBoolean("spiritgate.network.debug"))for(var instance:List.of(server,a))try{var diagnostics=instance.getClass().getMethod("diagnostics").invoke(instance);System.out.println(instance.getClass().getSimpleName()+" "+diagnostics.getClass().getMethod("summary").invoke(diagnostics));}catch(NoSuchMethodException ignored){}
                var threadBean=java.lang.management.ManagementFactory.getThreadMXBean();long workerCpu=0,workerBytes=0;
                for(long id:threadBean.getAllThreadIds()){var info=threadBean.getThreadInfo(id);if(info!=null&&info.getThreadName().startsWith("coop-")){workerCpu+=Math.max(0,threadBean.getThreadCpuTime(id));if(threadBean instanceof com.sun.management.ThreadMXBean allocation&&allocation.isThreadAllocatedMemorySupported())workerBytes+=Math.max(0,allocation.getThreadAllocatedBytes(id));}}
                System.out.printf(Locale.ROOT,"activeCoopThreadsCpuMs=%.3f activeCoopThreadsAllocatedBytes=%d%n",workerCpu/1e6,workerBytes);
                if(server.failure()!=null)throw new AssertionError("Server failure",server.failure());
                if(!a.connected()||!b.connected())throw new AssertionError("Disconnected: "+a.problem()+" / "+b.problem());
            }
        }
        long cpu=bean instanceof com.sun.management.OperatingSystemMXBean b?b.getProcessCpuTime()-cpu0:0;
        System.out.printf(Locale.ROOT,"processCpuMs=%.1f wallMs=%.1f%n",cpu/1e6,(System.nanoTime()-started)/1e6);
    }
    static double p(List<Double> v,double q){v.sort(Double::compare);return v.isEmpty()?Double.NaN:v.get(Math.min(v.size()-1,(int)Math.ceil(v.size()*q)-1));}
}
