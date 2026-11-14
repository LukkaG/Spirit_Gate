package com.projetok.rpg;

import com.projetok.domain.combat.AttributeCombatRules;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static com.projetok.rpg.RpgSkillCatalog.*;

/**
 * Executa os oito kits exclusivamente na thread da sessão sintética autoritativa.
 * Usa energia, geometria e ledger existentes; nenhum estado desta classe é salvo em disco.
 * O coordenador chama tick depois de atualizar os mundos e o encontro compartilhado.
 */
public final class RpgSkillEngine {
    /** Presença de sessão é distinta de ausência de recarga; recusas não têm deadlines. */
    public enum TimingStatus { PRESENT, ABSENT, INCOMPATIBLE }

    /** Um deadline absoluto com identidade do catálogo, sem slot inferido pelo cliente. */
    public record SkillDeadline(String skillId, long readyAtTick) { }

    /** Cópia de timing no processo atual, sem marks, projéteis, impactos ou histórico. */
    public record TimingRead(TimingStatus status, int participant, java.util.UUID identity,
            String region, String domain, long worldTick, long engineClock, List<SkillDeadline> deadlines) {
        /** Ausência continua nula; sessão presente conserva cinco prazos mesmo se prontos. */
        public TimingRead { if(deadlines!=null) deadlines=List.copyOf(deadlines); }
    }

    /**
     * Lê somente sessão já existente e seu binding. O mapa de participação e o contexto devem
     * pertencer ao mesmo owner deste executor; este método não cria sessão nem a remove.
     */
    TimingRead readTiming(int participant, RpgWorld world, Map<Integer,RpgWorld> participants,
            CharacterReadContext context) {
        java.util.Objects.requireNonNull(context).requireWorld(world);
        context.requireEngine(this);
        java.util.Objects.requireNonNull(participants);
        var state=world.networkState();
        var domain=world.domainProgression().domain();
        var session=sessions.get(participant);
        TimingStatus status;
        if(participant<1 || participants.get(participant)!=world) status=TimingStatus.INCOMPATIBLE;
        else if(session==null) status=TimingStatus.ABSENT;
        else if(session.world!=world || !session.region.equals(state.region())
                || !session.domain.equals(domain) || state.hp()<=0) status=TimingStatus.INCOMPATIBLE;
        else status=TimingStatus.PRESENT;
        List<SkillDeadline> deadlines=null;
        if(status==TimingStatus.PRESENT) {
            var kit=forDomain(domain);
            if(kit.size()!=5) status=TimingStatus.INCOMPATIBLE;
            else {
                var copy=new ArrayList<SkillDeadline>(5);
                for(int i=0;i<5;i++) copy.add(new SkillDeadline(kit.get(i).id(),session.ready[i]));
                deadlines=List.copyOf(copy);
            }
        }
        return new TimingRead(status,participant,state.identity().id(),state.region(),domain,
                state.ticks(),clock,deadlines);
    }

    /** Velocidade dos projéteis em unidades por tick de 30 Hz, compartilhada com a apresentação. */
    public static final float PROJECTILE_SPEED = 18f;
    /** Quantidade de trajetórias independentes do leque, sem dano acumulado na mesma vítima. */
    public static final int VOLLEY_ARROWS = 7;
    /** Queda de cada onda da chuva de flechas: 0,4 segundo no relógio autoritativo. */
    public static final int ARROW_RAIN_FALL_TICKS = 12;
    /** Espessura provisória do rastro elétrico; com corpo 12 e mob 19, o contato total alcança 47. */
    public static final float CIRCULAR_TRAIL_RADIUS = 16f;
    /** Intenção sem dano nem posição final de jogador. IDs de mobs conservam o prefixo wire. */
    public record Cast(int slot, int targetId, long generation, float x, float y) { }
    /** Evento visual aceito pelo servidor; coordenadas são extraídas do mundo e da mira validada. */
    public record Event(long sequence, long tick, int player, String skill,
                        float fromX, float fromY, float x, float y) { }
    /** Recursos privados do lançador, com cinco recargas em ordem de slot. */
    public record State(List<Integer> cooldowns, int sentryShots, boolean windCheckpoint) {
        /** O consumidor não recebe a coleção mutável do executor. */
        public State { cooldowns = List.copyOf(cooldowns); }
    }

    private static final int MOB_BASE = 1_000_000;
    private static final int MAX_EVENTS = 16;
    private final Map<Integer, Session> sessions = new HashMap<>();
    private final List<Pending> pending = new ArrayList<>();
    private final ArrayDeque<Event> events = new ArrayDeque<>();
    private final Map<String, Dot> dots = new HashMap<>();
    private long sequence, clock;

    /** Estado por lançador; referências são descartadas em morte, troca de região ou saída. */
    private static final class Session {
        final RpgWorld world;
        final String region, domain;
        final long[] ready = new long[5];
        final Map<Integer,WindMark> checkpoints=new java.util.LinkedHashMap<>();
        int sentryShots;
        long checkpointUntil, sentryUntil;
        double sentryReady;
        Session(RpgWorld world) {
            this.world = world; region = world.networkState().region(); domain = world.progressView().domain();
        }
    }

    /** Marca exclusiva do lançador, vinculada à geração e ao apoio alcançado no empurrão. */
    private record WindMark(long generation,Position position) { }

    /** Cria um executor vazio; a sessão e o domínio escolhido continuam pertencendo ao servidor. */
    public RpgSkillEngine() { }

    // --- Validação e lançamento sem consumo em recusas ---

