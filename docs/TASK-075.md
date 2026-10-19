# TASK-075 - Integracao geral

Suite completa preserva 241 testes historicos e todos os incrementos. Campanha offline ponta a ponta para ambos arquetipos, inventario/equipamento, quests, transicoes, masmorra/chefe, corrupcao e restauracao de save. Stress offline 20000 ticks; rede com falhas deterministicas 2500 ticks, dois processos de clientes e combate por TCP real ate recompensa compartilhada.

Regressao reproduzida ANTES da correcao em TASK-075-regressao-backup-antes.log: arquivo atual com formato Properties valido mas flag semanticamente corrompida podia substituir backup bom. load/backup agora validam restauracao completa antes de qualquer copia; falha preserva backup e exige recuperacao. Teste CorruptBackupProtectionTest protege esse caso. Novo candidato offline sera reconstruido com essa correcao; 0.66.0-rc2 permanece preservado.

Corrigido ainda lifecycle do servidor fechado antes de start e cleanup de bind malsucedido. Nenhuma conexao externa, alteracao de credenciais ou migracao de banco. Homologacao em outra maquina/LAN e transicoes online continuam pendentes explicitamente.
