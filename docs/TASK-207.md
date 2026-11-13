# TASK-207 — inventário multiplayer V6

31/10/2026. A integração reutiliza InventoryService, equip/unequip/use e
LootService.collect. O cliente anterior não recebia inventário privado.

Proposta aplicada: capacidade opt-in 6, mantendo V1–V5 e seus bytes históricos.
Opcode 17 transporta hash de revisão, combate e ficha privada; opcode 18 contém
sequência, operação e chave limitada. RevisionCommand passa a aceitar a nova
operação, mas o servidor só a executa para peers V6. Não há ID alheio, XP,
quantidade ou caminhos enviados pelo cliente. Frames continuam limitados a 4096.
Contagens de ficha, textos, slots e enum são validados antes da alocação ampla.

O fluxo Radmin usa LocalClient.gameplay; LocalClient.character conserva V5.
Servidor antigo recusa a capacidade com orientação de atualização, sem downgrade.
Reconexão conserva a capacidade e cria outra sessão, sem inventário permanente.

I ou botão Inventário abre janela não modal: equipar, desequipar, consumir e
recolher pendentes. O servidor é a única origem do estado mostrado. Operações
reutilizam o limite de 8 comandos por tick, sequência monotônica e bloqueio
por revisão/transição. Sem itens negativos ou alteração local otimista.

Arquivos: GameplayProtocol, Protocol, SessionSimulation, LocalSimulationServer,
RpgSessionWorld, LocalClient, RpgWorld, RadminSessionPanel,
NetworkInventoryPanel, RpgNetworkClientLauncher e Task207InventoryTest.
Sem nova aparência de equipamento sem asset compatível; cosméticos continuam
no catálogo próprio. A ficha não é save e não toca persistência pessoal.
