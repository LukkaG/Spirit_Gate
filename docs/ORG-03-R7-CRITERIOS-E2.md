# ORG-03-R7 — Critérios verificáveis do laboratório E2

## Estado e regra de aprovação

**PLANO NÃO EXECUTADO.** Todos os casos abaixo estão pendentes de autorização,
implementação e execução E2. A presença nesta tabela não significa teste aprovado.
R5 (1.196 aprovados/57 excluídos) é baseline histórico em RAM/headless, sem nova
rodada R7. A R6 é especificação, não evidência SQL.

Gate de entrada: aceites L01–L05 aplicáveis, alvo isolado comprovado conforme
[ambiente](ORG-03-R7-AMBIENTE-POSTGRESQL.md), limites do
[escopo](ORG-03-R7-ESCOPO-E2.md) e perfis sintéticos versionados. Cada execução
registra PASSOU, FALHOU, NÃO EXECUTADO ou INCONCLUSIVO por caso/perfil; nenhum
caso obrigatório ausente ou inconclusivo fecha o recorte E2 como aprovado.

## Critérios mensuráveis e rastreabilidade

| ID | Objeto / falha R6 | Experimento futuro e resultado exigido | Armazenamento e evidência mínima | Limite E3/E4 |
| --- | --- | --- | --- | --- |
| C01 | Constraints / D09–D11 | Um caso negativo por PK, unicidade de operação, concessão/origem, claim, versão e vínculo beneficiário/recibo; NULL/quantidade inválidos recusados. Zero estado parcial após rollback. Distinguir constraint física de validação do codec | PostgreSQL real; DDL efetivo, SQLSTATE, contagens e imagem antes/depois por violação | Não prova crash; E3 repetirá os invariantes persistidos |
| C02 | Revisões e concorrência / F01 | 20 pares sincronizados em duas conexões, alternando quem tenta primeiro, com mesma base e IDs distintos: exatamente um commit e um conflito por par; incremento +1 das duas revisões para mutação econômica, nenhum overwrite | PostgreSQL real; barreiras/conexões A/B, base, linhas afetadas, recibo vencedor e leitura observadora pós-commit | E4 precisa incluir todos os escritores reais, inclusive autosave |
| C03 | Commit e múltiplos efeitos / F17 | Recompensa sintética com XP/domínio/HP, moedas, item/direito e missão: tudo e recibo/revisões juntos. Injetar erro após cada escrita intermediária; zero efeitos/recibo parcial. Repetir agregado de poção e equipamento | PostgreSQL real; imagens completas, ponto de injeção e rollback observado por outra conexão | E3 crash real; E4 coerência de HP/combate/owner |
| C04 | Operação e grant duplicados / F02/F03 | 20 pares para mesma concessão sob IDs distintos: um efeito total. Mesmo ID/intenção devolve recibo original sem incrementar; mesmo ID divergente em kind, conteúdo ou beneficiário conflita/nega acesso, sem revelar recibo alheio | PostgreSQL real; constraints/índices, IDs/hashes e contagem de efeitos, inclusive retry com cabeça já avançada | E3 repetirá após restart/restore |
| C05 | Rollback e ausência definitiva | Erro de regra antes da aplicação e erro técnico no meio: nenhum recurso muda. Recusa não pode negar commit anterior do mesmo ID; timeout de cliente não é prova de rollback | PostgreSQL real; consulta independente da transação encerrada, estado de cada intenção e resultado conhecido/incerto | E3 conexão perdida durante commit e crash |
| C06 | Resposta perdida simulada / parte de F04 | Suprimir no harness a resposta de commit concluído; chamador marca UNCERTAIN; lookup autorizado por outra sessão encontra exatamente o recibo original e retry não muda recursos/revisões. Zero novo ID para escapar da incerteza | PostgreSQL real; duas visões do resultado, ponto da supressão e lookup posterior | Não é corte de rede/crash real; restante de F04 permanece E3 |
| C07 | Histórico incompleto / F09/F10 | Indisponibilizar arquivo sintético ou apontar referência esperada para recibo ausente em fixture corrompida isolada: erro de integridade/UNCERTAIN; nenhum NOT_FOUND usado para reexecutar concessão | PostgreSQL real + arquivo sintético controlado; marcador de proveniência, falha de lookup e ausência de nova escrita. Corrupção só via fixture privilegiada separada | E3 perda real de arquivo/nó e recuperação de backup |
| C08 | Snapshot antigo / F11 | Confirmar operação nova e tentar checkpoint da base anterior; CAS recusa e preserva integralmente economia atual. Retry de operação confirmada antiga pode retornar recibo histórico, mas não reinstala a versão antiga | PostgreSQL real; duas revisões, payload/hashes, recibos e consulta da cabeça | E4 rota real de autosave/owner |
| C09 | Legado sem prova / F15 | Fixtures com quest-done/defeat/pending sem recibos: recusa de recuperação integral A, nenhuma criação de recibo/CharacterId de jogador e zero alteração da observação legada | PostgreSQL real para caminho de armazenamento, mais verificação pura; hashes e classificação da fixture, sem save pessoal | Migração B/C exige decisão independente E5 |
| C10 | Retenção e replay / F16 | Cap experimental 32: pressão recusa novos recebimentos conforme perfil, sem purge. Arquivar recibo antigo mantendo índice/vínculo; retry idêntico retorna resultado e divergente conflita; arquivo inacessível não concede. Novo ID para grant velho não paga | PostgreSQL real + arquivo sintético; consulta ativo/arquivo, índice preservado, invariantes de quantidade e revisão | Reinício, recuperação de arquivo e retenção prolongada são E3; não fixar TTL |
| C11 | Identidade sintética e fence / D04/D06/D31 | Dois proprietários com contexto confiável distinto: consulta cruzada negada antes de expor payload. Disputar claim na mesma base em duas conexões: um vence. Após revogação/troca, zero commit com fence antigo; lookup de recibo antigo continua possível ao dono atual autorizado | PostgreSQL real + controlador de autorização sintética; permissão do runner, contagens, fences e resultados positivos/negativos | Não prova autenticação D05, lease D07, transporte D32 ou suspensão de ator E3/E4 |
| C12 | Pendentes com origem / D17/D18 | Dois direitos da mesma espécie e origens diferentes; coleta por item transfere quantidade cabível inteira do item preservando vínculo de ambos, outros itens ficam pendentes. Retry não recolhe novamente. Perfil pacote inteiro fica em namespace separado | PostgreSQL real; conservação de quantidade: recebido = bolsa/equipado + pendente + consumo autorizado, com origem/recibo por parcela | Legado agregado não permite reconstruir origem; E4 deve preservar input/UI existentes |
| C13 | Obrigações antigas / F26/D34 | Geração nova liquidada antes de intenção antiga autorizada: antiga permanece pendente e liquidável uma vez. Replay antigo sem prova continua recusado. Comparar ordenação (segura novo até antigo resolver), prefixo e lacunas; prefixo só avança com prova de liquidação de todas as obrigações conhecidas do trecho | PostgreSQL real; registro de intenções, lacunas/ordem, marcador antes/depois e somatório por concessão. Nenhum max-generation elimina dívida | E3 perda de processo no meio, nova admissão e restauração; sem trocar fence do mesmo payload |
| C14 | Precisão de deadlines / F25 | Round-trip de casos fracionários nativos, precisão não inicializada NaN, prazo pronto e ausência distinta, cinco IDs de domínio. Representação exata candidata não libera prazo antes da hora; decimal incompatível é recusado, nunca arredondado silenciosamente | PostgreSQL real para persistir/ler representação e teste puro para comparação de bits/validação; nove canais enumerados no relatório | Não homologa ticks→timestamp, offline elapsed, clock do SO/restart ou D21; E3/E4 exigem essas provas |
| C15 | Conflito/deadlock/retry / F29 | Separar conflito de CAS (sem retry cego), deadlock controlado e serialization failure. Em 10 rodadas por caso técnico, abortar uma tentativa quando provocado e repetir transação inteira com mesmo ID até no máximo 3 tentativas totais; zero concessão extra e nenhuma transação pendurada | PostgreSQL real, duas sessões com barreiras/locks em ordem invertida só no ensaio; SQLSTATE, configurações efetivas, contador e recibo final. Se não ocorrer a condição pretendida, caso inconclusivo | E3 falhas de processo/conexão; nenhuma conclusão de capacidade de produção |
| C16 | Distribuição sintética / D19 | Manifesto com dois beneficiários elegíveis/resultados fixos: pagar A e adiar B; reconciliação paga somente B e não rerrola resultado. Perfil global alternativo aborta ambos na falha. Comparações rotuladas, sem escolher gameplay | PostgreSQL real; intenção, duas cabeças/recibos, estado pendente e soma final | F23 de crash real continua E3; origem/morte no jogo é E4 |

