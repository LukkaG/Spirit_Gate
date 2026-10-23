# TASK-159 — Relatório

Encerramento técnico experimental da rodada autorizada até TASK-159, com limitações aceitas pelo escopo. Não iniciar TASK-160.

Pacote privado: `dist/SpiritGate-CorujaoIV-studio-0.159.0-rc2-20261023T033925Z-1d5d45ef.zip`, acompanhado de manifesto e SHA-256. Runtime Java integrado, Studio, RPG offline, servidor experimental, clientes A/B e inicializador localhost. Pacotes anteriores preservados.

Regressão final: 465/465 Java em ambiente gráfico; headless 457 aprovados e oito abortados ambientais, zero falhas/ignorados; Python 30/30. Dois clientes nativos observaram dois jogadores e aparências distintas, com encerramento normal dos três processos. Studio nativo abriu e encerrou normalmente. Evidências detalhadas em `TASK-159-TESTES.md`.

Inicialização do cliente corrigida: janela de conexão mantém o EDT vivo durante carregamento assíncrono. A falha inicial e a limitação do harness PowerShell foram preservadas e documentadas; o teste final reproduzível usa `tools/smoke_private_multiplayer.py`.

Protocolo: envelope V1 e bytes históricos preservados; extensão cosmética negociada pela aplicação 2, com peers legados recebendo State V1. Aparência usa IDs do catálogo, sem atributos, caminhos arbitrários ou persistência no save V2.

Entrega de rede é de movimento e aparência localhost. Combate RPG compartilhado permanece indisponível, LAN física pendente, sem contas/persistência online ou segurança de produção homologada. Quests autoradas usam ledger em memória; runtime de mapas continua limitado a 960×640. As 180 camadas PNG são temporárias. Offline e Coop histórico preservados.

Relatórios, benchmarks, capturas, inventários e roteiros humanos estão em `docs/CORUJAO-IV-*.md` e checkpoints individuais. Os 486 arquivos originais de assets permanecem idênticos por SHA-256. Não houve push, publicação ou alteração de saves pessoais. A correção de fronteira dos checkpoints 156/157 está declarada no relatório 157, com originais preservados.

Regressoes finais, executaveis privados do Studio, servidor e dois clientes RPG com camadas temporarias; consolidacao e encerramento do Corujao IV.

Limitações: Combate RPG compartilhado indisponivel; LAN fisica e seguranca de producao pendentes; sprites temporarias; quests autoradas em memoria; mapas runtime limitados a 960x640. Sem contas ou persistencia online.

Validação: JUnit: 465 executed, 457 passed, 8 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
