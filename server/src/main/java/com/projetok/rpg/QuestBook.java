package com.projetok.rpg;

import java.util.*;

/** Contrato declarativo de autoria, independente do save V2 e sem scripts executáveis. */
public record QuestBook(int version,List<QuestBook.Draft> quests) {
    public QuestBook {
        quests=List.copyOf(quests);
        if(version!=1||quests.isEmpty()||quests.size()>64)throw new IllegalArgumentException("Versão/quantidade de missões inválida");
    }
    /** Condições consultam os catálogos de produção; vazio significa qualquer raça/domínio. */
    public record Draft(String id,String name,String description,String npc,String region,
            String race,int minimumLevel,String domain,boolean unchosenDomain,
            List<String> prerequisites,QuestService.Definition definition,boolean resetContract) {
        public Draft {
            prerequisites=List.copyOf(prerequisites);Objects.requireNonNull(definition);
            if(!id.matches("[a-z][a-z0-9_]{0,30}")||name.isBlank()||name.length()>80||description.length()>4000
                ||minimumLevel<1||minimumLevel>1000||prerequisites.size()>32)throw new IllegalArgumentException("Identidade/requisitos inválidos");
            Objects.requireNonNull(npc);Objects.requireNonNull(region);Objects.requireNonNull(race);Objects.requireNonNull(domain);
        }
        /** Avaliação sem mutação; a escolha de domínio continua exclusiva do domínio real. */
        public boolean eligible(CharacterIdentity identity,long xp,DomainProgression progression,Set<String> completed) {
            return (race.isEmpty()||race.equals(identity.race()))&&RpgProgression.builtin().level(xp)>=minimumLevel
                &&(domain.isEmpty()||domain.equals(progression.domain()))&&(!unchosenDomain||!progression.chosen())
                &&completed.containsAll(prerequisites);
        }
    }
    /** Diagnósticos determinísticos, sem aplicar recompensas ou modificar personagens. */
    public List<String> validate() {
        List<String> errors=new ArrayList<>();Map<String,Draft> byId=new LinkedHashMap<>();RpgContent content=RpgContent.builtin();
        for(var q:quests) {
            if(byId.putIfAbsent(q.id(),q)!=null)errors.add(q.id()+": ID duplicado");
            try {
                content.get("npc",q.npc());content.get("region",q.region());
                if(!q.race().isEmpty())CharacterRaces.get(q.race());
                if(!q.domain().isEmpty()) {
                    var domain=Domains.get(q.domain());
                    if(!domain.race().equals(q.race()))throw new IllegalArgumentException("Domínio incompatível com a raça");
                    if(q.minimumLevel()<DomainProgression.INITIATION_LEVEL)throw new IllegalArgumentException("Domínio exige nível 15");
                    if(q.unchosenDomain())throw new IllegalArgumentException("Condições de domínio contraditórias");
                }
                QuestService.validateDefinition(content,q.definition());
                if(new HashSet<>(q.prerequisites()).size()!=q.prerequisites().size())throw new IllegalArgumentException("Pré-requisito repetido");
            }catch(IllegalArgumentException e){errors.add(q.id()+": "+e.getMessage());}
        }
        for(var q:quests)for(String previous:q.prerequisites())if(!byId.containsKey(previous))errors.add(q.id()+": Pré-requisito ausente: "+previous);
        Set<String> done=new HashSet<>(),active=new HashSet<>();for(String id:byId.keySet())if(cycle(id,byId,done,active)){errors.add("Dependência circular: "+id);break;}
        return List.copyOf(errors);
    }
    private static boolean cycle(String id,Map<String,Draft> map,Set<String> done,Set<String> active) {
        if(done.contains(id)||!map.containsKey(id))return false;if(!active.add(id))return true;
        for(String next:map.get(id).prerequisites())if(cycle(next,map,done,active))return true;
        active.remove(id);done.add(id);return false;
    }
    /** Recusa o documento inteiro antes de qualquer integração. */
    public void requireValid(){var errors=validate();if(!errors.isEmpty())throw new IllegalArgumentException(String.join("\n",errors));}
}
