package com.projetok.domain.movement;

import com.projetok.domain.entity.GameEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Grade uniforme de centros dos corpos; uso exclusivo pela thread da simulação. */
public final class SpatialBodyIndex {
    private final float cellSize;
    private final float maxRadius;
    private final List<CollisionService.Body> bodies;
    private final Map<Long, List<CollisionService.Body>> cells = new HashMap<>();
    private final Map<GameEntity, CollisionService.Body> byEntity = new IdentityHashMap<>();
    private final Map<GameEntity, Long> locations = new IdentityHashMap<>();

    /** Constrói a grade uma vez; movimentos posteriores exigem update ou CollisionService. */
    public SpatialBodyIndex(List<CollisionService.Body> bodies, float cellSize) {
        MovementValidation.positive(cellSize);
        this.cellSize = cellSize;
        this.bodies = List.copyOf(bodies);
        float maximum = 0;
        for (var body : bodies) {
            if (byEntity.put(body.entity(), body) != null)
                throw new IllegalArgumentException("Entidade duplicada no índice");
            maximum = Math.max(maximum, body.radius());
            update(body.entity());
        }
        maxRadius = maximum;
    }

    /** Atualiza somente o corpo movido; chamar também após teleporte externo. */
    public void update(GameEntity entity) {
        var body = byEntity.get(entity);
        if (body == null) return;
        var p = entity.getPosition();
        MovementValidation.finite(p.x(), p.y());
        long key = key(cell(p.x()), cell(p.y()));
        Long previous = locations.put(entity, key);
        if (previous != null && previous == key) return;
        if (previous != null) {
            var bucket = cells.get(previous);
            bucket.removeIf(candidate -> candidate == body);
            if (bucket.isEmpty()) cells.remove(previous);
        }
        cells.computeIfAbsent(key, ignored -> new ArrayList<>()).add(body);
    }

    /** Busca conservadora por área; percursos enormes usam fallback sem percorrer células vazias. */
    public List<CollisionService.Body> query(double minX, double minY, double maxX, double maxY) {
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX)
                || !Double.isFinite(maxY) || minX > maxX || minY > maxY)
            throw new IllegalArgumentException("Área de consulta inválida");
        int left = cell(minX - maxRadius), right = cell(maxX + maxRadius);
        int top = cell(minY - maxRadius), bottom = cell(maxY + maxRadius);
        if ((double) ((long) right - left + 1) * ((long) bottom - top + 1) > 4096)
            return bodies;
        List<CollisionService.Body> result = new ArrayList<>();
        for (long x = left; x <= right; x++) for (long y = top; y <= bottom; y++) {
            var bucket = cells.get(key((int) x, (int) y));
            if (bucket != null) result.addAll(bucket);
        }
        return List.copyOf(result);
    }

    /** Confere a composição ao vincular o índice ao contexto. */
    boolean containsExactly(List<CollisionService.Body> other) {
        if (bodies.size() != other.size()) return false;
        for (int i = 0; i < bodies.size(); i++) if (bodies.get(i) != other.get(i)) return false;
        return true;
    }

    /** Saturação para coordenadas extremas gera candidatos extras, nunca os elimina. */
    private int cell(double coordinate) { return (int) Math.floor(coordinate / cellSize); }

    /** Empacota duas coordenadas de célula sem colisão entre chaves. */
    private long key(int x, int y) { return ((long) x << 32) ^ (y & 0xffffffffL); }
}
