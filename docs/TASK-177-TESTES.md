# Testes e roteiro Radmin

Automação: `python tools/run-local-tests.py`; `python tools/task177-verify.py server/target/local-tests/<run>` executa a mesma compilação com Swing real. Probes adicionais em tools/Task177*.java. Empacotamento privado: `python tools/build-task177.py`; validar o JSON produzido com `python tools/smoke-task177.py <packages.json>`.

Casos de geometria: distância/contato exato de círculos, tangência, ator parado/outro em movimento, dois atores em sentidos opostos, deslocamento lateral livre após contato, diagonal normalizada, corredor de 24 unidades, quina, parede de 1 unidade atravessada por uma tentativa longa, limites históricos em quatro direções, spawn após movimento do ocupante, estado visual atrasado sem alterar a física e respawn de inimigo bloqueado até liberar contato de 31 unidades. Regressores existentes cobrem combate PvE, respawn do jogador, portais, Shunpo, quatro raças, domínios, saves V1/V2, cosméticos, Quest Builder, World Studio, launcher e coop histórico.

Rede artificial: `Task177DelayedTcpTest` encaminha frames TCP inteiros e ordenados por proxy loopback, com atraso de 20 ms num sentido, jitter de 20/40/80 ms no outro e retenção adicional de 120 ms a cada oito frames. Dois clientes movimentam-se, ticks nunca retrocedem e a sessão deve continuar ativa. Não simula perda UDP. `Task177TimelineTest` injeta agrupamentos, intervalos irregulares, ticks atrasados e lacunas longas; verifica posições numéricas e atraso limitado. `Task177PresentationProbe` compara o mesmo trajeto regular com os dois renderizadores.

Radmin entre duas máquinas — PENDENTE, sem acesso a segundo computador:

1. Extrair os novos ZIPs por inteiro, sem substituir a release anterior. Anfitrião e convidado devem usar TASK-177.
2. Abrir SpiritGate.exe; hospedar pelo launcher existente no IPv4 Radmin local e conectar nos dois computadores. Não mudar firewall/VPN por este roteiro.
3. Para logs detalhados, definir temporariamente `JAVA_TOOL_OPTIONS=-Dspiritgate.network.debug=true` no terminal que inicia o jogo/servidor; não alterar variáveis permanentes. F2 mostra o diagnóstico visual. Capturar saída dos processos e vídeos dos dois lados.
4. Registrar RTT mediano/p95, intervalos/idade de snapshot, buffer, FPS e duração de paint. Tick/ACK do servidor são observados no log do servidor; ACK individual não existe no protocolo e fica indisponível. Não subtrair relógios de máquinas diferentes.
5. Caminhar lado a lado, cruzar trajetórias, aproximar lateralmente, mover em diagonal, contornar o outro ator e o obstáculo central. Comparar vermelho/verde e a distância visual até contato.
6. Trocar roupas e cabelos; repetir parado e andando nas quatro direções. O círculo físico deve ficar constante.
7. Atacar o mesmo inimigo, conferir HP/eventos/recompensas compartilhados; testar morte/respawn e respawn do inimigo com área ocupada.
8. Testar NPC/quest/portal com outro jogador próximo à chegada. Testar Shunpo com perfis sintéticos de nível 20 (o padrão é nível 1); conferir custo/recarga, destino livre e bloqueio por parede.
9. Repetir com as mesmas máquinas/rota/posição por pelo menos 60 s e registrar configuração, CPU, logs e capturas. Uma comparação de 15 Hz exige reiniciar o servidor com a propriedade técnica; default continua 10 Hz.
10. Encerrar pelo launcher e verificar que não restaram processos/sessões. Registrar aprovação humana ou problemas; capturas automáticas de componentes não equivalem a homologação física.

Não prometer redução do RTT físico da VPN. Uma rota ruim pode continuar com RTT elevado mesmo com menor atraso de apresentação.
