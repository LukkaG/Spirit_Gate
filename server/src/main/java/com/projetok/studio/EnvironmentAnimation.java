package com.projetok.studio;
import java.util.*;

/** Animacao ambiental de autoria; o runtime recebe explicitamente o quadro base. */
public final class EnvironmentAnimation {
    /** Sequencia limitada de imagens existentes, com revisoes explicitas. */
    public record Sequence(List<String> frames,List<String> hashes,int duration,boolean loop){
        public Sequence {frames=List.copyOf(frames);hashes=List.copyOf(hashes);if(frames.isEmpty()||frames.size()>32||frames.size()!=hashes.size()||duration<30||duration>5000||frames.stream().anyMatch(s->!s.matches("[a-f0-9]{64}"))||hashes.stream().anyMatch(s->!s.matches("[a-f0-9]{64}")))throw new IllegalArgumentException("Sequencia invalida: 1..32 quadros, 30..5000 ms");}
        /** Relogio monotono externo, independente da quantidade de repaints. */
        public int index(long elapsedNanos){long tick=Math.max(0,elapsedNanos)/(duration*1_000_000L);return loop?(int)(tick%frames.size()):(int)Math.min(frames.size()-1,tick);}
    }
    private EnvironmentAnimation(){}
    /** Le propriedades declarativas, sem carregar ou executar arquivos. */
    public static Optional<Sequence> sequence(WorldDocument.Element e){var p=e.properties();if(!p.containsKey("animation.frames"))return Optional.empty();String loop=p.get("animation.loop");if(!Set.of("true","false").contains(loop==null?"":loop))throw new IllegalArgumentException("Loop invalido");return Optional.of(new Sequence(List.of(p.get("animation.frames").split(",")),List.of(p.getOrDefault("animation.hashes","").split(",")),Integer.parseInt(p.getOrDefault("animation.ms","0")),Boolean.parseBoolean(loop)));}
    /** Configura a sequencia conservando o asset base para exportacao estatica. */
    public static WorldDocument.Element configure(WorldDocument.Element e,Sequence s){if(e.kind()!=WorldDocument.Kind.OBJECT&&e.kind()!=WorldDocument.Kind.TILE)throw new IllegalArgumentException("Use objeto ou tile ambiental");var p=new HashMap<>(e.properties());p.put("animation.frames",String.join(",",s.frames()));p.put("animation.hashes",String.join(",",s.hashes()));p.put("animation.ms",""+s.duration());p.put("animation.loop",""+s.loop());return replace(e,p);}
    private static WorldDocument.Element replace(WorldDocument.Element e,Map<String,String> p){return new WorldDocument.Element(e.id(),e.kind(),e.asset(),e.sha(),e.crop(),e.x(),e.y(),e.width(),e.height(),e.layer(),e.visible(),e.locked(),e.reference(),e.direction(),p);}
    /** Remove somente propriedades de previa da copia exportada; projeto permanece intacto. */
    public static WorldDocument staticExport(WorldDocument d){return d.withElements(d.elements().stream().map(e->{var p=new HashMap<>(e.properties());p.keySet().removeIf(k->k.startsWith("animation."));return replace(e,p);}).toList());}
}
