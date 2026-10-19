# Rodada autônoma Spirit Gate - TASK-007 a TASK-011

Planejamento fornecido pelo usuário em 18/10/2026. Fonte de verdade: checkout local, base TASK-006.1 com 170 testes aprovados e teste de parede presente. Windows 10 amd64, Java/jpackage Temurin 17.0.20.1, Maven 3.10.0. Sem Git nesta pasta; checkpoints locais e diffs isolados em cada etapa. Sem push, publicação, banco ou integração de rede.

Rodada concluída na TASK-011. Todas as cinco tarefas estão IMPLEMENTADAS, com automação aprovada. A conferência local do EXE foi realizada; a homologação humana posterior continua pendente.

| Task | Testes inicial -> final | Resultado |
|---|---|---|
| 007 | 170 -> 171 | Pacote e EXE local aprovados |
| 008 | 171 -> 177 | Demo e colisões aprovados |
| 009 | 177 -> 189 | Estados e feedback do HUD aprovados |
| 010 | 189 -> 202 | Sequências e repetição offline aprovadas |
| 011 | 202 -> 202 | RC reconstruída, auditada e executada |

Todos os resultados finais têm zero falhas, erros ou ignorados. São 32 testes novos; os 26 arquivos de teste originais e os baselines históricos foram preservados.

## TASK-007 - Pacote Windows portátil

Status: IMPLEMENTADA. AUTOMATIZADO APROVADO; EXECUCAO LOCAL CONFERIDA. EXE abriu e fechou normalmente fora dos fontes, em pasta com espaços, sem Java/Maven no PATH. Captura e JSON de evidência disponíveis. Inspeção de sockets indisponível por restrição do ambiente; nenhuma homologação externa foi declarada.

Testes: inicial 170; final 171, zero falhas/erros/ignorados. Mudanças: build com jpackage/jlink, staging por lista permitida, runtime integrado com jdk.random, manifest/inventário/SHA-256 e teste de entrada sem JFrame.

Artefatos: [relatório](TASK-007.md), [PDF](Spirit_Gate_TASK-007.pdf), [testes](TASK-007-testes.txt), [log](TASK-007-final.log), [diff](TASK-007.diff). ZIP 0.7.0-task007 em dist; candidato consolidado será gerado na TASK-011.

Pendências manuais: outro Windows sem Java/Maven, controles e sensação de jogo. Nenhuma implementação foi reservada ao usuário nesta rodada.

## TASK-008 - Arbustos e poste leves

Status: IMPLEMENTADA. AUTOMATIZADO APROVADO. Testes: inicial 171; final 177, zero falhas/erros/ignorados. Falha inicial de limpeza de arquivos somente leitura do jpackage foi corrigida no build antes das mudanças de cenário e a base foi repetida com sucesso.

Mudanças: variante demo usada pelo runtime, dois arbustos e um poste SOFT_OBSTACLE com hitboxes explícitas, snapshot imutável e Java2D. Mapa original/replay preservados. Seis testes novos cobrem colisões, passagem, pouso e renderização pura.

Artefatos: [relatório](TASK-008.md), [PDF](Spirit_Gate_TASK-008.pdf), [testes](TASK-008-testes.txt), [diff](TASK-008.diff), [captura](TASK-008-demo.png) e logs inicial/final.

Pendências manuais: roteiro de três minutos para WASD, Shift, parede, hitboxes e R. A captura é renderizada; homologação humana ainda pendente.

## TASK-009 - HUD contextual

Status: IMPLEMENTADA. AUTOMATIZADO APROVADO. Testes: inicial 177; final 189, zero falhas/erros/ignorados. Doze casos novos e captura renderizada em quatro estados.

Mudanças: views imutáveis, mapper executado na simulação, disponibilidade/vigor/recarga/atividade/espera e motivos temporários reais de cast. Bloqueio deriva do deslocamento efetivo antes da recuperação, sem inferir parede pelo cursor. Nenhuma mudança nas regras ou na fila; lastEvent histórico preservado.

Artefatos: [relatório](TASK-009.md), [PDF](Spirit_Gate_TASK-009.pdf), [testes](TASK-009-testes.txt), [diff](TASK-009.diff), [captura](TASK-009-hud.png) e logs inicial/final.

Pendências manuais: legibilidade e transições no HUD jogando. Espera/liberação automatizadas no mapper e domínio; R determinístico será acrescentado na etapa seguinte.

