# TASK-067 - Fronteira autoritativa e protocolo V1

Inspecionados KServer, Network e Kryonet legados: startup tenta banco e mensagens de movimento carregam coordenadas vindas do cliente. Nenhum desses entrypoints foi executado. O experimento novo fica em com.projetok.multiplayer e nao acessa PostgreSQL nem reutiliza login/senhas legados.

Cliente envia somente intencoes MOVE/ATTACK/LIGHTNING_STEP/CRYOSTASIS com sequencia e alvo/direcao. Identidade e vinculada ao socket pelo servidor; cliente nao envia ID de outro jogador, HP, dano ou moedas. Sessao transitoria nao representa autenticacao de conta.

Frames TCP: int32 big-endian de tamanho 2..4096, byte versao=1, byte tipo (1 Hello, 2 Welcome, 3 Intent, 4 State, 5 Bye, 6 Error). Strings UTF-8 prefixadas por byte, maximo 240 bytes; nomes 3..24 caracteres. State: tick int64, contagem byte <=8, atores tipados. A ordem dos quatro codigos de acao e parte do contrato V1 e nao deve ser reordenada sem nova versao.

DTOs imutaveis validam finitude, IDs, limites e duplicatas. Codec recusa truncamento, desconhecidos, dados excedentes e versoes incompativeis. Sem ObjectInputStream. Fila de comandos e limites de sessao serao implementados no servidor; IO nao pertence a thread de simulacao.

RPG offline 0.66.0-rc2 congelado em ZIP auditado, SHA-256 2f59766f860df6e8db66f2dbc2ca219bad970cffd9e7e0df9f800a13658f8f9e. Nenhum save offline sera compartilhado automaticamente.
