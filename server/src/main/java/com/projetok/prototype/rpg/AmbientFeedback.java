package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import java.util.function.Consumer;

/** Feedback por mudança de snapshot confirmado, nunca por repaint. */
final class AmbientFeedback {
    private RpgWorld.View previous;
    private long sequence;
    private final Consumer<RpgAudio.Cue> output;
    AmbientFeedback(Consumer<RpgAudio.Cue> output){this.output=output;}
    /** Consome cada evento de rejeição uma vez e não reproduz estado histórico inicial. */
    void update(RpgWorld.View view){
        if(previous!=null){
            if(!view.state().region().equals(previous.state().region()))output.accept(RpgAudio.Cue.UI);
            else if(view.state().flags().stream().anyMatch(f->f.startsWith("quest-done:")&&!previous.state().flags().contains(f)))output.accept(RpgAudio.Cue.LEVEL);
            else if(view.state().coins()!=previous.state().coins()||view.state().inventory().values().stream().mapToInt(Integer::intValue).sum()>previous.state().inventory().values().stream().mapToInt(Integer::intValue).sum())output.accept(RpgAudio.Cue.UI);
            for(var event:view.events())if(event.sequence()>sequence&&event.kind()==RpgCombatEvent.Kind.REJECT)output.accept(RpgAudio.Cue.UI);
        }
        sequence=Math.max(sequence,view.events().stream().mapToLong(RpgCombatEvent::sequence).max().orElse(0));previous=view;
    }
}
