# TASK-087 - Corujao I

Estado: **VALIDADA COM RESSALVAS**.

Chefe publica raio 110/135, ticks restantes e fase a partir do dominio. Overlay pontilhado em grade 6 px consulta a mesma linha de visao do golpe e usa cache de geometria; HUD exibe HP, preparacao, recuperacao e fase enfurecida. Sem mudanca nos padroes/tempos de ataque. Retomada durante preparacao testada. Ressalva herdada: visibilidade amostrada a cada ate 6 px pode tolerar trajetos rasantes a quinas; primeiro fixture presumiu raycast exato e foi corrigido para testar parede efetivamente bloqueante, log preservado. Sem promessa de raycast continuo.

Checkpoints recuperaveis, hashes e diff isolado: `.task-checkpoints/TASK-087/corujao-i/`. Logs da suite: `docs/TASK-corujao-087-*`. A aprovacao artistica permanece humana; os testes nao equivalem a homologacao perceptiva.
