package com.projetok.domain.mob;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/** Catálogo de espécies independente de arquivos, rede e banco de dados. */
public final class MobCatalog {
    private final Map<String, MobDefinition> definitions;

    /** Recebe definições já validadas, recusando IDs repetidos. */
    public MobCatalog(Collection<MobDefinition> entries) {
        var indexed = new TreeMap<String, MobDefinition>();
        for (var entry : entries) {
            if (indexed.putIfAbsent(entry.id(), entry) != null)
                throw new IllegalArgumentException("ID de mob duplicado: " + entry.id());
        }
        definitions = Map.copyOf(indexed);
    }

    /** Adapta o formato local histórico; um futuro adaptador SQL poderá usar o construtor. */
    public static MobCatalog fromProperties(Map<String, String> entries) {
        var parsed = new TreeMap<String, MobDefinition>();
        entries.forEach((id, value) -> {
            String[] fields = value.split("\\|", -1);
            if (fields.length != 6 && fields.length != 7)
                throw new IllegalArgumentException("Definição de mob inválida: " + id);
            int[] n = Arrays.stream(fields).mapToInt(Integer::parseInt).toArray();
            parsed.put(id, new MobDefinition(id, n[0], n[1], n[2], n[3], n[4], n[5],
                    n.length == 7 ? n[6] : 1));
        });
        return new MobCatalog(parsed.values());
    }

    /** Resolve uma espécie por ID estável; referências ausentes são erros de conteúdo. */
    public MobDefinition get(String id) {
        var definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Mob ausente: " + id);
        return definition;
    }

    /** Expõe o catálogo imutável, sem dados mutáveis de uma sessão. */
    public Collection<MobDefinition> all() { return definitions.values(); }
}
