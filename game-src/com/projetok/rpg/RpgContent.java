package com.projetok.rpg;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Catalogo local versionado. Nenhum script ou classe arbitraria e executado. */
public final class RpgContent {
    public record Definition(String id, String name, String region, int value) { }
    private final Map<String, Map<String, Definition>> entries;
    private RpgContent(Map<String, Map<String, Definition>> entries) { this.entries = Map.copyOf(entries); }
    public Definition get(String kind, String id) {
        Definition definition = entries.getOrDefault(kind, Map.of()).get(id);
        if (definition == null) throw new IllegalArgumentException("Referencia ausente: " + kind + "." + id);
        return definition;
    }
    public Collection<Definition> all(String kind) { return entries.getOrDefault(kind, Map.of()).values(); }
    public static RpgContent builtin() {
        return Builtin.VALUE;
    }
    private static final class Builtin { static final RpgContent VALUE=loadBuiltin(); }
    private static RpgContent loadBuiltin() {
        try (InputStream in = RpgContent.class.getResourceAsStream("/rpg/content.properties")) {
            if (in == null) throw new IOException("Catalogo RPG ausente");
            return load(in);
        } catch (IOException error) { throw new IllegalStateException("Conteudo RPG invalido", error); }
    }
    public static RpgContent load(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(65_537);
        if (bytes.length > 65_536) throw new IllegalArgumentException("Catalogo excessivo");
        Properties properties = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("ID duplicado: " + key);
                return super.put(key,value);
            }
        };
        properties.load(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        if (!"1".equals(properties.remove("schema")) || properties.size()>512) throw new IllegalArgumentException("Schema/limites invalidos");
        Map<String, Map<String, Definition>> entries = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            String[] parts = key.split("\\.", -1), values = properties.getProperty(key).split("\\|", -1);
            if (parts.length!=2 || !Set.of("item","enemy","region","npc","quest").contains(parts[0])
                    || !parts[1].matches("[a-z][a-z0-9_-]{0,39}") || values.length!=3
                    || values[0].isBlank() || values[0].length()>80 || !values[1].matches("[a-z][a-z0-9_-]{0,39}"))
                throw new IllegalArgumentException("Definicao invalida: " + key);
            int value;
            try { value = Integer.parseInt(values[2]); } catch (NumberFormatException e) { throw new IllegalArgumentException("Numero invalido: " + key,e); }
            if (value<0 || value>100_000) throw new IllegalArgumentException("Valor fora do limite");
            entries.computeIfAbsent(parts[0], k->new TreeMap<>()).put(parts[1], new Definition(parts[1],values[0],values[1],value));
        }
        Map<String,Map<String,Definition>> immutable = new TreeMap<>();
        entries.forEach((k,v)->immutable.put(k,Map.copyOf(v)));
        RpgContent result = new RpgContent(immutable);
        for (String kind : Set.of("item","enemy","region","npc","quest")) {
            if (result.all(kind).isEmpty()) throw new IllegalArgumentException("Categoria ausente: " + kind);
            for (Definition definition : result.all(kind)) result.get("region",definition.region());
        }
        return result;
    }
}
