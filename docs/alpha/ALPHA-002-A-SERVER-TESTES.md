# ALPHA-002-A-SERVER — Testes e evidências

## Ambiente e isolamento

Checkout `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. Java 17, Maven pelo wrapper Python já existente em `tools/server_quality`, sem dependências novas e com `--offline`. O wrapper copia fontes e assets para `spiritgate-quality-*`, usa home/logs/docs temporários e build separado. Não executado `mvn clean`, banco ou saves pessoais.

Evidência arquivada: `.task-checkpoints/ALPHA-002-A-SERVER/`. Contém baseline, diff anterior, backups, hashes finais, preservação, `rounds.json` e subpastas de logs/XML por rodada. `LocalSimulationServer.java` foi comparado por hash ao baseline e permaneceu idêntico ao patch de handshake preexistente.

## Comandos finais exatos

Executados da raiz do checkout em PowerShell:

```powershell
python tools/server_quality/run_quality.py --mode test --offline --tests 'Alpha002LightningTest,Alpha002LightningTcpTest,RpgSkillEngineTest,Task225MaterializationTest,RpgSkillCatalogTest,Task225CollisionTest,ProtocolTest,ProtocolBoundaryTest,SkillProtocolTest,LocalServerTest,NetworkRobustnessTest,FinalNetworkIsolationTest,Task225ProjectileTcpTest,RpgReconnectIsolationTest,Task208ProgressionTest,Task210QuestJourneyTest,Task207InventoryTest,InventoryServiceTest,QuestRewardsTest,QuestLifecycleTest,AuthoredQuestRuntimeTest,DomainProgressionTest,RpgProgressionTest,MobCollisionTest,PlayerBlockCollisionTest,SoftSlideCollisionTest,CollisionPolicyTest,Task225PhasePresentationTest,LauncherSessionTest'

python tools/server_quality/run_quality.py --mode test --offline --tests 'Alpha002LightningSessionTest'

