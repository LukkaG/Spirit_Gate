# TASK-063 - Inventario e equipamentos na UI

I abre janela redimensionavel com 20 slots, quantidades, quatro espacos de equipamento, descricoes, raridade e estatisticas. Botoes usar/equipar/desequipar, mover/separar por destino/quantidade e recolher loot pendente. Toda operacao e comando na thread do dominio; listas atualizam apenas por snapshot e preservam selecao. Erros informados sem alterar estado.

Renderer compartilhado ganhou HUD/paleta opcionais, com padrao historico intacto. RPG exibe controles corretos e cores distintas para aparencia/NPCs, removendo instrucoes de Criostase que pertencem a arena. Validacao usa testes transacionais existentes e suite completa; revisao visual no marco B.
