-- RASCUNHO: PostgreSQL próprio. Não executado nem usado pelo runtime.
-- Escopo: espécies; não armazena instâncias vivas, spawns ou recompensas.
BEGIN;
CREATE SCHEMA game_catalog;
CREATE TABLE game_catalog.mob_definition (
    id text PRIMARY KEY CHECK (id ~ '^[a-z][a-z0-9_-]{0,39}$'),
    hp integer NOT NULL CHECK (hp BETWEEN 1 AND 10000),
    attack integer NOT NULL CHECK (attack BETWEEN 1 AND 100),
    speed integer NOT NULL CHECK (speed BETWEEN 1 AND 200),
    aggro integer NOT NULL CHECK (aggro BETWEEN 40 AND 800),
    leash integer NOT NULL CHECK (leash BETWEEN aggro AND 900),
    respawn_ticks integer NOT NULL CHECK (respawn_ticks BETWEEN 0 AND 9000),
    level integer NOT NULL CHECK (level = 1)
);
COMMENT ON TABLE game_catalog.mob_definition IS
    'Espécies compatíveis com MobDefinition; IDs preservam referências locais existentes.';
COMMENT ON COLUMN game_catalog.mob_definition.respawn_ticks IS
    'Duração em ticks do runtime; zero conserva ausência de respawn automático.';
COMMIT;
