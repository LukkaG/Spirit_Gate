# Corujao I - relatorio consolidado

**Estado tecnico: VALIDADA COM RESSALVAS. Homologacao humana pendente.** Rodada TASK-079 a TASK-090, 20/10/2026, America/Sao_Paulo. Nenhuma TASK-091 iniciada.

## Resultado

Sprite experimental com 50% de escala uniforme, quatro frames Walk por direcao e 250 ms/frame. Nearest-neighbor, transparencia, pivos e fallback preservados. Velocidade logica continua 215 unidades/s. Estados visuais, efeitos de combate, HUD, habilidades adicionais, IA localizada e lifecycle foram integrados no desktop e na copia web. Nenhuma arte nova foi gerada e o atlas oficial de 59 frames/100 ms nao foi alterado.

Site: https://spirit-gate-sprites-lab.eageremu2049.chatgpt.site . Publicacao Sites versao 4, estado succeeded, commit `21b132e00e31ceec758ccefc9b160eef7c3abaf3`. Mesmo endereco e audiencia publica. Autorizacao atual para o site prevaleceu sobre a restricao de publicacao do roteiro somente nesse experimento. Sem push GitHub nem commit no checkout principal; o fluxo Sites gravou apenas a fonte do checkout secundario. Canal updates e releases anteriores preservados.

## Testes e etapas

Baseline real antes de implementar: 314 Java e 30 Python; duas falhas e seis erros Java causados pelos PNGs modificados, recusados pelos hashes antigos. A referencia 307/25 era historica. A integracao autorizada atualizou metadados e ciclo, preservando a validacao. Final: **335 Java e 30 Python, zero falhas, erros ou ignorados**. Vinte e um testes Java novos. Expectativas historicas de escala/duracao e ciclo de dois frames foram atualizadas para o contrato solicitado; fontes anteriores recuperaveis nos checkpoints.

| TASK | Estado | Java/Python | Producao/testes alterados | Evidencia e ressalva |
| --- | --- | --- | --- | --- |
| 079 | VALIDADA COM RESSALVAS | 315/30 | CorujaoSpritesTest.java, RpgPlayerRefinementTest.java, RpgPlayerSpritesTest.java, experimental-player.properties, RpgPlayerAnimation.java, RpgPlayerSprites.java, testar-rpg-sprites.cmd | [079 relatorio](corujao-i/TASK-079-RELATORIO-FINAL.md); checkpoints e diff em TASK-079/corujao-i |
| 080 | VALIDADA COM RESSALVAS | 317/30 | CorujaoVisualStateTest.java, RpgCombatEvent.java, RpgWorld.java, RpgPlayerSprites.java, RpgVisualState.java | [080 relatorio](TASK-080-RELATORIO-FINAL.md); checkpoints e diff em TASK-080/corujao-i |
| 081 | VALIDADA COM RESSALVAS | 319/30 | CorujaoFeedbackTest.java, DebugPanel.java, RpgWorld.java, RpgCombatFeedback.java, RpgLauncher.java | [081 relatorio](TASK-081-RELATORIO-FINAL.md); checkpoints e diff em TASK-081/corujao-i |
| 082 | VALIDADA COM RESSALVAS | 320/30 | CorujaoAttackPresentationTest.java, RpgPlayerSprites.java | [082 relatorio](TASK-082-RELATORIO-FINAL.md); checkpoints e diff em TASK-082/corujao-i |
| 083 | VALIDADA COM RESSALVAS | 322/30 | CorujaoHudTest.java, RpgCombatStatus.java, RpgHud.java | [083 relatorio](TASK-083-RELATORIO-FINAL.md); checkpoints e diff em TASK-083/corujao-i |
| 084 | VALIDADA | 324/30 | CorujaoControlsTest.java | [084 relatorio](TASK-084-RELATORIO-FINAL.md); checkpoints e diff em TASK-084/corujao-i |
| 085 | VALIDADA COM RESSALVAS | 326/30 | CorujaoAbilitiesTest.java, RpgSecondaryAbility.java, RpgWorld.java, RpgLauncher.java, RpgPlayerSprites.java | [085 relatorio](TASK-085-RELATORIO-FINAL.md); checkpoints e diff em TASK-085/corujao-i |
| 086 | VALIDADA | 328/30 | CorujaoEnemyTest.java, EnemyService.java | [086 relatorio](TASK-086-RELATORIO-FINAL.md); checkpoints e diff em TASK-086/corujao-i |
| 087 | VALIDADA COM RESSALVAS | 330/30 | CorujaoBossThreatTest.java, EnemyService.java, RpgBossThreat.java, RpgSight.java, RpgWorld.java, RpgBossOverlay.java, RpgHud.java, RpgLauncher.java | [087 relatorio](TASK-087-RELATORIO-FINAL.md); checkpoints e diff em TASK-087/corujao-i |
| 088 | VALIDADA | 333/30 | CorujaoRuntimeTest.java, RpgLauncher.java, RpgRuntime.java | [088 relatorio](TASK-088-RELATORIO-FINAL.md); checkpoints e diff em TASK-088/corujao-i |
| 089 | VALIDADA | 335/30 | CorujaoCombatRegressionTest.java | [089 relatorio](TASK-089-RELATORIO-FINAL.md); checkpoints e diff em TASK-089/corujao-i |
| 090 | VALIDADA COM RESSALVAS | 335/30 | RpgLauncher (layout), RpgBossOverlay (legibilidade), builder/auditoria, copia web e documentacao | Suite final, EXE RC2, modo geometrico, capturas reais, auditoria ZIP e deploy Sites; arte humana pendente |

