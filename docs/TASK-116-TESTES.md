# TASK-116 — Testes e evidências

## Resultado

Suíte completa final: **386 testes, 0 falhas, 0 erros, 0 ignorados**. Log local: `.task-checkpoints/TASK-116/full-suite-final.log`. Resumo versionável: [test-summary.json](task-116/test-summary.json).

```powershell
mvn -o "-Dmaven.repo.local=$PWD/.maven-repository" -f server/pom.xml test
```

O cache Maven já existente no workspace permite executar sem rede.

## Cobertura

`CharacterSelectionVisualTest` adiciona dois testes que verificam cards normais/selecionados/focados por renderização, resumo com valores reais, backup selecionável e não arquivável, save inválido, lista vazia, chamadas das cinco ações, dimensões e limites dos controles, resumo legível, vinte entradas com viewport de largura fixa, navegação pela ação de seta e atualização da prévia. Renderiza a tela em 1000×620, 800×600 e 640×480, com sprite frontal real e fallback.

`CharacterSelectionTest` da TASK-115 continua verificando seleção, vazio, inválido, classe/nível, Enter/Escape, fallback, confirmação obrigatória do arquivamento, conteúdo arquivado recuperável, preservação byte a byte de backup e recuperação em arquivo separado. `CharacterSavesTest`, testes de sprites e a suíte de RPG/arena/Coop também passaram.

As chamadas de Criar, Arquivar e Arena no novo teste verificam o contrato dos controles por listeners. A confirmação e persistência do arquivamento são verificadas pelo teste de serviço existente; não se trata de um percurso automatizado por todos os diálogos dessas ações. A arena não foi aberta pelo probe visual.

## Janelas reais

`tools/SpiritGateSelectionVisualProbe.java` é uma variante da ferramenta anterior, preservada, que usa dados temporários isolados. Percorre entrada offline, catálogo vazio, criação por diálogo, jogo, salvar/retornar, reabrir personagem, Voltar, perfil lembrado, save corrompido, backup recuperável e fallback. Adiciona captura compacta de 800×600.

```powershell
javac -encoding UTF-8 -cp server/target/classes -d server/target/test-classes tools/SpiritGateSelectionVisualProbe.java
java -cp "server/target/classes;server/target/test-classes" com.projetok.prototype.rpg.SpiritGateSelectionVisualProbe docs/task-116/capturas
java "-Dsun.java2d.uiScale=1.5" -cp "server/target/classes;server/target/test-classes" com.projetok.prototype.rpg.SpiritGateSelectionVisualProbe docs/task-116/capturas-dpi150
```

Ambas as execuções terminaram com sucesso: [probe padrão](task-116/capturas/probe.txt) e [probe 150%](task-116/capturas-dpi150/probe.txt).

## Capturas

- [Lista vazia](task-116/capturas/02-vazio.png).
- [Seleção com sprite, janela padrão](task-116/capturas/04-selecao-retorno.png).
- [Save inválido](task-116/capturas/06-corrompido-backup.png).
- [Backup recuperável](task-116/capturas/07-backup.png).
- [Fallback, 640×480](task-116/capturas/08-fallback-640x480.png).
- [Compacto, 800×600](task-116/capturas/09-compacto-800x600.png).
- [Escala 150%, 640×480](task-116/capturas-dpi150/08-fallback-640x480.png).

As imagens `.png` são `printAll` das janelas Swing reais. Não são mockups. As tentativas `.desktop.png` via Robot retornaram desktop preto neste ambiente; foram preservadas como registro, mas não comprovam apresentação no monitor. A execução em 150% exercita a escala do Java2D; a captura por `printAll` continua em coordenadas lógicas e pode apresentar diferenças no chrome da janela e no subtítulo. Não equivale a homologação em monitor de DPI alto.

Setas e atalhos são exercitados por ações Swing; Tab/Shift+Tab permanecem no mecanismo nativo, com foco desenhado nos botões e cards. Digitação física, percurso real de Tab e preferência visual aguardam avaliação humana.

## Ajustes durante validação

A primeira suíte completa encontrou um erro no novo teste: o caminho dos sprites partia de `server/`, enquanto os assets ficam na raiz. O teste foi corrigido para seguir o padrão dos testes de sprites existentes. A primeira revisão das capturas também motivou reduzir a altura do resumo/cabeçalho e fixar a largura da lista ao viewport. Os testes visuais passaram após os ajustes; a suíte completa foi executada novamente sobre o código final e passou integralmente.
