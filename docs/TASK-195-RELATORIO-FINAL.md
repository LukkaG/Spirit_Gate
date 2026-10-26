# TASK-195 — posse de sessão no laboratório sintético

Estado final sujeito ao checkpoint: VALIDADA no escopo local do ADR-193. SyntheticSessionLaboratory mantém handles opacos por identidade de objeto, grant privado e mapa sincronizado de posse por UUID. Criar/retomar/save/disconnect/close pertencem exclusivamente ao controlador local confiável; não há integração ao launcher, endpoint, conta ou autenticação pública.

O repositório permite apenas ao auxiliar local do mesmo pacote emitir novo grant após load validado; grants antigos continuam inválidos em nova instância. Disconnect invalida o handle antigo antes de liberar nova posse. Close invalida todos. Um handle de outro registro, encerrado, nulo ou com snapshot de outra identidade não grava nem revoga a posse correta.

Três testes positivos/negativos: isolamento/UUID/duplicação/revogação; restart e retry integral sem nova revisão; oito aquisições concorrentes do mesmo UUID com exatamente uma aceita e sete recusadas. Regressões RPG/multiplayer e contagens reais nos logs, fontes integralmente recompilados em checkpoint exclusivo. Diffs e hashes anexados. Testes gráficos restantes serão executados na regressão final do bloco 196.

Limites: exclusão de sessão pertence ao registro local, não é lock distribuído entre processos ou múltiplos controladores independentes. UUID seleciona uma fixture confiável e não serve como credencial. CAS/lock do repositório continuam a evitar commits concorrentes sobre a mesma base. Segurança pública/TLS é projeto futuro descrito em TASK-195-PLANO-LABORATORIO.md, sem implementação ou dependência nova. Não há logout/save automático silencioso: o controlador confirma o snapshot explicitamente antes de desconectar.


Evidências: `.task-checkpoints/TASK-195/corujao-vi/run-20261026T000904Z-ffd6efbd`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta execução (before/after): server/src/main/java/com/projetok/rpg/SyntheticOnlineRepository.java, server/src/main/java/com/projetok/rpg/SyntheticSessionLaboratory.java, server/src/test/java/com/projetok/rpg/Task195SessionOwnershipTest.java.
