# ORG-03-R6 — Matriz verificável de falhas futuras

## Estado e fases

**BLOQUEADA POR EVIDÊNCIA — D21/D28/D29/D33.** Todos os cenários abaixo são
testes futuros **não executados na R6**. Existirem numa tabela não os aprova.
R5/R4 são referências limitadas de laboratório, nunca substitutos da evidência
pedida na coluna correspondente.

Etapas: E2 = PostgreSQL descartável autorizado; E3 = processos/restart/failover
isolados; E4 = integração owner/transporte autorizada. Aprovações e isolamento
estão em [gates](ORG-03-R6-GATES-DE-IMPLEMENTACAO.md). Políticas pendentes devem
ser aprovadas ou comparadas explicitamente como candidatas, sem ativá-las no jogo.

| ID | Cenário/injeção | Comportamento esperado do contrato proposto | Evidência necessária | Etapa | Decisões |
| --- | --- | --- | --- | --- | --- |
| F01 | Duas gravações distintas na mesma revisão do personagem | Uma confirma; outra conflita, sem sobrepor saldo/estado | Duas conexões SQL com barreira, revisões antes/depois, recibos e imagem integral | E2 | D09/D11 |
| F02 | Dois OperationId para a mesma concessão | Uma concessão e nenhum crédito extra | Unicidade de origem/GrantId, contagem de efeitos e conflito da segunda | E2/E3 | D10/D26 |
| F03 | Mesmo OperationId com payload/kind/beneficiário divergente | Conflito e proteção de dados; recibo original imutável | Comando original/divergente, autorização e ledger após rollback | E2/E3 | D03/D31 |
| F04 | Resposta perdida após commit | UNCERTAIN no chamador; lookup autorizado acha o mesmo resultado | Cortar resposta em barreira pós-commit, nova conexão e valores/recibo iguais sem segunda concessão | E2/E3 | D10/D28 |
| F05 | Crash antes do commit | Sem efeito parcial confirmado; intenção durável anterior, se houver, continua identificável como pendente | Encerrar processo de teste antes do commit, reiniciar e comparar head, recursos, recibos e journal | E3 | D10/D24 |
| F06 | Crash depois do commit antes do callback | Estado confirmado e recibo sobrevivem no perfil aprovado; nenhum replay do efeito | Restart real dos processos descartáveis, consulta ao armazenamento e callback idempotente | E3/E4 | D12/D28 |
| F07 | Callback da geração/fence antigos | Não altera ator atual; se houve commit, encaminha reconciliação | Interleaving com nova admissão, referência Session antiga e snapshot do observador | E3/E4 | D06/D24 |
| F08 | Banco indisponível ou conexão cai durante commit | Sem anúncio definitivo não comprovado; UNCERTAIN quando resultado desconhecido; backpressure aprovado | Falha de rede controlada, limites de fila, origem pendente e recuperação por ID | E3/E4 | D12/D25 |
| F09 | Ledger/arquivo parcialmente disponível | Não tratar falta de parte como ausência de concessão | Tornar arquivo/partição de histórico inacessível; consulta incerta/bloqueada e zero novos efeitos | E2/E3 | D26/D28 |
| F10 | Recibo ausente apesar de cabeça avançada | Erro de integridade/quarentena; não inventar recibo de saldo ou flag | Cópia de dados de teste inconsistente, diagnóstico e nenhuma ativação/grant | E2/E3 | D02/D10 |
| F11 | Snapshot antigo chega após mutação econômica nova | Não regrava XP/bolsa/saldo antigos; CAS ou rebase autorizado preserva cabeça nova | Escrita econômica seguida de checkpoint atrasado, ambas revisões e payload canônico | E2/E4 | D09/D24 |
| F12 | Conteúdo/regras mudam entre captura, commit e ativação | Não ativar candidato incompatível; commit existente é reconciliado, não apagado | Hashes/versionamentos em cada fronteira e recusa sem corpo parcial | E4 | D29/D32 |
| F13 | Spawn ocupado após reserva ou posição inválida | Revalidar; reter sem corpo, sem teleporte arbitrário ou perda de estado | Geometria/revisão/corpos atuais, reserva expirada e observador sem ator duplicado | E4 | D07/D29 |
| F14 | Clock reinicia ou relógio do sistema recua/avança | Não comparar ticks de épocas diferentes nem liberar recarga por salto | Âncoras, margem de incerteza, políticas e testes de ajuste de relógio | E2/E3 | D20/D21 |
| F15 | Legado sem recibos, com quest-done/defeat/pending | A continua restrição; nenhum recibo retroativo, nenhum save alterado | Fixtures de legado e decisão explícita de rejeição/observação não confirmada | E2 | D02/D27 |
| F16 | Retenção esgotada ou recibo movido ao arquivo | Falhar seguro/degradar sob política; retry antigo não gera concessão nova | Antes/depois do arquivo, restart, consultas idênticas/divergentes e índices preservados | E2/E3 | D26 |
| F17 | Recompensa afeta XP, domínio, moedas, itens, direito e entrega | Tudo junto ou nada; resultado autorizado/efetivo registrado | Falhar a cada escrita intermediária da transação; comparar todos os recursos e recibo | E2/E3 | D10/D14/D15 |
| F18 | Shutdown com transação pendente | Prazo finito; não fabricar sucesso; confirmado/incerto/pendente durável distinguíveis | Drenagem limitada, processo encerrado, restart e reconciliação dos IDs | E3/E4 | D24/D25 |
| F19 | Duas conexões/processos tentam o mesmo personagem | Um claim válido; não surgem dois atores/escritores autorizados | Contenção SQL e partição de rede com observação física dos atores e fences | E3/E4 | D05/D06/D07 |
| F20 | Lease expira com owner antigo ainda vivo | Escritas antigas recusadas e interações antigas impedidas conforme política aprovada | Isolamento do processo antigo e tentativa real de dano/comando/commit após nova autoridade | E3/E4 | D07/D33 |
| F21 | Proprietário incorreto consulta operação/personagem | Negar acesso mesmo conhecendo UUIDs; nenhuma informação econômica privada | Dois proprietários sintéticos/autorizações distintas e tentativas cruzadas | E3/E4 | D05/D31 |
| F22 | A-04: retirada comandada pelo harness durante Quatro relâmpagos | Capturar antes da remoção; manter custos/prazos/estado confirmado; não restaurar fase, impactos, itens ou XP extras | RpgSessionWorld/engine reais, armazenamento aprovado, comparação do estado e spawn; sem timeout automático novo | E4 | D23/D29/D30 |
| F23 | Crash entre pagar beneficiário A e B | A não recebe duas vezes e B permanece reconciliável, ou grupo aborta junto na alternativa aprovada | Manifesto durável de elegíveis/resultados, duas carteiras e falha no meio da distribuição | E2/E3/E4 | D19 |
| F24 | Recuperação com ROOT/STUN/SLOW/BLEED/BURN ou efeito desconhecido | Não limpar penalidade por omissão, não recriar executor/pulsos antigos | Fonte completa de restrições e policy version; parâmetros de duração/intensidade e aprovação de gameplay | E2/E4 | D21/D22 |
| F25 | Ataque básico fracionário, NaN de precisão, prazo pronto/ausente e domínio trocado | Preservar origem/precisão; não converter ausência em zero nem liberar cedo | Valores nativos extremos, codecs, cinco IDs reais e erro de representação/conversão | E2/E3 | D20/D21 |
| F26 | Geração mais nova termina antes de concessão antiga legitimamente pendente | Não perder obrigação comprovada; replay sem prova continua recusado | Intenções duráveis, ordem de execução, regras de watermark/settlement aprovadas e contagem final | E2/E3 | D19/D34 |
| F27 | Fila cheia ou callback confirmado não cabe no retorno | Memória limitada; não descartar único registro de confirmação nem publicar benefício sem aceite | Itens/bytes/latência, resposta de contingência e retomada pelo armazenamento | E4 | D24/D25 |
| F28 | Restore antigo/failover revive fence/OperationId/GrantId | Não admitir autoridade antiga ou pagar de novo | Backup restaurado em isolamento, processo antigo vivo e procedimento de revogação/era | E3 | D12/D26/D33 |
| F29 | Deadlock/serialization failure/timeout de lock | Rollback e retry limitados com mesma intenção; sem sucesso fictício | Barreiras de locks, logs, contadores de tentativas e recibos finais | E2/E3 | D11/D25 |
| F30 | Perfil de commit/replicação não atende garantia declarada | Gate de CONFIRMED falha; não promover resultado a durável sob perfil inválido | Configuração efetiva, queda real em ambiente isolado e leitura no nó usado na reconciliação | E3 | D12/D28 |

## Evidência mínima por execução futura

**RECOMENDADA — D28.** Cada relatório deve registrar versão/configuração efetivas,
revisões e autoridade de base, OperationId/kind, origem/concessão, ponto exato da
falha, transação observada, estado após nova conexão/restart, recursos/recibos e
diagnóstico do owner/observador. Não registrar segredo ou dados pessoais em log.
Somar asserts verdes em RAM não substitui esse registro.

## Cobertura anterior: somente referência

R5 prova CAS/idempotência/injeção em memória e demonstra perda ao recriar ledger.
R4 prova captura e associação real no owner TCP local. R1 mantém ativação não
autorizada. Esses resultados ajudam a desenhar F01–F30; não aprovam seus ensaios
PostgreSQL, autenticação, restart, lease ou relógio duráveis.
