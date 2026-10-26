package com.projetok.rpg;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.MovementService;
import java.util.List;

/** Mundo offline: uma thread proprietaria, geometria compartilhada com a arena. */
public final class RpgWorld {
    private float physicalRadius=18;
    private com.projetok.domain.movement.SessionCollisionProfile sessionProfile;
    private boolean contentFork;
    /** Explicit session-only geometry; historical offline/save contract remains 18. */
    public void useNetworkFootprint(){configureSyntheticCollision(com.projetok.domain.movement.SessionCollisionProfile.DEFAULT);}
    public void configureSyntheticCollision(com.projetok.domain.movement.SessionCollisionProfile profile){if(!isSynthetic()||ticks!=0||sessionProfile!=null)throw new IllegalStateException("Session geometry only before simulation");java.util.Objects.requireNonNull(profile);var candidate=com.projetok.domain.movement.RpgCollisionProfile.scenery(space,profile);var position=body.getPosition();if(!new CollisionService().isFree(body,position.x(),position.y(),profile.actorRadius(),candidate))throw new IllegalArgumentException("Perfil obstruiria o spawn");sessionProfile=profile;physicalRadius=profile.actorRadius();space=candidate;}
    public float physicalRadius(){return physicalRadius;}
    /** Preserve historical protocol coordinate bounds (18..942 / 18..622).
     * The smaller ground body must not expand the wire format's valid map area. */
    private CollisionService.Space networkBounds(CollisionService.Space source){
        return sessionProfile==null?source:com.projetok.domain.movement.RpgCollisionProfile.boundaries(source,physicalRadius);
    }
    private CollisionService.Space networkScenery(CollisionService.Space raw){return sessionProfile==null?raw:com.projetok.domain.movement.RpgCollisionProfile.scenery(raw,sessionProfile);}
    /** Owner-thread content swap candidate. Caller validates all participants before activating.
     * No disk IO, personal save restore, or mutation of the old world. */
    public RpgWorld forkSyntheticRevision(RpgSceneSet next,com.projetok.domain.movement.SessionCollisionProfile profile){
        if(!isSynthetic()||scenes==null||!next.maps().containsKey(region))throw new IllegalStateException("Regiao ocupada removida ou sessao nao sintetica");
        var copy=new RpgWorld(identity,next);copy.configureSyntheticCollision(profile);copy.region=region;copy.space=copy.networkScenery(next.maps().get(region).space());copy.body.setPosition(body.getPosition());
        copy.hp=hp;copy.xp=xp;copy.coins=coins;copy.ticks=ticks;copy.domain=domain;copy.stamina=stamina;copy.facingX=facingX;copy.facingY=facingY;
        copy.inventory.replace(InventoryService.restore(copy.items,20,inventory.snapshot()));copy.equipment.clear();copy.equipment.putAll(equipment);copy.quests.putAll(quests);copy.flags.clear();copy.flags.addAll(flags);
        copy.abilityReadyTick=abilityReadyTick;copy.evadeUntilTick=evadeUntilTick;copy.attackReadyTick=attackReadyTick;copy.portalReadyTick=portalReadyTick;copy.potionReadyTick=potionReadyTick;
        copy.eventSequence=eventSequence;copy.events.addAll(events);if(authoredQuests!=null)copy.authoredQuests=authoredQuests.copy();copy.contentFork=true;
        if(!new CollisionService().isFree(copy.body,copy.body.getPosition().x(),copy.body.getPosition().y(),profile.actorRadius(),copy.space))throw new IllegalArgumentException("Revisao ocuparia um jogador");
        return copy;
    }
    public boolean syntheticPositionFree(List<CollisionService.Body> bodies){if(!isSynthetic())throw new IllegalStateException();var p=body.getPosition();return new CollisionService().isFree(body,p.x(),p.y(),physicalRadius,new CollisionService.Space(space.width(),space.height(),space.obstacles(),bodies));}
    public void restoreSyntheticSpawn(){if(!isSynthetic()||scenes==null)throw new IllegalStateException();var target=scenes.maps().get(region).spawn();if(!new CollisionService().isFree(body,target.x(),target.y(),physicalRadius,space))throw new IllegalArgumentException("Spawn ocupado");body.setPosition(target);}
    private final CharacterIdentity identity;
    private final java.util.ArrayDeque<RpgCombatEvent> events=new java.util.ArrayDeque<>();
    private long eventSequence;
    private void event(RpgCombatEvent.Kind kind,int target,float x,float y,int amount) {
        events.addLast(new RpgCombatEvent(++eventSequence,ticks,kind,target,x,y,amount));
        while(events.size()>64)events.removeFirst();
    }
    private final RpgContent content = RpgContent.builtin();
    private final ItemCatalog items = new ItemCatalog(content);
    private final RpgProgression progression = RpgProgression.builtin();
    private final InventoryService inventory = new InventoryService(items,20);
    private int hp,coins;
    private long xp;
    private String region="village";
    private final java.util.Map<String,String> equipment=new java.util.TreeMap<>();
    private final java.util.Map<String,Integer> quests=new java.util.TreeMap<>();
    private final java.util.Set<String> flags=new java.util.TreeSet<>();
    private long potionReadyTick;
    private boolean transitioning;
    private int stamina=1000;
    private long abilityReadyTick;
    private long evadeUntilTick;
    private DomainProgression domain = DomainProgression.initial();
    private AuthoredQuestSession authoredQuests;
    /** Sessão autorada somente em memória, construída sem carregar um save pessoal. */
    public RpgWorld(CharacterIdentity identity,RpgSceneSet scenes,QuestBook book){this(identity,scenes);authoredQuests=new AuthoredQuestSession(book);}
    public boolean isSynthetic(){return scenes!=null||authoredQuests!=null;}
    /** Corpos externos de uma sessão sintética; nunca habilitado em personagem offline comum. */
    public void setSyntheticBodies(List<CollisionService.Body> bodies){if(!isSynthetic()||bodies.size()>65)throw new IllegalStateException("Corpos de sessão indisponíveis");space=networkBounds(new CollisionService.Space(space.width(),space.height(),space.obstacles(),bodies));}
    /** Checkpoint efêmero de autoria em mapa padrão; não escreve arquivos nem muda schema. */
    public RpgWorld forkSynthetic(){if(authoredQuests==null||scenes!=null)throw new IllegalStateException("Retomada em memória disponível apenas na sessão de quests com mapa padrão");var copy=restore(save());copy.authoredQuests=authoredQuests.copy();return copy;}
    public List<QuestService.View> authoredQuestViews(){return authoredQuests==null?List.of():authoredQuests.views();}
    private boolean authoredNpc(String id){return authoredQuests!=null&&hp>0&&!transitioning&&npcs.nearest(region,body.getPosition()).filter(n->n.id().equals(authoredQuests.draft(id).npc())&&lineClear(body.getPosition(),new Position(n.x(),n.y()))).isPresent();}
    public boolean acceptAuthoredQuest(String id){
        if(!authoredNpc(id)||!authoredQuests.eligible(id,identity,xp,domain)||!authoredQuests.accept(id))return false;
        authoredQuests.event("talk",authoredQuests.draft(id).npc(),"authored_accept",ticks+1);authoredQuests.refresh(inventory,region);message="Missão autorada aceita (sintética)";return true;
    }
    /** Pré-valida o plano inteiro antes de publicar a conclusão única. */
    public boolean completeAuthoredQuest(String id){
        if(!authoredNpc(id)||!authoredQuests.eligible(id,identity,xp,domain))return false;
        authoredQuests.refresh(inventory,region);if(authoredQuests.state(id)!=QuestService.State.READY)return false;
        QuestRewards.Plan plan;try{plan=QuestRewards.prepare(inventory,coins,xp,domain,authoredQuests.draft(id).definition());}
        catch(IllegalArgumentException|IllegalStateException|ArithmeticException e){message=e.getMessage();return false;}
        if(!authoredQuests.complete(id))return false;int oldMax=maxHp();inventory.replace(plan.inventory());coins=plan.coins();xp=plan.xp();domain=plan.domain();hp+=maxHp()-oldMax;message="Missão autorada concluída (sem persistência V2)";return true;
    }
    public static final int SHUNPO_LEVEL = 20;
    public static final int SHUNPO_COST = 25, SHUNPO_COOLDOWN = 90;
    private float facingX=1,facingY;
    public DomainProgression domainProgression() { return domain; }
    public boolean chooseDomain(String id) {
        if(hp==0||transitioning)return false;
        try { domain=domain.choose(identity.race(),level(),id); }
        catch(IllegalArgumentException|IllegalStateException rejected) { message=rejected.getMessage();return false; }
        message="Dominio escolhido: "+Domains.get(id).name();return true;
    }
    /** Dados de pagamento nao sao inventados pelo cliente offline. A regra pura de reset
     * fica em DomainProgression; a execucao paga depende do servico de carteira. */
    public String resetRequirements() {
        return "Reset: 35 Spirit Coins (R$ 35), missao de reset e intervalo de um mes. "
            +"Pagamento e missao ainda indisponiveis; nivel, itens e equipamentos serao preservados.";
    }
    public boolean canShunpo() { return level()>=SHUNPO_LEVEL; }
    /** Shunpo ofensivo: aproxima de alvo visivel sem atravessar geometria. */
    public boolean castAbility() {
        if(!shunpoReady())return false;
        var position=body.getPosition();
        var target=enemies.current(region).stream().filter(e->e.hp>0
            &&(!e.definition.id().equals("warden")||bossUnlocked())
            &&distance(position,e.getPosition())<=240&&lineClear(position,e.getPosition()))
            .min(java.util.Comparator.comparingDouble(e->distance(position,e.getPosition()))).orElse(null);
        if(target==null){message="Shunpo ofensivo exige um alvo visivel no alcance";return false;}
        var direction=movement.direction(position.x()-target.getPosition().x(),position.y()-target.getPosition().y());
        if(direction.x()==0&&direction.y()==0)direction=new Position(-facingX,-facingY);
        float x=target.getPosition().x()+direction.x()*42, y=target.getPosition().y()+direction.y()*42;
        if(!new CollisionService().isFree(body,x,y,physicalRadius,space)
                ||!lineClear(position,new Position(x,y))||!safeShunpoPath(position,new Position(x,y))){message="Destino do Shunpo bloqueado";return false;}
        body.setPosition(new Position(x,y));facingX=-direction.x();facingY=-direction.y();
        consumeShunpo();message="Shunpo ofensivo";return true;
    }
    private boolean shunpoReady() {
        if(!canShunpo()){message="Shunpo exige nivel 20 de personagem";return false;}
        if(hp<=0||transitioning){message="Shunpo indisponível neste estado";return false;}
        if(ticks<abilityReadyTick){message="Shunpo em recarga compartilhada";return false;}
        if(stamina<SHUNPO_COST*10){message="Vigor insuficiente para Shunpo";return false;}
        return true;
    }
    /** Valida o corpo inteiro ao longo da aproximação, usando o raio físico já existente. */
    private boolean safeShunpoPath(Position start,Position end){var collisions=new CollisionService();if(sessionProfile!=null)return collisions.canTraverseSegment(body,start,end,physicalRadius,space);int samples=Math.max(1,(int)Math.ceil(distance(start,end)/3));for(int i=1;i<=samples;i++)if(!collisions.isFree(body,start.x()+(end.x()-start.x())*i/samples,start.y()+(end.y()-start.y())*i/samples,physicalRadius,space))return false;return true;}
    private void consumeShunpo() {
        stamina-=SHUNPO_COST*10;abilityReadyTick=ticks+SHUNPO_COOLDOWN;
        flags.removeIf(f->f.startsWith("ability-until:"));flags.add("ability-until:"+abilityReadyTick);
    }
    private String message="Bem-vindo a Aurora";
    public int level() { return progression.level(xp); }
    public int maxHp() { return identity.baseHp()+(level()-1)*progression.hpPerLevel(); }
    public void awardExperience(long amount) {
        int previous=maxHp();long nextXp=progression.award(xp,amount);
        DomainProgression nextDomain=domain.award(amount);
        xp=nextXp;domain=nextDomain;
        if(hp>0) hp+=maxHp()-previous;
    }
    public void receiveDamage(int amount) { if(amount<0||amount>10000) throw new IllegalArgumentException("Dano invalido");
        if(amount>0&&hp>0&&ticks<evadeUntilTick){message="Shunpo: esquiva!";return;}
        int before=hp;hp=Math.max(0,hp-amount);
        if(before>hp)event(RpgCombatEvent.Kind.HIT,1,body.getPosition().x(),body.getPosition().y(),before-hp);
        if(before>0&&hp==0)event(RpgCombatEvent.Kind.KO,1,body.getPosition().x(),body.getPosition().y(),0); }
    public boolean use(String id) {
        ItemCatalog.Item item=items.get(id);
        if(item.category()!=ItemCatalog.Category.CONSUMABLE || hp==0 || hp==maxHp() || transitioning || ticks<potionReadyTick || inventory.count(id)==0) return false;
        int beforeHp=hp;inventory.remove(id,1); hp=Math.min(maxHp(),hp+item.healing()); potionReadyTick=ticks+60;
        flags.removeIf(f->f.startsWith("potion-until:")); flags.add("potion-until:"+potionReadyTick);
        event(RpgCombatEvent.Kind.HEAL,1,body.getPosition().x(),body.getPosition().y(),hp-beforeHp);
        message=item.name()+": vida recuperada"; return true;
    }
    public InventoryService inventory() { return inventory; }
    public boolean activate(String action){boolean accepted=switch(action){case "rpg.attack"->attack();case "rpg.unarmed"->punch();case "rpg.ability"->castAbility();case "rpg.secondary"->castSecondaryAbility();case "item.potion"->use("potion");default->throw new IllegalArgumentException("Acao de hotbar desconhecida");};if(!accepted)event(RpgCombatEvent.Kind.REJECT,1,body.getPosition().x(),body.getPosition().y(),0);return accepted;}
    public int attackPower() { return identity.baseAttack()+(level()-1)*progression.attackPerLevel()+equipmentPower("weapon"); }
    public int defense() { return equipmentPower("torso")+equipmentPower("legs")+equipmentPower("accessory"); }
    private int equipmentPower(String slot) { return equipment.containsKey(slot)?items.get(equipment.get(slot)).power():0; }
    public boolean equip(String id) {
        ItemCatalog.Item item=items.get(id);
        if(hp==0 || item.category()!=ItemCatalog.Category.EQUIPMENT || item.minimumLevel()>level()) return false;
        InventoryService copy=inventory.copy(); if(!copy.remove(id,1)) return false;
        String old=equipment.get(item.slot()); if(old!=null && !copy.add(old,1)) return false;
        inventory.replace(copy); equipment.put(item.slot(),id); return true;
    }
    public boolean unequip(String slot) {
        if(hp==0 || !equipment.containsKey(slot)) return false;
        InventoryService copy=inventory.copy(); if(!copy.add(equipment.get(slot),1)) return false;
        inventory.replace(copy); equipment.remove(slot); return true;
    }
    private final GameEntity body;
    public RpgWorld() { this(CharacterIdentity.create("Viajante", "reaper", "azure")); }
    public RpgWorld(CharacterIdentity identity) {this(identity,(RpgSceneSet)null);}
    /** Configuracao exclusiva de sessao sintetica; construtor padrao conserva a campanha. */
    public RpgWorld(CharacterIdentity identity,RpgSceneSet scenes) {
        this.scenes=scenes;
        this.identity = java.util.Objects.requireNonNull(identity);
        content.get("region", identity.initialRegion());
        if(scenes!=null)region=scenes.initialRegion();
        space=scenes==null?new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(380,180,120,120)),List.of()):scenes.maps().get(region).space();
        body = new GameEntity(1, identity.name(), scenes==null?new Position(140,320):scenes.maps().get(region).spawn()) { };
        enemies=scenes==null?new EnemyService(content,space):new EnemyService(content,space,scenes.enemies(),r->scenes.maps().get(r).space());
        npcs=scenes==null?new NpcService(content):new NpcService(content,scenes.npcs());
        portals=scenes==null?new PortalService(content):new PortalService(content,scenes.portals());
        objects=scenes==null?new WorldObjects(content,items):new WorldObjects(content,items,scenes.objects());
        hp=identity.baseHp(); inventory.add("potion",3);
        if(identity.race().equals("reaper"))equipment.put("weapon","small_sword");
        flags.add("visited:"+region);
    }
    private final MovementService movement = new MovementService(new CollisionService());
    private final RpgSceneSet scenes;
    private CollisionService.Space space;
    private EnemyService enemies;
    private SharedRpgEncounter sharedEncounter;
    /** Opt-in exclusivo de personagens sintéticos; o encontro controla IA e recompensas. */
    void attachEncounter(SharedRpgEncounter encounter,EnemyService population) {
        if(!isSynthetic()||sharedEncounter!=null||ticks!=0&&!contentFork)throw new IllegalStateException("Encontro indisponivel");
        contentFork=false;
        sharedEncounter=encounter;enemies=population;
    }
    void sharedKill(EnemyService.Enemy enemy){
        if(sharedEncounter==null)throw new IllegalStateException("Encontro ausente");
        if(authoredQuests!=null){authoredQuests.event("kill",enemy.definition.id(),enemy.spawn,enemy.generation);authoredQuests.refresh(inventory,region);}
    }
    private final LootService loot=new LootService(content,items);
    private java.util.random.RandomGenerator random;
    private long attackReadyTick;
    private final NpcService npcs;
    private final PortalService portals;
    private final WorldObjects objects;
    public boolean harvest(String id) {
        if(hp==0||transitioning)return false;
        var object=objects.views(region,ticks,flags).stream().filter(o->o.id().equals(id)&&o.available()&&distance(body.getPosition(),new Position(o.x(),o.y()))<=65).findFirst();
        if(object.isEmpty())return false;var o=object.get();if(!inventory.add(o.item(),o.amount())){message="Inventario cheio; objeto preservado";return false;}
        if(o.respawn()==0)flags.add("opened:"+o.id());else{flags.removeIf(f->f.startsWith("object-until:"+o.id()+":"));flags.add("object-until:"+o.id()+":"+(ticks+o.respawn()));}
        questService.refresh(quests,flags,inventory,region);if(authoredQuests!=null)authoredQuests.refresh(inventory,region);message="Coletado: "+items.get(o.item()).name()+" x"+o.amount();return true;
    }
    private long portalReadyTick;
    public boolean travel(String id) {
        var portal=portals.get(id);
        if(hp==0||transitioning||ticks<portalReadyTick||!portal.region().equals(region)||distance(body.getPosition(),new Position(portal.x(),portal.y()))>65)return false;
        if(portal.destination().equals("ruins")&&!quests.containsKey("sanctuary")){message="Conclua Ecos no bosque e aceite a missao do Santuario com a Ancia";return false;}
        if(!portal.quest().isEmpty()&&!quests.containsKey(portal.quest()))return false;
        CollisionService.Space destination=scenes==null?space:networkScenery(scenes.maps().get(portal.destination()).space());
        if(!new CollisionService().isFree(body,portal.arrivalX(),portal.arrivalY(),physicalRadius,destination))return false;
        transitioning=true;
        try{space=destination;region=portal.destination();flags.add("visited:"+region);body.setPosition(new Position(portal.arrivalX(),portal.arrivalY()));closeDialogue();portalReadyTick=ticks+30;if(region.equals("ruins"))flags.add("dungeon-started");questService.refresh(quests,flags,inventory,region);if(authoredQuests!=null)authoredQuests.refresh(inventory,region);message="Chegou a "+content.get("region",region).name();return true;}finally{transitioning=false;}
    }
    private boolean bossUnlocked(){return enemies.current("ruins").stream().filter(e->!e.definition.id().equals("warden")).allMatch(e->e.hp==0);}
    public boolean returnHome(){if(hp>0)return false;region=scenes==null?"village":scenes.initialRegion();if(scenes!=null)space=networkScenery(scenes.maps().get(region).space());body.setPosition(scenes==null?new Position(140,320):scenes.maps().get(region).spawn());hp=maxHp();stamina=1000;closeDialogue();message="Recuperado em Aurora. Progresso preservado.";return true;}
    private String activeNpc="";
    /** Mercadorias oferecidas pelo comerciante local, sem estoque persistente. */
    public static java.util.Set<String> merchantItems(){return java.util.Set.of("potion","sword","robe");}
    /** Cotação unitária usada tanto na transação quanto na apresentação. */
    public static int tradePrice(ItemCatalog.Item item,boolean buying){return buying?item.price():Math.max(1,item.price()/2);}
    public boolean trade(String id,int amount,boolean buying) {
        var item=items.get(id);if(amount<1||amount>99)throw new IllegalArgumentException("Quantidade de comercio invalida");
        if(hp==0||transitioning||!activeNpc.equals("trader")||npcs.nearest(region,body.getPosition()).filter(n->n.id().equals("trader")).isEmpty())return false;
        if(item.category()==ItemCatalog.Category.QUEST||item.price()==0||buying&&!merchantItems().contains(id))return false;
        int value=tradePrice(item,buying)*amount;var copy=inventory.copy();
        if(buying){if(coins<value||!copy.add(id,amount))return false;}else if(coins>1_000_000-value||!copy.remove(id,amount))return false;
        inventory.replace(copy);coins+=buying?-value:value;message=buying?"Compra realizada":"Venda realizada";return true;
    }
    private final DialogueService dialogues=new DialogueService();
    private final QuestService questService=new QuestService(content);
    public boolean acceptQuest(String id){if(!canTalkElder()||!questService.accept(id,quests,flags))return false;questService.event("talk","elder","accept_"+id,ticks+1,quests,flags);questService.refresh(quests,flags,inventory,region);if(authoredQuests!=null)authoredQuests.refresh(inventory,region);message="Missao aceita";return true;}
    public boolean completeQuest(String id){
        if(!canTalkElder())return false;questService.refresh(quests,flags,inventory,region);if(authoredQuests!=null)authoredQuests.refresh(inventory,region);if(questService.state(id,quests,flags)!=QuestService.State.READY)return false;
        QuestRewards.Plan plan;
        try{plan=QuestRewards.prepare(inventory,coins,xp,domain,questService.definition(id));}
        catch(IllegalArgumentException|IllegalStateException|ArithmeticException rejected){message=rejected.getMessage();return false;}
        int previousMax=maxHp();inventory.replace(plan.inventory());coins=plan.coins();xp=plan.xp();domain=plan.domain();hp+=maxHp()-previousMax;
        questService.complete(id,quests,flags);message="Missao concluida: XP, moedas e itens";return true;
    }
    private boolean canTalkElder(){return hp>0&&activeNpc.equals("elder")&&npcs.nearest(region,body.getPosition()).filter(n->n.id().equals("elder")).isPresent();}
    private boolean dialogueCondition(String condition){return switch(condition){case "always"->true;case "first_ready"->questService.state("first_steps",quests,flags)==QuestService.State.READY;case "first_done"->flags.contains("quest-done:first_steps");case "boss_ready"->questService.state("sanctuary",quests,flags)==QuestService.State.READY;default->false;};}
    private String dialogueNode="";
    public DialogueService.Node dialogue() { return dialogueNode.isEmpty()?null:dialogues.node(dialogueNode,this::dialogueCondition); }
    public void closeDialogue(){dialogueNode="";activeNpc="";}
    public boolean choose(int choice) {
        var node=dialogue();if(node==null||choice<0||choice>=node.choices().size()||hp==0)return false;
        if(npcs.nearest(region,body.getPosition()).filter(n->n.id().equals(activeNpc)).isEmpty()){closeDialogue();return false;}
        var selected=node.choices().get(choice);
        boolean accepted=switch(selected.action()){case "accept_first"->acceptQuest("first_steps");case "claim_first"->completeQuest("first_steps");case "accept_boss"->acceptQuest("sanctuary");case "claim_boss"->completeQuest("sanctuary");default->true;};
        if(!accepted){message="Missao indisponivel ou ja registrada";return false;}
        if(selected.target().equals("close"))closeDialogue();else dialogueNode=selected.target();return true;
    }
    public boolean interact() {return interact(true);}
    public boolean interactSyntheticNpc(){if(sharedEncounter==null)throw new IllegalStateException("Encontro ausente");return interact(false);}
    private boolean interact(boolean usePortals) {
        if(hp==0||transitioning)return false;
        var portal=portals.nearest(region,body.getPosition());if(usePortals&&portal.isPresent())return travel(portal.get().id());
        var npc=npcs.nearest(region,body.getPosition());
        if(npc.isEmpty()||!lineClear(body.getPosition(),new Position(npc.get().x(),npc.get().y()))){closeDialogue();var object=objects.views(region,ticks,flags).stream().filter(o->o.available()&&distance(body.getPosition(),new Position(o.x(),o.y()))<=65).findFirst();if(object.isPresent())return harvest(object.get().id());message="Aproxime-se de NPC, portal ou objeto e pressione F";return false;}
        activeNpc=npc.get().id();if(authoredQuests!=null)authoredQuests.event("talk",activeNpc,activeNpc,ticks+1);questService.event("talk",activeNpc,activeNpc,ticks+1,quests,flags);questService.refresh(quests,flags,inventory,region);if(authoredQuests!=null)authoredQuests.refresh(inventory,region);dialogueNode=npc.get().dialogue();message=npc.get().name()+": seja bem-vindo!";return true;
    }
    public RpgWorld(CharacterIdentity identity,java.util.random.RandomGenerator random) { this(identity);this.random=java.util.Objects.requireNonNull(random); }
    /** Ataque racial inicial; feixe de Spirit nao concede o dominio Luz. */
    public int basicAttackRange() {
        if(domain.domain().equals("bow_long"))return 280;
        if(domain.domain().equals("bow_short"))return 140;
        return switch(identity.race()){case "spirit"->180;case "reaper"->82;default->60;};
    }
    /** Qualquer raca pode optar por socos sem receber especializacao Marcial. */
    public boolean punch() { return strike(60,identity.baseAttack()+(level()-1)*progression.attackPerLevel(),true); }
    public boolean attack() { return strike(basicAttackRange(),attackPower()); }
    /** Shunpo defensivo com janela curta e recarga compartilhada. */
    public boolean castSecondaryAbility() {
        if(!shunpoReady())return false;
        var position=body.getPosition();
        movement.moveContinuous(body,-facingX*70,-facingY*70,physicalRadius,space);
        if(distance(position,body.getPosition())<1){message="Esquiva bloqueada";return false;}
        consumeShunpo();evadeUntilTick=ticks+6;
        flags.removeIf(f->f.startsWith("evade-until:"));flags.add("evade-until:"+evadeUntilTick);
        message="Shunpo defensivo";return true;
    }
    private boolean strike(int range,int power) {
        return strike(range,power,false);
    }
    private boolean strike(int range,int power,boolean unarmed) {
        if(hp==0||transitioning||ticks<attackReadyTick)return false;
        EnemyService.Enemy target=enemies.current(region).stream().filter(e->e.hp>0&&(!e.definition.id().equals("warden")||bossUnlocked())&&distance(body.getPosition(),e.getPosition())<= range&&lineClear(body.getPosition(),e.getPosition()))
            .min(java.util.Comparator.comparingDouble(e->distance(body.getPosition(),e.getPosition()))).orElse(null);
        if(target==null||!lineClear(body.getPosition(),target.getPosition()))return false;
        attackReadyTick=ticks+12;int previousHp=target.hp;target.hp=Math.max(0,target.hp-power);
        message="Golpe em "+target.getName()+": "+(previousHp-target.hp);
        if(sharedEncounter!=null) {
            sharedEncounter.hit(this,target,previousHp-target.hp,ticks);
        } else if(target.hp==0) {
            target.readyTick=ticks+target.definition.respawn();
            if(loot.award(target.spawn,target.generation,target.definition.id(),inventory,flags,random==null?new java.util.Random(741L^((long)target.spawn.hashCode()<<16)^target.generation):random)) {
                questService.event("kill",target.definition.id(),target.spawn,target.generation,quests,flags);if(authoredQuests!=null)authoredQuests.event("kill",target.definition.id(),target.spawn,target.generation);
                questService.refresh(quests,flags,inventory,region);if(authoredQuests!=null)authoredQuests.refresh(inventory,region);
                awardExperience(3);coins=Math.min(1_000_000,coins+5);
                message="Derrotado: +XP, loot e moedas";
                if(target.definition.id().equals("warden")){flags.add("boss-defeated");flags.add("dungeon-completed");message="Santuario concluido! Volte a Ancia com a reliquia.";}
            }else{target.hp=1;target.readyTick=0;message="Libere espaco nas recompensas pendentes antes da derrota";}
        }
        event(unarmed?RpgCombatEvent.Kind.PUNCH:RpgCombatEvent.Kind.ATTACK,target.getId(),target.getPosition().x(),target.getPosition().y(),0);
        if(previousHp>target.hp)event(RpgCombatEvent.Kind.HIT,target.getId(),target.getPosition().x(),target.getPosition().y(),previousHp-target.hp);
        if(target.hp==0)event(RpgCombatEvent.Kind.KO,target.getId(),target.getPosition().x(),target.getPosition().y(),0);
        return true;
    }
    private static double distance(Position a,Position b) { return Math.hypot(a.x()-b.x(),a.y()-b.y()); }
    private boolean lineClear(Position a,Position b) {
        if(sessionProfile!=null){var raw=scenes==null?com.projetok.domain.movement.RpgSessionGeometry.raw().obstacles():scenes.maps().get(region).space().obstacles();return RpgSight.clear(a,b,new CollisionService.Space(space.width(),space.height(),raw,sharedEncounter==null?space.bodies():List.of()));}
        return RpgSight.clear(a,b,sharedEncounter==null?space:new CollisionService.Space(space.width(),space.height(),space.obstacles(),List.of()));
    }
    public void collectLoot() { loot.collect(inventory,flags);message="Recompensas pendentes recolhidas quando houver espaco"; }
    private long ticks;
    public RpgSave save() {
        domain.writeTo(flags);
        var persistent=new java.util.TreeSet<>(flags);persistent.removeIf(f->f.startsWith("stamina:")||f.startsWith("mob:")||f.startsWith("attack-until:")||f.startsWith("portal-until:")||f.startsWith("facing:"));persistent.add("stamina:"+stamina);persistent.add("facing:"+facingX+":"+facingY);persistent.add("attack-until:"+attackReadyTick);persistent.add("portal-until:"+portalReadyTick);persistent.addAll(enemies.saveFlags());
        return new RpgSave(identity,region,body.getPosition().x(),body.getPosition().y(),hp,xp,coins,inventory.snapshot(),equipment,quests,persistent,ticks);
    }
    public static RpgWorld restore(RpgSave state) {
        RpgWorld world=new RpgWorld(state.identity());
        if(!new CollisionService().isFree(world.body,state.x(),state.y(),world.physicalRadius,world.space)) throw new IllegalArgumentException("Posicao de save obstruida");
        world.body.setPosition(new Position(state.x(),state.y())); world.ticks=state.ticks();
        world.inventory.replace(InventoryService.restore(world.items,20,state.inventory()));
        world.hp=state.hp(); world.xp=state.xp(); world.coins=state.coins(); world.region=state.region();
        for(var entry:state.equipment().entrySet()) if(!world.items.get(entry.getValue()).slot().equals(entry.getKey())) throw new IllegalArgumentException("Equipamento incompativel no save");
        world.equipment.clear();world.equipment.putAll(state.equipment()); world.quests.putAll(state.quests()); world.flags.addAll(state.flags());
        world.domain=DomainProgression.read(state.flags());
        if(!world.domain.domain().equals("none")&&!Domains.get(world.domain.domain()).race().equals(world.identity.race()))throw new IllegalArgumentException("Dominio incompativel com a raca");
        if(!world.domain.domain().equals("none")&&world.level()<15)throw new IllegalArgumentException("Dominio antes do nivel 15");
        for(String flag:world.flags) if(flag.startsWith("potion-until:")) world.potionReadyTick=Long.parseLong(flag.substring(13));
        for(String flag:world.flags) {
            if(flag.startsWith("ability-until:"))world.abilityReadyTick=Long.parseLong(flag.substring(14));
            if(flag.startsWith("evade-until:"))world.evadeUntilTick=Long.parseLong(flag.substring(12));
            if(flag.startsWith("stamina:"))world.stamina=Integer.parseInt(flag.substring(8));
            if(flag.startsWith("attack-until:"))world.attackReadyTick=Long.parseLong(flag.substring(13));
            if(flag.startsWith("portal-until:"))world.portalReadyTick=Long.parseLong(flag.substring(13));
            if(flag.startsWith("facing:")){var v=flag.split(":");if(v.length!=3)throw new IllegalArgumentException("Direcao invalida");world.facingX=Float.parseFloat(v[1]);world.facingY=Float.parseFloat(v[2]);if(!Float.isFinite(world.facingX)||!Float.isFinite(world.facingY)||Math.abs(Math.hypot(world.facingX,world.facingY)-1)>0.0001)throw new IllegalArgumentException("Direcao invalida");}
        }
        if(world.stamina<0||world.stamina>1000)throw new IllegalArgumentException("Vigor invalido");
        world.enemies.restore(state.flags(),world.space);
        world.validateFlags(state.flags());
        if(world.hp>world.maxHp()) throw new IllegalArgumentException("HP acima do maximo"); return world;
    }
    private void validateFlags(java.util.Set<String> source) {
        var keys=new java.util.HashSet<String>();
        for(String flag:source) {
            String[] p=flag.split(":");String key=p[0];
            switch(p[0]) {
                case "stamina","potion-until","ability-until","attack-until","portal-until","evade-until"->{if(p.length!=2||Long.parseLong(p[1])<0||Long.parseLong(p[1])>1_000_009_000L)throw new IllegalArgumentException("Recurso/recarga invalido");}
                case "domain","mastery-half","reset-last","reset-quest","domain-skill"->{if(p.length!=2)throw new IllegalArgumentException("Estado de dominio invalido");if(p[0].equals("domain-skill"))key+=":"+p[1];}
                case "migration"->{if(p.length!=2||!java.util.Set.of("guardian-to-reaper","mystic-to-human").contains(p[1]))throw new IllegalArgumentException("Migracao invalida");}
                case "facing"->{if(p.length!=3)throw new IllegalArgumentException("Direcao invalida");}
                case "mob"->{if(p.length!=7&&p.length!=9)throw new IllegalArgumentException("Estado de mob invalido");key+=":"+p[1];}
                case "pending"->{if(p.length!=3)throw new IllegalArgumentException("Loot pendente invalido");items.get(p[1]);int n=Integer.parseInt(p[2]);if(n<1||n>9999)throw new IllegalArgumentException("Loot pendente fora do limite");key+=":"+p[1];}
                case "defeat"->{if(p.length!=3||Long.parseLong(p[2])<1||Long.parseLong(p[2])>1_000_000_000L)throw new IllegalArgumentException("Ledger invalido");boolean known=content.all("region").stream().flatMap(r->enemies.views(r.id()).stream()).anyMatch(e->e.spawn().equals(p[1]));if(!known)throw new IllegalArgumentException("Spawn do ledger ausente");key+=":"+p[1];}
                case "visited"->{if(p.length!=2)throw new IllegalArgumentException("Visita invalida");content.get("region",p[1]);key+=":"+p[1];}
                case "quest-done"->{if(p.length!=2||!quests.containsKey(p[1])||quests.get(p[1])!=questService.required(p[1]))throw new IllegalArgumentException("Conclusao de quest invalida");key+=":"+p[1];}
                case "qo"->{if(p.length!=4)throw new IllegalArgumentException("Objetivo invalido");var objectives=questService.definition(p[1]).objectives();int index=Integer.parseInt(p[2]),count=Integer.parseInt(p[3]);if(index<0||index>=objectives.size()||count<0||count>objectives.get(index).count())throw new IllegalArgumentException("Progresso de objetivo invalido");key+=":"+p[1]+":"+index;}
                case "qe"->{if(p.length!=4||!java.util.Set.of("kill","talk","collect","visit").contains(p[1])||!p[2].matches("[a-z][a-z0-9_]{0,39}")||Long.parseLong(p[3])<1||Long.parseLong(p[3])>1_000_000_001L)throw new IllegalArgumentException("Evento de quest invalido");key+=":"+p[1]+":"+p[2];}
                case "opened","object-until"->{if(p.length!=(p[0].equals("opened")?2:3))throw new IllegalArgumentException("Objeto invalido");boolean known=content.all("region").stream().flatMap(r->objects.views(r.id(),0,java.util.Set.of()).stream()).anyMatch(o->o.id().equals(p[1]));if(!known||p.length==3&&(Long.parseLong(p[2])<0||Long.parseLong(p[2])>1_000_009_000L))throw new IllegalArgumentException("Objeto ausente/recarga invalida");key+=":"+p[1];}
                case "dungeon-started","dungeon-completed","boss-defeated"->{if(p.length!=1)throw new IllegalArgumentException("Estado de masmorra invalido");}
                default->throw new IllegalArgumentException("Flag de save desconhecida: "+p[0]);
            }
            if(!keys.add(key))throw new IllegalArgumentException("Estado duplicado: "+key);
        }
    }
    public record View(float x, float y, long ticks, CharacterIdentity identity, RpgSave state, int attack, int defense, String message, int maxHp, int level,List<EnemyService.View> enemies,List<NpcService.View> npcs,String activeNpc,DialogueService.Node dialogue,List<QuestService.View> journal,List<PortalService.Portal> portals,List<WorldObjects.ObjectView> objects,List<RpgCombatEvent> events) { public View { events=List.copyOf(events);enemies=List.copyOf(enemies);npcs=List.copyOf(npcs);journal=List.copyOf(journal);portals=List.copyOf(portals);objects=List.copyOf(objects); } }
    public void tick(float x, float y) {
        Position direction=movement.direction(x,y);
        if(hp>0) {
            if(sessionProfile==null)movement.moveDirected(body,direction,215,1f/30,physicalRadius,space);
            else movement.moveContinuous(body,direction.x()*215/30,direction.y()*215/30,physicalRadius,space);
            if(x!=0||y!=0){facingX=direction.x();facingY=direction.y();}
            stamina=Math.min(1000,stamina+5);
        }
        ticks++;
        if(sharedEncounter==null)enemies.respawn(ticks,region);
        questService.refresh(quests,flags,inventory,region);if(authoredQuests!=null)authoredQuests.refresh(inventory,region);
        if(sharedEncounter==null)enemies.update(region,ticks,body.getPosition(),hp>0,movement,space,n->receiveDamage(Math.max(1,n-defense())),p->lineClear(body.getPosition(),p),bossUnlocked());
    }
    public View snapshot() { return new View(body.getPosition().x(), body.getPosition().y(), ticks, identity,save(),attackPower(),defense(),message,maxHp(),level(),enemies.views(region,ticks),npcs.views(region),activeNpc,dialogue(),questService.views(quests,flags),portals.views(region),objects.views(region,ticks,flags),List.copyOf(events)); }
}
