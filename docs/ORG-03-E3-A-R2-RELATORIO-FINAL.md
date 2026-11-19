# ORG-03-E3-A-R2 — Relatório final

Responsável: Lucas, confirmado explicitamente nesta conversa em 15/11/2026.
Run-id: `20261115-lucas-e3a-r2-8c71d4a902`.
**E3-A NÃO APROVADO: 1 PASSOU, 1 FALHOU no harness, 7 NÃO EXECUTADOS.**

## Resultado C01–C09

| Caso | Resultado | Evidência e limite |
| --- | --- | --- |
| C01 — Antes do commit | PASSOU | BEGIN sem COMMIT; backend 43164 encerrado; worker novo 32116 consulta C01-1 e recebe NOT_FOUND; imagens integrais antes/depois iguais. |
| C02 — Resposta perdida | FALHOU — HARNESS | PAUSE antes de COMMIT; observador confirmou C02-2; 18 bytes retidos; reset 10054 no relay, exit 1; sem novo lookup/retry. |
| C03 — Crash pré-confirmação | NÃO EXECUTADO | Rodada interrompida em C02; zero immediate. |
| C04 — Crash pós-confirmação | NÃO EXECUTADO | Nenhum crash/recovery. |
| C05 — Resultado incerto | NÃO EXECUTADO | Evidência parcial C02 não substitui este caso. |
| C06 — Beneficiários A/B | NÃO EXECUTADO | Sem reconstrução de manifesto/coordenador. |
| C07 — Obrigação antiga | NÃO EXECUTADO | Sem prova de watermark/obrigação após restart. |
| C08 — Arquivo/histórico | NÃO EXECUTADO | Sem ensaio de arquivo indisponível/restaurado. |
| C09 — Encerramento pendente | NÃO EXECUTADO | Nenhum dos três subcasos específicos executado. |

[Resultados brutos](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/results.json),
[diagnóstico completo](ORG-03-E3-A-R2-DIAGNOSTICO.md),
[isolamento e descarte](ORG-03-E3-A-R2-ISOLAMENTO.md).

## Execução e consumo

Uma única chamada operacional, encerrada com código 1:

```powershell
python -B temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/scripts/tests.py
```

Um cluster PostgreSQL 18.6 criado com binários existentes; quatro workers
totais, dois simultâneos observados, uma instância de relay; duas intenções,
uma tentativa cada; zero crashes e zero reinícios. Duração total medida:
30,041 s; C01: 2,679 s. Nenhum cenário atingiu seu limite de 120 s.
Pico medido da árvore: 42.797.893 bytes, abaixo de 256 MiB. Evidências finais
abaixo de 1 MiB, conforme validação de entrega, abaixo de 32 MiB.
Espaço livre inicial: 127.303.045.120 bytes, acima de 2 GiB.

Os limites eram tetos: não se gastaram os crashes/reinícios restantes após
a falha. Não houve segundo initdb, retirada de claim, fallback ou nova rodada.

## O que foi comprovado

A integração passou nos 23 testes puros antes da execução. SHA-256 final
confere as fontes executadas com as revisadas e os 4.023 arquivos anteriores,
sem diferenças. Há 49 eventos sequenciais e seis snapshots com hashes válidos.
C01 comprovou ausência de efeitos/recibos/índices parciais após fim do backend,
nos quatro personagens do owner 1 exportados pelo modelo sintético.

A R1 classificou a saída real do relay como EXITED_CONFIRMED, inclusive
quando o primeiro poll estava ativo e o poll após CIM retornou 1. Nenhum
terminate foi enviado ao relay já encerrado. Essa observação valida esse
caminho do lifecycle nesta rodada; não homologa toda a R1 nem aprova C02.

O cluster foi reidentificado, parado em fast e descartado com as provas
operacionais de ausência. 5432 continuou com os mesmos listeners/PID 3700.
Os dois arquivos de diagnóstico de encerramento são preservados: o nome
`shutdown-impediment.json` registra a falha do relay relançada depois do
descarte seguro; não invalida o `shutdown.json` que comprova a parada.

## Limites das conclusões

Não há prova de crash-recovery, sobrevivência de confirmado a crash, RPO/RTO,
perda física, restore/failover ou D33. Configurações fsync/synchronous_commit/
full_page_writes em on não substituem os cenários C03/C04.

Não foram alcançados os controles transversais finais de fence obsoleto,
CAS/snapshot antigo, histórico, testes negativos de privilégio nem a consulta
SQL global de integridade final. Não declarar zero órfãos globais ao término.
O stderr do relay ficou incompleto por UnicodeDecodeError; o evento TCP
estruturado preservou tipo/errno/origem/barreira da falha primária.

Sem alteração em Java/POM, produção, gameplay, saves, contas, configurações
existentes ou ferramentas anteriores; sem conexão a 5432, instalação,
commit, push, publicação, E3-B/E4/E5, A-04, ORG-04 ou TASK-226.
O manifesto de preservação cobre os 4.023 arquivos listados, não todos os
arquivos pessoais ou dados vivos do serviço 5432.

## Entrega e pendências

Evidências, scripts, claim, aceite e hashes permanecem no run-id encerrado.
[Validação de entrega](../temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/evidence/delivery-validation.json)
comprova preservação e descarte; seu sucesso não significa gate aprovado.

Próximo passo apenas sugerido: correção separada do tratamento de reset
esperado e da captura de encoding, com testes e revisão antes de propor
outro ensaio. Não foi implementada nem iniciada nova rodada. As fontes
operacionais permanecem exatamente como executadas. Não reutilizar este run-id.

Reversão: não há banco ativo a reverter. Conservar evidências; eventual
retirada das adições R2 exige revisão para preservar trabalho posterior.
Não alterar os relatórios E2/E3/R1 nem usar reset/clean.
