# Proposta de persistência online

A integração online continua efêmera. Saves V1/V2 e personagens pessoais não são importados. `SyntheticOnlineRepository` é protótipo isolado, sem chamada nos launchers, que aceita diretório `online-synthetic-*` e gera personagens sintéticos por `create(race)`.

Documento binário online V1: magic SGO1, versão, UUID, revisão positiva, comprimento, payload validado pelo codec offline V2 e SHA-256 de cabeçalho+payload. Leitura limitada, sem desserialização Java. `RpgWorld.restore` valida inventário/equipamentos, progressão, raça, domínio, quests de catálogo, região e posição. O wrapper não altera o schema pessoal e não inclui quests autoradas.

Gravação exige grant opaco emitido pela instância do repositório para o UUID. O grant não sobrevive ao restart e não representa autenticação. CAS de revisão e lock de arquivo protegem escrita concorrente; arquivo temporário é validado e sincronizado antes de movimento atômico. Sem suporte a movimento atômico, a gravação falha conservando o arquivo anterior. Backup mantém a revisão anterior. Recuperação lê backup validado; aplicação de recuperação exige decisão explícita futura. SHA-256 detecta corrupção, não autentica dados contra um invasor.

Evolução proposta: identidade de personagem online separada da conexão; concessão de escrita apenas pelo dono autorizado; worker de persistência fora do tick; snapshot com revisão e idempotency key; confirmação após commit; política explícita de rollback/backup. Incluir IDs de autoria/regiões grandes requer novo contrato versionado. Uma integração incompatível continua dependendo de aprovação.

`SyntheticOnlineRepositoryTest`: roundtrip, conflito de revisão, backup, corrupção, restart e recusa de grant de outra instância. Sem contas comerciais ou credenciais permanentes.
