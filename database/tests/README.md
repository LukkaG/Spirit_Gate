# Testes descartáveis do catálogo — ORG-02

Preparação sem driver, credenciais ou conexão:

```powershell
python database/tests/prepare_mob_catalog_test.py
```

O gerador lê o SQL real e as 10 espécies de `enemies.properties`; não cria espécies
fictícias para substituir arquivos ausentes. Casos de fronteira isolados são usados
somente para verificar constraints. Gera `temp/org02-schema-test.sql`.

Quando houver PostgreSQL e ferramentas cliente disponíveis, criar uma base **nova
e descartável**, com nome `org02_disposable_mobs`, e executar nela:

```powershell
psql -X -v ON_ERROR_STOP=1 -d org02_disposable_mobs -f temp/org02-schema-test.sql
```

Usar o usuário/host do laboratório, sem gravar senhas no repositório. Não apontar
para bases existentes. O SQL recusa outro prefixo e recusa schema game_catalog
preexistente. O prefixo é uma proteção operacional, não prova que uma base é descartável:
o operador ainda deve conferir o destino. `-X` ignora configurações pessoais do psql.

O teste retira somente o COMMIT final do rascunho, insere espécies reais, exporta
CSV para `temp/org02-mobs-roundtrip.csv`, reimporta e compara todas as colunas nos
dois sentidos. Confere 30 rejeições esperadas (limites, leash, IDs, nulidade e PK)
e duas linhas válidas de fronteira. Todo o DDL e os dados terminam em ROLLBACK.
O CSV local permanece como evidência; nenhuma tabela de catálogo deve persistir.
Em erro, ON_ERROR_STOP encerra o cliente e a transação aberta é revertida pela conexão.

Estado nesta ORG-02: gerador executado; testes PostgreSQL **não executados**, pois
psql/postgres/initdb/docker não foram encontrados no PATH. Não instalar ferramentas
nem conectar o jogo ao banco como parte desta etapa.
