# TASK-178 — Testes

Executar `python tools/run-local-tests.py`. O runner compila para Java 17 e grava classes/logs em server/target/local-tests/run-*. Para Swing real, usar o mesmo classpath compilado com -Djava.awt.headless=false. Evidência final consolidada em `.task-checkpoints/TASK-178/collision-fix/validation-summary.json`.

## Casos novos

Task178CollisionTest cobre:

1. Regressão da lateral cinza: prova numérica de sobreposição alpha de 7.5 sem penetração do círculo antigo; contato novo deixa alpha antes da face.
2. Frente: centro y=312 contra a base y=300.
3. Lateral esquerda: x=358 contra face x=380.
4. Lateral direita: x=522 contra face x=500.
5. Trás: y=168 contra início do sólido y=180.
6. Diagonal normalizada na parede: X bloqueado, Y avança apenas a componente válida.
7. Contorno de quina: deslocamentos longos por eixos preservam o espaço livre.
8. Corredor de 44 unidades admite a cápsula; 43 bloqueia.
9. Dois jogadores tangentes a 24 junto ao mesmo obstáculo continuam sem sobreposição.
10. Todas as 20 poses base (Idle/Walk, quatro direções) mantêm posição/colisor e alpha lateral fora do sólido.
11. Objeto alto: pixels e ordem verificam oclusão atrás, ator à frente e empate no apoio.
12. Quina interpolada: dois endpoints livres cujo segmento linear entra no sólido; usa autoridade sem regressão visual para a amostra inválida.
13. Movimento da sessão para na nova face e continua rejeitando velocidade inválida.
14. Obstáculo de uma unidade não pode ser saltado por deslocamento longo.
15. Roupa/cabelo e aparência base produzem os mesmos snapshots e contato de parede.
16. Servidor TCP e dois clientes junto ao obstáculo: posições concretas livres, corpos separados e sessão ativa.

Task178ShunpoTest cobre passagem ofensiva recusada em parede fina sem gasto, esquiva contínua que para no contato x=158 de uma parede x=180, e ataque Spirit junto à parede preservando alcance 180 e dano 18. A margem de navegação não se aplica à linha de visão.

Regressões TASK-177 continuam verificando velocidade/diagonal, contato 24, combate compartilhado, cosméticos, respawn ocupado, portais e Shunpo. A suíte geral cobre RPG offline, coop histórico, raças/domínios, saves V1/V2, Quest Builder, World Studio e launcher Radmin. Sem modificar testes históricos para apagar uma falha: o limite antigo de parede continua válido como limite superior, e os testes novos exigem o contato exato atual.

## Capturas e pacotes

Task178CollisionProbe é compilável contra classes originais da TASK-177 e novas. Captura esquerda/direita/frente/trás com mesma escala e pose, antes/depois. São renders controlados de componente, não fotos da captura ausente nem homologação humana. As posições estão nos arquivos before-geometry.txt e after-geometry.txt.

Após regressões, build-task178.py gera novos ZIPs privados nativos com Java integrado, sem substituir versões anteriores. smoke-task178.py valida hashes/manifestos, launchers e dois clientes após extração. integrar_launcher_radmin.py gera os dois bootstraps atualizáveis; smoke_atualizavel_radmin.py valida migração do iniciador antigo, papéis, saves sentinela, reutilização incremental, falha de integridade e dois clientes. Não publicar.

Teste humano pendente: em dois computadores Radmin, aproximar-se pelas quatro faces e quinas do bloco, testar caminhar lado a lado, trocar cabelo/roupa, usar portais e Shunpo, conferir F2 e combate compartilhado. Radmin físico e comparação com a imagem original ausente não são declarados homologados.
