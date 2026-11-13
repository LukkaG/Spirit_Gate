"""Prepara SQL de regressão para PostgreSQL descartável, sem abrir conexão."""
from pathlib import Path
import re


def literal(value):
    """Escapa um literal SQL de texto; os IDs também são validados antes da emissão."""
    return "'" + value.replace("'", "''") + "'"


def prepare():
    """Usa o catálogo real e gera um teste transacional com rollback de todo o DDL."""
    root = Path(__file__).resolve().parents[2]
    output = root / "temp" / "org02-schema-test.sql"
    csv = root / "temp" / "org02-mobs-roundtrip.csv"
    draft = (root / "database/drafts/001_mob_catalog.sql").read_text(encoding="utf-8")
    if not draft.rstrip().endswith("COMMIT;"):
        raise ValueError("Rascunho mudou: revisar isolamento transacional antes de gerar")
    draft = draft.rstrip()[:-len("COMMIT;")]
    rows = []
    ids = set()
    for line in (root / "server/src/main/resources/rpg/enemies.properties").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        mob_id, data = line.split("=", 1)
        if not re.fullmatch(r"[a-z][a-z0-9_-]{0,39}", mob_id) or mob_id in ids:
            raise ValueError("ID inválido ou duplicado no catálogo real")
        ids.add(mob_id)
        numbers = [int(value) for value in data.split("|")]
        if len(numbers) == 6:
            numbers.append(1)
        if len(numbers) != 7:
            raise ValueError("Formato de mob incompatível")
        rows.append("(" + literal(mob_id) + "," + ",".join(map(str, numbers)) + ")")
    if not rows:
        raise ValueError("Catálogo real vazio")
    checks = []
    low = [1, 1, 1, 40, 40, 0, 1]
    high = [10000, 100, 200, 800, 900, 9000, 1]

    def insert(mob_id, numbers):
        return "INSERT INTO game_catalog.mob_definition VALUES (" + mob_id + "," + ",".join(map(str, numbers)) + ")"

    def reject(statement, state):
        checks.append("SELECT pg_temp.must_reject(" + literal(statement) + "," + literal(state) + ");")

    for index in range(7):
        below, above = low.copy(), high.copy()
        below[index] -= 1
        above[index] += 1
        reject(insert("'invalid'", below), "23514")
        reject(insert("'invalid'", above), "23514")
        nulls = low.copy()
        nulls[index] = "NULL"
        reject(insert("'invalid'", nulls), "23502")
    reject(insert("'invalid'", [40, 6, 65, 230, 229, 240, 1]), "23514")
    for mob_id in ["", "1mob", "Mob", "mob x", "mob\n", "a" * 41]:
        reject(insert(literal(mob_id), low), "23514")
    reject(insert("NULL", low), "23502")
    reject(insert(literal(sorted(ids)[0]), low), "23505")
    sql = r"""\set ON_ERROR_STOP on
-- Executar somente em uma base descartável nova cujo nome comece com org02_disposable_.
SELECT left(current_database(),17) = 'org02_disposable_'
       AND NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname='game_catalog') AS safe_database \gset
\if :safe_database
\else
\echo 'RECUSADO: exige base org02_disposable_* sem schema game_catalog.'
\quit 3
\endif
""" + draft + "\n"
    sql += "INSERT INTO game_catalog.mob_definition VALUES\n" + ",\n".join(rows) + ";\n"
    sql += "CREATE TEMP TABLE expected_mobs AS SELECT * FROM game_catalog.mob_definition;\n"
    sql += "CREATE TEMP TABLE imported_mobs (LIKE game_catalog.mob_definition INCLUDING ALL);\n"
    sql += "\\copy expected_mobs TO " + literal(csv.as_posix()) + " WITH (FORMAT csv)\n"
    sql += "\\copy imported_mobs FROM " + literal(csv.as_posix()) + " WITH (FORMAT csv)\n"
    sql += """DO $$ BEGIN
    IF EXISTS ((SELECT * FROM expected_mobs EXCEPT SELECT * FROM imported_mobs)
        UNION ALL (SELECT * FROM imported_mobs EXCEPT SELECT * FROM expected_mobs)) THEN
        RAISE EXCEPTION 'Roundtrip de catálogo divergente';
    END IF;
END $$;
CREATE FUNCTION pg_temp.must_reject(statement text, expected_state text) RETURNS void
LANGUAGE plpgsql AS $$ BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN RETURN; END IF;
        RAISE;
    END;
    RAISE EXCEPTION 'Constraint ausente para: %', statement;
END $$;
"""
    sql += "\n".join(checks) + "\n"
    sql += insert("'a'", low) + ";\n" + insert(literal("a" * 40), high) + ";\n"
    sql += "ROLLBACK;\n\\echo 'ORG-02: catálogo, constraints e roundtrip aprovados; DDL revertido.'\n"
    output.parent.mkdir(exist_ok=True)
    output.write_text(sql, encoding="utf-8")
    print(f"Preparado: {output} ({len(rows)} espécies reais, {len(checks)} rejeições esperadas).")
    print("Nenhuma conexão foi aberta. Execute com psql -X -v ON_ERROR_STOP=1 -d org02_disposable_<nome> -f " + str(output))


if __name__ == "__main__":
    prepare()
