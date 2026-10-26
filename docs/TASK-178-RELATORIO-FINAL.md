# TASK-178 — Relatório técnico

Corrigidos dois problemas distintos: folga lateral insuficiente da apresentação junto ao bloco cinza e ausência de profundidade de estruturas altas. O caso reproduzido tinha 7.5 unidades de sobreposição visual sem penetração física. O anexo não continha a imagem original; foi usado o obstáculo central real da sessão, com posições/pivôs registrados.

O jogador mantém raio12 contra jogadores e inimigos. Contra cenário usa cápsula horizontal44×24, com contrato compartilhado, varredura contínua e contatos laterais x=358/522. Geometria bruta do bloco, alcance/dano, velocidade, progressão e raios de interação não mudam. A linha de visão usa a geometria bruta original, não a margem de movimento. Shunpo da sessão valida o segmento inteiro.

O bloco conserva base sólida120×120 e recebe volume visual explicitamente projetado48 unidades acima. Atores e estrutura são ordenados pelo apoio. Sprite/cosméticos preservam pivôs/escala, sem mudar física conforme a animação. Interpolação que corta uma quina usa a última posição autoritativa recebida válida e não volta para o trecho visual inválido enquanto o buffer não alcançar o tick.

Verificação final: consultar validation-summary.json no checkpoint `.task-checkpoints/TASK-178/collision-fix/`. Contém contagens finais de testes, hashes, diffs por arquivo e caminhos dos smokes dos novos ZIPs nativos e atualizáveis. Capturas before/after mostram as quatro faces; geometria e pixels de oclusão possuem testes automatizados. Os modos offline/coop e ferramentas continuam usando o render histórico sem opt-in.

Regressão completa com Swing real: **514/514 testes aprovados**, sem falhas ou abortados. Headless: 505 aprovados e os nove casos que dependem de GUI abortados por premissa. No cenário TCP real com cosméticos e dois clientes, a autoridade parou em (358,242.66672), sem falha do servidor. Os limites alpha laterais medidos passaram de avanço7.5 para folga2.5 à esquerda. As capturas são controladas/automáticas e não equivalem à aprovação visual humana.

Os pacotes atualizáveis mantêm a migração do launcher Radmin e o canal comum. Distribuir Anfitrião/Convidado da TASK-178 juntos; a entrega local não ativa o feed remoto antes de publicação autorizada. ZIPs antigos foram preservados. Não houve push ou publicação, alteração de VPN/firewall, modificação de saves pessoais ou início da TASK-179.

Pendências externas: homologação Radmin entre dois computadores, revisão humana das capturas e comparação com a imagem original ausente. Não se declara homologação remota nem ganho de latência/performance nesta correção.
