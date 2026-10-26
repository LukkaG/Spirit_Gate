# TASK-193 — contrato atual e lacunas

O repositório atual usa SGO1, versão 1, UUID, revisão, comprimento, payload do codec V2 e SHA-256. Leitura é limitada a aproximadamente 263 KiB e valida o `RpgSave` com o motor. Locks/CAS e rename atômico protegem a confirmação do primário; o grant é efêmero e local à instância. As únicas referências de produção ao tipo continuam dentro do próprio protótipo, sem ligação aos launchers.

Probe executado: `tools/Task193RepositoryAuditProbe.java`, compilado contra as classes atuais da regressão 192 após conferência integral dos hashes de fontes. Tudo foi criado em novas pastas `online-synthetic-*` do checkpoint da 193; nenhum personagem pessoal foi lido ou copiado.

| Caracterização atual | Resultado |
| --- | --- |
| Roundtrip e CAS de revisão | PASS |
| Backup da revisão anterior | PASS |
| Grant anterior recusado após restart | PASS |
| Cópia/migração de wrapper somente sintético | PASS; origem preservada |
| Duas escritas na mesma revisão | Um commit, um conflito de lock na mesma JVM; documento final válido |
| Identidade de backup trocado | Lacuna confirmada: retorna B ao pedir A |
| Gravar estado B com grant A | Rejeitado; não houve corrupção do primário A |
| Replay idêntico com revisão-base antiga | Conflito, sem confirmação idempotente |

O resultado `PASS_CHARACTERIZATION` não é aprovação de todos os contratos desejados. Backup/retomada, uniformidade de conflito e idempotência precisam do trabalho descrito no [ADR-193](ADR-193-PERSISTENCIA-SINTETICA.md). O protótipo persistente não foi modificado nesta auditoria.

Testes previstos para 194: UUID de nome/cabeçalho/payload/backup, corrupção/checksum, schema incompatível, conflito de revisão, concorrência, falha antes/depois de rename e retry de snapshot. Para 195: posse de sessão local, grant de outra instância/UUID, duplicação e handle encerrado. Para 196: ciclo de criação, snapshot de XP/coins/quest sintéticos, desconexão/restart e restauração sem reaplicar a mesma recompensa/snapshot, com região/posição validadas no conteúdo conhecido do harness.

Não se pressupõe suporte a quests autoradas no payload V2 nem persistência de conteúdo arbitrário. O harness precisa registrar exatamente o catálogo/revisão usados e recusar combinações incompatíveis. Contas reais, TLS e integração do launcher continuam fora deste contrato até proposta/aprovação separadas.
