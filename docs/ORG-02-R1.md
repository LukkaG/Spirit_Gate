# ORG-02-R1 — Estabilização dos testes gráficos e regressão geral

## Contexto e motivação

A ORG-02 registrou 1064 casos e três falhas gráficas. As duas classes envolvidas
passaram juntas posteriormente; isso não comprovou a causa das falhas gerais.

## Estado atual verificado

09/11/2026, branch `beco`, HEAD `13b91dc`. Checkout com muitas alterações locais
anteriores, preservadas. Java Temurin 17.0.20.1 e Maven 3.10.0; módulo único
`server/pom.xml`, JUnit 5.11.4, Surefire 3.5.2. Monitor histórico sem pendentes
no último status; TXT de World Studio/IDE lidos como diagnóstico, fora desta correção.

Os XMLs originais apontam para `GamePanelsIntegrationTest.focus`, linha 102:
comparação de identidade do foco AWT. Os nomes `hudDomainSkill1/3` são mensagens
da asserção; não são variáveis booleanas de disponibilidade de habilidades.

## Objetivo

Reproduzir e distinguir falhas de habilitação, aquisição de foco e entrega de
teclado; corrigir somente uma causa demonstrada, preservando os contratos atuais.

## Escopo incluído

Auditoria dos seis componentes solicitados, launcher que os integra, testes e
helpers; matriz de ordens, duas regressões gerais se viáveis, mob e recursos;
evidências de AWT/EDT e comparação SHA-256.

## Fora do escopo

Regras, habilidades, protocolo 6/7, SQL, persistência, mobs, IA, colisões,
World Studio, distribuição, instalação e publicação; próximas etapas de organização.

## Arquivos/módulos envolvidos (confirmados)

Produção auditada em `server/src/main/java/com/projetok/multiplayer/`:
`GameInputRouter`, `NetworkSkillControls`, `NetworkGameHud`, `NetworkHotbar`,
`GamePanels`, `NetworkSkillPreview`, `RpgNetworkClientLauncher`.
Testes no pacote correspondente: `SkillInputRefinementGuiTest`,
`Task224CastingGuiTest`, `GamePanelsIntegrationTest`, `SwingKeys`,
`GameInputRoutingTest`, `SkillAimInputRoutingTest`, `GuiInputTrace`.

## Pré-requisitos e dependências

Desktop Windows interativo e cache Maven existente. Nenhum download ou instalação.
Não executar dois testes gráficos concorrentes no mesmo desktop.

## Plano de implementação incremental

1. Preservar estado local, XMLs históricos e hashes anteriores.
2. Executar isolados, juntos, invertidos e após HUD/hotkeys.
3. Observar eventos sem consumir nem substituir teclado; separar falhas distintas.
4. Aplicar patch somente com causa comprovada; não fabricar correção para falha ausente.
5. Reexecutar lifecycle completo, testes dirigidos e comparação de integridade.

## Critérios de aceite

Regressão geral incluindo os novos testes da ORG-02 aprovada, sem reduzir
asserções, desabilitar casos ou alterar disponibilidade/regras para contornar falhas.
Falha persistente ou causa indeterminada deve constar como bloqueio de estabilização.

## Plano de testes

Comandos, ordem real, resultados intermediários e limites em
[ORG-02-R1-TESTES.md](ORG-02-R1-TESTES.md).

## Riscos e compatibilidade

AWT depende do desktop nativo. Testes aprovados com eventos na fila AWT não
equivalem a teclado físico/Radmin. Instrumentação pode influenciar timing;
registrar essa limitação ao interpretar ausência de reprodução.

## Plano de rollback

Remover somente as anotações `ExtendWith(GuiInputTrace.class)` das duas classes
e o helper novo, se entregues. Documentos são independentes do runtime. Preservar
todos os demais diffs; não usar restauração global. Evidências em `temp/org02-r1/`.

## Entregáveis e relatório final

[Análise GUI](ORG-02-R1-ANALISE-FALHAS-GUI.md),
[testes](ORG-02-R1-TESTES.md), [relatório final](ORG-02-R1-RELATORIO-FINAL.md)
e adendo no documento de organização. Duas regressões gerais passaram nos 1068
casos cada; 23 testes dirigidos de mob/recursos passaram. Duas rodadas dirigidas
falharam por ausência de destinatário AWT. Estabilização com bloqueio de causa
raiz; sem patch corretivo especulativo. Nenhuma próxima etapa foi iniciada.