    /**
     * Valida slot, raça/domínio, alvo/geração, mira, colisão, energia e recarga antes de alterar estado.
     * Um alvo obrigatório inválido nunca vira ataque livre nem usa o inimigo mais próximo.
     */
    public boolean cast(int player, RpgWorld world, Cast request, Map<Integer, RpgWorld> players) {
        if (player < 1 || world == null || request == null || players.get(player) != world
                || request.slot() < 1 || request.slot() > 5 || !finite(request.x(), request.y())
                || world.level() < DomainProgression.INITIATION_LEVEL) return false;
        var kit = forDomain(world.progressView().domain());
        if (kit.isEmpty()) return false;
        var skill = kit.get(request.slot() - 1);
        var session = session(player, world);
        long now = world.skillTick();
        if (skill.mechanics().pattern() == Pattern.SENTRY && session.sentryShots > 0 && now < session.sentryUntil && world.networkState().hp()>0) {
            stopSentry(session); world.skillNotice("Sentinela cancelada."); return true;
        }
        if(!world.skillReady())return false;
        if (now < session.ready[request.slot() - 1]) return false;
        Position origin = world.skillPosition(), aim = new Position(request.x(), request.y());
        EnemyService.Enemy enemy = null;
        RpgWorld ally = null;
        CollisionService.Space reviveSpace = null;
        boolean hybrid = skill.damageScope() == DamageScope.MOBS_ONLY;
        if (request.targetId() >= MOB_BASE + 1) enemy = exact(world, request.targetId(), request.generation());
        if (skill.targeting() == Targeting.TARGET && !hybrid) {
            if (enemy == null || !reachable(world, origin, enemy.getPosition(), skill.range())) return false;
            aim = enemy.getPosition();
        } else if (skill.targeting() == Targeting.ALLY || hybrid && request.targetId() < MOB_BASE + 1) {
            ally = request.targetId() == 0 ? world : players.get(request.targetId());
            if (ally == null || !sameRegion(world, ally)
                    || !reachable(world, origin, ally.skillPosition(), skill.range())) return false;
            if (skill.mechanics().pattern() == Pattern.REVIVE ? ally.networkState().hp() != 0 : ally.networkState().hp() <= 0) return false;
            if (skill.mechanics().pattern() == Pattern.REVIVE) {
                reviveSpace = occupiedFor(ally, players);
                if (!freeRevive(ally, reviveSpace)) return false;
            }
            if (skill.mechanics().pattern() == Pattern.HEAL && ally.networkState().hp() >= ally.maxHp()) {
                world.skillNotice("A vida do alvo já está cheia."); return false;
            }
            aim = ally.skillPosition();
        } else if (hybrid) {
            if (enemy == null || !reachable(world, origin, enemy.getPosition(), skill.range())) return false;
            aim = enemy.getPosition();
        } else if (skill.targeting() == Targeting.SELF) aim = origin;
        else if (skill.targeting() == Targeting.LINE || skill.targeting() == Targeting.CONE)
            aim = endpoint(world, origin, eightDirections(world.combatFacing()), skill.range());
        else {
            if (!inside(world, aim) || !reachable(world, origin, aim, skill.range()) || distance(origin, aim) < .001) return false;
        }
        Pattern pattern = skill.mechanics().pattern();
        Position dashEnd = null;
        if (pattern == Pattern.DASH) {
            if (world.skillMovementBlocked()) return false;
            dashEnd=world.skillRushEnd(aim);
            if(distance(origin,dashEnd)<1)return false;
            aim=dashEnd;
        }
        if (pattern == Pattern.CIRCULAR_DASH && world.skillMovementBlocked()) return false;
        if (pattern == Pattern.AREA_HEAL && players.values().stream().noneMatch(recipient ->
                sameRegion(world, recipient) && recipient.networkState().hp() > 0
                && recipient.networkState().hp() < recipient.maxHp()
                && reachable(world, origin, recipient.skillPosition(), skill.radius()))) {
            world.skillNotice("Não há aliados feridos no alcance da cura."); return false;
        }
        if (pattern == Pattern.CHECKPOINT_PULL) {
            if(now>=session.checkpointUntil||windVictims(session,world,skill).isEmpty())return false;
        }
        if (!world.skillSpend(skill.energyCost())) return false;
        session.ready[request.slot() - 1] = now + skill.cooldownTicks();
        if (!aim.equals(origin)) world.skillFace(aim);
        if (pattern != Pattern.CONE_VOLLEY && pattern != Pattern.SENTRY && pattern != Pattern.CHECKPOINT_PULL && pattern != Pattern.BODY_SLAM && pattern != Pattern.CIRCULAR_DASH && !skill.id().equals("aetheri_3")
                && !(pattern == Pattern.MULTI_HIT && skill.kit().equals("martial"))) {
            boolean local = pattern == Pattern.HEAL || pattern == Pattern.REVIVE || pattern == Pattern.CLEANSE;
            emit(player, world, skill, local ? aim : origin, aim);
        }
        switch (pattern) {
            case PROJECTILE, ARROW -> pending.add(new Projectile(player, world, skill, origin, aim, enemy));
            case LINE_PROJECTILE, WEB, BEAM, FISSURE, FISSURE_ERUPTION, KNOCKBACK_CHECKPOINT ->
                pending.add(new TravelingLine(player, world, skill, origin, aim, false, new HashSet<>()));
            case MELEE_AREA, GROUND_STOMP -> area(player, world, skill, origin, skill.radius(), true);
            case PRISON -> hit(player, world, skill, enemy, 1000, true);
            case VORTEX, RAIN, DAMAGE_POOL, AREA_PRISON, QUICKSAND, CONE_COMBO -> {
                if(skill.id().equals("aetheri_3"))pending.add(new ArrowRain(player,world,skill,aim));
                else startZone(new Zone(player, world, skill, origin, aim, now, false));
            }
            case DASH -> { world.skillDashing(true); pending.add(new Dash(player, world, skill, origin, dashEnd)); }
            case MULTI_HIT -> {
                if(skill.id().equals("wind_2")){
                    pending.add(new Projectile(player,world,skill,origin,aim,enemy));
                    pending.add(new WindCuts(player,world,skill,enemy));
                }else pending.add(new Multi(player, world, skill, enemy));
            }
            case CHAIN -> pending.add(new Chain(player, world, skill, enemy));
            case CHECKPOINT_PULL -> {
                var victims=windVictims(session,world,skill);
                for (var victim : victims) {
                    Position from=session.checkpoints.get(victim.getId()).position();
                    world.skillDisplace(victim, stopBefore(victim.getPosition(), origin, world.physicalRadius() + 19 + 5));
                    hit(player, world, skill, victim, 1000, true);
                    emit(player, world, skill, from, origin);
                }
                session.checkpoints.clear();session.checkpointUntil=0;
            }
            case CIRCULAR_DASH -> { world.skillDashing(true);world.skillIntangible(true);pending.add(new LightningField(player,world,skill,origin)); }
            case SENTRY -> { session.sentryShots = 1; session.sentryUntil = now + skill.mechanics().durationTicks(); session.sentryReady = now; world.skillSentry(true); }
            case CONE_VOLLEY -> {
                var heading = direction(origin, aim); var sharedHits = new HashSet<String>();
                double halfAngle = Math.atan2(skill.radius(), skill.range());
                for (int i = 0; i < VOLLEY_ARROWS; i++) {
                    double angle = -halfAngle + 2 * halfAngle * i / (VOLLEY_ARROWS - 1);
                    var ray = rotate(heading, angle); Position end = endpoint(world, origin, ray, skill.range());
                    emit(player, world, skill, origin, end);
                    pending.add(new TravelingLine(player, world, skill, origin, end, true, sharedHits));
                }
            }
            case HEAL -> {
                if (ally != null) { int before = ally.networkState().hp(); ally.skillHeal(skill.healing()); world.skillNotice("Vida restaurada: +" + (ally.networkState().hp() - before) + "."); }
                else { hit(player, world, skill, enemy, 1000, true); world.skillNotice("Luz atingiu o inimigo selecionado."); }
            }
            case AREA_HEAL -> {
                for (var recipient : players.values()) if (sameRegion(world, recipient)
                        && reachable(world, origin, recipient.skillPosition(), skill.radius()) && recipient.skillHeal(skill.healing())) {
                    if (recipient != world) emit(player, world, skill, recipient.skillPosition(), recipient.skillPosition());
                }
                world.skillNotice("Cura aplicada aos aliados feridos na área.");
            }
            case REVIVE -> { ally.skillRevive(skill.healing(), reviveSpace); world.skillNotice("Aliado revivido no local."); }
            case CLEANSE -> ally.skillClearControls();
            case PUNCH -> hit(player, world, skill, enemy, 1000, true);
            case BODY_SLAM -> {
                world.skillDisplace(enemy, stopBefore(origin, aim, -8));
                Position landed=enemy.getPosition();
                if(distance(origin,landed)>.001)emit(player,world,skill,origin,landed);
                hit(player, world, skill, enemy, 1000, true);
                emit(player,world,skill,landed,landed);
            }
        }
        return true;
    }