## TASK-010 - Sequências determinísticas

Status: IMPLEMENTADA. AUTOMATIZADO APROVADO. Testes: inicial 189; final 202, repetidos offline com os mesmos resultados por classe e zero falhas/erros/ignorados. Treze casos novos. Os 26 arquivos originais de teste estão intactos e os baselines históricos foram preservados.

Mudanças: extração interna do tick real do runtime e fábrica injetável para teste; sequências reais de fila/bindings, R, morte, recursos, Freeze e física. Um bug de apresentação foi reproduzido por teste antes de corrigir: saldo insuficiente arredondado para 35/35 ou55/55. Corrigido somente o texto redundante; regras de vigor mantidas.

Artefatos: [relatório e lacunas antes/depois](TASK-010.md), [PDF](Spirit_Gate_TASK-010.pdf), [testes](TASK-010-testes.txt), [diff](TASK-010.diff), logs completos final/offline e reprodução/correção do bug.

Pendências manuais: apenas conferência de sensação e transições jogando. Reinício e manutenção dos objetos do demo agora estão cobertos pelo caminho de produção sem sleep/JFrame. Nenhum bloqueio automatizado permanece.

## TASK-011 - RC e kit de homologação

Status: IMPLEMENTADA. AUTOMATIZADO APROVADO; EXECUCAO LOCAL CONFERIDA. Base e build final com 202 testes, zero falhas/erros/ignorados. O pacote foi gerado dos fontes consolidados, sem reaproveitar o JAR histórico.

RC: dist/SpiritGate-0.11.0-rc1-20261018T220040Z-b2e659ed-portable.zip, 29.359.239 bytes. SHA-256: 7484f90f5de5ca0475c8d73a918a8868b20a089d5a3fb3a12dd2e40f5fc19d01. Sidecar e manifest externo junto ao ZIP. Versão anterior preservada.

Auditoria independente: ZIP íntegro, 149 arquivos, manifest com 148, inventário com 147; 122 classes idênticas à compilação atual, sem arquivos de teste/privados ou legado de banco/servidor. Verificador recusou uma cópia adulterada, sem alterar o original. Limitações de busca de credenciais são explícitas.

EXE executado em pasta com espaços fora dos fontes e com PATH sem Java/Maven. Módulos do processo confirmam jvm.dll do runtime integrado; janela e processo filho fecharam com código 0. Captura real mostra objetos leves e HUD atualizados. Inspeção de sockets por CIM indisponível; não se declara validação dinâmica de rede.

Artefatos: [relatório](TASK-011.md), [PDF](Spirit_Gate_TASK-011.pdf), [testes](TASK-011-testes.txt), [log completo](TASK-011-final.log), [diff](TASK-011.diff), [auditoria](TASK-011-auditoria.json), [smoke](TASK-011-portable-smoke.json), [captura real](TASK-011-exe.png), [estados do HUD](TASK-011-hud.png), [changelog RC](CHANGELOG-RC.md) e [guia do tester](LEIA-ME-TESTER.txt).

Pendências manuais: seguir [roteiro por tarefa](kit-homologacao/ROTEIRO-HOMOLOGACAO.md) em outro Windows x64 sem Java/Maven; usar [modelo de bug](kit-homologacao/MODELO-RELATO-BUG.md). EXE sem assinatura e espera de saída ocupada permanecem limitações. Nenhuma pendência de implementação foi transferida ao usuário.

## Integridade da entrega e encerramento

PDFs e diffs individuais estão em docs/TASK-007 a TASK-011. Os JSON locais em .task-checkpoints preservam preimagens e hashes das etapas; não integram o pacote portátil. O diff é textual e os PDFs/capturas são entregues separadamente. Falhas intermediárias de harness/limpeza foram corrigidas, mantidas em logs e explicadas; a falha de precisão do HUD foi reproduzida antes da correção, sem mudar o teste para mascará-la.

Não há bloqueios automatizados em aberto, nova dependência externa de runtime ou alteração em PostgreSQL/Kryonet/login. A suíte offline estável confirma execução com dependências locais. O pacote inclui apenas a arena e runtime necessários. Não se iniciou TASK-012 ou outra mecânica. O próximo passo é a revisão humana das cinco entregas, sem exigir implementação adicional do usuário.
