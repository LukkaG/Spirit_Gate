# CORUJ?O VI ? TASK-184
Estado: **VALIDADA COM RESSALVAS** (roteiro documental pronto; homologa??o f?sica PENDENTE).
Branch/HEAD verificados: beco / ad66caed14e3f94bc369f7edd9861900d5a68636.
Escopo implementado: checklist para dois computadores, adaptador/IP expl?citos, builds pareados, logs isolados A/B, revis?o/READY/ACTIVE/RESUME, preview separado, movimento/PvE/portal, rollback e desconex?o, clientes hist?ricos e rein?cio.
Arquivos alterados: docs/TASK-184-RADMIN-ROTEIRO.md e novos registros desta task. Game Server inalterado.
Verifica??o executada: revis?o est?tica contra RpgLauncher, RadminSessionPanel, RpgNetworkClientLauncher, propriedades NetworkDiagnostics/RpgMenu e guias TASK-180. PASS documental; comandos fornecidos n?o executados em VPN. N?o inventadas m?tricas ausentes no console.
Testes f?sicos Radmin: N?O EXECUTADOS; campos A/B e matriz permanecem PENDENTES.
Integridade protegida: invent?rio SHA-256 herdado da 183 em before.json; apenas documentos novos criados, sem execu??o de processos do jogo nesta task.
Evid?ncias: .task-checkpoints/TASK-184/corujao-vi/run-20261025T175754Z-b59bb0ba.
Regress?es ou riscos: nenhum patch de produ??o; coleta manual necess?ria. N?o representa autentica??o/TLS nem prova entre dois computadores.
Pend?ncias humanas/externas: operadores A/B e pacotes pareados; atualizador bloqueado pela pol?tica; revis?o de permiss?es/propostas 181.
Gate A: atendido com ressalvas f?sicas/ambientais expl?citas: agente auditado, baseline reproduzido por runner existente, laborat?rio TCP validado, roteiro pronto.
Pr?xima a??o: TASK-185 automaticamente; homologa??o f?sica n?o ? pr?-requisito cr?tico segundo Gate A do Plano Diretor.
