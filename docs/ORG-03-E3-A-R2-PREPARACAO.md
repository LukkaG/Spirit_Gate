# ORG-03-E3-A-R2 — Preparação controlada

Data: 15/11/2026. Branch `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
R2-01–05 recebidos explicitamente nesta conversa. O campo Responsável veio
como `[preencher]`; Lucas foi confirmado posteriormente pelo usuário com
“Sim, registrar Lucas”. A rodada foi então executada uma única vez;
resultado no [relatório final](ORG-03-E3-A-R2-RELATORIO-FINAL.md).

Run-id aprovado na proposta e reservado nesta preparação:
`20261115-lucas-e3a-r2-8c71d4a902`.
Caminho absoluto:
`C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902`.
Data/secrets são filhos diretos deste caminho. Nenhum cluster foi criado
durante a preparação. Na execução posterior foram criados uma única vez e
descartados após comprovação; execution.claim e aceite nominal foram preservados.

## Integração revisada

As fontes runtime/schema/runner são cópias da rodada anterior; Lab/Relay,
lifecycle e os 23 testes vêm das cópias R1. As fontes anteriores não foram
executadas nem alteradas. O diff integral está em `evidence/integration.diff`.

- O CLI do novo relay valida run-id, aceite e claim; usa bind exclusivo em
  127.0.0.1:55434 e upstream fixo 127.0.0.1:55433, chamando pump R1.
- Mantidas as quatro classificações de identidade e todos os bloqueios R1.
  O stderr é drenado, limitado e sanitizado. Os dois pipes são drenados antes
  da avaliação final; saída não zero ou zero inesperada continuam falhas.
- Eventos possuem lock para preservar sequência/append entre threads.
- Cada snapshot completo é persistido com fsync antes do evento com hash.
  Falha intermediária mantém evidências mesmo sem retorno do cenário.
- Limite de duas instâncias de relay verificado antes do spawn. C09 possui
  subcaso próprio sem terceiro relay: COMMIT_DISPATCHED sem consumir ACK,
  término do worker e backend, novo worker e reconciliação integral.
  Não é usado como substituto da retenção TCP obrigatória em C02/C05.
- Workers limitados antes do spawn. Até três workers e uma sessão de
  identidade; bootstrap continua separado, sem sessões normais abertas.
- Bind exclusivo revalidado em 55433; ACL efetiva de secrets conferida por
  SID, herança bloqueada e ausência de outros destinatários antes de segredos.
- Uma falha diagnóstica do relay é conservada enquanto o cluster próprio
  segue sua parada normal fast. Descarte continua exigindo todos os handles,
  PIDs e listeners ausentes e nenhuma identidade bloqueada. Não há kill extra.
- Runner exige validação e hashes revisados antes de consumir execution.claim.

## Limites preservados

Um initdb, dois immediate, quatro reinícios no mesmo data, oito personagens,
dois proprietários, 64 intenções, três tentativas/intenção, payload 16 KiB,
32 workers totais, três simultâneos, quatro conexões SQL, dois relays totais,
um simultâneo, 120 segundos/caso, 1.200 segundos/rodada, 256 MiB incluindo
32 MiB de evidências, mínimo inicial de 2 GiB livres.
Stop 20 s, start/recovery 45 s, CIM 8 s e wait de término 5 s, sem fallback.
Medição periódica não equivale a quota física do filesystem.

## Verificação e estado

`python -B tools/org03_e3a_r2/validate.py`: sintaxe, preservação por SHA-256,
diff e 23 testes puros, sem SQL/socket/processos fixture. Resultado em
`evidence/preparation-validation.json`; saída em `evidence/test-output.txt`.
Revisão manual do diff: listener/upstream, lifecycle, guardas, quotas,
barreiras de C01–C09, consulta/retry, limpeza e ausência de alterações SQL.
`lifecycle.py` e `schema.sql` idênticos às respectivas fontes R1/anterior.

A inspeção TCP preliminar encontrou 5432 no PID 3700 e nenhum listener em
55433/55434. Isso não substitui o preflight imediatamente antes da execução.
Sem conexão a 5432 e sem comando dirigido ao seu processo.

Estado ao término da preparação: C01–C09 NÃO EXECUTADOS. Após confirmação
nominal: C01 PASSOU, C02 FALHOU, C03–C09 NÃO EXECUTADOS; gate não aprovado.
Crash/recovery e integridade SQL final não possuem prova R2; o descarte
operacional posterior foi comprovado, conforme relatório e isolamento.
Não houve commit, push, instalação, produção/gameplay nem gate posterior.

Referência técnica consultada: [pg_ctl 18](https://www.postgresql.org/docs/18/app-pg-ctl.html).
O ensaio exigirá logs reais de redo, além de immediate e novo start, para
declarar crash-recovery; fast/clean restart será classificado separadamente.

Reversão da preparação: retirar apenas as adições R2 após revisão, preservando
qualquer execução posterior. Não usar reset/clean nem tocar nos laboratórios antigos.
