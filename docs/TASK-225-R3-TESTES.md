# TASK-225-R3 — Testes e evidências

Execução de 03–04/11/2026. Baseline R2: 1.013 casos. Branch `beco`, HEAD inicial `6d72c65`. Evidências locais em `.task-checkpoints/CORUJAO-IX/TASK-225-R3/`.

## Rodadas executadas

| Rodada | Casos | Falhas / erros | Skips | Evidência |
|---|---:|---:|---:|---|
| `lab-first` | 32 | 0 / 0 | 0 | 20 R3 e 12 de 194/195/196; logs/XMLs preservados. |
| `lab-repeat` | 46 | 0 / 0 | 0 | 20 R3 + 14 R2 + 12 R1; inclui TCP anterior, sem recuperação TCP R3. |
| `full-headless` | 1.033 | 0 / 0 | 54 | Suíte completa, BUILD SUCCESS, sem desabilitar testes. |
| `review-tree` | 20 | 0 / 0 | 0 | HEAD exportado em pasta separada + somente dois fontes R3; verifica independência do trabalho local anterior. |
| `gui` | 106 | 0 / 0 | 0 | Complemento gráfico de 26 classes, BUILD SUCCESS. |

Não houve falha intermediária nessas rodadas. Os 20 novos casos são adição aos 1.013 anteriores, não uma substituição. Reexecuções são relatadas separadamente e não somadas ao agregado por classe.

**Agregado final por XML mais recente de cada classe: 1.033 casos em 262 classes, todos aprovados, zero falhas, erros ou skips remanescentes.** O complemento gráfico exercitou os 54 skips headless. Não somar suas 106 reexecuções aos 1.033 casos. `final-reports/` e `verification.json` preservam o agregado.

Integridade final: 291 arquivos de produção e 186 arquivos de skills (152 PNGs) idênticos ao baseline. Entre os 2.728 arquivos protegidos, apenas os dois documentos de acompanhamento receberam acréscimos R3. Saídas históricas regeneradas foram arquivadas em `suite-generated/` e restauradas do baseline; fontes/testes/documentos R1/R2 preservados.

## Matriz de segurança R3

`Task225R3RecoveryLabTest`, 20 casos (incluindo parametrizações), timeout de 15 segundos por caso:

| Exigência | Evidência | Limite |
|---|---|---|
| Recuperação válida | Snapshot inteiro igual; UUID/raça/domínio/XP/nível, HP, energia 430/1000, inventário/equipamento/moedas | Recupera DTO retido, não instala no multiplayer. |
| Recuperação duplicada / itens | Dez retries recusados; mesmo snapshot, uma geração ativa | Atomicidade de uma instância em memória. |
| Identidade incorreta | Outra credencial com mesmo nome/aparência e credencial de outra instância recusadas | Provas opacas locais, sem autenticação de rede. |
| Token expirado | Retirada 100/TTL 60: aceita 159; recusa 160/161 e não renova prazo | TTL é fixture; não é decisão de retenção de produto. |
| Reconexões simultâneas | Dois threads reais, barreira, um vencedor e uma recusa | Não são dois peers TCP recuperando. |
| Spawn bloqueado | Predicate recusado sem consumo, depois aceite; caso adicional com posição 400/200 recusada pela geometria real builtin | Não escolhe outro spawn nem usa mapa multiplayer autorado. |
| Mudança de revisão/época | Hash diferente e época diferente recusados sem consumo | Não publica revisão nem migra snapshot. |
| Troca de região | `forest` recusada para snapshot `village`; contexto original ainda pode recuperar | Não teletransporta/converte região. |
| Queda pré-commit | Falha injetada antes de publicar, zero ativos, retry íntegro | Exceção no mesmo processo, não crash físico. |
| Queda pós-commit | Resposta perdida simulada, um ativo, recibo por operação, ticket consumido | Não prova durabilidade/reconciliação de peer. |
| Recompensas recebidas | Harness 196 aplica recompensa real builtin de missão e baú; snapshot passa pela recuperação; replay real não duplica | Objetivo pronto do harness é preparação declarada; não simula morte TCP nova. |
| Cooldown ativo | Deadline 900 → 750 ticks restantes ao recuperar no 150; ataque ainda com 50; energia/HP não recarregam | Deadlines são modelo, não importação em `RpgSkillEngine`. |
| Missão parcial | Progresso builtin 1/2 e autoria sintética `[1,0]` conservados; mapas/listas imutáveis | Autoria é DTO; não restaura `AuthoredQuestSession` real. |
| Intangibilidade antiga | Fase antiga, quatro projéteis, oito comandos e duas animações desaparecem dos transitórios do novo handle | Não é uma execução real de Quatro relâmpagos retomada. |
| Admissão antiga | Handle e ticket de geração anterior recusados; nova geração preservada | Não recicla IDs de peers. |
| Schema e campos | Schema desconhecido e cooldown omitido recusados, sem default zero | Contrato limitado e versionado, não schema definitivo. |
| Capacidade/restart | Terceiro registro recusado; nova instância recusa credencial/ticket antigos | Envelope não persiste e restart não recupera progresso. |

