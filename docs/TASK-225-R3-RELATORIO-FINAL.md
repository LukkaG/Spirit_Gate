# TASK-225-R3 — Relatório final

Execução local de 03–04/11/2026, branch `beco`, HEAD inicial `6d72c65`. Escopo arquitetural e protótipo sintético isolado. Commit/push da R3 autorizados diretamente pelo usuário; nenhuma autorização de publicação/ativação decorre disso.

## Resultado e recomendação

Entregue protótipo exclusivo de teste com prova opaca local, geração de admissão, snapshot imutável/schema 1, expiração, consumo único, validação de spawn/contexto e recargas absolutas. A recomendação é **política B, retirada com recuperação controlada**, condicionada a implementar o contrato completo antes de ativar o prazo A-04.

**A-04 permanece bloqueante.** O protótipo não instala personagem no multiplayer, não autentica peers, não persiste seu envelope nem recupera após restart. O prazo de 92 ticks não foi integrado ao engine; a TASK-226 não foi iniciada.

## Auditoria e decisões

RpgSave, cópias de revisão e laboratório 193–196 são úteis, mas não cobrem automaticamente atributos manuais, energia absoluta, autoria, cinco cooldowns e identidade de rede. Recuperação precisa capturar o estado autoritativo **antes de `remove`/`leave`**, que descartam recargas, e restaurá-lo por APIs explícitas futuras.

A política A conserva a perda de progresso/reset de recargas e não atende a uma correção transparente. C não garante apoio em mundo totalmente bloqueado; reserva ou deslocamento mudariam regras e exigem aprovação. B separa término físico de perda do personagem, desde que a captura seja íntegra, posse/commit sejam únicos e expiração de credencial não apague silenciosamente o estado.

HP e energia devem permanecer nos valores retidos, sem cura/refill. Nível deriva do XP; inventário/equipamento/moedas e ledgers são snapshot inteiro, não soma. Cooldowns seguem a época/relógio do servidor; transitórios, filas, animações e fase bloqueada não são retomados. Cosméticos de sessão, atributos/pontos, autoria real, efeitos prejudiciais e contribuição pendente ainda exigem contrato completo e testes de importação.

## O que foi comprovado

Vinte casos novos verificam recuperação exata do envelope, duplicação/itens, identidade errada com nome igual, expiração 159/160/161, corrida de dois threads com um vencedor, spawn bloqueado e geometria real builtin, revisão/época/região, falhas antes/depois do commit, recibos, recompensa builtin real já concedida, cooldown ativo, missão parcial, transitórios descartados, geração antiga, versão/campo obrigatório, capacidade dois e restart recusado.

Os replays reais de baú/missão usam o harness 196 e pasta sintética temporária. A corrida é real entre threads; recuperação e falhas são de modelo em uma instância. Igualdade do DTO não é prova de restauração de atributos/energia/autoria no runtime. A memória é perdida se o processo cair; o recibo pós-commit só prova reconciliação local de resposta simuladamente perdida.

## TCP e protocolo

Os testes TCP R2 continuam aprovados e demonstram retirada/readmissão efêmera, sem recuperação R3. O cenário condicional de recuperação com dois TCP não foi implementado: aplicações 6/7 não carregam prova de posse e não há importador completo. Não se criou canal alternativo/endpoint ou ponte do repositório sintético para o multiplayer.

A ADR contém proposta separada de capacidade negociada para credencial protegida, geração/operação, expiração/revogação, consumo único, resultado e reconciliação. Não escolhe opcode/versão nem implementa autenticação pública. Nome/IP/ordem de conexão não são provas de identidade. Nenhuma recuperação por contas ou saves pessoais foi conectada.

## Arquivos entregues

