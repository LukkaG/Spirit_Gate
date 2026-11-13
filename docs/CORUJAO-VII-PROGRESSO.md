# Corujão VII — progresso

Atualização: 31/10/2026. Branch `beco`, HEAD
`6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`.

O usuário aprovou a proposta da TASK-201 após a auditoria e acrescentou o
fluxo de janelas. Em 31/10 autorizou prosseguir com a TASK-202.
Em seguida autorizou todas as TASKs 201-212 e pediu relatorio PDF ao final. A entrega e privada, sem publicacao.

| TASK | Estado |
|---|---|
| 201 | Implementada e validada localmente; fontes congeladas. Próximo passo: revisão local do responsável. |
| 202 | Implementada localmente; catálogo e testes em TASK-202.md e TASK-202-TESTES.md. Revisão visual pendente. |
| 203 | Implementada e validada localmente; ver TASK-203-RELATORIO-FINAL.md. |
| 204 | Implementada e validada localmente; ver TASK-204-RELATORIO-FINAL.md. |
| 205 | Implementada e validada localmente; ver TASK-205-RELATORIO-FINAL.md. |
| 206 | Implementada e validada localmente; ver TASK-206-RELATORIO-FINAL.md. |
| 207 | Implementada e validada localmente; ver TASK-207-RELATORIO-FINAL.md. |
| 208 | Implementada e validada localmente; ver TASK-208-RELATORIO-FINAL.md. |
| 209 | Implementada e validada localmente; ver TASK-209-RELATORIO-FINAL.md. |
| 210 | Implementada e validada localmente; ver TASK-210-RELATORIO-FINAL.md. |
| 211 | Implementada e validada localmente; ver TASK-211-RELATORIO-FINAL.md. |
| 212 | Implementada e validada localmente; ver TASK-212-RELATORIO-FINAL.md. |

Gates A-E verificados tecnicamente no ambiente local. Revisao humana e
homologacao fisica em dois computadores permanecem pendentes. Relatorio
consolidado e PDF: CORUJAO-VII-RELATORIO-FINAL.md.

## Entrega da TASK-201 nos fontes

- Escolha de nome, quatro raças, prévia e cosméticos compatíveis dentro do
  modal Radmin, com fallbacks explícitos e validação autoritativa.
- Aplicação 5/frame de criação; contrato V1–V4 preservado no caminho legado.
- Reconexão com a escolha anterior e nova identidade de sessão.
- Launcher fecha após confirmação do processo vivo; modal e navegação são
  descartados ao entrar; servidor do anfitrião sobrevive à transferência e
  reconexão, encerrando com a janela final.
- Prévia e jogo recebem os mesmos atlas/camadas carregados pelo worker.
- Cancelamento Radmin e encerramento durante reconexão preservam a decisão
  do usuário; erro tardio não reabre janela. Falha ao gravar ACK encerra somente
  o processo novo e conserva o lock até confirmar sua saída.

O baseline da auditoria foi preservado como histórico. Os testes e números
das rodadas novas constam em [TASK-201-TESTES.md](TASK-201-TESTES.md).
A rodada final compilou 243 fontes e 209 arquivos de testes: 671 executados,
651 aprovados em headless, 20 abortados por GUI e zero falhas/skips. O consolidado
Swing aprovou 28 execuções, cobrindo os 20 abortos e repetindo oito casos.
Python: 97 aprovados, incluindo três de handoff; PowerShell: 39 verificações
do atualizador e 21 do transporte GitHub aprovadas após a correção do ACK.

A integridade final registra 1.242 arquivos do baseline: 14 alterações
previstas, 1.228 intactos e nenhum ausente; 12 arquivos novos de código/testes.
Foram preservados 705 assets, oito arquivos pessoais e as 2.432 exclusões
anteriores. O patch contra o baseline está em
`.task-checkpoints/TASK-201/corujao-vii/implementation/task201-source.patch`.

## Disponibilidade

A implementação está nos fontes locais. A distribuição GitHub 0.201.0
permanece íntegra e imutável, com executáveis/scripts anteriores. Não houve
novo pacote, preview, commit, push ou publicação. Para executar os fontes:
`rodar-prototipo.cmd` e opções Radmin do Portal de entrada.

Detalhes: [TASK-201.md](TASK-201.md).
Continuidade: [CONTINUIDADE-SPIRIT-GATE.md](../CONTINUIDADE-SPIRIT-GATE.md).


## Fechamento local de 31/10/2026

Java final: 689 executados, 668 aprovados em headless, 21 abortos GUI, zero
falhas/skips. Complemento: 29 execucoes graficas aprovadas, cobrindo os 21 abortos
e repetindo oito casos. Python: 97 aprovados; atualizador: 39 checks; transporte
GitHub simulado: 21 checks. Pacotes privados 0.212.0-private verificados com
391 arquivos por papel e smoke TCP de tres processos usando runtimes/JARs dos
pacotes. A distribuicao oficial 0.201.0 permanece preservada.
