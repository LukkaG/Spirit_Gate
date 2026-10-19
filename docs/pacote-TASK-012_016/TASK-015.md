# TASK-015 - Cenario visual validado e dados de colisao separados

**Prioridade P2 | Evolucao visual incremental**

## Objetivo
Preparar o uso de tiles e elementos do bairro inspirado em Karakura, sem trocar as regras da arena nem assumir que o atlas existente ja esta correto.

## Implementacao autorizada
1. Reutilize o inventario de assets TASK-013. Se nao houver tiles validados, crie apenas formato de mapa + renderer de demonstracao com tiles sinteticos; sem forcar uso de PNGs invalidos.
2. Defina entidades de cenario/tiles visuais e descritores de colisao fisica **explicitos**, separados do tamanho grafico. Respeite `SOFT_OBSTACLE`, `SOLID_OBSTACLE`, `WORLD_BOUNDARY` e as politicas do dash.
3. Crie carregamento de mapa **offline/local** e um mapa minimo demonstrativo reprodutivel, com validacao de limites, objetos e ausencia de sobreposicoes iniciais invalidas.
4. Preserve `DebugWorld.demo()`, os tres leves atuais, os solidos e os replays existentes como referencia. Evite alterar o mundo autoritativo baseado em dados de imagem.
5. Se houver nova variante de mapa, forneca seletor de teste isolado sem mudar comportamento padrao ate a homologacao humana. Nao adicione banco, rede, autenticação, persistencia ou edicao visual de mapas nesta rodada.

## Aceite
- Testes de carregar mapa valido e rejeitar dados invalidos, mapas repetiveis, hitbox solida/leve independente da arte, spawn valido e render sem mutacao.
- Fallback funcional na ausencia de arte validada.
- Retorno deterministico de R, gameplay/regressoes preservados.
- Documentacao PDF, diff e captura com descricao de assets validados ou placeholders.
