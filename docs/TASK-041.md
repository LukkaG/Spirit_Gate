# TASK-041 - Save local V1

Snapshot imutavel RpgSave inclui identidade, localizacao, HP, experiencia, moedas, inventario, equipamento, missoes, flags e tick. Formato Properties legivel, schema=1; sem ObjectInputStream. Colecoes copiadas, IDs e limites validados. Rejeita duplicatas/campos desconhecidos, NaN, posicoes invalidas e versoes incompatíveis.

LocalSaveStore grava temporario no mesmo diretorio, valida releitura e substitui atomicamente quando suportado. Backup validado antes da substituicao; arquivo atual corrompido exige recuperacao explicita. Falha de escrita preserva ultimo save. API de IO separada do tick; integracao assincrona com UI sera ampliada no menu.

Testes reais em diretorio temporario: salvar/carregar/continuar, save ausente, escrita interrompida, backup, corrupcao e versao. Estado de futuros sistemas tem contrato persistente, comportamento sera integrado conforme suas tarefas. Nao alegar persistencia funcional de sistemas ainda inexistentes.
