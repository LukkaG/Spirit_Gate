# ALPHA-001 — Auditoria de prontidão do Alpha 0.1

Referência da inspeção: **17/11/2026**, America/Sao_Paulo. Auditoria documental e estática; nenhuma implementação autorizada ou realizada.

## Resultado executivo

**NÃO LIBERAR** o Alpha fechado para instalação externa neste estado de evidência. A-04 permanece **BLOQUEADO / P0**: o runtime mantém intangibilidade enquanto não encontra apoio, sem prazo máximo. A jornada de 20–30 minutos, o EXE em instalação limpa e os dois PCs ainda precisam de prova. Essa recomendação não afirma que a exploração tenha sido demonstrada no mapa distribuído.

A conferência atual confirmou **40 definições**, **71 clips declarados efetivos (40 principais e 31 auxiliares)**, geometria dos 71 atlas e os **56 frames Human/Reaper** com SHA-256 compatível com os metadados. Não houve execução do loader Java nem inspeção visual dos PNGs nesta etapa. A classificação histórica de 24 skills sem ressalvas e 16 com ressalvas continua sendo evidência de 02/11, não homologação do HEAD atual.

Entregáveis: [matriz](ALPHA-001-MATRIZ-PRONTIDAO.md), [riscos e gates](ALPHA-001-RISCOS-GATES.md), [roteiro](ALPHA-001-ROTEIRO-HOMOLOGACAO.md) e [skills/direções](ALPHA-001-MAPA-SKILLS-E-DIRECOES.md).

## Baseline e integridade

- Raiz informada e confirmada por `git rev-parse --show-toplevel`: `C:/Users/lucas/Documents/GitHub/Projeto-K`.
- Branch: `beco`; HEAD completo: `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`; assunto do último commit: `test`.
- `git diff --stat`: vazio. Nenhum arquivo rastreado modificado/excluído no baseline.
- `git status --short`: **50 entradas não rastreadas**, incluindo diretórios agregados. Não equivale a 50 arquivos. Relação abaixo.
- Inventário SHA-256 em memória de **3823 arquivos existentes**, a partir de `git ls-files` e `git ls-files --others --exclude-standard`. Não cobre ignorados, arquivos inacessíveis nem estado anterior ao início desta conversa. Nenhuma restauração foi feita.
- Java consultado: Temurin **17.0.20.1**; Maven **3.10.0**, usando esse Java. Ambiente informa Windows 10 amd64.
- Único POM localizado no inventário normal: [server/pom.xml](../server/pom.xml), `com.projetok:projeto-k:1.0-SNAPSHOT`, release Java 17, JUnit 5.11.4, Surefire 3.5.2 e exec plugin 3.5.0. Cliente, servidor e Studio compartilham esse módulo; não há agregador Maven na raiz.
- Ignorados relevantes, conforme [.gitignore](../.gitignore): `server/target/`, `dist/`, `.task-checkpoints/`, `.maven-repository/`, `temp/`, `saves/`, `studio-projects/`, `browser-preview/` e relatórios de erros/monitor. Seus dados não foram copiados.
- A enumeração `git status --short --ignored` encontrou caminhos longos e diretórios de testes sem permissão. Seu inventário é **parcial**; não há alegação de integridade recursiva desses locais. Nenhuma configuração de permissões/long paths foi alterada.

### Entradas locais anteriores

