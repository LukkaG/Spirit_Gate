# Teste de zoom 0,75x e 1,25x — 0.200.8-teste

Abra o `SpiritGate-Launcher.exe` do Anfitrião em `dist/SpiritGate-Teste-Zoom-0.200.8`. Feche jogo e launcher anteriores. O amigo deve extrair o ZIP Convidado dessa mesma entrega. Não houve publicação online.

O seletor **Zoom**, no topo da partida, oferece **0,75x, 1x, 1,25x, 2x e 3x**. Inicia em **1,25x**. Compare tamanho, legibilidade e caminhada nos dois valores pedidos. O zoom amplia ou reduz cenário e personagens juntos; a interface mantém seu tamanho normal.

Em 0,75x, quando o mapa inteiro cabe na área disponível, ele fica centralizado com margens. A câmera não assume origem negativa e o mouse nas margens não vira uma coordenada válida do mundo. Em 1,25x, a câmera acompanha o jogador dentro das bordas. A conversão de mira continua inversa à transformação de desenho.

A Reaper original e a grade lógica do Human/cosméticos foram mantidas. Nearest-neighbor continua ativo. Escalas fracionárias não garantem pixels de largura uniforme: 0,75x pode descartar pixels e 1,25x pode repetir alguns deles. Os PNGs, a colisão, a velocidade e a posição do servidor não mudam.

## Verificação

- Suíte de apresentação: **184 testes passaram**, 1 abortado por exigir UI, nenhuma falha. Log em `server/target/local-tests/run-_39ocfd8/test-results.log`.
- Os testes de câmera incluem os dois valores, quatro tamanhos de janela, dois tamanhos de mapa, bordas, mira, HUD, paleta e entradas inválidas.
- Probe do JAR distribuído: **56 poses**, nos valores 0,75x/1x/1,25x, sem cores interpoladas e com limites visíveis dentro da tolerância de um pixel.
- Dois clientes reais dos pacotes alternaram os cinco valores pelo seletor e produziram dez capturas. A posição autoritativa ficou inalterada.
- ZIPs e payloads conferidos por SHA-256; fontes de probes não entram no pacote do jogo.

Evidências em `.task-checkpoints/teste-zoom-0.200.8`. A comparação de sprites está em `comparison/fractional-scale-comparison.png`; capturas da partida em `smoke/a-zoom-0.75x.png` e `smoke/a-zoom-1.25x.png`. A verificação é local, sem teste físico entre computadores pela Radmin.