    /**
     * Dispara durante a janela imóvel; agilidade aumenta a cadência sem consumir munição fixa.
     * O coordenador impede fallback para ataque básico enquanto o estado estiver ativo.
     */
    public boolean sentryAttack(int player, RpgWorld world, Cast request, Map<Integer, RpgWorld> players) {
        var s = sessions.get(player);
        if (s == null || s.world != world || players.get(player) != world || !world.skillReady()
                || s.sentryShots <= 0 || world.skillTick() >= s.sentryUntil || world.skillTick() < s.sentryReady) return false;
        var enemy = exact(world, request.targetId(), request.generation());
        var skill = get("aetheri_2");
        if (enemy == null || !reachable(world, world.skillPosition(), enemy.getPosition(), skill.range())) return false;
        world.skillFace(enemy.getPosition());
        s.sentryReady = AttributeCombatRules.nextBasicReady(s.sentryReady, world.skillTick(),
                skill.mechanics().intervalTicks(), world.skillAgility());
        emit(player, world, skill, world.skillPosition(), enemy.getPosition());
        pending.add(new Projectile(player, world, skill, world.skillPosition(), enemy.getPosition(), enemy));
        return true;
    }

    // --- Relógio, disponibilidade e ciclo de vida ---

    /** Básico do arqueiro fora da Sentinela: flecha frontal, sem alvo, custo ou recarga de técnica. */
    public boolean basicArrow(int player,RpgWorld world,Map<Integer,RpgWorld> players){
        if(players.get(player)!=world||!world.skillBowUser())return false;
        var s=session(player,world);
        if(s.sentryShots>0&&world.skillTick()<s.sentryUntil||!world.skillReserveBasicArrow())return false;
        var skill=get("aetheri_1");var from=world.skillPosition();
        var end=endpoint(world,from,cardinal(world.combatFacing()),world.basicAttackRange());
        world.skillFace(end);emit(player,world,skill,from,end);
        pending.add(new TravelingLine(player,world,skill,from,end,true,new HashSet<>(),true));return true;
    }

    /** Sincroniza somente eventos e efeitos da população ao relógio global do coordenador. */
    public void clock(long tick) {
        if (tick < clock) throw new IllegalArgumentException("Relógio global não pode retroceder");
        clock = tick;
    }

    /** Avança impactos e deslocamentos uma vez por tick, sempre após a atualização do encontro. */
    public void tick(Map<Integer, RpgWorld> players) {
        for (int player : List.copyOf(sessions.keySet())) {
            var s = sessions.get(player);
            if (players.get(player) != s.world || s.world.networkState().hp() <= 0
                    || !s.region.equals(s.world.networkState().region()) || !s.domain.equals(s.world.progressView().domain())) remove(player);
        }
        var work = new ArrayList<>(pending); pending.clear();
        for (var task : work) if (sessions.containsKey(task.player) && players.get(task.player) == task.world
                && task.world.networkState().hp() > 0 && task.region.equals(task.world.networkState().region())) {
            if (task.last != task.world.skillTick()) {
                task.last = task.world.skillTick();
                if (task.advance()) pending.add(task);
            } else pending.add(task);
        }
        for (var s : sessions.values()) {
            if (s.sentryShots > 0 && s.world.skillTick() >= s.sentryUntil) stopSentry(s);
            if (s.world.skillTick() >= s.checkpointUntil) s.checkpoints.clear();
        }
    }

    /** Consulta confirmada pelo servidor, sem transformar recarga em relógio de parede. */
    public State state(int player) {
        var s = sessions.get(player);
        if (s == null) return new State(List.of(0, 0, 0, 0, 0), 0, false);
        long now = s.world.skillTick();
        var cooldowns = new ArrayList<Integer>(5);
        for (long until : s.ready) cooldowns.add((int) Math.max(0, until - now));
        // O campo legado de 0..5 agora sinaliza os segundos restantes (4..1); wire inalterado.
        return new State(cooldowns, s.sentryShots > 0 && now < s.sentryUntil ? (int)((s.sentryUntil-now+29)/30) : 0,
                !s.checkpoints.isEmpty() && now < s.checkpointUntil);
    }

