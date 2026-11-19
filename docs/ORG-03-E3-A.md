# ORG-03-E3-A — Durabilidade entre processos e recuperação de operações

## Resultado e escopo

Responsável: **Lucas**. Data: **14/11/2026**.
Run-id: `20261114-lucas-e3a-5fbb9a8beb`.
Branch `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.

**Rodada interrompida com segurança: C01 PASSOU, C02 FALHOU no harness,
C03–C09 NÃO EXECUTADOS. E3-A não aprovado tecnicamente.** Não houve crash
ou reinício PostgreSQL, nem prova de recuperação após queda do banco.
O cluster único foi parado em modo fast e seus dados/segredos descartados.

O objetivo permanece demonstrar reconciliação de operações sintéticas após
falhas reais de processos, sem integrar persistência ao jogo. O recorte não
afirma resistência à perda física de armazenamento/cluster. Uma falha do
harness em C02 interrompeu a expansão, conforme os limites aprovados.

## Autorizações efetivamente recebidas

Os cinco itens e quotas foram expressamente autorizados na conversa.
O nome inicialmente veio como `[preencher]`; Lucas o confirmou antes do início.
[Registro sanitizado do aceite](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/authorization.json).
L01–L05 do E2 não foram usados como autorização E3.

| Item | Aceite | Ação efetivamente realizada |
| --- | --- | --- |
| E3A-01 | AUTORIZADO | Cópias/adaptações dos scripts no novo run-id; E2 preservado. |
| E3A-02 | AUTORIZADO | Um initdb/cluster novo em data do run-id aprovado; 127.0.0.1:55433 e relay próprio em 127.0.0.1:55434. |
| E3A-03 | AUTORIZADO | Encerramento de dois clientes psql próprios; relay terminou sem comando kill, após a falha de inspeção. |
| E3A-04 | AUTORIZADO | NÃO EXECUTADO; zero chamadas immediate. |
| E3A-05 | AUTORIZADO | Observação SQL, evidências sanitizadas, parada fast e descarte próprios. Zero reinícios; sem recovery de crash. |

## Quotas aprovadas e consumo observado

| Recurso | Limite aprovado | Observado nesta rodada |
| --- | --- | --- |
| Clusters / initdb | 1 / 1 | 1 / 1; quota consumida |
| Interrupções abruptas | Até 2, exclusivamente pg_ctl stop -m immediate | 0 |
| Reinícios além do start inicial | Até 4 | 0 |
| Identidades sintéticas | 8 personagens / 2 proprietários | Fixtures criadas; imagens C01 de quatro personagens do owner 1 |
| Intenções / tentativas | 64; até 3 tentativas por intenção; payload até 16 KiB | 2 intenções, 1 aplicação cada |
| Workers | 3 simultâneos; 32 instâncias totais | 2 simultâneos; 4 instâncias psql ao todo |
| Conexões SQL | Até 4 simultâneas | Até 2 normais; bootstrap separado |
| Tempo | 120 s/cenário; 20 min/rodada | C01 2,526 s; rodada 28,155 s incluindo encerramento |
| Parada / recuperação | Até 20 s/parada; 45 s/start/recovery | Parada fast concluída; sem recovery |
| Disco | 256 MiB totais; 32 MiB logs/evidências incluídos; mínimo 2 GiB livres | Pico medido 42.901.104 bytes; 133.539.946.496 bytes livres no preflight |

Tamanhos são medições periódicas, não quotas físicas. Encerramento e relatório
ficaram abaixo dos limites; evidências após a entrega em delivery-validation.json.
Um controlador e um relay próprios são distintos dos workers. Não foram ampliadas
quotas, recriado o banco, removida a guarda de uso único ou criado outro run-id.

## Reconhecimento e preservação

Git inicial: apenas seis documentos docs/ORG-03-E2*.md não versionados;
diffs rastreados e do índice vazios. AGENTS.md, relatórios E2, R7/critério E2,
scripts finais e logs locais foram consultados. O status do monitor é histórico;
erros antigos World Studio/IDE/cliente não foram corrigidos ou homologados.

R7 registra HEAD `13b91dca20e16d6729d2dced79c1a4431b74ca09`, alterações locais
extensas e E2 proposto. E2 registra o HEAD atual, início limpo e SQL concluído.
São baselines diferentes; não se reaplicou o estado histórico da R7.
E2 C01–C16/200 intenções/137 recibos são evidências anteriores, não repetidas.
Três hashes de scripts e 250 entradas do manifesto E2 foram conferidos.

O [manifesto de preservação](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/preservation-before.json)
cobre 3.951 arquivos: versionados, seis docs E2 e árvore local E2. Não cobre
todo arquivo ignorado/pessoal. Código de produção, protocolos 6/7, 40 habilidades,
RpgWorld/RpgSessionWorld/LocalSimulationServer/RpgSkillEngine/QuestRewards/
LootService/InventoryService, POM, saves/contas, HUD, assets, offline/Coop,
World Studio, launcher e A-04 não foram modificados.

## Artefatos e próximos limites

Scripts executados preservados em scripts/ do run-id, com
[source-sha256.json](../temp/org03-e3a/20261114-lucas-e3a-5fbb9a8beb/evidence/source-sha256.json).
runtime.py deriva do E2; lab_runtime.py separa lifecycle e identidade;
relay.py controla fluxo TCP; tests.py contém C01–C09; schema.sql mantém API
sintética e acrescenta export/manifest; e2_reference_tests.py é referência não executada.
As versões documentais anteriores ao resultado estão em evidence/documents-before-results.

[Testes](ORG-03-E3-A-TESTES.md), [matriz](ORG-03-E3-A-MATRIZ-FALHAS.md),
[isolamento](ORG-03-E3-A-ISOLAMENTO.md), [pendências](ORG-03-E3-A-PENDENCIAS.md)
e [relatório final](ORG-03-E3-A-RELATORIO-FINAL.md).

Não há autorização para outro cluster. Nova rodada requer aceite de recursos
e run-id novos; não se repetem os cinco aceites já atendidos nesta rodada.
Sem E3-B/E4/E5/ORG-04/TASK-226, integração, migração, commit/push/release/publicação.
