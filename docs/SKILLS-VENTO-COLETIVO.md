# Vento — retorno coletivo sem alvo

Revisão local de 02/11/2026, branch `beco`, base `6d72c65`.

O slot 4 mantém seu skillshot frontal e o empurrão de área. Agora o executor registra cada inimigo afetado, com geração e posição após o deslocamento. O slot 5 é uma ativação direta, sem seleção: puxa todos os marcados ainda vivos, na mesma região e geração, ao alcance de 300 unidades e com caminho visível contra paredes. Não depende da distância entre as vítimas e não inclui inimigos que apenas se aproximaram depois.

A janela do combo continua em 150 ticks (cinco segundos). O retorno consome o conjunto de marcas uma única vez, custa 18 de energia e conserva recarga e dano. Se nenhum marcado estiver elegível, recusa sem gastar energia ou iniciar recarga. Uma marca de geração antiga não impede o retorno dos demais inimigos válidos.

Cada vítima retornada produz um evento próprio para o efeito inverso já aprovado. O catálogo `SELF` faz a hotbar habilitar e enviar a ativação sem alvo pela infraestrutura existente. Estado e dano continuam autoritativos; não houve alteração de mensagens, serialização, saves, arte, offline/Coop ou publicação.

Arquivos alterados: `RpgSkillCatalog.java`, `RpgSkillEngine.java`, `RpgSkillCatalogTest.java`, `RpgSkillEngineTest.java`, `SkillSlashRefinementGuiTest.java` e a linha correspondente em `SKILLS-CATALOGO.md`.

Validação: **162 casos, zero falhas, erros ou skips**, com JDK 17 e Maven offline (`mvn test -o`, cache local `.maven-repository`, `-Djava.awt.headless=false`). Foram selecionadas as seis classes de executor, catálogo, apresentação, protocolo e interação/animação gráfica. Logs e XML em `.task-checkpoints/SKILLS40/vento-coletivo/`. Os testes incluem múltiplos marcados separados, vizinho não marcado, geração substituída, consumo único, expiração, colisões e retorno TCP sem seleção. As capturas históricas V6 foram restauradas após arquivar as novas no checkpoint. `git diff --check` sem erros. Homologação física e Radmin permanecem pendentes.

Para conferir: no treino de Vento, use slot 4 contra um grupo e depois slot 5 sem selecionar ninguém. Para reverter, comparar apenas estes arquivos com `baseline/` desse checkpoint, preservando mudanças posteriores; não usar reset/clean. Sem commit, push ou distribuição automática.
