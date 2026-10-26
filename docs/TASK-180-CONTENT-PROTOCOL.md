# Contrato de conteúdo V1 / aplicação 4

Envelope V1 e MAX_FRAME=4096 preservados. Aplicação 4 é negociada por Hello(4), somente em servidor com supportsContent(). Não existe downgrade silencioso. Os opcodes 1–12 e seus bytes permanecem históricos.

| Opcode | Conteúdo |
|---|---|
| 13 | Content: kind, transaction long, hash SHA-256, index int, data até 2800 bytes |
| 14 | RevisionState: hash e CombatState histórico aninhado |
| 15 | RevisionCommand: hash e Intent/WorldRequest/AppearanceRequest histórico aninhado |

Kinds: BEGIN, CHUNK, END, READY, ACTIVATE, ACTIVE, ABORT, RESUME e REJECT. BEGIN declara o tamanho total e o hash-base ativo do cliente; ingresso inicial usa base vazia. CHUNK exige offset contíguo. END só vale com exatamente o total anunciado. O descritor completo tem limite de 4 MiB; frames menores não ampliam esse limite. A profundidade de wrappers é limitada e wrappers arbitrários/recursivos são rejeitados.

O descritor contém versão int=1, número long, hash UTF, mapa-raiz UTF, quantidade int e documentos `.sgmap` canônicos precedidos pelo tamanho. O mapa identifica dimensões, camadas, tiles/crops, objetos, geometria, spawn, portais, NPCs/inimigos e perfil em metadata. As referências de assets são IDs/hash, nunca caminhos de download/escrita. Os IDs resolvem exclusivamente no catálogo local verificado de cada pacote.

O hash lógico mantém a canonicalização da TASK-179: mapas ordenados por ID, ID UTF-8, separador zero e propriedades canônicas sem comentários/timestamps. Número, sessão, fase e data de publicação ficam fora do hash lógico. O início de publicação é forest e a região é única no conjunto, evitando ambiguidade do root. O hash-base pertence ao envelope da transação, não ao conteúdo lógico.

Perfil: collision.id, collision.version=1, collision.radius=8..18, collision.scenery entre raio e 28. Padrão 12/22; offset (0,0); inimigos 19. Todo o conjunto é validado por StudioExport/Validator e pelo motor. Não há publicação separada de física e aparência de mapa.

READY só confirma a candidata oferecida ao próprio participante. ACTIVE só confirma a fase de ativação/recuperação correspondente. ACK com transação/hash/fase incorretos é ignorado; duplicatas não avançam a máquina. Um Content enviado pelo jogador não pode iniciar publicação: o socket aceita apenas ACKs de transações já oferecidas pelo servidor.

Snapshots só entram na apresentação quando o hash está ativo e a barreira está liberada. A referência ao snapshot guarda seu hash para impedir que uma leitura atrasada atravesse uma troca de renderer. Inputs V4 também carregam hash; comandos de outra revisão são descartados no owner. Peers V4 não podem contornar isso enviando Intent histórico sem wrapper.

Aplicações 1/2/3 não recebem opcodes novos. Mantêm seus snapshots/ações compatíveis no conteúdo inicial. Os aliases históricos de portal são resolvidos no baseline do launcher. Clientes antigos conectados bloqueiam Save; depois de conteúdo autorado, novos clientes antigos são recusados com mensagem de revisão incompatível. Não são expulsos para permitir uma publicação.

SHA-256 verifica identidade/integridade, não autentica uma pessoa ou protege sozinho contra atacante na rede. O cliente aceita conteúdo somente pelo socket do servidor que o usuário selecionou. O transporte experimental continua TCP sem autenticação de contas/TLS; não se afirma proteção contra servidor malicioso ou MITM. Administração não usa esse socket.
