# Protocolo de desenvolvimento

Baseline V1: envelope binário com comprimento de quatro bytes, versão de um byte e opcode. Limite 4096 bytes por frame, até oito atores, texto UTF-8 até 240 bytes. Sem desserialização Java. Tipos atuais: Hello, Welcome, Intent, State, Bye e Error.

Hello declara versão de aplicação e nome público. Welcome atribui ID de sessão. Intent contém sequência crescente, ação e dois valores finitos. No movimento, valores são direções -1..1; nunca coordenadas finais. State contém tick e atores validados, com posição, vida, vigor, moedas, direção e flags históricas. Esses valores são produzidos pelo servidor.

O cliente limita sua fila a 32 intenções; servidor limita fila de comandos a 64, saída por peer a um snapshot, capacidade a dois clientes e entrada a 120 mensagens/s, além de limite por tick no backend. Handshake tem timeout de dois segundos; peers e clientes possuem timeouts posteriores. Sessão de desenvolvimento tem limite de duração.

Falhas previstas: versão incompatível, tamanho excessivo, frame truncado, opcode inválido, números não finitos, sequência repetida, fila cheia, socket encerrado e servidor indisponível. Uma confirmação de envio pelo cliente significa enfileiramento, não aceitação autoritativa da ação.

A expansão cosmética não deve quebrar o envelope ou mensagens de clientes Coop V1. Capacidade adicional precisa ser negociada no handshake e as mensagens adicionais devem ser enviadas somente aos peers que a anunciaram. Nenhuma imagem ou save completo deve transitar em snapshots.

## Extensão implementada — TASK-158

Envelope permanece V1; Hello com versão de aplicação 2 solicita capacidade cosmética, aceita somente por backend habilitado. Hello de aplicação 1 e seus bytes históricos permanecem válidos. O servidor envia State puro aos peers V1 e SessionState somente aos peers que negociaram aplicação 2.

AppearanceRequest (opcode 7) contém sequência, ID de cabelo e ID de roupa. Não contém ID de outro jogador, raça, domínio, atributos ou caminho de arquivo. A conexão determina a sessão afetada. IDs precisam existir no catálogo real e a sequência compartilha o ledger dos comandos. SessionState (opcode 8) contém o State V1 e até duas aparências atribuídas pelo servidor: sessão, raça, cabelo e roupa. Extensão aninhada é recusada; frame total continua limitado a 4096 bytes.

Não são transmitidos PNGs. Falta de recurso local usa fallback de camada; ela não concede raça ou domínio. Disconnect remove metadados e reconexão começa sem aparência anterior, salvo nova escolha explícita. Coexistência de peer cosmético e peer V1 foi testada em conexão TCP real.