A fixture de nível/domínio/equipamento usa APIs reais de `RpgWorld`. XP para nível 15, moedas 45 e progresso parcial são preparação explícita, não ganhos de combate demonstrados. `RpgSave` e a geometria reais validam o subconjunto builtin; energia/atributos/autoria/deadlines extras são valores de teste no envelope. Não se apresenta igualdade desse DTO como restauração de todos os campos internos do runtime.

O caso de recompensa usa `SyntheticSessionLaboratory` apenas em `@TempDir/online-synthetic-r3`: commit de snapshot real, revisão 2, XP 80, moedas 45, cinco poções e uma espada, com replays de baú/missão recusados pelo harness 196. Nenhum arquivo pessoal é aberto. Esse caso reaproveita a prova existente, sem prometer ledger multiplayer ou exactly-once distribuído.

## TCP e A-04

R2 TCP foi repetida: saída equivalente e espontânea, observadores 6/7, fila antiga, remoção visual, spawn recusado e readmissão efêmera. Nenhuma dessas provas passou a ser chamada de recuperação R3.

O cenário 15 da ordem é condicional à viabilidade das interfaces/isolamento. A recuperação R3 **não foi conectada a dois TCP**, pois faltam prova de recuperação em 6/7 e importador completo de personagem/cooldowns/autoria. Implementar side channel ou substituir sessões por reflexão esconderia justamente essas lacunas e não atenderia ao contrato de recuperação autenticada. Proposta de capacidade separada na ADR; nenhum teste existente foi desabilitado para esse limite.

O modelo R2 31/32/91/92/93 permanece na regressão. A R3 só descreve captura antes de cancelamento e continuidade após retirada. Não integra prazo no engine nem encerra automaticamente participação real.

## Reprodução

Na raiz:

```powershell
python .task-checkpoints/CORUJAO-IX/TASK-225-R3/run-maven.py lab-first `
  '-Djava.awt.headless=true' '-Dtest=Task225R3RecoveryLabTest,Task194PersistenceTest,Task195SessionOwnershipTest,Task196ResumeTest'
python .task-checkpoints/CORUJAO-IX/TASK-225-R3/run-maven.py lab-repeat `
  '-Djava.awt.headless=true' '-Dtest=Task225R3RecoveryLabTest,Task225R2*,Task225R1MaterializationTest,Task225R1PhaseObserversTest'
python .task-checkpoints/CORUJAO-IX/TASK-225-R3/run-maven.py full-headless '-Djava.awt.headless=true'
python .task-checkpoints/CORUJAO-IX/TASK-225-R3/review-publish-tree.py
$r3GuiCases = (Get-Content -Encoding UTF8 .task-checkpoints/CORUJAO-IX/TASK-225-R3/gui-tests.txt -Raw).Trim()
python .task-checkpoints/CORUJAO-IX/TASK-225-R3/run-maven.py gui '-Djava.awt.headless=false' "-Dtest=$r3GuiCases"
```

Os wrappers executam `mvn.cmd test -o` em `server/`, com cache local `.maven-repository` e logs de erro dentro do checkpoint. Limite externo de 600 segundos, encerrando a árvore do processo se necessário. Concorrência de teste tem barreiras de três segundos e `Future.get` de cinco segundos. Nenhum loop infinito, instalação ou Maven concorrente.

Após o commit, o teste novo pode ser reproduzido em `server/` com `mvn test -Dtest=Task225R3RecoveryLabTest` no ambiente Maven já configurado. Neste ambiente o comando foi executado com `-o` e `-Dmaven.repo.local` explícito pelo wrapper. O recorte para Git foi validado também sobre o HEAD limpo: não depende de classes locais não versionadas de skills ou HUD.

## Limitações de homologação

Não executados: recuperação autenticada por rede, processos concorrentes disputando o mesmo ticket R3, persistência/crash real do envelope, hardware/Radmin físicos, launcher distribuído, carga sustentada ou validação estética humana. Os processos separados da TASK-196 testam seu repositório/harness, não durabilidade da R3. A-04 continua aberto.
