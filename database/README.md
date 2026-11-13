# Integração PostgreSQL — preparação

Decisão de 09/11/2026: PostgreSQL próprio, esquema novo.

`drafts/` contém propostas SQL para revisão, não migrations homologadas.
Nenhum script é executado na inicialização. Não existem credenciais, driver JDBC,
pool ou adaptador PostgreSQL nesta entrega. Não conectar o cliente ao banco.

O primeiro rascunho representa apenas o contrato de espécies MobDefinition.
Usa os IDs textuais existentes para preservar spawns e referências de conteúdo.
O nível continua restrito a 1 porque recompensas de níveis superiores ainda não
foram definidas. Não há seed que habilite recompensas para mobs experimentais.

Antes de promover o rascunho a migration: testar numa base descartável, verificar
constraints e ida/volta de todas as espécies, definir usuário de aplicação sem DDL,
versionamento e ferramenta de migrations. Não aplicar a um esquema existente.
Reversão desta preparação: remover somente os arquivos novos; nenhum banco mudou.

Separar catálogo de conteúdo, estado de personagens e estado do mundo. A persistência
de personagens precisa contemplar atributos manuais, energia, inventário, equipamentos,
missões e revisões; RpgSave e SyntheticOnlineRepository não comprovam esse contrato.
Escritas devem sair do tick, usar transações e detectar revisão concorrente. Recompensas
precisam de recibos idempotentes antes de qualquer integração durável.