```text
?? docs/ORG-03-E2-ISOLAMENTO.md
?? docs/ORG-03-E2-MATRIZ-RESULTADOS.md
?? docs/ORG-03-E2-PENDENCIAS-E3.md
?? docs/ORG-03-E2-RELATORIO-FINAL.md
?? docs/ORG-03-E2-TESTES.md
?? docs/ORG-03-E2.md
?? docs/ORG-03-E3-A-ENCERRAMENTO.md
?? docs/ORG-03-E3-A-ISOLAMENTO.md
?? docs/ORG-03-E3-A-MATRIZ-FALHAS.md
?? docs/ORG-03-E3-A-PENDENCIAS.md
?? docs/ORG-03-E3-A-R1-ANALISE-RELAY.md
?? docs/ORG-03-E3-A-R1-PROPOSTA-REEXECUCAO.md
?? docs/ORG-03-E3-A-R1-RELATORIO-FINAL.md
?? docs/ORG-03-E3-A-R1-TESTES-HARNESS.md
?? docs/ORG-03-E3-A-R1.md
?? docs/ORG-03-E3-A-R2-DIAGNOSTICO.md
?? docs/ORG-03-E3-A-R2-ISOLAMENTO.md
?? docs/ORG-03-E3-A-R2-P1-ANALISE-TRANSPORTE.md
?? docs/ORG-03-E3-A-R2-P1-RELATORIO-FINAL.md
?? docs/ORG-03-E3-A-R2-P1-TESTES.md
?? docs/ORG-03-E3-A-R2-P1.md
?? docs/ORG-03-E3-A-R2-PREPARACAO.md
?? docs/ORG-03-E3-A-R2-RELATORIO-FINAL.md
?? docs/ORG-03-E3-A-R3-01-ACEITES-OPERACIONAIS.md
?? docs/ORG-03-E3-A-R3-01-RELATORIO-FINAL.md
?? docs/ORG-03-E3-A-R3-01-TESTES.md
?? docs/ORG-03-E3-A-R3-CRASH-RECOVERY.md
?? docs/ORG-03-E3-A-R3-ISOLAMENTO.md
?? docs/ORG-03-E3-A-R3-PROPOSTA-EXECUCAO.md
?? docs/ORG-03-E3-A-R3-RELATORIO-FINAL.md
?? docs/ORG-03-E3-A-RELATORIO-FINAL.md
?? docs/ORG-03-E3-A-TESTES.md
?? docs/ORG-03-E3-A.md
?? docs/ORG-03-E3-B-A1-ALTERNATIVAS-D33.md
?? docs/ORG-03-E3-B-A1-COMPARATIVO-BACKUP.md
?? docs/ORG-03-E3-B-A1-CONTRATO-S0-S1-R.md
?? docs/ORG-03-E3-B-A1-DECISOES-PENDENTES.md
?? docs/ORG-03-E3-B-A1-FASES-E-CENARIOS.md
?? docs/ORG-03-E3-B-A1-RELATORIO-FINAL.md
?? docs/ORG-03-E3-B-A1-REVISAO-ESCOPO.md
?? docs/ORG-03-E3-B-CRITERIOS-ACEITE.md
?? docs/ORG-03-E3-B-MATRIZ-RISCOS.md
?? docs/ORG-03-E3-B-PENDENCIAS.md
?? docs/ORG-03-E3-B-PLANO.md
?? docs/ORG-03-E3-B-RELATORIO-PREPARACAO.md
?? docs/evidencias/
?? tools/org03_e3a_r1/
?? tools/org03_e3a_r2/
?? tools/org03_e3a_r3/
?? tools/org03_e3a_transport_fix/
```

## Governança e método

Lidos [AGENTS.md](../AGENTS.md), [logs/README.md](../logs/README.md), [skill local](../.agents/skills/spiritgate-server-engineer/SKILL.md), [plano Corujão IX](CORUJAO-IX-PLANO.md) e o Plano Diretor original preservado em [.task-checkpoints/CORUJAO-IX/TASK-223/user-directive.txt](../.task-checkpoints/CORUJAO-IX/TASK-223/user-directive.txt). Não foi encontrado arquivo autônomo com nome Plano Diretor fora desse registro; a referência foi identificada pelo plano técnico. A ordem histórica de iniciar somente TASK-223 contextualiza aquela rodada; o pedido atual autoriza somente ALPHA-001.

O padrão local de relatórios é plano em `docs/` com nomes em maiúsculas; os cinco arquivos ALPHA-001 seguem esse padrão. Nenhum documento anterior, Plano Diretor, gate ou ORG-03 foi editado. [ORG-03.md](ORG-03.md) foi consultado apenas para reconhecer a fronteira: captura/contratos locais não demonstram importador, autenticação ou persistência online. Os laboratórios 193–196, E3 e D33 não foram executados nem investigados internamente.

Inspeção: Git, busca de arquivos, leitura de fontes/testes/documentos, versões Java/Maven e script Python inline de biblioteca padrão, via `python -B -`, exclusivamente leitor de fontes, manifestos e PNGs públicos do checkout. O script não importou módulos do projeto, não acessou saves, contas, banco ou rede; sua saída foi mantida em memória para montar estas tabelas.

Os estados usados na matriz são exatamente **IMPLEMENTADO, TESTADO, PENDENTE, BLOQUEADO, NÃO VERIFICADO**. TESTADO sempre identifica escopo e origem. Artefato existente não é prova de execução. Nesta auditoria, TESTADO para assets significa apenas inspeção binária/metadados; comportamento de jogo permanece NÃO VERIFICADO ou BLOQUEADO.

