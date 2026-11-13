# TASK-225-R2 — Relatório final

Referência: 03/11/2026, branch `beco`, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`. Entrega local exclusivamente de laboratório; não inicia TASK-226.

## Objetivo e resultado

A alternativa de retirada coordenada é tecnicamente exercitável pela infraestrutura existente, sem segunda arquitetura de sessão. O modelo candidato validou as bordas 31/32/91/92/93 usando engine, mundos e encontro reais. O TCP com dois clientes reais demonstrou retirada equivalente durante a fase, fechamento do peer, ausência de consultas privadas para sessão retirada, limpeza visual, recusa de comandos antigos e readmissão revalidada.

**O timeout de 92 ticks não foi ativado. A-04 continua bloqueante; a TASK-225 não está plenamente homologada.** A R1 está aprovada tecnicamente pela ordem recebida; isso não autoriza integração definitiva da alternativa A.

## Evidência e limites da conclusão

O modelo deixa a tentativa real ocorrer antes da decisão candidata aos 92. Apoio nos ticks 91 ou 92 evita saída. Sem apoio, há uma retirada lógica, flags limpas, nenhuma repetição aos 93 e nenhum crédito tardio ao mundo retirado quando o fixture libera a câmara. O controle sem candidato confirma que a produção ainda espera aos 93.

O ensaio TCP usa um adaptador exclusivamente de teste ao redor de `RpgSessionWorld`, implementado pela própria interface `SessionSimulation`. Jobs são executados no owner após `step`, antes dos snapshots. A retirada candidata chama `leave` e mantém um pedido independente; `disconnectRequested` faz o transporte real fechar o peer. O callback posterior chama `leave` novamente, sem repetir a retirada lógica. Nenhuma skill manipula sockets.

O observador 6/7 continua conectado; a fase de outro lançador 7 não é cancelada. O cliente retirado detecta fechamento, fica sem skills visíveis e recusa novos comandos. Comandos colocados na fila real do servidor antes da retirada são drenados depois dela e recusados por ID antigo. Spawn bloqueado recusa readmissão sem corpo parcial; spawn liberado permite personagem novo com ID e UUID distintos, domínio inicial e recargas zero. Saída espontânea durante fase também converge para uma retirada lógica e invalida pedido atrasado.

A prova TCP injeta causa equivalente durante a fase normal; não faz o engine emitir pedido depois de 92 ticks sem apoio. O token TCP vincula uma fase única por sequência de evento inicial; **não substitui geração de execução ativa em produção**. O modelo testa divergência de geração/revisão/região/identidade, mas não implementa esses campos no runtime. O [plano](TASK-225-R2-PLANO-INTEGRACAO-A04.md) exige validar a integração completa posteriormente.

## Arquivos entregues

- `server/src/test/java/com/projetok/rpg/Task225R2DeadlineLabTest.java`: 11 casos de prazo, apoio nas bordas, duplicatas, lifecycle e token antigo; modelo candidato separado do runtime.
- `server/src/test/java/com/projetok/multiplayer/Task225R2ShutdownTcpLabTest.java`: três casos com servidor e até dois clientes TCP, aplicações 6/7, fila real, retirada, saída espontânea, renderers e readmissão.
- `docs/TASK-225-R2.md`, `docs/TASK-225-R2-TESTES.md`, este relatório e `docs/TASK-225-R2-PLANO-INTEGRACAO-A04.md`.
- Acréscimos R2 em `docs/CORUJAO-IX-PROGRESSO.md` e `docs/CORUJAO-IX-PENDENCIAS.md`; histórico 225/R1 preservado.
- Evidências, baseline e executores em `.task-checkpoints/CORUJAO-IX/TASK-225-R2/`; análise das falhas intermediárias em `logs/analises/20261103-task225-r2-testes-laboratorio.md`.

Nenhuma fonte de `server/src/main` foi modificada nesta entrega. Reflexão e falhas controladas existem somente nos dois testes novos; não há endpoint, backdoor, opção administrativa ou modo de gameplay exposto.

## Testes e ocorrências

Baseline de comparação: 999 casos na R1. Rodada focalizada final: **26 casos aprovados**, sendo 14 novos R2 e 12 R1. Regressão completa headless: **1.013 casos, zero falhas/erros e 54 skips gráficos**. Comandos, complemento gráfico, XMLs e agregado por classe constam em [TASK-225-R2-TESTES.md](TASK-225-R2-TESTES.md).

Complemento gráfico: **106 execuções em 26 classes, zero falhas/erros/skips**. Agregado pelo XML mais recente por classe: **1.013 casos aprovados em 261 classes, sem falhas, erros ou skips remanescentes**. Não somar as repetições ao total da suíte.

As duas primeiras rodadas documentam quatro erros de desenho dos testes: identificador incorreto, expectativa de HIT após retirada antecipada do alvo, expectativa de renderer vazio apesar da outra fase ativa e sequência zero inválida no DTO. Corrigidos apenas nos testes novos; não houve adaptação de produção para obter aprovação. Logs intermediários preservados.

Todos os novos casos têm prazo JUnit e laços finitos. Esperas no owner/fila têm cinco segundos, TCP sete segundos e Maven limite externo de 600 segundos. Não foram instaladas dependências, executado Maven concorrente ou acessado banco externo.

## Compatibilidade e integridade

O checkpoint preserva 2.722 hashes/cópias do estado recebido, incluindo 291 arquivos de produção, 269 fontes de teste anteriores e 186 arquivos de skills (152 PNGs). Verificação final em `verification.json`; saídas históricas regeneradas pela suíte são arquivadas antes de restaurar suas cópias anteriores.

Conferência concluída: zero alterações em produção/assets e zero diferenças inesperadas entre os arquivos protegidos. Somente os dois documentos de progresso/pendências diferem do baseline, pelos acréscimos R2. Os dois testes e quatro documentos R2 são novos.

Foram arquivados/restaurados 82 PNGs históricos e os dois TXT de medição A-04 de 225/R1 regenerados pela suíte; os números das investigações anteriores não foram substituídos pelos da execução R2.

Preservados por ausência de mudanças de runtime e regressão disponível: catálogo das 40 skills, dano/energia/cooldown, aplicações 6/7, colisões, movimento, contribuição/recompensas, progressão/inventário/saves, offline/Coop, World Studio, launcher e correção visual da TASK-225. Não há homologação nova de ambientes distribuídos por essa afirmação. ACK e trajetória permanecem propostas separadas.

## Decisão sobre experiência e pendências

Aceitar perda de progresso e recargas é premissa **somente do laboratório**. A readmissão atual cria outro personagem efêmero; não recupera XP, itens, moedas, missões, domínio ou cooldowns. Para aplicar A aos jogadores, o responsável precisa aceitar expressamente essa consequência e o risco de reinicializar recargas. Se preservar progresso/identidade for requisito, a opção B — recuperação antes da ativação — é gate obrigatório e demanda trabalho próprio.

O feedback atual é genérico de conexão. Causa específica não foi inserida em aplicações 6/7; `Protocol.Error` não constitui solução de partida homologada. O plano compara aviso experimental fora da partida e capacidade negociada separada.

Pendências técnicas: pedido real originado no engine, geração ativa, consumo/expiração de tombstones, queries privadas concorrentes, falta real de apoio até 92 por TCP, carga do owner e testes do patch definitivo. Pendentes também homologação física/Radmin, launcher nativo distribuído e revisão humana. Nenhum desses testes foi declarado executado.

## Reversão e próximo passo

Remover somente os dois testes e documentos R2 e reverter seus acréscimos nos dois documentos do Corujão, a partir do baseline R2 e após comparar trabalho posterior. Preservar os relatórios e evidências se desejado. Não há wire/save/banco a migrar; não usar reset/clean global.

Próximo passo sugerido: o responsável revisar o plano e decidir consequências de readmissão, feedback e autorização para patch definitivo. Não ativar A-04 nem iniciar TASK-226 automaticamente. Sem commit, push, release, publicação, build distribuída ou mudança de versão.
