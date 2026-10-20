# Corujão I — pendências e limites

- Homologação humana de arte, ordem perceptiva dos passos, pivôs, efeitos e balanceamento permanece pendente. Os arquivos foram ativados experimentalmente por autorização do responsável, sem redesenho.
- Há quatro Idle e 16 Walk. Não existem PNGs aprovados de Run, Attack, Hit ou KO nesta pasta. Run não foi inventado; ataque/dano usam sinais geométricos, e KO conserva o fallback anterior com feedback e HP zero.
- A linha de visão mantém a política histórica amostrada em até seis pixels. O aviso de chefe usa o mesmo predicado; não se declara raycast contínuo perfeito em trajetos rasantes a quinas. O pontilhado visual usa grade de seis pixels.
- As habilidades adicionais compartilham recarga com Q; não são um novo sistema de slots persistentes. São de alvo único e não mudam schema. Acesso adicional pelo botão da interface, sem migrar arquivos de atalhos.
- Métricas da TASK-089 cobrem quatro encontros fixos. Não equivalem a um estudo estatístico de toda a campanha nem aprovam artisticamente a experiência.
- Site continua público por autorização expressa do pedido atual, preservando a audiência existente. Isso excepciona a proibição de publicação do roteiro somente para o experimento web. O pacote Windows é local/experimental; não foi enviado ao canal público do atualizador nem publicado como release.
- O fluxo Sites exige commit/push no repositório de fonte do próprio Site. Foi usado apenas o checkout secundário browser-preview. Não houve commit no checkout principal nem push para GitHub nesta rodada. A branch GitHub remota pode continuar na revisão anterior; a revisão publicada está registrada em CORUJAO-I-DEPLOY.json.
- Coop, protocolos, banco, LAN física, autenticação e persistência online permanecem fora da rodada. Não se declara MMORPG completo.
- Falha fatal interrompe a simulação e informa o usuário; não há promessa de recuperar progresso posterior ao último save confirmado. Não se introduziu fsync nem coordenação de dois processos sobre um mesmo save.