F01/F02/F03/F09/F10/F11/F15/F16/F17/F25/F26/F29 estão cobertos explicitamente.
Também há os recortes adicionais sintéticos de resposta perdida, claim e
distribuição pedidos pelo escopo; isso não fecha automaticamente F04/F19/F23.

## Recorte temporal de C14: representação, não recuperação

Enumerar separadamente os nove canais: ataque básico, Shunpo, poção, viagem e
skills de domínio nos slots 0, 1, 2, 3 e 4, associados aos IDs reais do catálogo
da fixture. Cada caso conserva origem (WorldTick/SessionTick/SkillEngineClock
conforme a fonte), época, versão, observação e deadline; não pressupor clocks
iguais. Inteiros não perdem unidades e representação finita binary64 candidata
conserva bits, inclusive valor fracionário e zero com sinal quando presente.

NaN de precisão não inicializada e ausência recebem tags distintas de valor
finito, em envelope experimental; não usar NaN JSON inválido nem interpretar
qualquer um deles como pronto. Testar valor imediatamente antes/no/depois do
prazo, subunidade fracionária e incompatibilidade com decimal R1. Não adaptar
automaticamente o DTO R1 ou arredondar para fazê-lo aceitar a fixture.

Para recuperação futura ainda faltam: fonte de tempo persistente confiável,
amostragem correlacionada com captura, margem/erro conservador sem liberação
antecipada, detecção de saltos e prova de continuidade/restart. Reconexão
imediata/parcial/expirada/desconhecida exige D20/D21 e E3/E4. Nenhum valor de
erro tolerável ou conversão ticks→timestamp foi aprovado pelo agente.

