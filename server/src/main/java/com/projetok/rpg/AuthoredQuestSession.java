package com.projetok.rpg;

import java.util.*;

/** Ledger em memória de autoria; os objetivos são executados pelo QuestService real. */
public final class AuthoredQuestSession {
    private final QuestService service;
    private final QuestBook book;
    private final Map<String,QuestBook.Draft> drafts=new TreeMap<>();
    private final Map<String,Integer> progress=new TreeMap<>();
    private final Set<String> flags=new TreeSet<>();
    public AuthoredQuestSession(QuestBook book){book.requireValid();this.book=book;service=new QuestService(RpgContent.builtin(),book);book.quests().forEach(q->drafts.put(q.id(),q));}
    /** Cópia somente em memória de um ledger interno já validado. */
    public AuthoredQuestSession copy(){var next=new AuthoredQuestSession(book);next.progress.putAll(progress);next.flags.addAll(flags);return next;}
    public QuestBook.Draft draft(String id){var q=drafts.get(id);if(q==null)throw new IllegalArgumentException("Missão autorada ausente");return q;}
    public boolean eligible(String id,CharacterIdentity identity,long xp,DomainProgression domain){Set<String> completed=new HashSet<>();for(String f:flags)if(f.startsWith("quest-done:"))completed.add(f.substring(11));return draft(id).eligible(identity,xp,domain,completed);}
    public boolean accept(String id){return service.accept(id,progress,flags);}
    public boolean complete(String id){return service.complete(id,progress,flags);}
    public QuestService.State state(String id){return service.state(id,progress,flags);}
    public void event(String type,String target,String source,long sequence){service.event(type,target,source,sequence,progress,flags);}
    public void refresh(InventoryService inventory,String region){service.refresh(progress,flags,inventory,region);}
    public List<QuestService.View> views(){return service.views(progress,flags);}
}
