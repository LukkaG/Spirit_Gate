# ORG-03-E3-A-R3 — Relatório final operacional

Responsável: **Lucas**. Data: **15/11/2026**. Branch `beco`, HEAD
`c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`, sem alteração.
Run-id: `20261115-lucas-e3a-r3-9b62f13a70`.

**PARECER TÉCNICO: APROVADO no escopo do laboratório sintético ORG-03-E3-A-R3.**
**C01–C09: 9 PASSOU, 0 FALHOU, 0 INCONCLUSIVO, 0 NÃO EXECUTADO.**
Controles finais, integridade global, preservação e descarte comprovados.
Nenhum gate posterior foi iniciado.

## Execução autorizada e resultado por caso

R3-02–05 foram recebidos explicitamente do responsável. Registro operacional
verdadeiro em `<run>/operational-authorization.json`, separado de R3-01;
texto recebido e hash em `operational-preflight/authorization-source.txt`.
Os documentos da preparação com aceites pendentes permanecem preservados como
histórico. O aceite e este resultado são os registros posteriores da operação.

Uma única chamada ao runner revisado, sem alterações de suas fontes:

```powershell
python -B temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/scripts/runner.py
```

Foi iniciada pelo launcher próprio, com stdout/stderr integral do controlador
em `controller-output.txt`. Controlador PID **12924**, saída **0**. Intervalo
observado: **13:59:32–14:01:51, horário de São Paulo**. Duração interna total
**139,104 s**. O claim exclusivo foi consumido uma vez e permanece preservado.

| Caso | Resultado / duração | Prova efetiva |
| --- | --- | --- |
| [C01](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C01.json) — antes do commit | PASSOU / 3,015 s | BEGIN aplicado sem COMMIT; cliente identificado encerrado; backend ausente; novo worker consulta C01-1 e recebe NOT_FOUND; imagem integral anterior igual à posterior. |
| [C02](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C02.json) — resposta perdida | PASSOU / 9,136 s | PAUSE antes de COMMIT; observador CONFIRMED; 18 bytes retidos; permit/ACK e terminate identificados; reset esperado; relay exit 0; novo worker 19204 consulta C02-2 e repete a intenção sem alterar a imagem pós-commit. |
| [C03](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C03.json) — crash antes da confirmação | PASSOU / 13,720 s | Barreira sem envio de COMMIT para C03-3; immediate próprio; recovery com redo; novo worker recebe NOT_FOUND; imagem completa igual e confirmados anteriores preservados. |
| [C04](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C04.json) — crash após confirmação | PASSOU / 13,791 s | ACK + observador confirmam C04-4; flush LSN registrado; immediate; recovery real; mesmo recibo/cabeça/recursos e retry sem novo efeito. |
| [C05](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C05.json) — resultado incerto | PASSOU / 9,676 s | Sem consulta econômica pré-corte; ECONOMIC_UNCERTAIN durável; resposta retida/reset previsto; backend encerrado; novo worker 51984 reconcilia C05-5 como CONFIRMED e retry conserva efeitos únicos. |
| [C06](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C06.json) — beneficiários A/B | PASSOU / 16,228 s | Manifesto fixo persistido; A confirmado antes de B; novo coordenador após restart limpo identifica apenas C06-B-7 pendente; A inalterado e B liquidado uma vez. |
| [C07](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C07.json) — obrigação antiga | PASSOU / 16,398 s | Obrigação nova paga com antiga pendente; prefixo permanece 0; após restart limpo, C07-old é reencontrada e liquidada com os mesmos IDs; prefixo vai a 2; retries não duplicam. |
| [C08](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C08.json) — arquivo/histórico | PASSOU / 7,299 s | Arquivo próprio com fsync/hash; novo leitor; renomeação reversível provoca UNCERTAIN/ARCHIVE_UNAVAILABLE; retry devolve ARCHIVED sem premiar; restauração recupera recibo e conserva imagem integral. |
| [C09](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/C09.json) — encerramento pendente | PASSOU / 11,494 s | PREPARED → NOT_CONFIRMED; COMMIT_ACK → CONFIRMED; COMMIT_DISPATCHED sem ACK consumido → ECONOMIC_UNCERTAIN, depois CONFIRMED em novo worker; mesmas intenções e imagens verificadas, sem terceiro relay. |

Resultados brutos completos:
[results.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/results.json).
Há **306 eventos sequenciais**, **58 snapshots completos com hash válido** e
**450 registros no transcript SQL**. A auditoria releu os snapshots e conferiu
versões, cabeças, recursos e bijeção recibos/operações/grants, inclusive recibo
arquivado com hash e conteúdo histórico.

## C02 e transporte

Cliente original **21332**, relay próprio **23728** conforme manifesto;
o novo worker identificado foi **19204**. O estado do transporte conserva
ECONOMIC_UNCERTAIN/NOT_EVALUATED e não é convertido sozinho em sucesso SQL.
A aprovação econômica aparece somente no resultado posterior à reconciliação.

Nas duas conexões C02/C05: **1.372 bytes encaminhados**, **1.390 recebidos** e
**18 retidos**, com encaminhamento imutável depois de PAUSE. Ambos os resets
foram client/recv_client/10054, após pedido de encerramento, permit e ACK.
Os dois relays terminaram com **código 0**, reconhecidos pelos handles originais.
Captura de stderr completa, sem erro de leitura/decodificação ou truncamento.
Nenhum mismatch/UNKNOWN, erro de protocolo ou fallback foi registrado.

## Recuperação e controles finais

Foram executados **dois immediate**, exclusivamente em C03/C04, e **quatro
reinícios no mesmo data**: dois crash-recoveries e dois controles limpos em
C06/C07. Logs contêm recuperação automática, início/fim de redo, checkpoint de
fim de recuperação e pronto para conexões. Nenhuma recriação ou reparo por initdb.
[Análise e cronologia dos recoveries](ORG-03-E3-A-R3-CRASH-RECOVERY.md).

