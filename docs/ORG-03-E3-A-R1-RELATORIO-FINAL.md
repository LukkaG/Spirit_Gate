# ORG-03-E3-A-R1 — Relatório final

## Resultado

Em 15/11/2026, correção controlada nas cópias concluída no recorte de lógica
simulada: **23 testes passaram**. Nenhum cluster, SQL, crash ou processo real
de fixture foi executado. Não se reexecutou E2 nem E3-A.

A janela entre poll e CIM existe no código anterior e está coberta por teste;
a causa específica do exit 1/retorno CIM original continua desconhecida,
porque esses diagnósticos não foram gravados naquela execução.

## Mudanças e evidências

Cinco novos documentos R1 em docs, cópias `lab_runtime.py`/`relay.py`, módulo
`lifecycle.py`, testes, diffs e manifestos em `tools/org03_e3a_r1/`.
O registro local adicional fica em `logs/analises/20261115-org03-e3a-r1.md`.

O fluxo diferencia RUNNING_MATCH, EXITED_CONFIRMED, IDENTITY_MISMATCH e UNKNOWN;
saída confirmada evita terminate, divergência/ausência inconclusiva bloqueiam.
São preservados identidade esperada/observada, polls, exit, barreira e stderr.
Falhas de socket são diagnosticadas e relançadas. Saída não zero ou saída zero
inesperada do relay não vira sucesso de C02.

Comando e cobertura: [testes](ORG-03-E3-A-R1-TESTES-HARNESS.md).
SHA-256 antes/depois, conferência dos originais E3 e manifesto E2, sintaxe e
resultado estruturado: [validation.json](../tools/org03_e3a_r1/validation.json).
Hashes das novas fontes e documentos: `tools/org03_e3a_r1/artifact-sha256.json`.

## Limites e preservação

Os originais são preservados por manifesto de 4.005 arquivos. A validação final
registra eventuais diferenças e só é considerada aprovada se não houver nenhuma.
Não houve mudança em Java/POM, sistemas do jogo, protocolos, skills, saves,
contas, offline/Coop, World Studio, launcher ou A-04; sem instalação, acesso
a 5432, commit, push ou publicação.

O pacote não é runner operacional: não contém runtime/SQL e o relay recusa
ativação por CLI. Integração do listener e parâmetros da nova rodada ainda
precisam de revisão no pacote autorizado. CIM/handles reais, concorrência de
threads em execução longa, rede e reconciliação econômica não foram homologados
pelos mocks. Sanitização é limitada aos padrões documentados e eventos do relay
nunca devem transportar payload. Não há afirmação de recovery, RPO/RTO ou D33.

## Estado dos gates e próximo passo

**E3-A anterior inalterado: C01 PASSOU; C02 FALHOU no harness;
C03–C09 NÃO EXECUTADOS. E3-A não aprovado.**

Pacote de novo aceite: [proposta de reexecução](ORG-03-E3-A-R1-PROPOSTA-REEXECUCAO.md).
Os aceites antigos não autorizam novo cluster. Revisão/aceite separado é
necessário antes da rodada. Nenhum gate posterior foi iniciado.

Reversão segura: retirar somente adições R1 após revisão, conservando evidências
e alterações posteriores. Não há alteração de banco a reverter; sem reset/clean.
