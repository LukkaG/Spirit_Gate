# ORG-03-E3-A-R2-P1 — Análise do transporte

Lucas, 15/11/2026. Correção isolada, sem execução econômica.

## Evidência de origem

Na R2, eventos 29–34 comprovam PAUSE, COMMIT, observador CONFIRMED e 18 bytes
retidos, com 1.372 encaminhados constantes. Eventos 35–39 comprovam identidade,
pedido de terminate do worker, reset client/10054 e saída do cliente. Eventos
40–43 mostram erro de decodificação e EXITED_CONFIRMED do relay com código 1.
O lifecycle R1 deixou de confundir a saída com mismatch, mas o pump relançou
todo reset e o adaptador abortou antes de novo worker/lookup/retry.

## Precondições para EXPECTED_CLIENT_DISCONNECT

O permit transporta cópias dos metadados e hash canônico. O contexto do pump
é criado com a conexão efetivamente aceita. Validações cumulativas:

1. Cenário C02/C05 ou a fixture explícita; fase COMMIT_DISPATCHED e chamador
   ECONOMIC_UNCERTAIN. Ausência ou fase diferente recusa armamento.
2. Identidade completa do cliente; observação RUNNING_MATCH com esperado/observado
   iguais, polls ativos e erro ausente; vínculo PID/pai/controlador/handle.
3. Endpoint aceito e connection_id coincidentes. O adaptador deve conferir
   OwningProcess do socket via SO; a fixture faz essa consulta efetivamente.
4. PAUSE < COMMIT < STATS < observação de identidade < PROCESS_TERMINATE.
   No C02, a confirmação do observador fica entre COMMIT e STATS.
5. PROCESS_TERMINATE deve nomear a mesma identidade. A função de guarda R1
   emite esse evento imediatamente antes do terminate; o adaptador registra o
   evento, envia o permit por stdin privado e exige ACK/hash do pump.
6. PAUSE e STATS pausados; contadores inteiros não negativos e coerentes;
   zero retidos no PAUSE, resposta retida positiva em STATS, forwarded imutável.
7. No erro, recv_client da mesma conexão; EOF ou ConnectionResetError com errno
   10054 (Windows) / 104 (compatibilidade explicitamente codificada). Número
   sozinho não autoriza. Retidos podem crescer; received=forwarded+held e
   forwarded permanece no valor do PAUSE.

O permit é de uso único e copiado; alteração posterior do dicionário do
controlador não muda o pump. O hash correlaciona ACK e evento final, não é
assinatura de segurança contra outro processo com controle do próprio usuário.
O canal stdin privado e a identidade/socket verificados compõem a fronteira.

Se o cliente já saiu, a guarda não emite novo terminate; não há armamento
retroativo nem conversão de um reset anterior em evento esperado. Divergência
CIM/identidade continua bloqueando antes do armamento. Falha de ACK interrompe
a ação antes de terminate; o cenário deve falhar, preservando a evidência.

## Caminhos do pump revisados

| Operação | Classificação |
| --- | --- |
| recv_client com EOF/reset e permit válido | EXPECTED_CLIENT_DISCONNECT; retorno normal do pump. |
| recv_client sem permit ou com fase/contador divergente | Falha; sem aprovação econômica. |
| recv_server, EOF/reset do upstream | Falha, origem server. |
| send_server | Falha atribuída ao servidor, mesmo quando o último recv foi do cliente. |
| send_client | Falha atribuída ao cliente/operação send_client; não equivale a recv_client previsto. |
| ConnectionAbortedError, timeout, select/erro inesperado | Diagnóstico estruturado e exceção; sem catch/pass. |
| Comando desconhecido ou armamento repetido | Falha de protocolo; nenhum fallback. |
| Buffer retido acima de 16 KiB | Recusa antes de acrescentar bytes ao buffer. |

São registrados origem, operação, tipo, errno, pausa, contadores nas duas
direções, pedido de encerramento e hash do permit. Não se registra conteúdo
TCP. Bytes retidos são descartados somente no fechamento da conexão já
classificada, após seu registro de contadores. O estado UNCERTAIN do chamador
é persistido antes de observar/encerrar; não é removido pelo evento esperado.

