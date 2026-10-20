package com.projetok.rpg;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import com.projetok.domain.movement.MovementService;
import java.util.List;

/** Mundo offline: uma thread proprietaria, geometria compartilhada com a arena. */
public final class RpgWorld {
    private final CharacterIdentity identity;
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
    private float facingX=1,facingY;
    public boolean castAbility() {
        var ability=Archetypes.get(identity.archetype());
        if(hp==0||transitioning||ticks<abilityReadyTick||stamina<ability.cost()*10||ability.ability().equals("mend")&&hp==maxHp())return false;
        if(ability.ability().equals("dash")) movement.moveContinuous(body,facingX*ability.effect(),facingY*ability.effect(),18,space);
        else hp=Math.min(maxHp(),hp+ability.effect());
        stamina-=ability.cost()*10;abilityReadyTick=ticks+ability.cooldown();
        flags.removeIf(f->f.startsWith("ability-until:"));flags.add("ability-until:"+abilityReadyTick);
        message=ability.ability().equals("dash")?"Investida!":"Restauracao!";return true;
    }
    private String message="Bem-vindo a Aurora";
    public int level() { return progression.level(xp); }
    public int maxHp() { return identity.baseHp()+(level()-1)*progression.hpPerLevel(); }
    public void awardExperience(long amount) {
        int previous=maxHp(); xp=progression.award(xp,amount);
        if(hp>0) hp+=maxHp()-previous;
    }
    public void receiveDamage(int amount) { if(amount<0||amount>10000) throw new IllegalArgumentException("Dano invalido"); hp=Math.max(0,hp-amount); }
    public boolean use(String id) {
        ItemCatalog.Item item=items.get(id);
        if(item.category()!=ItemCatalog.Category.CONSUMABLE || hp==0 || hp==maxHp() || transitioning || ticks<potionReadyTick || inventory.count(id)==0) return false;
        inventory.remove(id,1); hp=Math.min(maxHp(),hp+item.healing()); potionReadyTick=ticks+60;
        flags.removeIf(f->f.startsWith("potion-until:")); flags.add("potion-until:"+potionReadyTick);
        message=item.name()+": vida recuperada"; return true;
    }
    public InventoryService inventory() { return inventory; }
    public boolean activate(String action){return switch(action){case "rpg.attack"->attack();case "rpg.ability"->castAbility();case "item.potion"->use("potion");default->throw new IllegalArgumentException("Acao de hotbar desconhecida");};}
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
    public RpgWorld() { this(CharacterIdentity.create("Viajante", "guardian", "azure")); }
    public RpgWorld(CharacterIdentity identity) {
        this.identity = java.util.Objects.requireNonNull(identity);
        content.get("region", identity.initialRegion());
        body = new GameEntity(1, identity.name(), new Position(140, 320)) { };
        hp=identity.baseHp(); inventory.add("potion",3);
        flags.add("visited:village");
    }
    private final MovementService movement = new MovementService(new CollisionService());
    private final CollisionService.Space space = new CollisionService.Space(960, 640,
            List.of(new CollisionService.Obstacle(380, 180, 120, 120)), List.of());
    private final EnemyService enemies=new EnemyService(content,space);
    private final LootService loot=new LootService(content,items);
    private java.util.random.RandomGenerator random;
    private long attackReadyTick;
    private final NpcService npcs=new NpcService(content);
    private final PortalService portals=new PortalService(content);
    private final WorldObjects objects=new WorldObjects(content,items);
    public boolean harvest(String id) {
        if(hp==0||transitioning)return false;
        var object=objects.views(region,ticks,flags).stream().filter(o->o.id().equals(id)&&o.available()&&distance(body.getPosition(),new Position(o.x(),o.y()))<=65).findFirst();
        if(object.isEmpty())return false;var o=object.get();if(!inventory.add(o.item(),o.amount())){message="Inventario cheio; objeto preservado";return false;}
        if(o.respawn()==0)flags.add("opened:"+o.id());else{flags.removeIf(f->f.startsWith("object-until:"+o.id()+":"));flags.add("object-until:"+o.id()+":"+(ticks+o.respawn()));}
        questService.refresh(quests,flags,inventory,region);message="Coletado: "+items.get(o.item()).name()+" x"+o.amount();return true;
    }
    private long portalReadyTick;
    public boolean travel(String id) {
        var portal=portals.get(id);
        if(hp==0||transitioning||ticks<portalReadyTick||!portal.region().equals(region)||distance(body.getPosition(),new Position(portal.x(),portal.y()))>65)return false;
        if(portal.destination().equals("ruins")&&!quests.containsKey("sanctuary")){message="Conclua Ecos no bosque e aceite a missao do Santuario com a Ancia";return false;}
        if(!new CollisionService().isFree(body,portal.arrivalX(),portal.arrivalY(),18,space))return false;
        transitioning=true;
        try{region=portal.destination();flags.add("visited:"+region);body.setPosition(new Position(portal.arrivalX(),portal.arrivalY()));closeDialogue();portalReadyTick=ticks+30;if(region.equals("ruins"))flags.add("dungeon-started");questService.refresh(quests,flags,inventory,region);message="Chegou a "+content.get("region",region).name();return true;}finally{transitioning=false;}
    }
    private boolean bossUnlocked(){return enemies.current("ruins").stream().filter(e->!e.definition.id().equals("warden")).allMatch(e->e.hp==0);}
    public boolean returnHome(){if(hp>0)return false;region="village";body.setPosition(new Position(140,320));hp=maxHp();stamina=1000;closeDialogue();message="Recuperado em Aurora. Progresso preservado.";return true;}
    private String activeNpc="";
    public boolean trade(String id,int amount,boolean buying) {
        var item=items.get(id);if(amount<1||amount>99)throw new IllegalArgumentException("Quantidade de comercio invalida");
        if(hp==0||transitioning||!activeNpc.equals("trader")||npcs.nearest(region,body.getPosition()).filter(n->n.id().equals("trader")).isEmpty())return false;
        if(item.category()==ItemCatalog.Category.QUEST||item.price()==0||buying&&!java.util.Set.of("potion","sword","robe").contains(id))return false;
        int value=(buying?item.price():Math.max(1,item.price()/2))*amount;var copy=inventory.copy();
        if(buying){if(coins<value||!copy.add(id,amount))return false;}else if(coins>1_000_000-value||!copy.remove(id,amount))return false;
        inventory.replace(copy);coins+=buying?-value:value;message=buying?"Compra realizada":"Venda realizada";return true;
    }
    private final DialogueService dialogues=new DialogueService();
    private final QuestService questService=new QuestService(content);
    public boolean acceptQuest(String id){if(!canTalkElder()||!questService.accept(id,quests,flags))return false;questService.event("talk","elder","accept_"+id,ticks+1,quests,flags);questService.refresh(quests,flags,inventory,region);message="Missao aceita";return true;}
    public boolean completeQuest(String id){
        if(!canTalkElder())return false;questService.refresh(quests,flags,inventory,region);if(questService.state(id,quests,flags)!=QuestService.State.READY)return false;
        var d=questService.definition(id);var copy=inventory.copy();for(var o:d.objectives())if(o.type().equals("collect")&&!copy.remove(o.target(),o.count()))return false;
        if(coins>1_000_000-d.coins()||!copy.add(d.item(),d.quantity())){message="Libere espaco para a recompensa";return false;}
        inventory.replace(copy);coins+=d.coins();awardExperience(d.xp());questService.complete(id,quests,flags);message="Missao concluida: XP, moedas e itens";return true;
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
    public boolean interact() {
        if(hp==0||transitioning)return false;
        var portal=portals.nearest(region,body.getPosition());if(portal.isPresent())return travel(portal.get().id());
        var npc=npcs.nearest(region,body.getPosition());
        if(npc.isEmpty()||!lineClear(body.getPosition(),new Position(npc.get().x(),npc.get().y()))){closeDialogue();var object=objects.views(region,ticks,flags).stream().filter(o->o.available()&&distance(body.getPosition(),new Position(o.x(),o.y()))<=65).findFirst();if(object.isPresent())return harvest(object.get().id());message="Aproxime-se de NPC, portal ou objeto e pressione F";return false;}
        activeNpc=npc.get().id();questService.event("talk",activeNpc,activeNpc,ticks+1,quests,flags);questService.refresh(quests,flags,inventory,region);dialogueNode=activeNpc+".start";message=npc.get().name()+": seja bem-vindo!";return true;
    }
    public RpgWorld(CharacterIdentity identity,java.util.random.RandomGenerator random) { this(identity);this.random=java.util.Objects.requireNonNull(random); }
    public boolean attack() {
        if(hp==0||transitioning||ticks<attackReadyTick)return false;
        EnemyService.Enemy target=enemies.current(region).stream().filter(e->e.hp>0&&(!e.definition.id().equals("warden")||bossUnlocked())&&distance(body.getPosition(),e.getPosition())<= (identity.archetype().equals("mystic")?145:82))
            .min(java.util.Comparator.comparingDouble(e->distance(body.getPosition(),e.getPosition()))).orElse(null);
        if(target==null||!lineClear(body.getPosition(),target.getPosition()))return false;
        attackReadyTick=ticks+12;target.hp=Math.max(0,target.hp-attackPower());
        message="Golpe em "+target.getName()+": "+attackPower();
        if(target.hp==0) {
            target.readyTick=ticks+target.definition.respawn();
            if(loot.award(target.spawn,target.generation,target.definition.id(),inventory,flags,random==null?new java.util.Random(741L^((long)target.spawn.hashCode()<<16)^target.generation):random)) {
                questService.event("kill",target.definition.id(),target.spawn,target.generation,quests,flags);
                questService.refresh(quests,flags,inventory,region);
                awardExperience(content.get("enemy",target.definition.id()).value());coins=Math.min(1_000_000,coins+5);
                message="Derrotado: +XP, loot e moedas";
                if(target.definition.id().equals("warden")){flags.add("boss-defeated");flags.add("dungeon-completed");message="Santuario concluido! Volte a Ancia com a reliquia.";}
            }else{target.hp=1;target.readyTick=0;message="Libere espaco nas recompensas pendentes antes da derrota";}
        }
        return true;
    }
    private static double distance(Position a,Position b) { return Math.hypot(a.x()-b.x(),a.y()-b.y()); }
    private boolean lineClear(Position a,Position b) {
        int samples=Math.max(1,(int)(distance(a,b)/6));
        for(int i=1;i<samples;i++)if(!new CollisionService().isFree(body,a.x()+(b.x()-a.x())*i/samples,a.y()+(b.y()-a.y())*i/samples,1,space))return false;
        return true;
    }
    public void collectLoot() { loot.collect(inventory,flags);message="Recompensas pendentes recolhidas quando houver espaco"; }
    private long ticks;
    public RpgSave save() {
        var persistent=new java.util.TreeSet<>(flags);persistent.removeIf(f->f.startsWith("stamina:")||f.startsWith("mob:")||f.startsWith("attack-until:")||f.startsWith("portal-until:")||f.startsWith("facing:"));persistent.add("stamina:"+stamina);persistent.add("facing:"+facingX+":"+facingY);persistent.add("attack-until:"+attackReadyTick);persistent.add("portal-until:"+portalReadyTick);persistent.addAll(enemies.saveFlags());
        return new RpgSave(identity,region,body.getPosition().x(),body.getPosition().y(),hp,xp,coins,inventory.snapshot(),equipment,quests,persistent,ticks);
    }
    public static RpgWorld restore(RpgSave state) {
        RpgWorld world=new RpgWorld(state.identity());
        if(!new CollisionService().isFree(world.body,state.x(),state.y(),18,world.space)) throw new IllegalArgumentException("Posicao de save obstruida");
        world.body.setPosition(new Position(state.x(),state.y())); world.ticks=state.ticks();
        world.inventory.replace(InventoryService.restore(world.items,20,state.inventory()));
        world.hp=state.hp(); world.xp=state.xp(); world.coins=state.coins(); world.region=state.region();
        for(var entry:state.equipment().entrySet()) if(!world.items.get(entry.getValue()).slot().equals(entry.getKey())) throw new IllegalArgumentException("Equipamento incompativel no save");
        world.equipment.putAll(state.equipment()); world.quests.putAll(state.quests()); world.flags.addAll(state.flags());
        for(String flag:world.flags) if(flag.startsWith("potion-until:")) world.potionReadyTick=Long.parseLong(flag.substring(13));
        for(String flag:world.flags) {
            if(flag.startsWith("ability-until:"))world.abilityReadyTick=Long.parseLong(flag.substring(14));
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
                case "stamina","potion-until","ability-until","attack-until","portal-until"->{if(p.length!=2||Long.parseLong(p[1])<0||Long.parseLong(p[1])>1_000_009_000L)throw new IllegalArgumentException("Recurso/recarga invalido");}
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
    public record View(float x, float y, long ticks, CharacterIdentity identity, RpgSave state, int attack, int defense, String message, int maxHp, int level,List<EnemyService.View> enemies,List<NpcService.View> npcs,String activeNpc,DialogueService.Node dialogue,List<QuestService.View> journal,List<PortalService.Portal> portals,List<WorldObjects.ObjectView> objects) { public View { enemies=List.copyOf(enemies);npcs=List.copyOf(npcs);journal=List.copyOf(journal);portals=List.copyOf(portals);objects=List.copyOf(objects); } }
    public void tick(float x, float y) {
        Position direction=movement.direction(x,y);
        if(hp>0) {
            movement.moveDirected(body,direction,215,1f/30,18,space);
            if(x!=0||y!=0){facingX=direction.x();facingY=direction.y();}
            stamina=Math.min(1000,stamina+5);
        }
        ticks++;
        enemies.respawn(ticks,region);
        questService.refresh(quests,flags,inventory,region);
        enemies.update(region,ticks,body.getPosition(),hp>0,movement,space,n->receiveDamage(Math.max(1,n-defense())),p->lineClear(body.getPosition(),p),bossUnlocked());
    }
    public View snapshot() { return new View(body.getPosition().x(), body.getPosition().y(), ticks, identity,save(),attackPower(),defense(),message,maxHp(),level(),enemies.views(region),npcs.views(region),activeNpc,dialogue(),questService.views(quests,flags),portals.views(region),objects.views(region,ticks,flags)); }
}