[controls.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/controls.json)
comprova:

- Intenção inédita com autoridade antiga recusada em **P0003**, sem efeito.
- Operação positiva com generation/fence atuais **2/2**.
- Checkpoint com revisões antigas retorna **CONFLICT**, sem sobrescrever a cabeça.
- Retry histórico de **C02-2** devolve o recibo antigo e conserva o estado atual.
- Login sem superuser/createdb/createrole/replication; leitura direta da tabela
  e acesso ao personagem de outro owner recusados em **42501**.

SQL global final: **11 recibos = 11 operações = 11 grants = revisão econômica
total 11**; **zero operações órfãs, grants órfãos, cabeças órfãs e transações
pendentes**. Cabeças 1–4: sr/er **8/8, 1/1, 1/1, 1/1**. Personagens 5–8 do
segundo owner permanecem em sr/er 0/0, generation/fence 1/1.
[final-integrity.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/final-integrity.json).

## Consumo dos limites

| Recurso | Consumido / observado | Limite |
| --- | --- | --- |
| Cluster / initdb | 1 / 1 | 1 / 1 |
| Immediate / reinícios | 2 / 4 | 2 / 4 |
| Personagens / proprietários | 8 sintéticos / 2 | 8 / 2 |
| Intenções / tentativas totais | 15 / 26 | 64 intenções |
| Tentativas por intenção | Máximo 3, em C02-2, incluindo retry histórico final | 3 |
| Payload | Todos os arquivos de intenção abaixo de 16 KiB | 16 KiB |
| Workers / relay totais | 28 / 2 | 32 / 2 |
| Simultâneos | Fluxo com 2 workers; 1 relay | 3 workers / 1 relay |
| SQL | 2 sessões persistentes; identidade transitória até mais 1; bootstrap separado | 4 conexões |
| Prazo total / maior caso | 139,104 s / 16,398 s | 1.200 s / 120 s |
| Pico medido da árvore | 45.130.189 bytes, cerca de 43,04 MiB | 256 MiB |
| Logs/evidências, incluindo preparação | 3.954.692 bytes após fechamento documental, cerca de 3,77 MiB | 32 MiB |
| Espaço livre inicial | 126.023.036.928 bytes | Mínimo 2 GiB |
| Capturas stderr completas | 30 streams; nenhum erro/truncamento | Prefixo/texto 64 KiB |
| TCP retido | 18 bytes em cada relay | 16 KiB |

O limite SQL é sustentado pela estrutura do fluxo e pelas guardas de sessões;
não houve amostragem contínua independente de todos os backends. Limites
stdout/filas foram aplicados sem violação registrada; seus picos internos não
foram persistidos. Medição periódica de disco não é quota física de filesystem.

## Falhas intermediárias e diagnóstico

Nenhum cenário obrigatório falhou, ficou inconclusivo ou foi repetido para
obter aprovação. Interrupções/desconexões deliberadas e controles negativos
foram mantidos integralmente nas evidências:

- C01/C09-PREPARED: fim do cliente com transação aberta e rollback visível.
- C02/C05: reset 10054 previsto; resposta retida e UNCERTAIN conservado até lookup.
- C03/C04: queda abrupta autorizada e recuperação comprovada.
- C08: indisponibilidade reversível do próprio arquivo, sem autorização de premiar.
- P0003/42501: recusas esperadas dos controles finais.

Warnings de ROLLBACK sem transação correspondem ao fechamento de sessões já
em autocommit. Os logs também retêm diagnósticos de limite de WAL nos recoveries;
sua análise, incluindo limites da inferência, está no documento de recovery.
Não houve PANIC/FATAL nem erro de captura. Saída 1 de background worker na
parada fast está registrada no contexto da parada, sem alteração do resultado
comprovado pelos recibos, integridade e término do cluster.

## Encerramento, preservação e parecer

Parada fast final às **14:01:34**, após identidade completa. Status 3, ausência
de postmaster.pid, processos/filhos/handles/listeners foram comprovados antes
de remover somente `data` e `secrets`. Sem erros de cleanup, recursos próprios
remanescentes ou listeners em 55435/55436 na auditoria posterior.
[shutdown.json](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/shutdown.json).

**4.147 arquivos anteriores preservados por SHA-256**, 43 artefatos da preparação
intactos e todas as fontes executadas iguais às revisadas. E2/E3/R1/R2/P1 e os
resultados anteriores permanecem historicamente inalterados. Nenhuma fonte do
pacote operacional foi corrigida durante ou depois desta execução.
[Validação independente da entrega](../temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70/evidence/delivery-validation.json),
[isolamento](ORG-03-E3-A-R3-ISOLAMENTO.md).

Scripts, logs, arquivos sintéticos arquivados, snapshots, autorizações, claim e
hashes permanecem preservados. Não houve conexão, consulta ou controle dirigido
à 5432; não houve alteração do jogo/produção, saves, contas, credenciais antigas,
Java/POM, protocolos, HUD, launcher, World Studio ou sprites. Sem instalação,
reset/clean, commit/push, publicação ou execução de E3-B/E4/E5/ORG-04/A-04/TASK-226.

**APROVADO para os critérios e limites experimentais E3-A executados nesta R3.**
A conclusão cobre o modelo sintético e duas interrupções de processos próprias.
Não homologa perda elétrica/física, cache de hardware, restore/failover, D33,
RPO/RTO de produção, recuperação de personagens reais ou o servidor do jogo.
Qualquer gate posterior depende de instrução própria. Este run-id está consumido
e não deve ser reutilizado.
