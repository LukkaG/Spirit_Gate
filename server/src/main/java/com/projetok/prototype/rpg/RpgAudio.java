package com.projetok.prototype.rpg;
import com.projetok.rpg.RpgPreferences;
import javax.sound.sampled.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
/** Tons originais sintetizados, fila limitada e fallback silencioso. */
public final class RpgAudio implements AutoCloseable {
    public enum Cue { UI,HIT,HURT,HEAL,LEVEL,AMBIENT }
    @FunctionalInterface public interface Backend{void play(Cue cue,int volume)throws Exception;}
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(8),r->{Thread t=new Thread(r,"spirit-gate-audio");t.setDaemon(true);return t;},new ThreadPoolExecutor.DiscardPolicy());
    private final Backend backend;private volatile RpgPreferences preferences;private final AtomicInteger unavailable=new AtomicInteger();
    private volatile boolean suspended;
    /** Silencia eventos novos e já enfileirados durante perda de foco ou pausa. */
    public void suspended(boolean value){suspended=value;}
    public RpgAudio(RpgPreferences preferences){this(preferences,RpgAudio::tone);}
    public RpgAudio(RpgPreferences preferences,Backend backend){this.preferences=preferences;this.backend=backend;}
    public void preferences(RpgPreferences value){preferences=value;}
    public int unavailableCount(){return unavailable.get();}
    CompletableFuture<Void> flush(){var result=new CompletableFuture<Void>();worker.execute(()->result.complete(null));return result;}
    public void play(Cue cue){var value=preferences;if(suspended||value.muted()||value.volume()==0||worker.isShutdown())return;worker.execute(()->{try{var current=preferences;if(!suspended&&!current.muted()&&current.volume()>0)backend.play(cue,current.volume());}catch(Exception error){unavailable.incrementAndGet();}});}
    private static void tone(Cue cue,int volume)throws Exception{
        float rate=22050;int samples=2205;byte[] pcm=new byte[samples*2];int hz=switch(cue){case UI->500;case HIT->260;case HURT->150;case HEAL->660;case LEVEL->880;case AMBIENT->220;};
        for(int i=0;i<samples;i++){double envelope=Math.sin(Math.PI*i/samples);short sample=(short)(Math.sin(2*Math.PI*hz*i/rate)*envelope*volume/100.0*4000);pcm[i*2]=(byte)sample;pcm[i*2+1]=(byte)(sample>>8);}
        try(Clip clip=AudioSystem.getClip()){clip.open(new AudioFormat(rate,16,1,true,false),pcm,0,pcm.length);clip.start();clip.drain();clip.stop();}
    }
    public void close(){worker.shutdownNow();}
}
