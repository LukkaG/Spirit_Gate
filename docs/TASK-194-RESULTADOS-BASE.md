# TASK-194 — persistência sintética

Estado final sujeito ao registro do checkpoint: VALIDADA no laboratório sintético autorizado pelo ADR-193. Nenhuma integração ao launcher, contas ou saves pessoais.

O repositório agora recusa backup cujo UUID difere do solicitado e converte OverlappingFileLockException em IOException, mantendo o documento confirmado. O CAS de write permanece estrito. A operação explícita writeIdenticalRetry reconcilia, sob o mesmo lock, apenas snapshot integralmente igual na revisão base+1; não toca arquivo nem backup nesse caso. Estado diferente, revisão distante, grant alheio ou instância antiga são recusados.

Reprodução anterior preservada: run-20261026T000504Z-bdc91ff5, quatro testes, um aprovado e três falhas reais (UUID/lock/concorrência). Tentativa intermediária com cinco testes passou, mas foi substituída por compilação integral nova para incluir a prova de processo filho, sem reutilizar classes antigas.

Seis testes específicos: UUID trocado; lock retido; doze rodadas de concorrência com um único commit por base; checksum/temporário órfão; vinte retries idênticos sem alterar bytes/revisão/backup e negativos; processo filho que confirma revisão 2, deixa temporário parcial e encerra abruptamente com código 23. Após reinicialização, primário 2 e backup 1 íntegros. A prova de interrupção acontece após commit e criação deliberada de temporário parcial; não afirma cobrir todas as janelas internas de rename ou falta de energia. A escrita continua validando/force/ATOMIC_MOVE sem fallback de cópia no primário.

Regressões essenciais: pacotes RPG e multiplayer, resultados reais em rpg.log e multiplayer.log. Fontes atuais compilados em isolamento novo, Java 17/dependências já existentes. Diffs/hashes em implementation.patch e source-delta.json, incluindo origem anterior à correção. Integridade protegida conferida pelo checkpoint; rehash integral será repetido ao concluir o bloco autorizado. Riscos residuais: administrador local, TOCTOU de filesystem, durabilidade física e concorrência multiprocesso dependem do sistema de arquivos. Não oferece exactly-once de comandos de gameplay.