    /** Cópia limitada dos eventos; a sequência é monotônica mesmo após clear. */
    public List<Event> events() { return List.copyOf(events); }

    /** Cancela ações pendentes e eventos do lançador em viagem, morte ou desconexão. */
    public void remove(int player) {
        var removed = sessions.remove(player);
        if (removed != null) { removed.world.skillDashing(false); stopSentry(removed); }
        pending.removeIf(p -> p.player == player);
        dots.values().removeIf(p -> p.player == player);
        events.removeIf(e -> e.player() == player);
    }

    /** Cancela o executor antes de ativar uma revisão; não retrocede o contador de eventos. */
    public void clear() {
        for (var s : sessions.values()) { s.world.skillClearControls(); s.world.skillDashing(false); stopSentry(s); for (var enemy : s.world.skillEnemies()) enemy.clearSkillEffects(); }
        sessions.clear(); pending.clear(); dots.clear(); events.clear();
    }

    // --- Impactos reutilizam dano e efeitos autoritativos ---

    /** Aplica dano por atributo e preserva dano zero das áreas exclusivas de controle. */
    private void hit(int player, RpgWorld world, Definition skill, EnemyService.Enemy enemy, int scale, boolean damage) {
        if (enemy == null || enemy.hp <= 0 || !enemy.region.equals(world.networkState().region())) return;
        if (damage && skill.baseDamage() > 0) {
            var kind = skill.kit().equals("martial") ? AttributeCombatRules.DamageKind.UNARMED_SKILL : AttributeCombatRules.DamageKind.DOMAIN_SKILL;
            int amount = Math.max(1, (skill.baseDamage() + world.skillAttributeBonus(kind)) * scale / 1000);
            world.skillDamage(enemy, enemy.generation, amount);
        }
        if (enemy.hp <= 0) return;
        for (var effect : skill.effects()) switch (effect.type()) {
            case ROOT -> enemy.skillRootUntil = Math.max(enemy.skillRootUntil, clock + effect.durationTicks());
            case STUN -> enemy.skillStunUntil = Math.max(enemy.skillStunUntil, clock + effect.durationTicks());
            case SLOW -> {
                enemy.skillSlowUntil = Math.max(enemy.skillSlowUntil, clock + effect.durationTicks());
                enemy.skillSlowScale = effect.potencyPermille() / 1000f;
            }
            case BURN, BLEED -> {
                // Renovação substitui o relógio anterior do mesmo caster/tipo/vítima, sem empilhar pulsos.
                String key = player + ":" + enemy.getId() + ":" + enemy.generation + ":" + effect.type();
                var existing = dots.get(key);
                if (existing != null) existing.end = world.skillTick() + effect.durationTicks();
                else { var dot = new Dot(player, world, skill, enemy, effect, key); dots.put(key, dot); pending.add(dot); }
            }
        }
    }

    /** Seleciona inimigos por geometria real, incluindo tangência e linha de visão. */
    private void area(int player, RpgWorld world, Definition skill, Position point, int radius, boolean damage) {
        for (var enemy : live(world)) if (distance(point, enemy.getPosition()) <= radius + 19
                && world.skillVisible(point, enemy.getPosition())) hit(player, world, skill, enemy, 1000, damage);
    }

    /** Cone frontal com largura proporcional à distância, sem atingir vítimas atrás do lançador. */
    private void cone(int player, RpgWorld world, Definition skill, Position from, Position aim, boolean damage) {
        var dir = direction(from, aim);
        for (var enemy : live(world)) {
            double dx = enemy.getPosition().x() - from.x(), dy = enemy.getPosition().y() - from.y();
            double projection = dx * dir.x() + dy * dir.y(), side = Math.abs(dx * dir.y() - dy * dir.x());
            if (projection >= 0 && projection <= skill.range() + 19 && side <= skill.radius() * Math.min(1, projection / skill.range()) + 19
                    && world.skillVisible(from, enemy.getPosition())) hit(player, world, skill, enemy, 1000, damage);
        }
    }

    /** Linha com espessura, sem atravessar o obstáculo para atingir um inimigo atrás dele. */
    private void line(int player, RpgWorld world, Definition skill, Position from, Position to, boolean damage) {
        int width = skill.mechanics().pattern() == Pattern.FISSURE_ERUPTION ? 24 : skill.radius();
        for (var enemy : live(world)) if (segmentDistance(enemy.getPosition(), from, to) <= width + 19
                && world.skillVisible(from, enemy.getPosition())) hit(player, world, skill, enemy, 1000, damage);
    }

    /** Áreas sem tempo de voo já afetam o tick de lançamento, mantendo a contagem de pulsos exata. */
    private void startZone(Zone zone) { zone.last = zone.world.skillTick(); if (zone.advance()) pending.add(zone); }

    // --- Ações temporais: projéteis, sequências, áreas, dano periódico e dashes ---

    /** Ação vinculada à sessão e à região originais; nunca retoma em outro mundo. */
    private abstract class Pending {
        final int player; final RpgWorld world; final Definition skill; final String region;
        long last = -1;
        Pending(int player, RpgWorld world, Definition skill) { this.player = player; this.world = world; this.skill = skill; region = world.networkState().region(); }
        /** Retorna verdadeiro enquanto a ação ainda precisa de outro tick. */
        abstract boolean advance();
    }

    /** Projétil que percorre segmentos reais até o alvo exato; um respawn não herda o acerto antigo. */
    private final class Projectile extends Pending {
        final int enemyId; final long generation; Position point; float remaining;
        Projectile(int player, RpgWorld world, Definition skill, Position from, Position to, EnemyService.Enemy enemy) {
            super(player, world, skill); point = from; enemyId = enemy.getId(); generation = enemy.generation; remaining = skill.range();
        }
        boolean advance() {
            var enemy = exact(world, MOB_BASE + enemyId, generation);
            if (enemy == null) return false;
            float step = (float) Math.min(Math.min(PROJECTILE_SPEED, remaining), distance(point, enemy.getPosition()));
            var heading = direction(point, enemy.getPosition());
            var desired = new Position(point.x() + heading.x() * step, point.y() + heading.y() * step);
            Position end = world.skillProjectileEnd(point, desired, 4);
            var contacts=new ArrayList<Contact>();
            for(var victim:live(world)){
                double at=contactFraction(victim.getPosition(),point,end,23);
                if(Double.isFinite(at)&&world.skillVisible(point,victim.getPosition()))contacts.add(new Contact(victim,at));
            }
            contacts.sort(java.util.Comparator.comparingDouble(Contact::fraction).thenComparingInt(c->c.enemy().getId()));
            if (!contacts.isEmpty()) {
                var first=contacts.get(0);double contact=first.fraction();
                Position impact = interpolate(point, end, (float) contact);
                hit(player, world, skill, first.enemy(), 1000, true); emit(player, world, skill, impact, impact); return false;
            }
            boolean blocked = distance(desired, end) > .001;
            remaining -= step; point = end;
            if (blocked) { emit(player, world, skill, point, point); return false; }
            if (remaining <= .001 || step <= .001)return false;
            return true;
        }
    }

