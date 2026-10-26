# TASK-195 — posse local de sessões sintéticas

Contrato aprovado no ADR-193: registro dedicado ao laboratório, sem chamadas do launcher ou sockets. Cada handle opaco contém vínculo por identidade de objeto com um registro efêmero; o grant permanece privado. Um UUID pode ter no máximo um handle ativo por registro. Após disconnect/close ou restart, handles antigos são inválidos. Um controlador local confiável pode retomar apenas um documento sintético validado e recebe nova autoridade efêmera.

A API local não é login e UUID não é senha. Outro registro não pode usar o handle anterior; o contrato não oferece exclusão de sessão entre processos independentes nem proteção contra administrador local. Escritas continuam protegidas por CAS/lock e retry integral da TASK-194. Testar duplicação de posse, handle estrangeiro, handle revogado, escrita com identidade alheia, concorrência de aquisição e restart.

Autenticação de produção permanece projeto futuro: conta/credencial separada da identidade do personagem; derivação segura de senha ou provedor; autorização por operação/posse; rate limiting; expiração/revogação; transporte TLS com validação de certificado; threat model e revisão independentes. Nada disso é implementado ou homologado por um handle local deste laboratório; nenhum novo pacote ou permissão de rede é necessário aqui.
