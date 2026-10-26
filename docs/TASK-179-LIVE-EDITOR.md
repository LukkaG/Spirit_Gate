# World Studio Live — uso

Abra `world-studio.cmd`. O iniciador existente compila os fontes locais e abre o editor; esta entrega não atualiza os ZIPs já distribuídos.

1. Abra um mapa válido com spawn PLAYER, ou acesse **Ferramentas → Laboratorio de Colisoes** e carregue um dos oito cenários sintéticos. Carregar um cenário pede confirmação quando houver alterações não aplicadas.
2. Clique **Iniciar Preview**. O servidor é criado em loopback e a janela jogável conecta dois personagens sintéticos. Nenhum IP Radmin é consultado ou selecionado automaticamente.
3. Edite tiles, camadas, hitboxes, objetos, NPCs, portais ou spawn. Gestos são agrupados por 200 ms, com coalescência de mudanças durante uma validação. O editor continua utilizável; validação, arquivos e imagens são tratados em workers.
4. A janela mostra a revisão efetivamente instalada. Um rascunho rejeitado mantém o preview na revisão anterior e apresenta o motivo. Use **Servidor → Sincronizar rascunho completo** para recuperação explícita.
5. WASD controla o jogador 1; setas controlam o jogador 2. SPACE ataca; SHIFT/Q usam os Shunpos reais quando o nível permite; F interage; P usa portal próximo; R solicita respawn. A perda de foco solta as teclas.
6. Use Pausar/Retomar, Reiniciar Preview, Restaurar spawn e reinício de raça/nível sintéticos. Raças: reaper, human, aetheri e spirit. Níveis suportados: 1 e 20. Reiniciar perfis é explicitamente destrutivo apenas para a sessão sintética.
7. F2 alterna o diagnóstico: sprite, pivô, círculo, cápsula e distância entre personagens. Roupas/cabelos são apresentação e não alteram física. O minimapa usa o documento da região atual.

**Salvar rascunho** grava somente o `.sgmap` escolhido, com as proteções e backup do DocumentStore. Não ativa conteúdo no jogo. Fechar/abrir outro projeto pede confirmação para alterações ainda não aplicadas, independentemente de já haver um rascunho em disco. **Descartar alteracoes** encerra o preview experimental e restaura o documento-base; não altera o destino salvo.

**Salvar - aplicar ao jogo** valida uma revisão congelada. Por padrão, recusa a aplicação no RPG/Radmin principal com mensagem clara, sem criar uma publicação. Para verificar o mecanismo isoladamente, use **Servidor → Iniciar destino sintetico de homologacao**. Ele é uma instância separada, não uma conexão com a sua partida real. Edits não chegam a esse destino; apenas Salvar executa a transação.

O laboratório mostra valores do rascunho, do preview e da base/publicação local separadamente. A janela só troca a revisão visual após carregar os recursos correspondentes; durante essa sincronização, solta os inputs e informa o estado. Não há predição local de posição.

Limites: 960×640, uma a três regiões, no máximo dois jogadores, 4 MiB de documentos por revisão e 64 MiB de imagens ativas por cache. Mapas maiores continuam editáveis, mas são recusados na execução. Gatilhos/eventos do editor não são executados neste Live; use o playtest exportado existente quando precisar deles. O conteúdo de combate/objetos continua restrito aos catálogos reais suportados.