    /** Contato ordenável da varredura; o primeiro corpo bloqueia flechas individuais do leque. */
    private record Contact(EnemyService.Enemy enemy, double fraction) { }

    /** Seleciona somente marcas vivas desta geração, com alcance e visibilidade revalidados. */
    private List<EnemyService.Enemy> windVictims(Session session,RpgWorld world,Definition skill){
        return live(world).stream().filter(victim->{
            var mark=session.checkpoints.get(victim.getId());
            return mark!=null&&mark.generation()==victim.generation
                    &&reachable(world,world.skillPosition(),victim.getPosition(),skill.range())
                    &&world.skillVisible(mark.position(),victim.getPosition());
        }).toList();
    }

    /** Registra todos os inimigos afetados no contato real, inclusive os atingidos pela área. */
    private void repel(int player,RpgWorld world,Definition skill,Position from,EnemyService.Enemy primary){
        Position center=primary.getPosition();
        var session=sessions.get(player);
        for(var victim:live(world))if(distance(center,victim.getPosition())<=skill.radius()+19
                &&world.skillVisible(from,victim.getPosition())){
            world.skillDisplace(victim,extend(from,victim.getPosition(),90));
            hit(player,world,skill,victim,1000,true);
            if(victim.hp>0)session.checkpoints.put(victim.getId(),new WindMark(victim.generation,victim.getPosition()));
        }
        if(!session.checkpoints.isEmpty())session.checkpointUntil=world.skillTick()+skill.mechanics().checkpointTicks();
    }

    /**
     * Skillshot percorre somente o segmento deste tick e testa inimigos nas posições atuais.
     * A fissura nasce atrás da ponta; sua erupção só acontece quando a frente termina o percurso.
     */
    private final class TravelingLine extends Pending {
        final Position from, end; final boolean stopAtFirst,basic; final Set<String> contacts;
        Position point;
        TravelingLine(int player, RpgWorld world, Definition skill, Position from, Position end,
                      boolean stopAtFirst, Set<String> contacts) {
            this(player,world,skill,from,end,stopAtFirst,contacts,false);
        }
        /** O projétil básico compartilha geometria e ledger, preservando dano/recarga do ataque básico. */
        TravelingLine(int player,RpgWorld world,Definition skill,Position from,Position end,boolean stopAtFirst,Set<String> contacts,boolean basic){
            super(player, world, skill); this.from = from; this.end = end; this.point = from; this.basic=basic;
            this.stopAtFirst = stopAtFirst || skill.mechanics().pattern()!=Pattern.FISSURE&&skill.mechanics().pattern()!=Pattern.FISSURE_ERUPTION; this.contacts = contacts;
        }
        boolean advance() {
            float step = (float) Math.min(PROJECTILE_SPEED, distance(point, end));
            var heading = direction(point, end);
            var desired = new Position(point.x() + heading.x() * step, point.y() + heading.y() * step);
            Position reached = world.skillProjectileEnd(point, desired, shotRadius(skill));
            var impacts = new ArrayList<Contact>();
            for (var enemy : live(world)) {
                double fraction = contactFraction(enemy.getPosition(), point, reached, shotRadius(skill) + 19);
                if (Double.isFinite(fraction) && world.skillVisible(point, enemy.getPosition())) impacts.add(new Contact(enemy, fraction));
            }
            impacts.sort(java.util.Comparator.comparingDouble(Contact::fraction).thenComparingInt(c -> c.enemy().getId()));
            for (var impact : impacts) {
                var enemy = impact.enemy();
                String key = enemy.getId() + ":" + enemy.generation;
                if (contacts.add(key)) {
                    if(basic)world.skillDamage(enemy,enemy.generation,world.attackPower());
                    else if(skill.mechanics().pattern()==Pattern.KNOCKBACK_CHECKPOINT)repel(player,world,skill,from,enemy);
                    else hit(player, world, skill, enemy, 1000, true);
                    Position location = interpolate(point, reached, (float) impact.fraction());
                    // Fissuras publicam a fase local somente ao terminar, para não antecipar a erupção.
                    if (!stopAtFirst && skill.mechanics().pattern() != Pattern.FISSURE && skill.mechanics().pattern() != Pattern.FISSURE_ERUPTION)
                        emit(player, world, skill, location, location);
                }
                if (stopAtFirst) { point = interpolate(point, reached, (float) impact.fraction()); return finish(true); }
            }
            boolean blocked = distance(desired, reached) > .001;
            point = reached;
            if (blocked || distance(point, end) <= .001 || step <= .001) return finish(blocked);
            return true;
        }
        /** Resolve a área final na posição realmente alcançada, sem atingir o outro lado da parede. */
        private boolean finish(boolean impact) {
            if(impact||skill.mechanics().pattern()==Pattern.FISSURE||skill.mechanics().pattern()==Pattern.FISSURE_ERUPTION)
                emit(player, world, skill, point, point);
            if (skill.mechanics().pattern() == Pattern.FISSURE) {
                var trail = new Zone(player, world, skill, from, point, world.skillTick(), true);
                trail.pulses = skill.mechanics().hits(); startZone(trail);
            } else if (skill.mechanics().pattern() == Pattern.FISSURE_ERUPTION)
                area(player, world, skill, point, skill.radius(), true);
            return false;
        }
    }