## Quatro resultados distintos

`TRANSPORT_EXPECTED_DISCONNECT` conclui a etapa TCP. `RELAY_EXITED_CONFIRMED`
exige o handle original encerrado e código zero. O adaptador exige evento final
coincidente com permit, socket/operação e contadores; zero espontâneo não basta.
Saída não zero falha. Captura incompleta falha. Close bem sucedido repetido
retorna o mesmo resultado, sem novo terminate ou inspeção destrutiva.

Esses resultados mantêm `sql_result=ECONOMIC_UNCERTAIN` e
`economic_test=NOT_EVALUATED`. Apenas `complete_c02` pode produzir
SCENARIO_PASSED: worker realmente novo/guardado, backend antigo encerrado,
mesma intenção/OperationId, vínculo do transporte ao cenário C02/identidade
do cliente/OperationId (fixture não serve), recibo histórico igual ao observado e ao retry,
imagem integral pós-commit idêntica à imagem após retry. O adaptador preparado
também exige incremento econômico único em relação à base anterior.
As entradas SQL continuam sendo responsabilidade dos oráculos do runner futuro;
os testes com mocks não fornecem evidência de transação real.

## Stderr binário e privacidade

Pipe binário, leitura em blocos de 4.096 bytes, prefixo transitório de até
65.536 bytes em memória. O consumo continua além do prefixo: conta e calcula
SHA-256 de todos os bytes observados. Hash com erro de leitura tem escopo
explicitamente parcial (ALL_OBSERVED_BYTES), não do fluxo que não foi lido.
Prefixo bruto nunca é escrito em disco.

Decoder incremental UTF-8 com surrogateescape identifica cada byte inválido;
a visualização substitui esses bytes por U+FFFD e registra invalid_utf8_bytes,
decode_replacements e text_lossy. U+FFFD válido não conta como erro. Sequências
divididas entre blocos são preservadas. Nenhuma suposição Windows-1252.

A persistência textual aceita apenas event/error/errno de diagnósticos JSON
conhecidos. Campos adicionais, linhas livres, traceback, URI, credenciais e
payload SQL são omitidos. Essa projeção é deliberadamente mais restritiva que
uma regex genérica: preserva causa/tipo/errno sem prometer sanitizar texto
arbitrário mantendo seu conteúdo. Fragmento final incompleto é omitido.
O texto projetado tem teto de 65.536 bytes e expansão controlada antes do append;
blocos/prefixo/texto têm limites fixos, além de overhead limitado do Python.
Não se alega quota física de memória do processo.

EOF, read_error, bytes lidos/retidos, hash, perda de representação, truncamento
bruto e textual são separados. `truncated=false` pode coexistir com read_error,
mas `capture_complete` será false e validate recusará conclusão. Erro da thread
fica em estado compartilhado e é propagado ao controlador via validate;
não há exceção de leitura que apenas suma da thread. Truncamento também falha.
UTF-8 inválido com leitura completa é tolerado e explicitamente marcado.

## Limites e falhas intermediárias

A primeira fixture falhou ao enviar pause sem serialização JSON enquanto o
leitor esperava JSON. Houve timeout no ACK; cleanup identificou os dois peers,
sem listener remanescente. O erro e os 15 eventos foram preservados no diretório
`fixture-20261115-af4bf39919a7`. Corrigido o contrato de envio por papel e o
erro do leitor de controle passou a ser encaminhado ao pump. Isso não alterou
o código R2 nem permitiu aprovar aquela tentativa.

A primeira validação combinada de 69 testes conservou um teste falho: o caso
de forwarded divergente também tinha PAUSE internamente incoerente e foi
recusado por uma guarda anterior. Ajustada apenas a fixture do teste para
isolar forwarded divergente; guardas mantidas. A validação final tem 71 testes
passando, incluindo casos adicionais para expansão textual sanitizada e
recusa de converter transporte da fixture em aprovação de C02.

Fixture atual comprovou reset real esperado/inesperado e cleanup. Não testa
PostgreSQL, rollback, commit, idempotência econômica, crash/recovery, Radmin
ou toda a variedade de falhas do Windows. R3 requer nova autorização.