## Inventário operacional

| Subsistema | Caminhos reais e entradas |
|---|---|
| Cliente RPG | `server/src/main/java/com/projetok/prototype/rpg/RpgLauncher.java:main`; `multiplayer/RpgNetworkClientLauncher.java:main/open` |
| Servidor | `multiplayer/LocalSimulationServer.java:start/main`; fábrica `prototype/rpg/LauncherSession.java:start`; simulação `multiplayer/RpgSessionWorld.java` |
| Coop legado | `multiplayer/CoopWorld.java`, `NetworkClientLauncher.java`; não equivale automaticamente ao RPG com aplicações 6/7 |
| World Studio | `studio/WorldStudio.java`, entrada `world-studio.cmd`, `tools/build-world-studio.ps1`; publicação separada e não executada |
| Launcher/distribuição | `prototype/rpg/RpgMenu.java`, `RadminSessionPanel.java`, `LauncherSession.java`; `tools/build-windows-portable.ps1`, `tools/integrar_launcher_radmin.py`; `dist/` ignorado |
| Sprites e skills | `assets/dev/player-base/`, `assets/dev/reaper/`, `assets/skills/`; metadados em `server/src/main/resources/rpg/` |
| Conteúdo | `server/src/main/resources/rpg/`, `studio-projects/` ignorado; sem leitura de projetos pessoais |
| Testes/ferramentas | `server/src/test/java/`, `tools/run-local-tests.py`, testes Python em `tools/test_*.py` |
| Documentos | `docs/TASK-213…225*`, Corujões VI–IX, ORG-02/ORG-03; checkpoints sob `.task-checkpoints/` |

## Resultados e divergências

1. O HEAD atual difere de `6d72c65` dos relatórios 223–225. O checkout também contém trabalho documental ORG-03 não rastreado. Nenhum resultado histórico foi atribuído a este HEAD.
2. [SKILLS-OITO-DIRECOES.md](SKILLS-OITO-DIRECOES.md), de 13/11, e `RpgSkillEngine.eightDirections` mostram direção autoritativa LINE/CONE em oito passos, posterior ao catálogo de auditoria antigo. `DirectionalAnimation.Direction` ainda contém quatro direções. Não há atlas corporal diagonal confirmado.
3. O catálogo efetivo de apresentação racial só declara IDLE e WALK. Human normaliza fontes maiores para canvas lógico 64×64; Reaper usa PNGs 64×64. Aetheri/Spirit usam Human provisório. RUN/ATTACK/HIT/DEATH próprios não foram encontrados nesse caminho; o atlas oficial de 59 posições é outro contrato.
4. [TASK-225-R1](TASK-225-R1-RELATORIO-FINAL.md) resolveu as fixtures antigas de progressão/missão; isso não reduziu os 10.000 HP do encontro com conteúdo. As falhas antigas não devem ser anunciadas como ainda reproduzidas nesta auditoria.
5. `Protocol.VERSION=1` é o framing base; `GAMEPLAY_APPLICATION=6` e `SKILL_APPLICATION=7` são aplicações negociadas. “V6” não descreve sozinho todas as capacidades atuais.
6. Oito kits de skills correspondem a **dez IDs de domínio**: dois arcos compartilham Aetheri e dois domínios Trevas compartilham Spirit. Não inventar duas famílias extras de skills.
7. A-03: `NetworkSkillPresentation.actors` conserva a limpeza do lançador morto/ausente. Trajetória intermediária e terminais do alvo continuam problemas separados; 28,676 unidades é medição histórica, não atual.
8. A-04: `LightningField.advance` só termina após `skillReappear` retornar verdadeiro. A busca local/global repete no owner; outros jogadores não são explicitamente desligados, mas podem sofrer atraso do tick. Servidor e jogador não estão em um loop Java infinito dentro de um único tick: a espera é reiterada entre ticks.
9. Readmissão chama novo `LocalClient`; `RpgSessionWorld.leave` remove sessão e skills. ID/UUID e recursos não representam recuperação do personagem anterior. Não prometer persistência online.
10. Diretórios de builds privadas posteriores existem em `dist/` (incluindo Energy 0.226.0-private). Nome de build **não comprova TASK-226**, conformidade de bytes, instalação limpa ou publicação.
11. [ORG-02-R2](ORG-02-R2-RELATORIO-FINAL.md) registra intermitência de foco ainda sem causa/correção demonstrada, apesar de rodadas verdes. Os logs do Studio também não são atestado de correção.

