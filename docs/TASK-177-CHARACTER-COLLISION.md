# Geometria de personagem

A referência histórica 64×64 não corresponde ao checkout. O manifesto original contém 20 PNGs de 132..163 × 148..159 pixels, pivôs individuais e escala .5. Não foi alterado nenhum PNG. `Task177SpriteProbe` mede alpha, dimensões, pivô, contato inferior e projeção horizontal da faixa inferior de 12 pixels fonte. CSV em `.task-checkpoints/TASK-177/network-collision/sprite-measurements.csv`.

A projeção tem largura 10.5..35.5 unidades de mundo; a mediana é 23.75. O modelo estável arredonda seu diâmetro para 24: círculo de raio 12, offset (0,0), centrado no pivô do solo. O centro da faixa inferior fica 3 unidades acima do pivô; o centro horizontal varia com a pose. Preservar o pivô como centro evita deslocamentos de física durante animação e mudanças no significado da posição lógica. O círculo é uma aproximação estável da região de apoio, não um contorno exato dos pés em cada pose.

Descritor único: `CharacterFootprint.RPG_NETWORK`, id `original-ground-v1`, shape `circle`, escala de referência .5. Não depende de raça/cabelo/roupa ou frame. Contato entre jogadores passa de 36 para 24 unidades. Contato jogador/inimigo passa de 37 para 31, sem reduzir o inimigo de raio 19. Alcances ofensivos, NPCs e portais permanecem com as regras existentes.

O mundo sintético seleciona esse descritor antes de simular. Offline e carregamento de saves mantêm o raio 18. Faixas de borda exclusivas da física da sessão preservam centros em x=18..942/y=18..622, exigidos pelo contrato histórico. Nenhuma validação do protocolo foi relaxada.

Varredura contínua impede saltar obstáculos finos e corpos. A resolução X/Y permite contornar lateralmente um jogador ao contato. Spawn busca um lugar livre; respawn/portal recusam destino ocupado; inimigos compartilhados aguardam respawn desocupado. O cliente não aplica posições de rede à física: mantém snapshots autoritativos imutáveis e apresentação separada.

F2: magenta = caixa do alpha real da pose; ciano = pivô apresentado; verde = footprint apresentado; vermelho = footprint autoritativo mais recente recebido; linha vermelha = diferença visual/lógica; laranja = obstáculo central. Os limites do mapa continuam visíveis como limites do cenário. O cliente não conhece a posição atual do servidor além do último snapshot. Não se desenha uma hurtbox fictícia: combate usa regras próprias de alcance, sem uma hurtbox independente exposta no protocolo.

Atualize anfitrião e convidado para TASK-177 juntos para reproduzir a mesma apresentação/debug. Wire V1 e capacidades 1/2/3 permanecem compatíveis; um cliente antigo não modifica a autoridade da física do servidor, mas exibe a apresentação antiga.
