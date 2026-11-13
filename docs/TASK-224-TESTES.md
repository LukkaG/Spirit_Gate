# TASK-224 — Testes, capturas e limitações

Referência: 02/11/2026, branch `beco`, HEAD `6d72c65`. JDK 17, Maven offline e cache existente; nenhuma dependência instalada.

## Rodadas de regressão

| Rodada | Execuções | Falhas/erros finais | Ignorados |
|---|---:|---:|---:|
| Suíte completa headless | 939 | 0 / 0 | 54 |
| Complemento gráfico, 26 classes | 106 | 0 / 0 | 0 |
| Rodada específica após estabilizar helpers de foco | 17 | 0 / 0 | 0 |

Logs no checkpoint `.task-checkpoints/CORUJAO-IX/TASK-224/`: `full-headless.log`, `gui.log`, `input-stabilized-final.log`. Os casos gráficos novos receberam depois cenários/capturas adicionais de alvo distante, obstáculo, outra região e desconexão, sem alteração da produção; os logs `extra-target-captures-fixed.log` e `final-target-states-rerun.log` registram essas verificações focalizadas.

O agregado final usa o XML mais recente de cada classe, sem somar execuções repetidas. A suíte headless e o complemento são rodadas distintas. XMLs e contagem exata ficam em `final-reports/` e `verification.json`.

**Agregado final: 939 casos aprovados em 250 classes, zero falhas, erros ou skips remanescentes.** A repetição final das duas funções de teste GUI, incluindo os cenários adicionais, passou em `final-target-states-rerun.log`.

Comando geral, a partir de `server/`:

```powershell
mvn test -o `
  "-Dmaven.repo.local=C:/Users/lucas/Documents/GitHub/Projeto-K/.maven-repository" `
  "-Dspiritgate.logs.dir=C:/Users/lucas/Documents/GitHub/Projeto-K/.task-checkpoints/CORUJAO-IX/TASK-224/test-errors" `
  "-Djava.awt.headless=true"
