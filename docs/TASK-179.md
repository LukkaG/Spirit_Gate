# TASK-179 — World Studio Live

Implementados preview isolado, sincronização incremental, gameplay real, laboratório de colisões e revisões transacionais em um destino de homologação sintético local. **A aplicação no servidor RPG/Radmin principal permanece bloqueada.** A tarefa não é declarada integralmente concluída: falta o contrato de distribuição/ativação de revisões para os clientes de produção, conforme a regra de parada das seções 11 e 19 do pedido.

Auditoria: WorldStudio, FileTools, EditorHistory, StudioExport, StudioPlaytest, DocumentStore, StudioMapRenderer, StudioGeometry, RpgSceneSet, RpgWorld, RpgSessionWorld, SessionSimulation, SharedRpgEncounter, MovementService, CollisionService, CharacterFootprint, LocalSimulationServer, LocalSaveStore e Protocol foram inspecionados. A TASK-178 foi verificada pelo checkpoint anterior e pelos hashes dos arquivos na entrada desta tarefa; registrava 514 testes GUI aprovados. Esse baseline e a branch/HEAD estão em `.task-checkpoints/TASK-179/world-studio-live/`.

Separação implementada:

| Contexto | Estado e escrita |
|---|---|
| Studio | documentos imutáveis, rascunho e histórico de edição |
| Preview | LocalSimulationServer próprio, 127.0.0.1, porta efêmera, owner próprio e diretório temporário exclusivo |
| Homologação sintética | outra instância/porta/estado; só o botão Salvar autoriza uma transação |
| RPG/Radmin principal | nenhum vínculo administrativo; não recebe os edits e não é alterado |

O gameplay usa RpgWorld, SharedRpgEncounter, MovementService e CollisionService reais. A mudança de conteúdo prepara instâncias candidatas e só as instala na fronteira de tick se todas as posições continuarem válidas. Estado do personagem, inventário, nível, cooldowns e posição são preservados nas atualizações de mapa/perfil. Inimigos mantêm estado quando a definição não mudou; definições de spawn alteradas criam a nova população sintética correspondente. Não há saves pessoais ou economia persistente.

Mapas `.sgmap` continuam V1; perfis usam metadados já suportados e o manifesto da revisão. Saves V1/V2 e protocolo público permanecem inalterados. Offline, coop histórico, launcher Radmin, Quest Builder e o playtest exportado continuam disponíveis. O playtest exportado só usa o perfil novo quando houver metadados físicos explícitos; o comportamento histórico sem esses metadados permanece.

Não houve push, release, alteração de VPN/firewall, acesso administrativo remoto ou início da TASK-180. Nenhum projeto pessoal foi sobrescrito; as provas de publicação e rollback usam um projeto sintético dentro do checkpoint.

Guia: TASK-179-LIVE-EDITOR.md. Limites físicos: TASK-179-COLLISION-LAB.md. Arquitetura: TASK-179-PREVIEW-SERVER.md. Publicação e dependência pendente: TASK-179-PUBLISHING.md.
