package com.projetok.multiplayer;

import com.projetok.rpg.RpgSkillCatalog;

/** Feedback da última intenção local; eventos existentes não são um ACK correlacionado por intenção. */
final class NetworkSkillSubmission {
    enum Status { NONE, WAITING, EVENT_RECEIVED, STATE_RECEIVED, UNKNOWN, NOT_SENT }
    private Status status=Status.NONE;
    private String message="",name="",skill="";
    private int player,slot,cooldown,sentry;
    private long floor,tick,began;
    Status status(){return status;}
    String message(){return message;}
    int waitingSlot(){return status==Status.WAITING?slot:0;}

    /** Enfileirar um pacote produz somente espera, nunca a indicação de aceite. */
    void sent(RpgSkillCatalog.Definition definition,SkillProtocol.State state,int self,long now,boolean queued){
        name=definition.name();skill=definition.id();slot=definition.slot();player=self;began=now;
        floor=state==null?0:state.events().stream().mapToLong(SkillProtocol.Event::sequence).max().orElse(0);
        tick=state==null?0:state.tick();cooldown=state==null?0:state.cooldowns().get(slot-1);sentry=state==null?0:state.sentryShots();
        status=queued?Status.WAITING:Status.NOT_SENT;
        message=queued?name+": enviada; aguardando confirmação.":name+": não foi possível enviar pelo cliente.";
    }
    /** Só descreve o dado observado; sem ACK não identifica rejeição nem garante causalidade da intenção. */
    void refresh(SkillProtocol.State state,long now){
        if(status!=Status.WAITING)return;
        if(state!=null){
            boolean event=state.events().stream().anyMatch(e->e.sequence()>floor&&e.tick()>=tick&&e.player()==player&&e.skill().equals(skill));
            if(event){status=Status.EVENT_RECEIVED;message="Evento de "+name+" recebido do servidor.";return;}
            if(state.cooldowns().get(slot-1)>cooldown){status=Status.STATE_RECEIVED;message="Recarga de "+name+" atualizada pelo servidor.";return;}
            if(skill.equals("aetheri_2")&&(state.sentryShots()>0&&sentry==0||state.sentryShots()==0&&sentry>0)){
                status=Status.STATE_RECEIVED;message=state.sentryShots()>0?"Sentinela ativa no estado do servidor.":"Sentinela não está mais ativa no estado do servidor.";return;
            }
        }
        if(now-began>=3_000_000_000L){status=Status.UNKNOWN;message=name+": sem confirmação identificável; resultado desconhecido.";}
    }
    /** Viagem/desconexão não podem atribuir dados de outra sessão à intenção anterior. */
    void clear(){status=Status.NONE;message="";skill="";}
}
