# Teste de câmera e grade de pixels — 0.200.7-teste

Abra `SpiritGate-Launcher.exe` do Anfitrião em `dist/SpiritGate-Teste-Camera-0.200.7`. O amigo deve extrair o ZIP Convidado dessa mesma entrega. Feche jogo e launcher anteriores antes de abrir o teste. A versão 0.200.6 permanece como referência para comparação; esta entrega não foi publicada no canal online.

## O que comparar

O seletor **Zoom**, no topo da partida, alterna **1x, 2x e 3x**. O teste inicia em **2x**. Compare o personagem parado, caminhando nas quatro direções, perto do amigo e junto às bordas do mapa. Personagens e cenário ampliam juntos; a interface mantém seu ajuste normal. Em zoom maior aparece uma parte menor do mapa e a câmera acompanha o jogador local.

Reaper usa os PNGs originais de 64 × 64 em 1x lógico, com pivô dos pés preservado. O Human, originalmente mais detalhado e maior, é reduzido uma única vez para um canvas lógico de 64 × 64, com pivô (32, 60). A redução continua sendo uma adaptação provisória da arte existente; ela não cria um Human redesenhado no estilo da Reaper.

Cabelo e roupa temporários do Human são normalizados para a mesma grade. As imagens ficam residentes em memória; nenhum PNG de origem foi alterado. Cada pixel dessa grade vira exatamente um bloco 2 × 2 ou 3 × 3 ao trocar o zoom. O desenho do mundo usa nearest-neighbor sem antialias. A interface pode continuar usando suavização de texto.

## Câmera

O modo é opcional em `DebugPanel` e foi ativado na entrada multiplayer deste teste. Modos históricos mantêm seu ajuste de janela anterior. Origem, deslocamento e zoom do mundo usam valores inteiros; em janelas menores, a visão é recortada em vez de reduzir a arte. HUD e câmera usam transformações separadas. Mira, colisão, posição autoritativa e regras do servidor permanecem em coordenadas do mundo.

A garantia automática é em pixels lógicos Java2D. Escala fracionária do monitor, como 125%, pode afetar a apresentação final em pixels físicos. Não foi medido input-to-photon nem realizado teste Radmin entre dois computadores.

## Verificação

- Suíte de apresentação/protótipo: **182 passaram**, 1 abortado por exigir UI, nenhuma falha (`server/target/local-tests/run-sny485za/test-results.log`).
- Suíte multiplayer: **150 passaram**, 1 abortado por exigir UI, nenhuma falha (`server/target/local-tests/run-5o24v64t/test-results.log`).
- Probe de pixel: **36 poses Reaper e 20 Human**, zero divergências entre ampliações inteiras; Reaper também coincide pixel a pixel com os PNGs originais. Evidências em `.task-checkpoints/pixel-scale-probe/after`.
- Os 180 planos cosméticos e suas composições foram verificados na mesma grade. Normalização recusa canvas que cortaria pixels ocupados.
- Dois processos clientes dos ZIPs, usando o Java integrado, abriram a interface real, trocaram o seletor em 1x/2x/3x e produziram seis capturas. A troca de câmera não alterou a posição no servidor.
- Hashes dos ZIPs, JAR e payload foram conferidos. Fontes de probes não entram no JAR distribuído.

Capturas da partida e resultado: `.task-checkpoints/teste-camera-0.200.7/smoke`. Comparação lado a lado: `.task-checkpoints/pixel-scale-probe/after/pixel-scale-comparison.png`. Build em `.task-checkpoints/teste-camera-0.200.7/build`.

O teste mantém as melhorias de rede anteriores. Nenhum commit, push ou deploy foi realizado.