    /** As três lâminas saem do apoio atual do lançador em ticks 0, 3 e 6; cada uma viaja de verdade. */
    private final class WindCuts extends Pending {
        final int enemyId;final long generation;long due;int launched=1;
        WindCuts(int player,RpgWorld world,Definition skill,EnemyService.Enemy enemy){
            super(player,world,skill);enemyId=enemy.getId();generation=enemy.generation;due=world.skillTick()+skill.mechanics().intervalTicks();
        }
        boolean advance(){
            if(world.skillTick()<due)return true;
            var enemy=exact(world,MOB_BASE+enemyId,generation);
            if(enemy==null||!reachable(world,world.skillPosition(),enemy.getPosition(),skill.range()))return false;
            var from=world.skillPosition();emit(player,world,skill,from,enemy.getPosition());
            pending.add(new Projectile(player,world,skill,from,enemy.getPosition(),enemy));launched++;due+=skill.mechanics().intervalTicks();
            return launched<skill.mechanics().hits();
        }
    }

    /** O dano de cada onda só é resolvido quando as flechas chegam ao ponto selecionado no chão. */
    private final class ArrowRain extends Pending {
        final Position ground;long due;int pulses;
        ArrowRain(int player,RpgWorld world,Definition skill,Position ground){
            super(player,world,skill);this.ground=ground;launch();due=world.skillTick()+ARROW_RAIN_FALL_TICKS;
        }
        private void launch(){emit(player,world,skill,new Position(ground.x(),Math.max(0,ground.y()-160)),ground);}
        boolean advance(){
            if(world.skillTick()<due)return true;
            area(player,world,skill,ground,skill.radius(),true);emit(player,world,skill,ground,ground);pulses++;
            if(pulses>=skill.mechanics().hits())return false;
            launch();due+=skill.mechanics().intervalTicks();return true;
        }
    }

    /** Multihit preserva alvo, alcance, geração e intervalo em cada impacto. */
    private final class Multi extends Pending {
        final int enemyId; final long generation; int hits; long due;
        Multi(int player, RpgWorld world, Definition skill, EnemyService.Enemy enemy) {
            super(player, world, skill); enemyId = enemy.getId(); generation = enemy.generation; due = world.skillTick();
        }
        boolean advance() {
            if (world.skillTick() < due) return true;
            var enemy = exact(world, MOB_BASE + enemyId, generation);
            if (enemy == null || !reachable(world, world.skillPosition(), enemy.getPosition(), skill.range())) return false;
            hit(player, world, skill, enemy, 1000, true); hits++; due = world.skillTick() + skill.mechanics().intervalTicks();
            emit(player,world,skill,world.skillPosition(),enemy.getPosition());
            return hits < skill.mechanics().hits();
        }
    }

    /** Corrente de quatro saltos; procura primeiro vítimas ainda não visitadas, com desempate estável. */
    private final class Chain extends Pending {
        EnemyService.Enemy enemy; long generation, due; int hits, scale = 1000;
        final Set<Integer> visited = new HashSet<>(); Position previous;
        Chain(int player, RpgWorld world, Definition skill, EnemyService.Enemy enemy) {
            super(player, world, skill); this.enemy = enemy; generation = enemy.generation; previous = world.skillPosition(); due = world.skillTick() + flight(previous, enemy.getPosition());
        }
        boolean advance() {
            if (world.skillTick() < due) return true;
            if (enemy.hp <= 0 || enemy.generation != generation) return false;
            Position origin = enemy.getPosition();
            if (!world.skillVisible(previous, origin)) return false;
            hit(player, world, skill, enemy, scale, true); visited.add(enemy.getId()); hits++;
            if (hits >= skill.mechanics().hits()) return false;
            var next = live(world).stream().filter(e -> !visited.contains(e.getId())
                    && distance(origin, e.getPosition()) <= skill.radius() && world.skillVisible(origin, e.getPosition()))
                    .min(java.util.Comparator.<EnemyService.Enemy>comparingDouble(e -> distance(origin, e.getPosition())).thenComparingInt(EnemyService.Enemy::getId)).orElse(null);
            if (next != null) { enemy = next; generation = enemy.generation; scale = 1000; }
            else {
                if (enemy.hp <= 0) return false;
                scale = Math.max(1, scale * skill.mechanics().repeatDamagePermille() / 1000);
            }
            previous = origin; due = world.skillTick() + skill.mechanics().intervalTicks();
            emit(player, world, skill, origin, enemy.getPosition()); return true;
        }
    }

    /** Áreas fixas e vórtices móveis compartilham relógio, mas conservam seus pontos de origem. */
    private final class Zone extends Pending {
        final Position from, aim; final long start, end; final boolean fissure;
        long due; int pulses;
        Zone(int player, RpgWorld world, Definition skill, Position from, Position aim, long start, boolean fissure) {
            super(player, world, skill); this.from = from; this.aim = aim; this.start = start; this.fissure = fissure;
            end = start + skill.mechanics().durationTicks(); due = start;
        }
        boolean advance() {
            long now = world.skillTick();
            if (now < due) return true;
            if (now > end && pulses > 0) return false;
            boolean damage = pulses < skill.mechanics().hits();
            if (fissure) line(player, world, skill, from, aim, damage);
            else if (skill.mechanics().pattern() == Pattern.CONE_COMBO) {
                Position delta = direction(from, aim), point = world.skillPosition();
                cone(player, world, skill, point, new Position(point.x() + delta.x(), point.y() + delta.y()), damage);
            } else area(player, world, skill, skill.targeting() == Targeting.SELF ? world.skillPosition() : aim, skill.radius(), damage);
            pulses++;
            due = now + (skill.mechanics().intervalTicks() > 0 ? skill.mechanics().intervalTicks() : 5);
            return now < end && (pulses < skill.mechanics().hits() || fissure
                    || skill.mechanics().pattern() == Pattern.AREA_PRISON || skill.mechanics().pattern() == Pattern.QUICKSAND);
        }
    }

    /** Um único relógio de DoT por lançador/tipo/geração, sem dano imediato fictício. */
    private final class Dot extends Pending {
        final EnemyService.Enemy enemy; final long generation; final Effect effect; final String key;
        long due,end;
        Dot(int player, RpgWorld world, Definition skill, EnemyService.Enemy enemy, Effect effect, String key) {
            super(player, world, skill); this.enemy = enemy; generation = enemy.generation; this.effect = effect; this.key = key;
            due = world.skillTick() + effect.intervalTicks(); end = world.skillTick() + effect.durationTicks();
        }
        boolean advance() {
            long now = world.skillTick();
            if (enemy.hp <= 0 || enemy.generation != generation || now > end) { dots.remove(key, this); return false; }
            if (now < due) return true;
            world.skillDamage(enemy, generation, effect.baseDamage()); due = now + effect.intervalTicks();
            boolean alive = due <= end && enemy.hp > 0;
            if (!alive) dots.remove(key, this);
            return alive;
        }
    }

