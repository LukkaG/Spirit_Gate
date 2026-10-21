# TASK-115 — Testes

## Automatizados

`CharacterSelectionTest` cobre carregamento, vazio, criação habilitada após leitura, seleção válida e inválida, classe/nível, Enter, Voltar/Escape via ações, renderização de fallback, ausência de controles de senha, confirmação obrigatória, arquivamento reversível, preservação byte a byte do backup, recusa de arquivar recuperação e recuperação em arquivo separado.

`CharacterSavesTest` cobre arquivo corrompido preservado e backup recuperável. `SpiritGateEntryTest` verifica arte embarcada e ação de entrada. Os testes existentes de sprites verificam arquivos ausentes, hashes inválidos e fallback. A suíte completa cobre regressões do RPG, arena e Coop.

```powershell
mvn -f server/pom.xml "-Dtest=CharacterSelectionTest,CharacterSavesTest,SpiritGateEntryTest" test
mvn -f server/pom.xml test
```

Neste ambiente Maven tentou usar `C:/.m2`, sem permissão. O cache existente foi copiado para `.maven-repository` no workspace; execução sem rede:

```powershell
mvn -o "-Dmaven.repo.local=$PWD/.maven-repository" -f server/pom.xml test
```

## Fluxo nas janelas reais

```powershell
javac -encoding UTF-8 -cp server/target/classes -d server/target/test-classes tools/SpiritGateSelectionProbe.java
java -cp "server/target/classes;server/target/test-classes" com.projetok.prototype.rpg.SpiritGateSelectionProbe docs/task-115/capturas
```

O probe cria uma pasta temporária independente e manipula os controles reais no EDT. Exercita entrada sem senha, perfil lembrado, catálogo vazio, diálogo de criação, jogo, salvar/retornar, seleção e reabertura do save, Voltar à entrada, save corrompido com backup e preview geométrico em resolução reduzida. Não usa saves pessoais. Os testes de teclado invocam ações Swing; digitação física e foco por Tab ainda dependem de revisão humana.

## Capturas

Capturas `.png` usam `printAll` dos componentes das janelas Swing abertas, sem mockups ou imagens geradas. Os arquivos `.desktop.png` registram a tentativa por Robot: o desktop deste ambiente devolveu imagens pretas, portanto não comprovam aparência na tela física. `probe.txt` registra o resultado do percurso. Não há aprovação visual humana implícita.

- [Entrada offline](task-115/capturas/01-entrada.png)
- [Lista vazia](task-115/capturas/02-vazio.png)
- [Personagem criado em jogo](task-115/capturas/03-personagem-criado.png)
- [Seleção após retorno](task-115/capturas/04-selecao-retorno.png)
- [Perfil lembrado](task-115/capturas/05-voltar-perfil.png)
- [Save corrompido](task-115/capturas/06-corrompido-backup.png)
- [Backup selecionado](task-115/capturas/07-backup.png)
- [Fallback em 640×480](task-115/capturas/08-fallback-640x480.png)

## Ocorrências

Uma execução intermediária da suíte encontrou texto com codificação incorreta; o teste de classe/nível detectou o problema. A codificação foi corrigida e os testes focados repetidos. A revisão das capturas também levou à correção do contraste da seleção. Logs locais ficam em `.task-checkpoints/TASK-115/`.

Resultado final: **384 testes, 0 falhas, 0 erros, 0 ignorados**. Resumo em [test-summary.json](task-115/test-summary.json).
