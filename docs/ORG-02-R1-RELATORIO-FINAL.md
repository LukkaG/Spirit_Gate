# ORG-02-R1 — Relatório final

## Resultado e limite da investigação

As duas regressões gerais passaram nos 1068 casos cada, incluindo os quatro novos
da ORG-02, sem falhas, erros ou skips. Os 23 casos dirigidos de mob/recursos
também passaram. A estabilização permanece **com bloqueio
de causa raiz**: duas rodadas dirigidas apresentaram ausência de destinatário
AWT, enquanto outras ordens passaram. Não declarar a intermitência corrigida.
O comando padrão foi repetido sem cópia manual de resources; as rodadas gerais
levaram 4min42s (trace explícito) e 4min32s (padrão).

Os três erros históricos foram localizados na asserção de identidade de foco do
helper. Não eram asserções de disponibilidade das habilidades. Não reproduzimos
esses três stack traces nesta etapa. Reproduzimos duas ocorrências diferentes de
`SwingKeys.post`: ausência de componente focado ao continuar a preparação de área.

Na ocorrência instrumentada, a EDT registrou `FOCUS_LOST` aos 2850ms,
`WINDOW_LOST_FOCUS` aos 2860ms e `WINDOW_DEACTIVATED` aos 2861ms, todos com
`opposite=null`. A sessão continuava conectada, mundo desbloqueado, nível 20,
domínio bow_long, personagem vivo e roteador aberto, sem modal. O slot de área
não tinha cooldown; o smartcast anterior tinha recarga real e energia consumida.
A falha posterior foi ausência de destinatário, não recusa de conjuração.

Isso comprova a desativação nativa nessa repetição, mas não identifica seu
causador nem estabelece a mesma causa para os erros originais. O monitor nativo
posterior observa primeiro plano do Windows; suas amostras não retroagem à falha.

## Arquivos modificados e correção aplicada

- `server/src/test/java/com/projetok/multiplayer/GuiInputTrace.java`: extensão
  JUnit observacional, limitada a 256 eventos, com remoção do listener ao final.
- `SkillInputRefinementGuiTest.java` e `Task224CastingGuiTest.java`: somente
  anotação de ativação dessa extensão. Fluxos e asserções originais preservados.
- Os quatro documentos ORG-02-R1 e adendo em `ORGANIZACAO-JOGO-2026-11-09.md`.

**Não foi aplicado patch corretivo em produção ou nos helpers de execução.**
Não há causa demonstrada suficiente para alterar ativação de janelas, input ou
disponibilidade. A extensão melhora a evidência de falhas futuras; não representa
uma correção da intermitência. Não adicionamos sleeps, chamadas diretas de ação,
fallback de destinatário, habilitação forçada nem redução de cobertura.

## Evidência anterior e posterior

Anterior: ORG-02, 1064 casos com três falhas de aquisição de foco, seguidos de
quatro casos aprovados juntos. Atual: matriz isolada, conjunta, invertida,
após HUD/hotkeys, repetições e lifecycle geral. A tabela completa e os comandos
estão em [ORG-02-R1-TESTES.md](ORG-02-R1-TESTES.md); a auditoria detalhada em
[ORG-02-R1-ANALISE-FALHAS-GUI.md](ORG-02-R1-ANALISE-FALHAS-GUI.md).

Todos os resultados intermediários foram preservados, inclusive rodadas 03 e 07
com falhas. As contagens de rodadas distintas se sobrepõem e não devem ser somadas.
As capturas regeneradas pelos testes são evidência automatizada, sem aceite visual humano.

## Compatibilidade e hashes

A comparação SHA-256 dos 1605 arquivos anteriores inventariados encontrou somente
as duas classes de teste anotadas como alteradas; nenhum arquivo ausente.
Fontes de produção, POM, resources, assets, SQL e ferramentas inventariadas
continuam iguais à baseline inicial. O helper novo não fazia parte da baseline.
`temp/org02-r1/before-sha256.json`, `after-sha256.json` e `hash-comparison.json`
permitem verificar essa comparação.
Retirar apenas a anotação nova de cada classe reproduz exatamente o SHA-256
anterior, confirmando que nem asserções nem fluxo nem quebras de linha mudaram.
As 106 capturas divergentes encontradas foram arquivadas separadamente e
restauradas ao backup documentado; nenhuma captura foi promovida a asset.

As 40 skills, dano, energia, cooldowns, protocolo, mobs/IA/colisões, progressão,
inventário, missões e saves não receberam alterações. Offline/Coop, World Studio,
launcher, conteúdo publicado e TASK-225/A-04 permanecem fora da modificação.
Não houve conexão PostgreSQL, SQL executado, instalação, commit, push ou publicação.

## Testes de teclado e limites

As regressões exercitam TAB no viewport, TAB/SHIFT+TAB na HUD, SPACE,
ENTER focado, F5–F9, campos editáveis, repetição de tecla sem duplicação,
preparação/confirmação/cancelamento, cooldown/energia, desconexão e reconexão,
comando de nove slots e resoluções 1366×768, 1000×700 e 640×480.
São eventos AWT e sessões TCP loopback automatizados; teclado físico,
Radmin entre máquinas, launcher nativo e avaliação estética humana seguem pendentes.

## Riscos remanescentes e menor próxima investigação

A aquisição e manutenção de foco dependem do desktop nativo compartilhado.
Não há evidência para escolher entre interferência externa, ativação recusada
ou corrida específica do fixture nas falhas históricas. A instrumentação pode
afetar timing; uma execução verde não demonstra cura.

Menor próximo passo: repetir exclusivamente os quatro casos em desktop reservado,
correlacionando o trace AWT com a janela/processo em primeiro plano do Windows
no instante da perda. Se a falha de aquisição original reaparecer, registrar
também retorno de `requestFocusInWindow`, visibilidade e habilitação do alvo
durante a espera. Só então decidir um patch de fixture/helper ou produção.
Nenhuma próxima etapa de organização foi iniciada.

## Rollback

Remover somente as duas anotações novas e `GuiInputTrace.java`; remover os
documentos/adendo desta etapa se desejado. Não restaurar arquivos inteiros pelo
Git, pois o checkout tem alterações locais anteriores. Preservar logs/checkpoints
e comparações. A instrumentação não muda recursos, saves ou estado do jogo.