git diff --check
```

## Resultado final, sem dupla soma

| Rodada | Seleção | Testes | Falhas | Erros | Ignorados | Evidência arquivada |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Regressão final | 29 classes do primeiro comando | 240 | 0 | 0 | 0 | `06-regressao-final/test.log`, `summary.json`, `test-reports/TEST-*.xml` |
| Coordenador final | Somente `Alpha002LightningSessionTest` | 2 | 0 | 0 | 0 | `08-coordenador-final/test.log`, `summary.json`, `test-reports/TEST-*.xml` |
| Total disjunto | 30 classes | **242** | **0** | **0** | **0** | `rounds.json` |

Não somar reexecuções intermediárias ou os 18 novos casos novamente ao total. A primeira rodada final veio de `C:/Users/lucas/AppData/Local/Temp/spiritgate-quality-x9woeg56`; a segunda, de `.../spiritgate-quality-vxatvkb3`. A produção não mudou entre as duas; foi acrescentado/ajustado somente o teste adicional do coordenador.

`git diff --check` sem erro de whitespace. Avisos informativos de conversão LF/CRLF não são falha de teste.

## Cobertura principal

| Conjunto | Casos | O que comprova |
| --- | ---: | --- |
| `Alpha002LightningTest` | 13 | Posição fixa, ocupação, imunidade finita, ações extras recusadas, controles de área, Cero sem iniciar ataque, IA externa e tick ativos, entrada/saída/tangência, ROOT/STUN independentes, parede, quatro impactos/recompensa única, lifecycle e token substituído, duas áreas sobrepostas. |
| `Alpha002LightningTcpTest` | 3 | Dois sockets reais: observador 6 e 7, corpo nos snapshots, terminal aos 32 ticks, quatro HIT, progresso/inventário/recarga na sessão corrente, movimento do observador e saída do lançador sem dano tardio. |
| `Alpha002LightningSessionTest` | 2 | Bootstrap de conteúdo com READY/ACTIVE/RESUME, sequências consumidas na recusa, replay antigo recusado após a janela, sem intenção de caminhada guardada, cooldown mantido na mesma sessão e revisão que não herda fase/impactos. |
| `RpgSkillEngineTest` | 104 | Quarenta técnicas e regras existentes, incluindo as expectativas atualizadas de Quatro Relâmpagos. |
| `RpgSkillCatalogTest` | 8 | Catálogo e invariantes das habilidades. |
| `Task225CollisionTest` | 18 | Colisões, cenários e outras famílias de técnicas. |
| `Task225MaterializationTest` | 12 | Câmara A-04 termina aos 32, sem deslocamento, sem espera aos 180; demais controles de geometria/lifecycle. Medição histórica executada somente na cópia temporária. |
| Demais classes dos comandos | 82 | Protocolos, isolamento, robustez de rede, progressão, quests, inventário, colisão e contrato de launcher em headless. |

O cenário A-04 antigo e seu teste foram preservados antes da edição em `before/server/src/test/java/com/projetok/rpg/Task225MaterializationTest.java`. O novo nome `noArtificialSupportEndsAtConfiguredDurationWithoutReentryOrRelocation` expressa a expectativa aprovada; o teste não foi desabilitado. A comparação de HP após a janela considera que inimigos podem atacar no tick terminal, evitando atribuir dano legítimo à falha de imunidade.

## Rodadas intermediárias

| Pasta de evidência | Resultado observado | Tratamento |
| --- | --- | --- |
| `01-expectativas-antigas` | 142 casos, quatro falhas e um erro. | Expectativas antigas de intangibilidade, reposicionamento e stun; fixture com sobreposição forçada; comparação de HP sem considerar retorno da IA. Atualizados os testes necessários ao contrato aprovado. |
| `02-xp-tipo` | Falha de compilação de testes; nenhuma execução JUnit. | XP real é `long`; corrigidos dois locais na fixture nova. |
| `03-fixture-regiao` | 154 casos, zero falhas e 12 erros. | Fixture colocava `warden` em `forest`, rejeitado pelo catálogo. Removido o spawn inválido do novo teste; produção preservou a validação. |
| `04-fixture-ia` | 157 casos, uma falha, zero erros. | Inimigo externo estava na própria casa, fora de aggro, sem motivo para caminhar. Fixture ajustada para posição externa à área e dentro de aggro. Três TCP já passaram nessa rodada. |
| `05-interrompida-galeria` | Interrompida; não contabilizada como rodada final. | Seleção incluía exportador legado; incidente detalhado abaixo. |
| `07-bootstrap-fixture` | Dois casos, duas falhas. | Faltavam ACKs de bootstrap e o coordenador permanecia congelado. Teste passou a drenar conteúdo e enviar READY/ACTIVE até RESUME, usando APIs reais. |

## Incidente de exportação legada

`NetworkSkillPresentationTest` incluído na rodada interrompida exportou 12 PNGs em `spiritgate-quality-sxqz9jl_/project/docs/`. A classe foi identificada, os processos daquela execução foram interrompidos e somente esses arquivos temporários foram removidos após conferir seu diretório. Nomes e SHA-256 estão em `galeria-temporaria-removida.json`. Os logs/XML foram preservados, sem arquivar imagens. Não houve alteração de assets do checkout. A suíte final foi inspecionada para excluir chamadas de `ImageIO.write` nas classes selecionadas.

## Não executado e limites

- Sem homologação física em dois PCs, Radmin, GUI, áudio, launcher EXE ou sessão prolongada de carga.
- Sem suíte completa irrestrita: exportadores de imagem, testes de persistência 193–196, ORG-03 e laboratórios históricos de retirada R1/R2/R3 não integram a seleção final.
- Sem implementação de reconexão com recuperação, transferência de cooldown em revisão ou garantia após restart. O teste de revisão cobre cancelamento; não promete preservar temporizações que o fluxo preexistente já limpa.
- Sem benchmark representativo de tick, análise estática nova ou fuzzing novo nesta tarefa. Não somar resultados da auditoria anterior.
- Não foi implementado renderer, novo protocolo, schema, autenticação ou arte. O teste visual legado de fase selecionado verifica o fusível antigo; não homologa o contrato visual futuro.

Os resultados aprovam o recorte de servidor testado. Não autorizam classificar o jogo como produção/Alpha/Radmin homologado.
