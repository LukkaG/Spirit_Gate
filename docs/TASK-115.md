# SPIRIT GATE — TASK-115

Fluxo de entrada e seleção de personagens do RPG offline. Data: 20/10/2026.

## Auditoria

O diretório de trabalho já continha `SpiritGateEntry`, `SpiritFlameButton`, arte de entrada, integração no `RpgLauncher` e alterações de World Studio. O checkpoint inicial inclui esse estado; essas alterações anteriores não são atribuídas à TASK-115.

- `SpiritGateEntry`: entrada Swing com arte existente, preferências locais em `saves/entry.properties`, sem autenticação. Exibia senha desabilitada e recuperação de senha; controles removidos e textos localizados.
- `RpgMenu`: já listava/criava/carregava personagens por `CharacterSaves`, `CharacterIdentity`, `RpgWorld.restore` e `LocalSaveStore`. A seleção visual reutiliza esse fluxo.
- `CharacterSaves`: valida saves, preserva arquivos inválidos e oferece backups recuperáveis. O carregamento de backup continua gravando em um novo arquivo de recuperação.
- `RpgLauncher`: já executava `saveAndPause`, mantinha a sessão em caso de erro e reabria o menu no fechamento. Não foi necessário alterar esse serviço nesta tarefa.
- Arte: fundo e botão em `server/src/main/resources/rpg/login`; sprite experimental validado por metadados/hash em `assets/dev/player-base`. O contrato possui um Idle estático por direção e quatro frames de caminhada. Assets originais não foram editados.

## Implementação

`CharacterSelection` apresenta lista local, nome, classe e nível calculado pelo mundo existente; Jogar, Criar personagem, Voltar e acesso preservado à Arena histórica. Inclui carregamento, lista vazia, save inválido, backup recuperável e falha de leitura. O preview usa `RpgPlayerSprites` com pose inicial frontal e movimento suave de respiração. Sem sprite validado, apresenta o personagem geométrico com a cor de aparência.

Enter joga com a seleção; setas navegam pela lista; Tab/Shift+Tab percorrem controles; Escape volta à entrada. Atalhos Alt+J/C/V/A e Alt+E/L permitem acessar controles. Layouts usam dimensões flexíveis, rolagem e limites de tela; seleção validada em 1000×620 e 640×480.

A retirada da lista é reversível: **Arquivar personagem** exige digitar exatamente `ARQUIVAR`. Move apenas o save principal para `saves/characters/archived/<uuid>-<arquivo>`, sem sobrescrever arquivos e sem remover `.bak`. Entradas de recuperação não podem ser arquivadas. Para restaurar, feche o jogo e copie o save arquivado para a pasta `characters`, usando um nome `.properties` que ainda não exista. Backups permanecem no local original.

O identificador do perfil é público e apenas sugere o nome na criação. Lembrar perfil local grava somente a propriedade `username`; desmarcar limpa o identificador persistido. Não armazena senha, token ou conta online. Os personagens continuam sendo o catálogo local compartilhado do computador, sem isolamento por perfil e sem criação de serviço de autenticação.

## Execução

Na raiz do repositório, com JDK 17 e Maven:

```powershell
mvn -f server/pom.xml test
mvn -f server/pom.xml compile
java -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher
```

Também é possível usar `jogar-rpg-offline.cmd` após compilar. Para testar sem tocar nos saves pessoais:

```powershell
java -Dspiritgate.rpg.dataDir=C:/Temp/spiritgate-task115 -cp server/target/classes com.projetok.prototype.rpg.RpgLauncher
```

Consulte [testes](TASK-115-TESTES.md) e [relatório final](TASK-115-RELATORIO-FINAL.md).