Cada etapa possui before/after JSON+ZIP recuperavel e isolated.diff; nenhuma evidencia historica foi sobrescrita. Falhas intermediarias de testes novos e de preparacao foram corrigidas e os logs preservados. Comando por etapa: `python tools/qualidade.py --offline --task corujao-NNN`. Logs Java/Python e JSON em docs/TASK-corujao-NNN-*.

## Metricas dos encontros fixos

| Arquetipo | Encontro | Ticks / segundos | Dano recebido | Dano aplicado | Vigor gasto | Usos da secundaria | Sobreviveu |
| --- | --- | --- | --- | --- | --- | --- | --- |
| guardian | slime | 13 / 0.433 | 6 | 40 | 20 | 1 | Sim |
| guardian | boss | 205 / 6.833 | 42 | 320 | 40 | 2 | Sim |
| mystic | slime | 13 / 0.433 | 6 | 40 | 25 | 1 | Sim |
| mystic | boss | 157 / 5.233 | 42 | 320 | 50 | 2 | Sim |

Taxa observada de uso = usos/segundos da janela, registrada no JSON de metricas; inclui a primeira acao pronta e nao representa frequencia sustentada. Recargas da secundaria: Guardian 120 ticks (4 s), Mystic 150 (5 s), compartilhadas com Q. Dano medio recebido por encontro nesses quatro fixtures: 24. Nao houve ajuste adicional de balanceamento sem desequilibrio reproduzido. O mundo com apresentacao manteve saves iguais ao controle sem feedback a cada tick.

## Evidencias reais

- `.task-checkpoints/TASK-090/corujao-i/visual/`: 21 capturas com os 16 Walk e quatro Idle, mais inicial, GIF frontal e tempos; escala 0.5/duracao 250 registradas.
- `visual-final/`: chefe em preparacao, ataque, dano/recuperacao, menu apos save e cliente retomado. Fixtures sinteticos foram usados para chegar ao chefe; as imagens sao de janelas reais, sem montagem.
- `visual/runtime-failure-dialog.png` e failure.json: falha controlada do loop informada na UI, sem alegacao de save. `visual/ko.png` e `ko-recovered.png`: derrota e retorno reais via dominio.
- `geometry.png` e JSON: fallback real sem sprites. `portable-rc2.png` e JSON: EXE com Java integrado, exit 0 e janela real.
- QA web publico Guardian/Mystic e capturas em `web-public-guardian/` e `web-public-mystic/`; nao houve envio automatico de feedback.

## Pacote experimental local

[SpiritGate-RPG-Sprites-Teste-0.90.0-rc2-20261020T034659Z-1a18d30f.zip](../dist/SpiritGate-RPG-Sprites-Teste-0.90.0-rc2-20261020T034659Z-1a18d30f.zip)

- Windows 64 bits, Java integrado, 169 arquivos e 20 PNGs conferidos por manifesto/SHA-256.
- Tamanho: 29989701 bytes. SHA-256: `7a40cc9df69cf0cbe32b9a529e7b5836aaf20a04a15580574bdc5f162d7be70a`.
- Nenhum save pessoal, credencial, banco ou Coop no ZIP. Alternativa Abrir-Geometrico.cmd incluida.
- RC1 intermediario preservado; o candidato a compartilhar nesta rodada e RC2. Nenhum pacote foi publicado no atualizador nem como release.

## Preservacao e ressalvas

Tres arquivos pessoais de save conferidos por hash e inalterados. Os 20 PNGs recebidos, documentos historicos e formatos de save foram preservados. Protocolos/banco/Coop e regras historicas da arena nao foram alterados. Extensoes no renderer compartilhado sao opcionais e nulas por padrao.

Consultar [pendencias](CORUJAO-I-PENDENCIAS.md), [inventario](CORUJAO-I-INVENTARIO.md), [roteiro de homologacao](CORUJAO-I-HOMOLOGACAO.md) e [continuidade](CORUJAO-I-CONTINUIDADE.md). Linha de visao amostrada, arte Attack/Hit/KO/Run ausente e homologacao em outras maquinas permanecem ressalvas. Nao se declara MMORPG completo, aprovacao artistica ou balanceamento humano. A execucao encerra na TASK-090.