## Testes e verificações realmente realizados

| Verificação atual | Resultado único | Limite |
|---|---|---|
| Inventário de catálogo | 40 IDs, oito kits, cinco slots por kit | Leitura estática; não conjuração |
| Manifestos base + V2…V8 | 71 clips efetivos, 40 principais/31 auxiliares; 71 PNGs com assinatura/dimensões e recortes dentro do atlas | Checagem de cabeçalho/geometria; sem ImageIO, decodificação integral ou avaliação estética |
| Human/Reaper | 56 entradas, 20 Human + 36 Reaper; 56 arquivos e hashes correspondentes | Não prova render, movimento, transparência visual ou poses extras |
| Baseline/final | Comparação dos hashes protegidos e Git; resultado final registrado abaixo | Ignorados não cobertos |

São **127 entradas de assets verificadas** (71 + 56), sem somar reexecuções da mesma coleta. Não são 127 testes JUnit. **0 casos JUnit, 0 TCP, 0 GUI, 0 sessões físicas** executados pela ALPHA-001.

Não executados: `mvn test`, scripts de teste/build, launchers, EXE, atualização, banco, Radmin, benchmark e jogo. Maven apenas consultado com `mvn -version`. A suíte existente grava PNGs históricos e medições: `Task225MaterializationTest.measuresCompleteSearchAndWritesBoundedReproductionEvidence` escreve em `docs/skills-preview-task225/`; R1 também grava no diretório histórico. `tools/run-local-tests.py` escreve em target e executa testes; compile-local grava em target/launcher e pode registrar falhas em logs; builders gravam em target/dist. Nenhum foi invocado.

É possível preparar futuramente uma árvore temporária com fontes/recursos públicos e cache offline, revisar os caminhos de todos os testes selecionados e isolar também o home/logs. Essa árvore não foi preparada nesta auditoria; portanto a ausência de testes de runtime é limitação de evidência, não incapacidade afirmada de executá-los com segurança.

Resultados antigos (não somáveis): R3 registra 1.033 casos únicos na sua revisão; ORG-02-R2 registra 1.068 em revisão posterior; oito direções registra 168 selecionados. Nenhum desses totais foi reexecutado aqui nem agregado como cobertura atual.

## Logs locais

`logs/monitor/status.json` consultado: modo codex/fase aguardando, listas vazias de pendências/falhas, registro antigo. Não comprova processo ativo nem ausência de erros atuais. Os TXT presentes incluem exceções de seleção/elemento/limites no World Studio em 08/11 e diagnósticos antigos da IDE. Apenas identificação do contexto, sem transcrição de dados particulares, correção ou marcação de atendimento. O pedido ALPHA-001 proíbe patches e arquivos novos fora da documentação; não foi criado registro em logs/analises.

## Limites, reversão e próximo passo

Sem alteração de produção, testes, scripts, assets, protocolo, configuração, saves, bancos, builds ou relatórios existentes. Sem commit/push/pull/instalação/publicação. Compatibilidade foi preservada por ausência de patch; não equivale a nova regressão executada.

A cobertura comportamental é parcial: segurança completa do decoder, todos os cenários de IA, estética e performance não foram comprovados no runtime. As matrizes explicitam os itens NÃO VERIFICADO e os roteiros necessários. Nenhum gate foi fechado.

Reversão documental: retirar individualmente apenas os cinco arquivos ALPHA-001, se solicitado, após conferir alterações posteriores. Próximo passo proposto: revisar as decisões de A-04, progresso de sessão, acesso às skills e perfil de conteúdo antes de autorizar ALPHA-002. A sequência proposta está no registro de riscos e não foi iniciada.

## Conferência final

Comparação final: **3.823 arquivos anteriores preservados por SHA-256; zero divergências ou ausências nesse inventário**. HEAD inalterado. As únicas cinco adições são os documentos ALPHA-001 listados no início; Git passou de 50 para 55 entradas não rastreadas. `git diff --stat` permanece vazio e `git diff --check` não apontou erro em arquivos rastreados; como os relatórios são novos/não rastreados, a estrutura e os links deles recebem conferência própria. Ignorados e caminhos inacessíveis continuam fora da garantia de hashes.

Conferência documental: 24 trilhas de prontidão, 40 linhas de skills e 192 combinações raça/ação/direção. Links relativos verificados contra arquivos presentes. Nenhum resultado de teste de runtime foi acrescentado por essa checagem.

