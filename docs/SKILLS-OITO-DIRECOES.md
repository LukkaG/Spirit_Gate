# Skill shots em oito direções

Implementação local de 13/11/2026 no Spirit Gate, branch `beco`, base
`6faf210177cc3c5eb968191fdd488beb48bef89e`.

As habilidades frontais `LINE` e `CONE` agora selecionam a direção mais próxima
em passos de 45 graus a partir do facing autoritativo do personagem. Caminhar
na diagonal permite lançar a skill nessa diagonal; parar conserva a última
direção. A direção enviada pelo cliente não substitui a decisão do servidor.
O smartcast do cliente usa a mesma seleção.

As diagonais têm vetor unitário, mantendo alcance e velocidade. Bordas do mapa
encurtam o trajeto ao longo da mesma direção. Colisões e impactos continuam
resolvidos pelo executor existente. A apresentação já rotacionava os frames
conforme o segmento confirmado e passa a receber os trajetos diagonais.
Os PNGs aprovados e as poses do personagem foram preservados; esta entrega
reutiliza a rotação existente dos efeitos, sem criar novas folhas de sprites.

Fontes alterados: `RpgSkillEngine.java` e `NetworkSkillControls.java`.
Testes alterados: `RpgSkillEngineTest`, `SkillTargetPresentationTest` e
`NetworkSkillPresentationTest`. Ataque básico, mensagens de rede, saves,
custos, recargas e regras de dano permanecem com seus contratos existentes.

## Validação

Java 17, Maven offline com o cache local. Rodada final: **168 testes aprovados**,
zero falhas, erros ou ignorados, nas sete classes abaixo:

```powershell
cd server
mvn -o '-Dmaven.repo.local=../.maven-repository' '-Djava.awt.headless=true' '-Dtest=RpgSkillEngineTest,SkillTargetPresentationTest,NetworkSkillPresentationTest,Task224SkillFeedbackTest,SkillProtocolTest,SkillDisplacementServerTest,Task225ProjectileTcpTest' test -q
```

Cobertura inclui oito direções autoritativas, alcance uniforme, quatro diagonais
com contato atrasado até a chegada do projétil, smartcast nas bordas, rotação
dos frames em imagem e regressão TCP nas aplicações 6/7. A primeira execução
identificou dois problemas nos novos testes (facing não normalizado no snapshot
e origem próxima à borda), corrigidos nos fixtures antes da rodada aprovada.

Cliente local recompilado com `powershell -NoProfile -ExecutionPolicy Bypass
-File tools/compile-local.ps1`: 286 fontes Java 17, saída em
`server/target/launcher/classes`. `git diff --check` sem erros de whitespace.

A conferência estética em partida e a homologação física/Radmin permanecem
pendentes. Não houve commit, push, publicação ou atualização dos pacotes
portáteis. Para testar, reiniciar o cliente e o servidor a partir dos fontes
locais. Para reverter, retirar somente os diffs desta entrega nos cinco arquivos
Java e este relatório, preservando alterações posteriores.