```

Complemento: mesmos parâmetros com `-Djava.awt.headless=false` e `-Dtest=<lista de gui-tests.txt>`. A lista é derivada dos skips da suíte e inclui também NetworkSkillPresentationTest e SkillTooltipTest. As rodadas focalizadas explicitam suas classes no cabeçalho/log Maven.

## Matriz dos vinte casos obrigatórios

| Caso | Evidência executada | Resultado/limite |
|---|---|---|
| 1. TAB no viewport | GameInputRoutingTest; TargetCombatGuiTest; Task224CastingGuiTest | Seleciona/cicla IDs de alvos, sem caminhar. |
| 2. TAB/SHIFT+TAB na HUD | GameInputRoutingTest; Task224CastingGuiTest | Navega por componentes sem trocar alvo; SHIFT+TAB não ativa Shunpo. |
| 3. SPACE com botão destacado | GameInputRoutingTest.tabSpaceEnterTypingAndWindowScopeUseTheCorrectContext | Incrementa básico, não o clique do botão. |
| 4. ENTER em controle focado | Mesmo teste, e captura de configuração | Ativa controle; durante preparação, confirma área por prioridade contextual. |
| 5. F5–F9 disponíveis | GameInputRoutingTest.fiveDomainHotkeysAndCustomMappingKeepSingleDispatchAndAreaTabDoesNotSelect; regressão dos slots | Cinco bindings exercitados com eventos AWT. |
| 6. Remapeamento | Mesmo teste e regressões de configuração | Tecla personalizada dispara a skill; a anterior deixa de disparar. |
| 7. Com/sem alvo | Task224CastingGuiTest; RpgSkillEngineTest | Ofensiva exige alvo compatível; frontal/self/área não exigem seleção. |
| 8. Aliados/inimigos | Task224CastingGuiTest; AllyTargetSupportServerTest | Nome, HP, tipo e elegibilidade; NPC rejeitado como alvo. |
| 9. Morto/desconectado/outra região | Task224SkillFeedbackTest; AllyTargetSupportServerTest; cenários TCP finais de Task224CastingGuiTest | Revive admite aliado derrotado, não vivo; seleção some com ator indisponível/fora da região e não pode ser refeita por ID ausente. |
| 10. Nível insuficiente | Fixture de apresentação de Task224CastingGuiTest | Slots e feedback exibem nível 15; não altera a ficha autoritativa. Validações reais do executor continuam nos testes existentes. |
| 11. Energia insuficiente | Mesmo fixture, mais testes de recusa sem consumo do executor | Feedback distinto e nenhum pacote causado pelo refresh de UI. |
| 12. Cooldown ativo | Task224CastingGuiTest, SkillProtocolTest e RpgSkillEngineTest | Recarga observada mantém slot indisponível e efeito confirmado chega aos dois clientes. |
| 13. Quatro direções do smartcast | Task224SkillFeedbackTest.smartcastPreviewKeepsAllFourAuthoritativeFacingDirections; SkillInputRefinementGuiTest; RpgSkillEngineTest | Cliente preserva cada facing cardinal; servidor segue validando a direção/contato. |
| 14. Área fora dos limites | Task224CastingGuiTest; Task224SkillFeedbackTest | Prévia limitada por alcance/bordas; pontos não finitos/fora do mapa não são anunciados válidos. |
| 15. Cancelar sem conjurar | Task224CastingGuiTest; SkillInputRefinementGuiTest | ESC cancela, sem cooldown/evento da skill preparada. |
| 16. Texto sem gameplay | GameInputRoutingTest | WASD/SPACE digitam; tecla remapeada de skill não lança. |
| 17. Desconectar durante preparação | Task224CastingGuiTest | Retículo e teclas de ajuste são descartados; nenhum evento da área é recebido pelo observador. |
| 18. Autorrepetição | GameInputRoutingTest | Repetir KEY_PRESSED sem release não duplica alvo, básico ou cinco skills. |
| 19. Reconectar sem tecla presa | Task224CastingGuiTest; RpgReconnectIsolationTest; SessionGameReconnectCloseTest | Nova sessão/ID, sem alvo/mira antiga; posição permanece estável sem comandos herdados. |
| 20. Dois clientes TCP | Task224CastingGuiTest, SkillProtocolTest, AllyTargetSupportServerTest | Ambos recebem eventos reais; nenhuma confirmação visual aplica dano. |

## Resultado desconhecido e prévias

Task224SkillFeedbackTest verifica ausência de falso ACK, timeout como desconhecido, evento novo do próprio jogador/skill, cooldown como estado observado, Sentinela sem atribuir causalidade de cancelamento e descarte do contexto anterior. O teste TCP envia área através de obstáculo: não aparece evento/cooldown correspondente e o cliente exibe resultado desconhecido, sem inventar rejeição estruturada.

Avisos de alcance e LOS mantêm o comando habilitado quando o destinatário é válido; não são uma segunda simulação autoritativa. O servidor continua podendo aceitar/recusar conforme o estado no tick do lançamento.

## Ocorrências intermediárias e ajustes dos testes

As primeiras rodadas específicas registraram “Evento sem componente focado” e falhas de aquisição de foco nativo. A espera de foco foi corrigida somente nos helpers: SwingKeys aguarda um destinatário AWT real; Fixtures/helpers repetem requestFocus enquanto a janela é ativada. Não há componente fictício de fallback, ActionMap chamado no lugar do teclado nem mudança de produção para contornar a falha. O ambiente gráfico ainda pode perder foco para outro aplicativo: houve repetição focalizada documentada depois das capturas adicionais.

Um fixture de cena adicional usou inicialmente float onde `WorldDocument.Element.at` exige int; foi corrigido no teste, sem mudança do jogo. Um cenário de viagem tentou enviar MOVE diretamente enquanto a janela emitia seu heartbeat parado; foi corrigido para usar o teclado AWT da janela do aliado. Os logs intermediários foram preservados; não são apresentados como rodadas aprovadas.

## Capturas e integridade

[Galeria e interpretação](skills-preview-task224/README.md). Capturas de nível/energia são explicitamente fixtures de UI, não comprovação de mudança de regras no servidor. As demais capturas mostram sessões TCP locais, inclusive layout 1000×700 e 640×480.

Baseline de 1.573 arquivos, fontes anteriores e progresso/pendências preservados no checkpoint. Relatório final de hashes separa os cinco arquivos existentes de produção alterados e os quatro arquivos de testes/helpers alterados; assets, catálogo, engine, mundo/servidor e protocolo devem permanecer idênticos. As capturas antigas e os três PNGs sintéticos regenerados pelos testes são arquivados/restaurados sem descartar o trabalho inicial.

Verificação final: somente os nove arquivos preexistentes previstos mudaram; **zero alteração inesperada** nos arquivos protegidos. Os 152 PNGs de skills estão intactos; 89 capturas históricas foram restauradas e 21 imagens novas da TASK-224 ficam na pasta própria. As 2.432 exclusões iniciais foram preservadas. `git diff --check` terminou com código 0. Links dos sete documentos/galeria verificados, sem destino ausente.

Não houve Radmin físico, teclado físico em outro computador, release, benchmark de carga ou homologação estética humana. Nenhuma falha crítica de imunidade, corrupção, travessia ou dano duplicado foi observada nesta etapa; o risco condicional A-04 não foi resolvido nem declarado inexistente.
