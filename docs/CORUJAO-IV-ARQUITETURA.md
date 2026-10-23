# Coruj?o IV ? arquitetura auditada

Data: 22/10/2026. Branch `beco`; HEAD inicial `52b1e5651821fe9e4960a104caf7f116c51725d6`. Checkout inicialmente limpo. Sem refatora??o do dom?nio nesta etapa.

## Identidade e progress?o

`CharacterRaces` ? o cat?logo de quatro ra?as. `CharacterIdentity.create` consulta esse cat?logo e rejeita Guardian/Mystic; seu construtor aceita normaliza??o legada preservando atributos-base. O componente `archetype` permanece por compatibilidade; `race()` oferece o nome atual.

`Domains` cont?m dez defini??es e filtra pela ra?a. As t?cnicas s?o descri??es conceituais. `DomainProgression.choose` exige n?vel 15, compatibilidade racial e aus?ncia de dom?nio escolhido. XP usa `long xpHalfUnits`; uma recompensa de N XP geral concede N meias unidades somente ap?s escolha, com `Math.addExact`. Reset exige dom?nio, quest, saldo e calend?rio; n?o existe servi?o financeiro autorizado no runtime.

`RpgProgression` carrega curva cumulativa provis?ria. O teto t?cnico de XP ? 1.000.000. `RpgWorld` orquestra combate, invent?rio, quests e dom?nio; Shunpo exige n?vel 20, vigor e recarga compartilhada. Aproxima??o ofensiva verifica alcance, visibilidade e destino livre; defensivo usa janela de esquiva. Par?metros existentes permanecem provis?rios.

## Persist?ncia

`RpgSave` faz c?pias dos mapas e flags, valida invent?rio, equipamento, regi?o e dom?nio. `LocalSaveStore` escreve V2, l? V1/V2, rejeita campos duplicados/desconhecidos, valida antes de substituir, preserva backup e tenta movimento at?mico. `CharacterSaves` lista entradas corrompidas e backups recuper?veis; arquivamento exige confirma??o expl?cita.

V1 usa `archetype`, V2 usa `race`; normaliza??o Guardian ? Reaper e Mystic ? Human preserva base. Estado de dom?nio fica nas flags existentes. N?o foi criado schema novo.

Limites relevantes: save valida posi??o x=18..942, y=18..622, regi?es e quests do cat?logo de produ??o. Conte?do de autoria novo n?o pode ser persistido arbitrariamente nesse contrato.

## Quests e inimigos

`QuestService` carrega `rpg/quests.properties`, validando refer?ncias no `RpgContent`. Objetivos atuais: kill, collect, talk e visit, at? oito objetivos, contagens 1..100. Recompensas possuem limites pr?prios. Depend?ncia de sanctuary ? first_steps est? codificada no servi?o. Eventos t?m ledger de sequ?ncia e conclus?o usa flags persistentes. `RpgWorld.completeQuest` exige NPC elder pr?ximo e verifica uma c?pia do invent?rio antes da aplica??o. N?o h? contrato injet?vel de autoria nesta baseline.

`EnemyService` reutiliza cat?logo, IA e geometria por regi?o. Tipos t?m n?vel 1; recompensas de n?veis superiores n?o est?o definidas. N?o inventar essas recompensas no editor.

## World Studio e adaptadores

`WorldStudio` ? Swing, com ferramentas separadas para pintura, camadas, portais, entidades, diagn?stico, exporta??o e playtest. A biblioteca j? possui pesquisa, categorias, SHA-256, metadados, miniaturas e indexa??o em SwingWorker. Salvamento e abertura do mapa usam workers. A interface atual usa predominantemente apar?ncia Swing clara, toolbar com quebra e pain?is divididos.

`WorldDocument`/`DocumentStore` preservam `.sgmap` V1. `StudioExport` comp?e fechamento de portais e cenas; manifesto de exporta??o V1 aceita at? tr?s regi?es. `StudioPlaytest` integra o runtime, zera entrada ao perder foco e usa diagn?sticos isolados. Mapas maiores existem no editor, mas n?o implicam persist?ncia V2 compat?vel. Adaptadores opcionais devem continuar isolando essas diferen?as.

## Launchers e rede

`compile-local.ps1` compila com --release 17, calcula fingerprint de fontes/recursos e confere hashes dos arquivos de sa?da. Compila em staging antes de substituir classes. `launch-local.ps1` suporta Rpg, Sprites, Studio e Map; sprites usam escala 0.5 e frame 250 ms por padr?o. N?o utilizar bytecode antigo como evid?ncia.

Coop permanece em `com.projetok.multiplayer`; testes locais TCP de dois processos e encontro cooperativo executaram na regress?o. Aus?ncia das classes antigas n?o significa remo??o do Coop. POM possui somente JUnit como depend?ncia, em escopo de teste. Nenhuma depend?ncia de rede antiga foi restaurada.

## Baseline e limites de evid?ncia

413 testes Java passaram em modo headless; seis foram abortados por suposi??o de ambiente gr?fico. Zero falhas e zero ignorados. Reexecu??o do pacote Studio com interface habilitada: 45 aprovados, zero abortados/ignorados/falhas. Python: 30 aprovados. As seis suposi??es s?o dependentes de ambiente, n?o testes obsoletos nem funcionalidades desativadas.

Benchmark real: ver `.task-checkpoints/TASK-134/corujao-iv/benchmark/metrics.json`. Uma amostra local, caches de filesystem n?o controlados; n?o serve para alegar ganho universal. Evid?ncias de interface antes/depois, Quest Builder e pacote Coruj?o IV ainda n?o foram produzidas.