    /** Corte frontal em oito passos; cada inimigo pode receber um único acerto na passagem real. */
    private final class Dash extends Pending {
        final Position from,to;final long start;final Set<String> crossed=new HashSet<>();boolean finishing;
        Dash(int player,RpgWorld world,Definition skill,Position from,Position to){
            super(player,world,skill);this.from=from;this.to=to;start=world.skillTick();
        }
        boolean advance() {
            if(!finishing){
                int duration=Math.max(1,skill.mechanics().durationTicks());float fraction=Math.min(1,(world.skillTick()-start)/(float)duration);
                Position before=world.skillPosition();boolean moved=world.skillRushMove(interpolate(from,to,fraction));Position after=world.skillPosition();
                if(moved)for(var enemy:live(world))if(segmentDistance(enemy.getPosition(),before,after)<=world.physicalRadius()+19+skill.radius()
                        &&world.skillVisible(before,enemy.getPosition())&&crossed.add(enemy.getId()+":"+enemy.generation))hit(player,world,skill,enemy,1000,true);
                finishing=!moved||fraction>=1;
            }
            if(finishing){if(!world.skillReappear())return true;world.skillDashing(false);emit(player,world,skill,world.skillPosition(),world.skillPosition());return false;}
            return true;
        }
    }

    /** Fase oculta/intangível seguida de quatro cortes sobre todo o círculo de conjuração. */
    private final class LightningField extends Pending {
        final Position center;final long start;boolean resolved;
        LightningField(int player,RpgWorld world,Definition skill,Position origin){
            super(player,world,skill);center=origin;start=world.skillTick();
            // Marcador de fase, sem deslocamento: precisa ser distinto mesmo olhando para fora na borda.
            var phase=new Position(center.x()+(center.x()<world.skillSpace().width()/2?1:-1),center.y());
            emit(player,world,skill,center,phase);
        }
        boolean advance(){
            if(world.skillTick()-start<skill.mechanics().durationTicks())return true;
            if(!resolved){for(int cut=0;cut<skill.mechanics().hits();cut++)area(player,world,skill,center,skill.radius(),true);resolved=true;}
            // Se todos os apoios estiverem ocupados, mantém a fase apenas até haver saída segura.
            if(!world.skillReappear()){
                world.skillNotice("Aguardando espaço livre para reaparecer.");return true;
            }
            world.skillDashing(false);emit(player,world,skill,center,center);return false;
        }
    }

    // --- Consultas geométricas e auxiliares sem estado externo ---

