# TASK-043 - Inventario transacional

20 slots por personagem; capacidade configuravel no servico ate 100. Pilhas maximas por catalogo, aquisicao/remocao, transferencia e separacao por quantidade. Adicao prepara copia antes de commit; remocao valida saldo total antes de alterar. Erros nao perdem nem duplicam itens. Snapshots imutaveis preservam indices dos slots no save, inclusive pilhas separadas.

Personagem inicia com tres pocoes. Save/restauracao agora integram inventario real e preservam os demais campos. Testes de cheio, rollback, quantidade zero, item inexistente, pilha, transferencia e round-trip do mundo; suite inteira executada.
