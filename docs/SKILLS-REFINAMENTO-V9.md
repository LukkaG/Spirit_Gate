# Refinamento V9 — investida frontal e especial de trovão

Implementação local de 02/11/2026, branch `beco`, HEAD `6d72c65`. O checkout já continha mudanças extensas; o checkpoint desta etapa está em `.task-checkpoints/SKILLS40/refinamento-v9/baseline/`. V9 identifica esta revisão de comportamento/apresentação, sem criar outra versão do protocolo.

## Comportamento

**Água 5, Vento 3 e Raio 4:** smartcast frontal nas quatro direções do personagem, sem alvo obrigatório. Percorrem até 240 unidades em oito ticks, encurtando o trajeto diante de cenário ou aliado. Atravessam mobs e acertam cada identidade/geração uma única vez durante o segmento efetivamente percorrido. Um root recebido no percurso interrompe a investida; não provoca acerto sem movimento. Raio conserva seu stun. O dano, custo e recarga declarados permanecem.

O rastro continua sendo construído sobre posições efetivamente desenhadas, com corte frontal anexado ao corpo e até três imagens residuais do próprio personagem. Reutiliza as folhas aprovadas, inclusive a paleta amarela/laranja. Não cria outra arquitetura de renderização nem substitui PNGs existentes.

**Raio 5:** o servidor reserva o movimento durante 32 ticks (aproximadamente 1,07 segundo), torna o personagem intangível e imune a dano, conforme a confirmação do desenvolvedor. Mobs deixam de selecioná-lo e de colidir com ele; aliados também podem atravessar sua posição. Sprite e nome são ocultados nos clientes de habilidades. Um círculo discreto delimita a área original da execução; o estado do ator continua nos snapshots para câmera e sincronização. O minimapa conserva seus marcadores existentes.

No encerramento, quatro pulsos atingem todo o círculo de raio 110, com 14 de dano-base por pulso e stun, respeitando a visibilidade contra paredes. O dano não depende das quatro linhas da animação. Os quatro cortes dourados aparecem em sequência, espaçados em 70 ms, com duração visual total de até 650 ms. Os pulsos de dano são resolvidos juntos no encerramento; a separação dos cortes é visual. O jogador reaparece nesse encerramento e volta a receber dano normalmente.

Se outro corpo ocupou a posição inicial ou o término de uma investida, a materialização procura um apoio livre, sem empurrar outros atores nem atravessar paredes. Em uma área completamente ocupada, o executor espera espaço e não repete o dano. O estado temporário é removido no descarte da sessão; não entra em saves.

## Arquivos

- `RpgSkillCatalog.java`: investidas `LINE` e descrição do especial.
- `RpgSkillEngine.java`: contato por segmento/geração e fase temporária do especial. O nome interno histórico `CIRCULAR_DASH` permanece, mas agora executa `LightningField`.
- `RpgWorld.java`: imunidade temporária, movimento de investida e materialização segura.
- `SharedRpgEncounter.java`: seleção de alvos e corpos atuais da região.
- `RpgSessionWorld.java`: exclusão do corpo intangível em movimento, admissão, retorno e portal.
- `NetworkSkillPresentation.java`, `RpgNetworkClientLauncher.java`, `RemoteTimeline.java`: ocultação confirmada, círculo, cortes, imagens residuais e interpolação sem bloqueio artificial do intangível.
- Testes em `RpgSkillEngineTest`, `NetworkSkillPresentationTest`, `Task177TimelineTest`, `LightningFieldServerTest` e `GrabDashRefinementGuiTest`.

## Compatibilidade e limites

Mantidas mensagens, serialização, negociação e persistência. As fases usam eventos já existentes da aplicação de habilidades 7: início com coordenadas distintas, término pontual. Cliente e servidor devem executar esta revisão juntos para apresentar o especial corretamente. Os modos antigos não recebem novas mensagens.

Não houve alteração de banco, saves, launcher, publicação ou arte aprovada. O contexto sintético de habilidades restringe os novos estados ao multiplayer correspondente. A suite de regressão verifica os demais modos; isso não substitui homologação física.

O cliente tem fusível visual de cinco segundos para uma fase sem término recebido. Em um cenário excepcional completamente ocupado, o servidor pode aguardar mais tempo para materializar; essa extensão e perdas definitivas de eventos permanecem um limite da representação sem novo campo de protocolo.

O vídeo de referência do YouTube não pôde ser reproduzido: as tentativas de acesso foram bloqueadas e o navegador disponível não tinha Chrome instalado. A implementação segue a descrição textual fornecida; não foi possível comparar quadros ou timing com o vídeo.

## Evidências e reversão

Validação em JDK 17, Maven offline com o cache `.maven-repository`:

| Rodada | Casos | Falhas / erros | Ignorados |
| --- | ---: | ---: | ---: |
| Suíte completa em headless | 919 | 0 / 0 | 51 |
| Complemento gráfico, 24 classes | 99 | 0 / 0 | 0 |
| Regressão final após correção do marcador na borda | 165 | 0 / 0 | 0 |

A correção final garante um início distinto do término mesmo quando o personagem olha para fora na borda do mapa. A última rodada inclui executor, catálogo, protocolo, apresentação, interpolação, travessia TCP e cinco cenários gráficos. O agregado do XML mais recente por classe reúne **920 casos aprovados em 247 classes**, sem falhas, erros ou skips remanescentes; não corresponde a uma única execução. Logs: `full-headless.log`, `gui.log`, `final-focused.log`; agregado em `verification-tests.json` no checkpoint V9. As primeiras rodadas registraram expectativas antigas de circuito e um ajuste no fixture para excluir intangíveis, corrigidos antes dessas validações.

Comandos: `mvn test -o` com `-Dmaven.repo.local=<raiz>/.maven-repository`, `-Dspiritgate.logs.dir=<checkpoint>/test-errors` e `-Djava.awt.headless=true` na suíte geral; `false` nas rodadas gráficas, com `-Dtest` das classes selecionadas. A ferramenta `SkillsLocalPreview.java` também compilou com `javac --release 17`. `git diff --check` terminou sem erros de whitespace; houve avisos preexistentes de normalização LF/CRLF.

Integridade: **152 PNGs aprovados e 415 fontes originais preservados por SHA-256** (`integrity.json`). As 76 capturas anteriores foram restauradas após arquivar as novas evidências da suíte; os três PNGs sintéticos regenerados pelos testes ficaram no checkpoint, restabelecendo as 2.432 exclusões preexistentes. As imagens novas V9 permanecem na pasta própria.

Capturas reais de dois clientes e instruções em [skills-preview-v9/README.md](skills-preview-v9/README.md). Resultados de testes e verificações de integridade em `.task-checkpoints/SKILLS40/refinamento-v9/`.

Para reverter, comparar e restaurar somente os arquivos desta etapa a partir do checkpoint, preservando trabalho posterior. Não usar reset/clean; os arquivos novos de relatório/teste podem ser retirados individualmente após conferir os diffs. Sem commit, push, release ou distribuição automática. A aprovação estética humana, o teste físico e Radmin permanecem pendentes.
