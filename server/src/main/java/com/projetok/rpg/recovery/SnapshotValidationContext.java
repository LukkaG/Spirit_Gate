package com.projetok.rpg.recovery;

import com.projetok.rpg.ItemCatalog;
import com.projetok.rpg.QuestService;
import com.projetok.rpg.RpgProgression;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import static com.projetok.rpg.recovery.RecoverableCharacterSnapshot.*;

/**
 * Evidência fornecida pelo chamador confiável, com catálogos já carregados fora da validação.
 * Ausência de evidência permanece explícita; este contexto não é autenticação ou mundo ativo.
 */
public record SnapshotValidationContext(RpgProgression progression, ItemCatalog items,
        Map<QuestKey, QuestService.Definition> quests, Map<String, RegionBounds> regions,
        Set<String> cosmeticIds, ContentHash contentHash, RulesVersion rulesVersion,
        WorldEpoch worldEpoch, Long previousRevision, Instant observedAt,
        Set<RestrictionKind> requiredRestrictions) {

    /** Copia índices e conjuntos; os tipos de catálogo expostos são de consulta ou records imutáveis. */
    public SnapshotValidationContext {
        quests=quests==null?null:Map.copyOf(quests);
        regions=regions==null?null:Map.copyOf(regions);
        cosmeticIds=cosmeticIds==null?null:Set.copyOf(cosmeticIds);
        requiredRestrictions=requiredRestrictions==null?null:Set.copyOf(requiredRestrictions);
    }

    /** Limites geométricos declarados; não verificam ocupação, colisão ou reserva de spawn. */
    public record RegionBounds(float minimumX, float minimumY, float maximumX, float maximumY) {
        /** Recusa contexto geométrico incoerente antes de validar qualquer candidato. */
        public RegionBounds {
            if (!Float.isFinite(minimumX)||!Float.isFinite(minimumY)||!Float.isFinite(maximumX)
                    ||!Float.isFinite(maximumY)||minimumX>maximumX||minimumY>maximumY)
                throw new IllegalArgumentException("Limites regionais inválidos");
        }
        /** Consulta pura dos limites; não representa aprovação física do ponto. */
        public boolean contains(float x,float y) {
            return x>=minimumX&&x<=maximumX&&y>=minimumY&&y<=maximumY;
        }
    }
}
