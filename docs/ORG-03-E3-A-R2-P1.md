# ORG-03-E3-A-R2-P1 — Correção isolada do transporte

Responsável: Lucas. Data: 15/11/2026. Branch `beco`, HEAD
`c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. Escopo: corrigir reset deliberado
e captura de stderr, testar sem PostgreSQL e preparar integração futura.

## Auditoria anterior à modificação

Lidos AGENTS.md, skill local `.agents/skills/spiritgate-server-engineer/SKILL.md`,
os quatro documentos R2, análise/testes R1, fontes executadas e evidências R2.
Revisados os 49 eventos, em especial PAUSE/COMMIT/retidos/terminate/reset e
UnicodeDecodeError. Conferidos os manifestos R2 (57 artefatos e preservação de
4.023 arquivos), R1 (16 artefatos), E2 (250 artefatos), sem diferenças.
Git continha documentos e tools de laboratórios anteriores não rastreados;
nenhuma mudança rastreada. Nenhum trabalho anterior foi descartado.

Baseline P1: **4.081 arquivos** rastreados e das árvores/documentos anteriores,
em `tools/org03_e3a_transport_fix/evidence/preservation-before.json`.
Auditoria final sem diferenças. Os run-ids encerrados permanecem imutáveis.
O monitor local conservava estado histórico sem pendentes declarados; não
se iniciou atendimento de erros do jogo fora deste escopo.

## Arquivos próprios

Tudo está em [tools/org03_e3a_transport_fix](../tools/org03_e3a_transport_fix):

| Arquivo | Papel |
| --- | --- |
| `transport.py` | Permit verificável, armamento no evento pré-terminate, separação dos resultados e gate de C02. |
| `relay.py` | Pump opaco com origem/operação exatas e classificação conservadora. CLI SQL bloqueado. |
| `lifecycle.py` | Reutiliza a classe OwnedProcess R1 intacta e implementa captura binária nova. |
| `legacy/` | Quatro cópias exatas R2 para conservar os 23 testes históricos e a classe de guarda. |
| `c02_integration.py` | Adaptador dos passos de C02, dependências injetadas; sem runtime/SQL/cluster. |
| `test_transport_fix.py` | 48 novos testes puros, incluindo recusas e integração simulada. |
| `fixture.py`, `fixture_peer.py` | Controle loopback com cliente/relay próprios, upstream sintético e prova de encerramento. |
| `audit.py`, `validate.py` | Baseline, hashes, sintaxe, diffs e suíte combinada. |
| `evidence/` | Resultados, falhas intermediárias, eventos, portas, manifestos antes/depois. |

Não foi copiado um runtime SQL operacional para esse pacote. A classe
OwnedProcess é importada diretamente de `legacy/lifecycle.py`, idêntica à R2;
RUNNING_MATCH/EXITED_CONFIRMED/IDENTITY_MISMATCH/UNKNOWN e seu latch são preservados.

## Mudança e critérios

O controlador só arma o pump durante PROCESS_TERMINATE da guarda original,
após RUNNING_MATCH recente, com o cliente, handle, socket aceito e barreiras
exatos. O ACK privado antecede a chamada terminate do handle. Reset no recv
desse cliente pode concluir transporte; reset fora do contrato continua erro.
Falha no ACK impede o terminate daquela ação. Nenhum fallback foi introduzido.

Stderr é binário, consumido continuamente, com prefixo em memória limitado,
hash de todos os bytes lidos, substituições explícitas, EOF/erro/truncamento e
sanitização por projeção permitida. Texto livre e SQL não são persistidos.
Captura incompleta invalida a finalização; bytes UTF-8 inválidos não quebram a thread.

Critérios verificados: 23 testes antigos e 48 novos passaram; fixture positiva
e negativa passaram com reset 10054 real; nenhuma promoção automática a
aprovação econômica; fontes anteriores preservadas. Detalhes e limites nos
[testes](ORG-03-E3-A-R2-P1-TESTES.md) e na
[análise de transporte](ORG-03-E3-A-R2-P1-ANALISE-TRANSPORTE.md).

## Fronteiras

Nenhum PostgreSQL iniciado, SQL executado, cluster criado, crash ou novo E3-A.
Não foram acessadas portas de serviços existentes. Sem produção, gameplay,
classes do jogo, protocolos 6/7, habilidades, sprites, HUD, launcher, World Studio,
saves/contas, dependências, commit, push ou gates posteriores.
O isolamento da fixture é coberto pelo pedido explícito desta tarefa; AGENTS.md
e a skill local não impõem aceite adicional para esses sockets/subprocessos próprios.

R2 continua C01 PASSOU/C02 FALHOU/C03–C09 NÃO EXECUTADOS. E3-A não aprovado.
[R3 é apenas proposta](ORG-03-E3-A-R3-PROPOSTA-EXECUCAO.md).