- `server/src/test/java/com/projetok/rpg/Task225R3RecoveryLab.java`: controlador e DTOs de laboratório, somente no classpath de teste.
- `server/src/test/java/com/projetok/rpg/Task225R3RecoveryLabTest.java`: 20 casos de segurança/continuidade e fixture real builtin.
- `docs/TASK-225-R3.md`, `docs/TASK-225-R3-TESTES.md`, este relatório e `docs/TASK-225-R3-ADR-CONTINUIDADE.md`.
- Acréscimos R3 em `docs/CORUJAO-IX-PROGRESSO.md` e `docs/CORUJAO-IX-PENDENCIAS.md`.
- Checkpoint `.task-checkpoints/CORUJAO-IX/TASK-225-R3/`: baseline, hashes, ordem, logs, XMLs e árvore isolada de verificação. Evidências locais não são publicadas automaticamente.

## Validação

Primeira rodada: 32 casos aprovados (20 R3 e 12 de 194–196). Repetição com R1/R2: 46 aprovados. Suíte completa headless: **1.033 casos, zero falhas/erros e 54 skips gráficos**. Árvore exportada do HEAD, acrescida apenas dos dois fontes R3: **20 aprovados**, comprovando que o recorte de teste para Git não depende do trabalho local anterior ainda não versionado.

Comandos, complemento gráfico e agregado em [TASK-225-R3-TESTES.md](TASK-225-R3-TESTES.md). Novos testes com prazo JUnit de 15 segundos e esperas finitas; cada Maven com limite externo de 600 segundos. Nenhum teste anterior foi desabilitado, nenhuma dependência instalada e nenhum banco acessado. Falhas de pré/pós-commit são injeções esperadas e verificadas, não falhas ocultadas da suíte.

Complemento Swing concluído: **106 execuções aprovadas, zero falhas/erros/skips**. Agregado mais recente por classe: **1.033 aprovados em 262 classes, sem falhas, erros ou skips remanescentes**. Repetições não foram somadas ao total da suíte.

## Compatibilidade e integridade

Nenhuma fonte de produção recebeu alteração R3. O baseline preserva 2.728 arquivos, incluindo 291 arquivos de produção e 186 de skills (152 PNGs); conferência e restauração de saídas históricas em `verification.json` do checkpoint. Os fontes/testes anteriores, incluindo R1/R2, são protegidos; só os dois documentos de progresso/pendências recebem acréscimos entre os arquivos anteriores.

Conferência final concluída: zero alterações em produção/assets e zero diferenças inesperadas. As capturas e medições históricas regeneradas foram arquivadas/restauradas, sem substituir os resultados das entregas anteriores.

## Recorte autorizado para Git

O envio abrange oito arquivos: dois fontes de teste R3, quatro documentos R3 e os dois documentos de acompanhamento atualizados. Os documentos de acompanhamento ainda não estavam versionados; entram completos com seu histórico local e o adendo R3. Não entram mudanças anteriores de runtime/assets, remoções portáteis, checkpoints, logs ou outros arquivos locais. A verificação em árvore isolada usou HEAD + apenas os dois fontes R3 e passou os 20 casos. SHA e confirmação do remoto são registrados na evidência local de envio e na resposta da entrega; não há release ou ativação associada.

Sem alteração das 40 skills, dano, energia, cooldowns de produção, aplicações 6/7, capacidade, colisões, saves, progressão, inventário, offline/Coop, World Studio, launcher ou correção visual da TASK-225. Essa preservação não homologa rede física nem durabilidade de produção.

## Pendências, rollback e próximo passo

Decisões pendentes: alcance e durabilidade de B, snapshot/importador completos, identidade/recompensas pendentes, política de efeitos, expiração/retencão, posse/protocolo/segurança do canal e feedback. Falta implementar e testar captura real no owner, falha de armazenamento e admissão/peer em uma transação de posse. Não há resolução definitiva do A-04 nesta entrega.

Reverter somente os dois testes, quatro documentos e acréscimos R3 nos dois documentos compartilhados, comparando baseline/trabalho posterior. Após envio ao Git, usar novo commit de reversão do recorte se solicitado; nunca reset/force push. Nenhum wire/save pessoal/banco foi migrado.

Próximo passo sugerido: revisar e aprovar o contrato de B e a proposta separada de recuperação antes de autorizar implementação definitiva. Não ativar timeout, integrar persistência ou iniciar TASK-226 por conta própria. Não houve release, distribuição ou publicação de conteúdo.
