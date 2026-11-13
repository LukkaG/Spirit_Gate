# Impacto sísmico V7 — mais detalhe e movimento

[Animação](martial_3.gif) · [16 estágios](contact.png) · [Captura do lançador](runtime/sismico-apice.png) · [Captura do observador](runtime/sismico-observador.png)

A revisão de 02/11/2026 mantém a simplicidade aprovada e substitui os oito braços largos por fissuras quebradas, bordas de pedra com contraste, pequenas fraturas secundárias e lascas que saltam e assentam. Um anel fragmentado de poeira aparece na abertura e se desfaz antes da fase de assentamento. Não há a grande explosão de pedras das versões antigas.

O tamanho de apresentação continua **240 × 180**, em canvas **320 × 240**. O raio de dano permanece **110**. São **16 frames**, com 56 ms por estágio, totalizando 896 ms de apresentação; a versão anterior usava 12 × 75 = 900 ms. Essa diferença de 4 ms é apenas visual. Dano, custo, recarga e hitbox não foram alterados.

O GIF repete para facilitar a inspeção; o efeito em jogo percorre o ciclo uma vez. A revisão estética final continua sendo do responsável pelo jogo.

## Arte e reprodução

Duas gerações/edições com `image_gen` nativo estão preservadas em `assets/skills/refined-v7/art/`. A primeira revisão trouxe detalhe e movimento; a segunda melhorou as bordas e o contraste no chão verde. Prompts e referência anterior ficam em `art/prompts.json`. Modelo/custo não foram retornados pela ferramenta e estão registrados como nulos.

`tools/prepare_skill_assets_v7.py` recorta as quatro faixas desenhadas, registra o apoio no chão e reduz com nearest, com uma única escala para os 16 estágios. Não amplia, repinta ou recolore por Python. O manifesto opcional V7 substitui apenas `martial_3`; os demais efeitos continuam nas versões aprovadas. O carregador recebeu somente a leitura desse overlay.

O [QA](qa.json) encontrou 16 frames não vazios, nenhum pixel na borda exportada, nenhuma ampliação e nenhum aviso. O primeiro estágio tem 4,42% da cobertura visível do ápice; o último, 1,37%, com limiar de alfa 16. A [reprodução independente](verification.json) gerou PNG e dois metadados idênticos. Os **130 PNGs anteriores** e os **415 originais** da pasta fornecida mantiveram seus hashes.

## Verificação e compatibilidade

Foi reutilizada a cobertura existente, sem acrescentar testes que apenas repetissem o empacotamento. Comando executado em `server/`:

```powershell
mvn test -o "-Dmaven.repo.local=../.maven-repository" "-Djava.awt.headless=false" "-Dtest=NetworkSkillPresentationTest,SkillSlashRefinementGuiTest"
```

Também foi usado `-Dspiritgate.logs.dir` isolado no checkpoint. Resultado: **32 testes aprovados**, sem falhas, erros ou ignorados — 28 de apresentação e quatro cenários Swing/TCP com servidor e dois clientes. O caso marcial foi inspecionado no tamanho real de jogo. As 37 capturas antigas dos diretórios V5/V6 foram preservadas e restauradas; as duas novas capturas marciais estão neste diretório.

Evidências, baseline da apresentação, diff e XMLs: `.task-checkpoints/SKILLS40/refinamento-v7/`. A suíte geral não foi repetida nesta revisão visual localizada. Não houve alteração de combate, protocolo, saves, offline/Coop, launcher ou publicação; não houve commit/push. Testes físicos/Radmin e launcher nativo não foram executados.

Para carregar a arte nova, reabra `tools/testar-skills.cmd` uma vez; a troca de domínios durante a sessão permanece disponível. Para reverter, retirar somente a leitura de `refined-v7/manifest.properties` do carregador, comparando com o baseline. O overlay V6 e todos os PNGs anteriores continuam intactos. Evitar reset global, pois o checkout contém trabalho preexistente.
