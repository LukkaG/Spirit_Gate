# TASK-077 — Auditoria da arquitetura

## RPG offline

`RpgRuntime` mantém proprietário único do mundo em executor a 30 Hz; fila limitada de 64 comandos, entrada/snapshot volatile e DTOs publicados. `RpgWorld` concentra regras de movimento, colisão, combate, inventário, equipamentos, comércio, missões, portais, objetos e montagem do save. Não há Swing ou sockets nesse mundo. Os catálogos são lidos dos resources na construção; não há leitura de disco de save por tick. Há acoplamento com conteúdo e coordenadas fixas (960×640 e obstáculo central), além de mensagens de UI dentro das regras. Isso limita expansão/autoria, sem quebrar os cenários atuais.

`RpgLauncher` adapta snapshots para `DebugPanel`, coordena controles, áudio, HUD e fechamento. `RpgMenu` lista/cria personagens via SwingWorker e usa UUID; a pasta — junto ao EXE quando empacotado, ou diretório de trabalho no desenvolvimento. Preferências e controles são arquivos separados. Salvamento captura comandos anteriores na thread proprietária e escreve em executor de I/O; `saveAndPause` pausa antes da escrita e desfaz pausa em erro. O fechamento normal da janela espera save antes de dispose e retorno ao menu. Isso não garante recuperação em desligamento abrupto do processo ou falta de energia.

`CharacterSaves`: até 100 arquivos regulares `.properties`, sem seguir links na listagem; conserva entradas inválidas e oferece backup como slot separado, recuperado em novo arquivo. `LocalSaveStore`: limite 262.144 bytes, schema 1, validação sintática e restauração semântica; temporário no mesmo diretório, backup apenas após validar save atual, movimento atômico com fallback se não suportado. Sincronização — por instância, não lock entre processos: uso simultâneo do mesmo personagem em duas instâncias não está coberto. Sem fsync explícito; não prometer durabilidade contra queda elétrica.

`InventoryService` adiciona em cópia e só confirma se couber; move/remove validam antes de alterar. Equipar, comerciar e recompensar quests em `RpgWorld` usam cópias e checagens antes do commit. `QuestService` mantém objetivos e ledger por fonte/sequência; conclusões persistem. `EnemyService` serializa vida, posição, geração, respawn, IA e recargas para todas as regiões. `PortalService` valida destinos/chegadas; `RpgWorld.travel` valida proximidade, gate de quest, colisão e cooldown, preservando os dados comuns entre regiões. Estado dos inimigos — por região; somente região ativa — atualizada. Os testes de retomada, campanha e stress sustentam esses contratos atuais.

Exceções de I/O chegam a futures/diálogos; corrupção não — apagada. Exceção inesperada no tick — registrada em `failure` e encerra loop. `RpgLauncher` não consulta `runtime.failure()`: o usuário pode permanecer diante de snapshot congelado (risco por inspeção, sem falha espontânea reproduzida). Há um achado de ciclo de vida reproduzido: após pausa, `action` ainda aceita comando e `close` não conclui o future pendente. Ver P077-01. O uso dos serviços mutáveis depende da disciplina de thread proprietária, não de sincronização interna universal.

## Arena histórica

Entrada `DebugLauncher`, loop `DebugRuntime`, simulação `DebugWorld`, render `DebugPanel`; serviços do domínio continuam compartilhados. Os testes de colisão, sequência de combate, stamina, Freeze, dash, foco, bindings e viewport passaram. Renderer mantém opções/paleta/HUD para RPG; RPG não substitui a entrada da arena. Confronto com checkpoint TASK-076 não revelou diferença de conteúdo no domínio. Suite `prototype` inclui testes RPG e apresentação, portanto não deve ser chamada integralmente de “120 testes exclusivos da arena”. Não houve homologação manual da arena nesta auditoria.

## Coop experimental

`LocalSimulationServer` separa leitura/escrita TCP e thread proprietária `CoopWorld`; snapshot volatile, fila de comandos 64, capacidade 2 incluindo handshake, outgoing por peer 1. Tick 30 Hz, snapshot a cada 3 ticks (10 Hz); limite 120 mensagens/s por conexão, handshake 2s, leitura posterior 5s e sessão 30min. Bind padrão 127.0.0.1; wildcard/público recusados; LAN privada exige opção explícita. `close` fecha listener/conexões, interrompe threads e encerra executor, sem reinício do objeto.

`Protocol` V1 usa DTOs explícitos, envelope com máximo 4.096 bytes, até 8 atores, texto até 240 bytes e validações de números/IDs/ações. Não usa serialização Java. `CoopWorld.accept` recusa sequências antigas e limita 8 comandos por sessão/tick; movimento expira após 15 ticks sem entrada. Movimento, colisão, stamina, cooldowns, dano e recompensas são calculados no servidor. Recompensa do chefe — concedida uma vez a participantes com dano. Reconectar cria nova sessão/ID, não recupera conta/progresso anterior.

`LocalClient`: connect 2s, read 3s, fila 32; aceita somente snapshots mais recentes, fecha socket e acorda waiters. Cliente visual envia MOVE periódico inclusive parado. `RemotePresentation` interpola posições remotas e preserva estado autoritativo e ator local, sem extrapolação. Não há referências a `LocalSaveStore` no mundo Coop; saves offline não são importados.

TASK-071: homologação física LAN pendente. TASK-074: transições online inexistentes, além dos limites conhecidos de rede; mapa único, sessões transitórias, sem autenticação/TLS ou persistência online. Os testes entre processos locais não homologam computadores distintos.
