# ORG-03-E2 — Matriz de resultados

Execução de referência: `20261114-lucas-600aa9e066`, em 14/11/2026. Responsável: Lucas.
Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
Autorização explícita L01–L05 recebida nesta conversa, com seis ressalvas;
registro local em [authorization.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/authorization.json).
As políticas definitivas D14–D22, D27 e D34 não foram aprovadas por este aceite.

## Execução final

| Caso | Estado | Resultado observado | Evidência |
| --- | --- | --- | --- |
| C01 — Constraints | PASSOU | 15 violações negativas, incluindo campo econômico ausente/null, PK, operação, grant, origem, quantidade, claim, versão e vínculo; rollback comparado com imagem integral. | [C01.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C01.json) |
| C02 — Concorrência de revisões | PASSOU | 20 pares A/B alternados: um commit e um conflito por base; incremento conjunto +1. Checkpoint separadamente avança somente StateRevision. | [C02.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C02.json) |
| C03 — Agregados atômicos | PASSOU | Recompensa/poção/equipamento: 21 falhas injetadas nos sete pontos de escrita, nenhuma imagem parcial; commit completo; fronteiras XP/moedas e políticas separadas. | [C03.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C03.json) |
| C04 — Deduplicação | PASSOU | 20 pares para mesma concessão/IDs distintos; retry idêntico histórico; divergência de kind/conteúdo/beneficiário; nova definição de quest sem novo pagamento. | [C04.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C04.json) |
| C05 — Rollback | PASSOU | 22023 antes da aplicação e P0099 no meio; consulta independente sem alteração; recusa divergente não invalida commit anterior. | [C05.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C05.json) |
| C06 — Resposta perdida | PASSOU | Resposta de commit conhecido suprimida; chamador UNCERTAIN; B consulta o mesmo recibo; retry sem novos efeitos. | [C06.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C06.json) |
| C07 — Histórico incompleto | PASSOU | Arquivo indisponível produz UNCERTAIN; corrupção administrativa isolada de recibo produz P0005; fixture restaurada; nenhuma reaplicação. | [C07.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C07.json) |
| C08 — Snapshot antigo | PASSOU | CAS de checkpoint antigo recusa; recibo histórico continua consultável sem reinstalar versão. | [C08.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C08.json) |
| C09 — Legado | PASSOU | Flags/pending sintéticos persistidos sem alteração do hash; recuperação integral classificada RECOVERY_DENIED_A; nenhum recibo retroativo. | [C09.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C09.json) |
| C10 — Retenção | PASSOU | 32 confirmações; 33ª recusa P0006; arquivo conserva índice/hash; retry histórico/divergente e grant velho não pagam de novo. | [C10.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C10.json) |
| C11 — Beneficiário e autoridade | PASSOU | Runner sem superuser/createdb/createrole/replication; DML/DDL negados; proprietário cruzado negado; claim concorrente único; fence antigo P0003; recibo antigo acessível ao dono. | [C11.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C11.json) |
| C12 — Direitos individuais | PASSOU | Duas origens de herb (2+3) recolhidas juntas por espécie; ore (4) continua pendente; conservação 9=5+4; pacote inteiro recusado em cenário próprio. | [C12.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C12.json) |
| C13 — Obrigação antiga | PASSOU | Perfis ordered/prefix/gaps; geração nova não elimina dívida antiga; prefixo intermediário não atravessa pendência; dois créditos exatos e replay sem prova recusado. | [C13.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C13.json) |
| C14 — Precisão temporal | PASSOU | Nove canais, slots 0–4 do domínio thunder (IDs thunder_1–5); bits binary64, -0, long máximo, absent e NaN distintos; decimal/origem/época/versão incompatíveis recusados. | [C14.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C14.json) |
| C15 — Deadlock e serialização | PASSOU | 10 rodadas 40P01 + 10 rodadas 40001; locks/barreiras registrados; retry integral com a mesma intenção, duas tentativas; 55P03 separado sem efeito. | [C15.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C15.json) |
| C16 — Distribuição | PASSOU | Manifesto SQL fixo para A/B; A pago, B adiado e reconciliado; nenhum RNG; alternativa global aborta ambos na falha. | [C16.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/C16.json) |

**16 PASSOU, 0 FALHOU, 0 NÃO EXECUTADO, 0 INCONCLUSIVO nesta execução final.**
Rodadas anteriores são separadas e não foram apagadas nem somadas como testes
únicos. Os três primeiros run-ids têm falhas de infraestrutura/harness;
ver [relatório final](ORG-03-E2-RELATORIO-FINAL.md).

## Leitura das evidências

Cada JSON contém cenário, fixture/intenção, bases/resultados e imagens
pertinentes. O [transcript SQL](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/sql-transcript.jsonl) completa
SQLSTATE, backend, linhas retornadas e comandos de commit/rollback.
Em SELECT de função, ROW_COUNT é o número de linhas retornadas pela função;
o campo `rows` de seu resultado indica o CAS/escrita semântica. Não confundir
esses dois contadores. C02/C04 incluem ordem de bloqueio, operação, revisões,
recibos, recursos e leitura observadora; C15 registra os mesmos dados por rodada.

Configuração/autorização/versão comuns ficam em
[preflight](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/preflight.json),
[configuração própria](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/effective-startup-files.json),
[identidade](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/cluster-identity.json),
[propriedade](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/ownership.json) e
[hashes do harness/DDL](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/source-sha256.json).
C09/C14 são também verificações puras sobre dados persistidos: não há mutação
econômica ou recibo a atribuir a um round-trip de representação.

## Limites das provas

F01/F02/F03/F09/F10/F11/F15/F16/F17/F25/F26/F29 receberam os ensaios E2;
F04/F19/F21/F23 apenas seus recortes sintéticos. F05/F06/F08/F28/F30 e demais
provas de processo/restore/owner continuam nos gates posteriores.
Nenhuma linha PASSOU equivale a homologação de conta, lease, tempo durável,
crash, gameplay real, rede/Radmin, launcher ou salvamento online.
