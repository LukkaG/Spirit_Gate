# TASK-179 — relatório de entrega

Entregue World Studio Live com preview isolado, dois personagens controláveis, edição incremental, perfil físico validado durante a sessão, sprites/cosméticos e minimapa. O motor é o RpgWorld/SharedRpgEncounter/MovementService/CollisionService real. Há pausa, reset, spawn e oito cenários de laboratório. Alterações inseguras são rejeitadas sem teleporte silencioso.

A aplicação transacional foi implementada e exercitada em um destino sintético local separado: antes de Salvar o destino permanece intacto; salvar rascunho também não o altera; Salvar confirma a revisão no tick; falhas e rollback preservam estado e ponteiro anterior. Há histórico, backup recuperável, hashes e detecção de corrupção.

**A TASK-179 não é declarada integralmente concluída.** O Salvar em Game Server RPG/Radmin permanece bloqueado: os clientes V3 atuais não negociam mapas/perfis/assets nem confirmam revisão visual. A alternativa de entrega isolada e proposta de contrato foi usada conforme as seções 11 e 19 do pedido. Não há confirmação falsa de ativação em produção. A autorização necessária e o desenho proposto estão em TASK-179-PUBLISHING.md.

Baseline da TASK-178 confirmado pelos hashes e pelo registro anterior de 514 testes GUI. A suíte geral foi ampliada com 18 testes Live; contagens e logs finais estão em validation-summary.json. As capturas mostram o editor e a sessão jogável durante mudanças não publicadas e após a transação/rollback sintéticos. Os resultados de desempenho medidos estão em final-probe/benchmark.json; não se alega ganho comparativo de latência/performance.

Limites explícitos: runtime 960×640 e até três regiões; offsets não zero, desligamento de colisões e coeficiente alternativo de slide não são suportados. Gatilhos do editor permanecem no playtest exportado, não são executados pelo Live. O parceiro usa a mesma física e não altera colisão com cosméticos. O protocolo e saves permanecem inalterados.

Não houve push, release, endpoint administrativo VPN, alteração de firewall/Radmin, gravação de save pessoal ou TASK-180. O diretório de publicação usado pelos testes pertence ao projeto sintético do checkpoint. A partida principal e os projetos anteriores foram preservados.

## Resultados finais

532 testes GUI aprovados, zero falhas e zero abortos. Execu??o headless: 523 aprovados e nove testes de interface corretamente abortados por aus?ncia de display. Foram conferidos 23 hashes protegidos de protocolo, save e sprites, al?m das diferen?as contra o baseline de entrada.

| Medi??o sint?tica (20 atualiza??es) | Mediana | p95 |
|---|---:|---:|
| Edit no Studio at? revis?o recebida | 322,500 ms | 367,835 ms |
| Valida??o/preparo | 4,830 ms | 6,650 ms |
| Tick observado | 0,585 ms | 6,721 ms |
| Paint | 2,613 ms | 6,986 ms |

In?cio frio: 9,399 s. Heap observado: 4 MiB antes e aproximadamente 118 MiB ao final; GC n?o controlado, portanto sem conclus?o de consumo permanente. N?o houve compara??o controlada com a vers?o anterior. A transa??o de homologa??o produziu tr?s revis?es de hist?rico, com ativa??o e rollback confirmados. Ativa??o em produ??o: **n?o realizada**.

Evid?ncias: [resumo verific?vel](../.task-checkpoints/TASK-179/world-studio-live/validation-summary.json), [edi??o ao vivo](../.task-checkpoints/TASK-179/world-studio-live/final-probe/live-edit-unpublished.png), [ap?s Salvar e rollback](../.task-checkpoints/TASK-179/world-studio-live/final-probe/saved-and-rollback.png) e [benchmark bruto](../.task-checkpoints/TASK-179/world-studio-live/final-probe/benchmark.json).
