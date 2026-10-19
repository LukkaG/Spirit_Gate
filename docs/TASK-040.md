# TASK-040 - Registro de conteudo

Catalogo Properties UTF-8 schema=1, embutido no classpath. Categorias item, enemy, region, npc, quest com IDs estaveis, nome, referencia regional e valor limitado. Mundo carrega e valida catalogo antes de iniciar. Limites: 64 KiB, 512 definicoes, IDs 40 caracteres, nomes 80, valores inteiros 0-100000. Duplicatas, referencias ausentes, schema desconhecido, valores nao finitos e categorias desconhecidas recusados.

Conteudo inicial: tres regioes, cinco itens, tres inimigos, dois NPCs e duas missoes. Contratos especificos serao estendidos e validados nas tarefas de cada sistema. Nenhum conteudo e executavel. Testes negativos e suite integral.
