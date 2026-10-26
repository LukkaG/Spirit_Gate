# Collision Lab

SessionCollisionProfile é conteúdo declarativo V1. ID, versão, contexto sintético, referência à sprite base original em escala .5 e forma são explícitos. Perfis não fazem parte do personagem nem do formato de save.

| Parâmetro | Suporte e limite |
|---|---|
| Raio de atores | 8..18, finito; mesmo raio nas relações jogador/jogador e jogador/inimigo |
| Meia largura contra cenário | entre o raio e 28 |
| Forma contra atores | círculo |
| Forma contra cenário | cápsula horizontal, meia altura igual ao raio |
| Inimigos | raio 19 preservado |
| Offset | somente (0,0), não configurável nesta resolução |
| Categorias | sólidas habilitadas; não há opção de desligar segurança |
| Slide | varredura X/Y real; coeficiente alternativo não é exposto |
| Tolerância física | tangência estrita existente; não há folga configurável |
| Combate | alcances e regras reais; hurtbox independente não é exposta por este runtime |

O padrão confirma a TASK-178: raio 12 e meia largura 22, com dilatação horizontal de retângulos em 10. Offline sem opt-in conserva raio 18. Não foram criados três raios arbitrários sem suporte do solver. Configurações desconhecidas `collision.*`, incluindo offsets não suportados, são rejeitadas, não ignoradas.

Metadados `.sgmap`: collision.id, collision.version, collision.radius e collision.scenery. O hash canônico da revisão inclui o documento e o perfil. Cabelos, roupas e frames não participam desse hash físico como atributos do corpo. Mudanças são candidatas: se o novo corpo ocupar parede, limite, outro jogador ou inimigo, toda a revisão é recusada sem mover o personagem silenciosamente.

O debug apresenta bounds alpha da pose, pivô ciano, círculo verde, cápsula laranja e corpo/distância do parceiro em amarelo. As posições desenhadas são snapshots autoritativos recebidos pela ponte local; não há uma física visual alternativa ou predição. O ponto de contato é avaliado pelo mesmo CollisionService; a interface mostra os contornos e a distância entre personagens, sem inventar uma manifold de contato que o solver atual não retorna.

Cenários: lado a lado, parede alta, corredor, diagonal, spawn, Shunpo, obstáculo cinza pela direita e roupas/cabelos. Nos cenários de parede/cinza, a base 120×120 e projeção 48 seguem a profundidade da TASK-178. Obstáculos comuns sem altura autorada usam a visualização geométrica normal; camadas ABOVE/ROOF continuam no renderizador Studio.

Movimento, inimigos compartilhados, Shunpo, NPCs e portais são do motor real. Mudanças de conteúdo preservam cooldown e estado do jogador. Reset de sessão/perfil de teste é um comando separado e explícito. Não há ganho de progresso real ou gravação de personagem.