## Protocolo de medição e evidência

Cada caso deve registrar run-id e perfil, versão do harness/DDL, PostgreSQL
efetivo, alvo comprovado, isolamento/timeouts, IDs sintéticos, autorização,
base e resultado de ambas as revisões, origem/grant, fase da barreira, SQLSTATE,
tentativas, conteúdo/valores nominais e efetivos, contagens de antes/depois,
consulta observadora e artefatos sanitizados. Nunca guardar senha ou string de
conexão com segredo. A contagem de asserts não substitui as imagens comparadas.

Para deadlock, a configuração e a ordem dos locks precisam permitir que o
detector atue antes do timeout do ensaio. Ordem consistente de locks é a
recomendação do fluxo normal; inversão deliberada pertence apenas à fixture.
Deadlock pode abortar uma transação e requer tratamento do chamador.
[Locks PostgreSQL 18](https://www.postgresql.org/docs/18/explicit-locking.html).

Latências e volume ficam como observações: nenhum p95 de captura R4, cap de
recibos R5 ou limite deste laboratório é orçamento definitivo do servidor.
Testar representação de horário em SQL não prova sua origem ou continuidade.

## Saída do gate e pendências separadas

O relatório futuro E2 deve listar cada caso/perfil com seu resultado, comandos
sanitizados, violações, limitações e situação do encerramento/descarte.
Fechar E2 exige cumprir o recorte autorizado inteiro, com evidências e revisão
humana; não muda por si os estados D01–D34 da R6.

Permanecem para E3: crash real antes/depois do commit, incerteza em conexão
interrompida, restart, recuperação de arquivo/backup, failover, RPO/RTO, não
ressuscitar fences/épocas e obrigações após restore. Permanecem para E4:
owner/fila/callback reais, corpo exclusivo, efeitos completos, relógio homologado,
spawn e conteúdo atual. Nenhum teste SQL isolado aprova recuperação de jogador.
