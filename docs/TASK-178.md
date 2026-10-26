# TASK-178 — Colisão visual e geometria

Patch da TASK-177 restrito ao RPG multiplayer sintético: contato com cenário, apresentação junto a quinas e profundidade do bloco central. Sem push, publicação, TASK-179, alteração de saves pessoais ou configuração de VPN/firewall.

O anexo da tarefa contém somente texto, sem a captura original. A regressão foi reproduzida no bloco cinza existente: sólido (380,180,120,120). Em (368,240), círculo 12 tangencia a face, mas a pose Walk LEFT 2 alcança x=387.5: 7.5 unidades de sobreposição visual sem penetração física. A amostra usa apresentação igual à autoridade; portanto a rede não é necessária para reproduzir. DebugPanel desenhava todos os obstáculos antes de todos os atores.

Contratos separados:

| Relação | Regra |
|---|---|
| Jogador × jogador | círculos 12+12, contato 24 |
| Jogador × inimigo | 12+19, contato 31 |
| Jogador × cenário | cápsula horizontal 44×24, meias dimensões 22×12 |
| Combate/visão | alcances/dano originais, sólidos brutos sem margem física |
| Sprite | alpha e pivôs originais por pose, escala .5 |
| Bloco central | base sólida 120×120 inalterada; projeção visual adicional de 48; apoio y=300 |

RpgCollisionProfile centraliza a cápsula, representada exatamente por raio 12 contra retângulos dilatados em 10 no eixo X. Isso reutiliza a varredura contínua, cantos arredondados e deslizamento existentes. Meia largura 22 = extensão horizontal máxima base 19.5 + margem de contorno 2.5; não usa o alpha inteiro como colisor nem muda conforme a roupa. Limites históricos de coordenadas continuam preservados.

RpgSessionGeometry compartilha sólido bruto e metadados visuais com cliente e servidor. StructureRenderer intercala atores e estruturas pelo apoio; em empate, ator à frente. Sombras ficam no chão. A altura visual de 48 é um contrato explícito novo para a primitiva cinza de desenvolvimento, não uma medida extraída de PNG.

RemoteTimeline rejeita posições de apresentação que caiam no cenário preparado e usa o último estado autoritativo recebido para aquele ator. Mantém essa correção até a timeline alcançar o tick, sem oscilar de volta para o trecho inválido. Não altera snapshots ou física e não prevê posições novas. Troca de região, teleporte e lacunas preservam seus resets.

Render antigo permanece padrão para offline, coop e ferramentas. A profundidade nova é opt-in no cliente RPG multiplayer. Saves V1/V2 e Protocol.java não mudam; sprites originais são preservadas. Launcher Radmin e atualização automática continuam no fluxo existente. Pacotes atualizáveis locais são gerados sem ativar o feed remoto até publicação autorizada.

Diagnóstico e capturas: `.task-checkpoints/TASK-178/collision-fix/`. Métodos e limitações em TASK-178-COLLISION-DEBUG.md e TASK-178-TESTES.md.
