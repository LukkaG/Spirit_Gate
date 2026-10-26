# CORUJÃO VI — TASK-188

Estado: **VALIDADA APÓS CORREÇÃO**. Branch beco, HEAD ad66caed14e3f94bc369f7edd9861900d5a68636. Transição: 187 concluída -> 188 -> 189 automaticamente após Gate B.

A matriz real TCP comprovou V1/State, V2/SessionState e V3/CombatState ao lado de V4; cada cliente antigo bloqueia Save com causa clara e permanece conectado até fechamento voluntário. Após commit autorado, novo ingresso antigo recebe Error sem downgrade e novo V4 recebe revisão confirmada. Durante READY, tentativas 1–4 são recusadas sem alterar a barreira, o hash ou os participantes.

Foi reproduzido e corrigido um defeito de admissão: RevisionCoordinator.depart iniciava recuperação até para ID nunca admitido. Fechar a primeira conexão recusada abortava a publicação legítima; uma tentativa seguinte podia receber Welcome porque essa barreira já havia sido desfeita. Patch mínimo: se clients.remove(id) não remove um participante, retornar sem alterar a transação. Saídas de participantes reais mantêm a recuperação anterior. Não houve commit falso, concessão de edição ou mudança de protocolo. O teste agora exige READY e base preservados após cada fechamento recusado.

Testes finais: específico 4/4 e multiplayer 125/125 aprovados, zero falhas/abortos/skips. Incluem negativos de publicação, recuperação, golden bytes 1–15, reconexão e pressão da 187. Nenhum teste desabilitado ou requisito afrouxado.

Tentativas preservadas: run-20261025T222711Z-69809ca5 (fixture READY não habilitava catálogo V2, corrigida); run-20261025T222825Z-db655d6f (3/4, defeito real de saída não participante). Fonte anterior e diff da correção estão no checkpoint final run-20261025T222955Z-8cf5a1a3. Não se trataram resultados anteriores como atuais.

Gate B atendido com ressalvas operacionais explícitas: protocolo, admissão, publicação e rollback passaram nas condições testadas; overflow de controles pode isolar emissor e não se promete aceitar todo tráfego abaixo de 120 mensagens/s. Radmin físico continua pendente. Nenhum endpoint administrativo, save, asset, dependência, política ou distribuição alterado. Sem push/publicação.


Evidências: `.task-checkpoints/TASK-188/corujao-vi/run-20261025T222955Z-8cf5a1a3`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta task: server/src/main/java/com/projetok/multiplayer/RevisionCoordinator.java, server/src/test/java/com/projetok/multiplayer/Task188AdmissionMatrixTest.java.
