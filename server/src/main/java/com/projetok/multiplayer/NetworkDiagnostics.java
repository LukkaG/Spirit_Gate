package com.projetok.multiplayer;

import java.util.Arrays;

/** Bounded opt-in local measurements; all durations use monotonic nanoseconds. */
public final class NetworkDiagnostics {
    public static final String PROPERTY="spiritgate.network.debug";
    public final boolean enabled=Boolean.getBoolean(PROPERTY);
    public final Samples rtt=new Samples(),snapshotIntervals=new Samples(),ticks=new Samples(),
            tickIntervals=new Samples(),paintIntervals=new Samples(),paints=new Samples(),writes=new Samples();
    private volatile long received;
    public volatile long staleSnapshots,replacedSnapshots,rejectedMessages;
    public volatile int outboundQueue,commandQueue,controlQueue;
    public final java.util.concurrent.atomic.AtomicLong bytesWritten=new java.util.concurrent.atomic.AtomicLong(),
            framesWritten=new java.util.concurrent.atomic.AtomicLong(),transportFailures=new java.util.concurrent.atomic.AtomicLong();
    public void write(java.io.OutputStream stream,Protocol.Message message)throws java.io.IOException {
        if(!enabled){Protocol.write(stream,message);return;}
        Protocol.write(new java.io.FilterOutputStream(stream){
            @Override public void write(int value)throws java.io.IOException{out.write(value);bytesWritten.incrementAndGet();}
            @Override public void write(byte[] bytes,int offset,int length)throws java.io.IOException{out.write(bytes,offset,length);bytesWritten.addAndGet(length);}
        },message);framesWritten.incrementAndGet();
    }
    public String summary(){return String.format(java.util.Locale.ROOT,
            "RTT med/p95=%.2f/%.2fms snapshot med/p95=%.2f/%.2fms age=%.2fms tick med/p95=%.3f/%.3fms tickHz=%.2f paint med/p95=%.3f/%.3fms FPS=%.2f queues(out/cmd/control)=%d/%d/%d replaced=%d stale=%d bytes=%d frames=%d rejected=%d",
            rtt.percentileMillis(.5),rtt.percentileMillis(.95),snapshotIntervals.percentileMillis(.5),snapshotIntervals.percentileMillis(.95),snapshotAgeMillis(System.nanoTime()),ticks.percentileMillis(.5),ticks.percentileMillis(.95),1000/tickIntervals.percentileMillis(.5),paints.percentileMillis(.5),paints.percentileMillis(.95),1000/paintIntervals.percentileMillis(.5),outboundQueue,commandQueue,controlQueue,replacedSnapshots,staleSnapshots,bytesWritten.get(),framesWritten.get(),rejectedMessages);}
    private static String number(double value){return Double.isFinite(value)?Double.toString(value):"null";}
    /** Evento local limitado; sem nomes, conteúdo, paths ou identificadores de sessão. */
    public String json(){return "{\"schema\":1,\"kind\":\"network\",\"enabled\":"+enabled
            +",\"tick_p50_ms\":"+number(ticks.percentileMillis(.5))+",\"tick_p95_ms\":"+number(ticks.percentileMillis(.95))
            +",\"paint_p95_ms\":"+number(paints.percentileMillis(.95))+",\"rtt_p95_ms\":"+number(rtt.percentileMillis(.95))
            +",\"snapshot_interval_p95_ms\":"+number(snapshotIntervals.percentileMillis(.95))
            +",\"outbound_queue\":"+outboundQueue+",\"command_queue\":"+commandQueue+",\"control_queue\":"+controlQueue
            +",\"replaced_snapshots\":"+replacedSnapshots+",\"stale_snapshots\":"+staleSnapshots
            +",\"rejected_messages\":"+rejectedMessages+",\"transport_failures\":"+transportFailures.get()
            +",\"bytes_written\":"+bytesWritten.get()+",\"frames_written\":"+framesWritten.get()
            +",\"tick_samples\":"+ticks.count()+",\"paint_samples\":"+paints.count()+"}";}
    public void received(long now){long previous=received;received=now;if(previous!=0)snapshotIntervals.add(now-previous);}
    public long receivedNanos(){return received;}
    public double snapshotAgeMillis(long now){return received==0?Double.NaN:Math.max(0,now-received)/1e6;}
    public static final class Samples {
        private final long[] values=new long[256];private int count,index;
        public synchronized void add(long nanos){if(nanos>=0){values[index]=nanos;index=(index+1)%values.length;count=Math.min(count+1,values.length);}}
        public synchronized double percentileMillis(double q){if(count==0)return Double.NaN;long[] copy=Arrays.copyOf(values,count);Arrays.sort(copy);return copy[Math.min(count-1,Math.max(0,(int)Math.ceil(q*count)-1))]/1e6;}
        public synchronized int count(){return count;}
    }
}