    /** Recria o estado privado se a sessão for substituída; não herda cooldowns entre personagens. */
    private Session session(int player, RpgWorld world) {
        var s = sessions.get(player);
        if (s == null || s.world != world || !s.domain.equals(world.progressView().domain()) || !s.region.equals(world.networkState().region())) {
            remove(player); s = new Session(world); sessions.put(player, s);
        }
        return s;
    }
    /** Libera a imobilidade voluntária sem remover controles negativos independentes. */
    private static void stopSentry(Session s) { s.sentryShots=0; s.sentryUntil=0; s.world.skillSentry(false); }
    /** Seleciona a direção de skill mais próxima em passos de 45 graus, com alcance unitário. */
    public static Position eightDirections(Position heading) {
        if(heading.x()==0&&heading.y()==0)return new Position(1,0);
        int octant=Math.floorMod((int)Math.round(Math.atan2(heading.y(),heading.x())/(Math.PI/4)),8);
        float diagonal=(float)Math.sqrt(.5);
        return switch(octant){
            case 0->new Position(1,0);
            case 1->new Position(diagonal,diagonal);
            case 2->new Position(0,1);
            case 3->new Position(-diagonal,diagonal);
            case 4->new Position(-1,0);
            case 5->new Position(-diagonal,-diagonal);
            case 6->new Position(0,-1);
            default->new Position(diagonal,-diagonal);
        };
    }
    /** Espelha as quatro poses do ataque básico, inclusive o empate vertical do atlas. */
    public static Position cardinal(Position heading) {
        if (Math.abs(heading.x()) > Math.abs(heading.y())) return new Position(heading.x()<0?-1:1,0);
        if(heading.x()==0&&heading.y()==0)return new Position(1,0);
        return new Position(0,heading.y()<0?-1:1);
    }
    /** IDs e gerações vêm validados pelo coordenador, mas são novamente checados em cada impacto. */
    private static EnemyService.Enemy exact(RpgWorld world, int wireId, long generation) {
        if (wireId <= MOB_BASE || generation < 1) return null;
        return world.skillEnemies().stream().filter(e -> e.getId() == wireId - MOB_BASE && e.generation == generation && world.skillTargetable(e)).findFirst().orElse(null);
    }
    /** Mantém a população elegível da própria região, sem atingir NPCs ou mobs dormentes. */
    private static List<EnemyService.Enemy> live(RpgWorld world) {
        return world.skillEnemies().stream().filter(world::skillTargetable).toList();
    }
    /** Revalida distância e visibilidade sem aceitar coordenada final como estado de combate. */
    private static boolean reachable(RpgWorld world, Position from, Position to, int range) {
        return distance(from, to) <= range && world.skillVisible(from, to);
    }
    /** Impede efeitos entre mapas mesmo quando suas coordenadas coincidem. */
    private static boolean sameRegion(RpgWorld a, RpgWorld b) { return a.networkState().region().equals(b.networkState().region()); }
    /** Revive recusa destino obstruído antes de debitar energia ou iniciar recarga. */
    private static boolean freeRevive(RpgWorld world, CollisionService.Space occupied) {
        var p = world.skillPosition();
        return new CollisionService().isFree(new com.projetok.domain.entity.GameEntity(-1, "Revive", p) { }, p.x(), p.y(), world.physicalRadius(), occupied);
    }
    /** Consulta apoios atuais para revive, inclusive após dashes e deslocamentos do mesmo tick. */
    private static CollisionService.Space occupiedFor(RpgWorld target, Map<Integer, RpgWorld> players) {
        var bodies = new ArrayList<CollisionService.Body>();
        for (var world : players.values()) if (world != target && world.networkState().hp() > 0 && !world.skillIntangible() && sameRegion(target, world))
            bodies.add(CollisionService.Body.block(new com.projetok.domain.entity.GameEntity(-1, "Aliado", world.skillPosition()) { }, world.physicalRadius()));
        for (var enemy : target.skillEnemies()) if (enemy.hp > 0) bodies.add(new CollisionService.Body(enemy, 19));
        var space = target.skillSpace();
        return new CollisionService.Space(space.width(), space.height(), space.obstacles(), bodies);
    }
    /** Mira espacial deve pertencer ao mapa e respeitar os limites do apoio existentes. */
    private static boolean inside(RpgWorld world, Position p) { return p.x() >= 18 && p.y() >= 18 && p.x() <= world.skillSpace().width() - 18 && p.y() <= world.skillSpace().height() - 18; }
    /** Padrões que precisam percorrer a linha, em vez de causar dano em toda a faixa no fim. */
    private static boolean travelingLine(Pattern pattern){return switch(pattern){case LINE_PROJECTILE,WEB,BEAM,FISSURE,FISSURE_ERUPTION->true;default->false;};}
    /** Raio do projétil distingue a frente da fissura, a flecha do leque e a erupção final. */
    private static float shotRadius(Definition skill){return skill.mechanics().pattern()==Pattern.FISSURE_ERUPTION?24:skill.mechanics().pattern()==Pattern.KNOCKBACK_CHECKPOINT?18:skill.mechanics().pattern()==Pattern.CONE_VOLLEY?4:Math.max(4,skill.radius());}
    /** O servidor determina a direção frontal e limita o ponto pelo alcance e pelos limites do mapa. */
    private static Position endpoint(RpgWorld world,Position from,Position heading,int range){
        var unit=direction(new Position(0,0),heading);float travel=range;
        if(unit.x()>0)travel=Math.min(travel,(world.skillSpace().width()-18-from.x())/unit.x());
        else if(unit.x()<0)travel=Math.min(travel,(18-from.x())/unit.x());
        if(unit.y()>0)travel=Math.min(travel,(world.skillSpace().height()-18-from.y())/unit.y());
        else if(unit.y()<0)travel=Math.min(travel,(18-from.y())/unit.y());
        travel=Math.max(0,travel);return new Position(from.x()+unit.x()*travel,from.y()+unit.y()*travel);
    }
    /** Giro preserva a origem no lançador, usado pelos sete raios do leque e pelos quatro dashes. */
    private static Position rotate(Position heading,double angle){return new Position((float)(heading.x()*Math.cos(angle)-heading.y()*Math.sin(angle)),(float)(heading.x()*Math.sin(angle)+heading.y()*Math.cos(angle)));}
    /** Primeiro contato de um segmento com a hitbox circular atual do inimigo, inclusive tangência. */
    private static double contactFraction(Position center,Position from,Position to,double radius){
        double x=(double)from.x()-center.x(),y=(double)from.y()-center.y(),dx=(double)to.x()-from.x(),dy=(double)to.y()-from.y();
        double c=x*x+y*y-radius*radius;if(c<=0)return 0;
        double a=dx*dx+dy*dy;if(a<1e-12)return Double.POSITIVE_INFINITY;
        double b=2*(x*dx+y*dy),discriminant=b*b-4*a*c;if(discriminant<0)return Double.POSITIVE_INFINITY;
        double hit=(-b-Math.sqrt(discriminant))/(2*a);return hit>=0&&hit<=1?hit:Double.POSITIVE_INFINITY;
    }
    /** Publica somente lançamento aceito ou uma fase real já resolvida pelo servidor. */
    private void emit(int player, RpgWorld world, Definition skill, Position from, Position to) {
        events.addLast(new Event(++sequence, clock, player, skill.id(), from.x(), from.y(), to.x(), to.y()));
        while (events.size() > MAX_EVENTS) events.removeFirst();
    }
    /** Tempo de voo provisório a dezoito unidades por tick; nunca causa acerto no lançamento. */
    private static int flight(Position a, Position b) { return Math.max(1, (int) Math.ceil(distance(a, b) / PROJECTILE_SPEED)); }
    /** Somente números finitos podem entrar na geometria. */
    private static boolean finite(float x, float y) { return Float.isFinite(x) && Float.isFinite(y); }
    /** Distância em precisão dupla evita overflow da subtração de floats. */
    private static double distance(Position a, Position b) { return Math.hypot((double) a.x() - b.x(), (double) a.y() - b.y()); }
    /** Direção unitária tem fallback estável quando dois apoios coincidem. */
    private static Position direction(Position a, Position b) {
        double length = distance(a, b); return length < .0001 ? new Position(1, 0) : new Position((float) ((b.x() - a.x()) / length), (float) ((b.y() - a.y()) / length));
    }
    /** Para antes de uma vítima, preservando espaço entre os corpos físicos. */
    private static Position stopBefore(Position from, Position to, float gap) {
        var d = direction(from, to); double step = Math.max(0, distance(from, to) - gap);
        return new Position((float) (from.x() + d.x() * step), (float) (from.y() + d.y() * step));
    }
    /** Projeta um ponto atrás da vítima na direção do golpe, sem usá-lo como teleporte. */
    private static Position extend(Position from, Position to, float amount) { var d = direction(from, to); return new Position(to.x() + d.x() * amount, to.y() + d.y() * amount); }
    /** Segmentos de dash conservam duração mesmo com precisão fracionária do tick. */
    private static Position interpolate(Position a, Position b, float t) { return new Position(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t); }
    /** Distância de um apoio ao segmento finito; usada em skillshots e contatos de dash. */
    private static double segmentDistance(Position p, Position a, Position b) {
        double dx = b.x() - a.x(), dy = b.y() - a.y(), length = dx * dx + dy * dy;
        double t = length == 0 ? 0 : Math.max(0, Math.min(1, ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / length));
        return Math.hypot(p.x() - a.x() - t * dx, p.y() - a.y() - t * dy);
    }
}
