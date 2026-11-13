# Corujão VII — pendências

## Estado final de 31/10/2026

TASKs 201–212 implementadas e validadas tecnicamente no ambiente local.
O usuário autorizou o plano completo e solicitou PDF. Relatório consolidado:
[CORUJAO-VII-RELATORIO-FINAL.md](CORUJAO-VII-RELATORIO-FINAL.md).

Pendências externas: revisão visual/artística, legibilidade e homologação entre
dois computadores/Radmin físico. O smoke empacotado usou runtimes/JARs; não
executou separadamente o EXE nativo. Launcher coberto pelas suites existentes.

Limites: Aetheri/Spirit usam Human provisório; Reaper sem novos acessórios;
combate usa sinais geométricos, sem quadros novos; técnicas descritivas de domínio
não são skills executáveis; Shunpo exige nível 20 e domínio nível 15; uma missão
instalada; duas regiões 960×640; progresso/inventário efêmeros. Loot individual
por contribuição de jogador vivo da região. Sem economia persistente ou conta real.

Pacotes privados em `dist/SpiritGate-CorujaoVII-0.212.0-private`, sem feed remoto.
Distribuição oficial 0.201.0 intacta. Sem push/release/deploy. Gates A–E verificados
localmente; revisão humana/externa não foi inventada.

O texto abaixo preserva o registro histórico anterior à autorização ampliada.

Atualização: 31/10/2026.

## Decisões resolvidas

O usuário aprovou explicitamente a implementação da TASK-201 depois da
auditoria: aplicação opt-in 5 e frame de criação antes da admissão, fallback
Human informado para Aetheri/Spirit e Reaper sem acessórios incompatíveis.
Também aprovou o ajuste do fluxo de janelas. A aprovação exigida pela seção
20 do [plano recebido](CORUJAO-VII-PLANO.md) não está mais pendente.

Formulário, ingresso autoritativo, reconexão, modal Radmin, navegação e
handoff WinForms já foram implementados. A apresentação carregada é
compartilhada pela prévia, pelo jogo e pela reconexão.

Também foram concluídas as correções verificadas de cancelamento Radmin,
reconexão durante confirmação de encerramento, supressão de erro tardio e
falha ao gravar o ACK após iniciar o processo. O consolidado final aprovou
28 execuções gráficas, cobrindo os 20 abortos headless e repetindo oito casos.
Java completo: 671 executados, 651 aprovados, 20 abortos GUI e nenhuma falha;
Python: 97 aprovados. Detalhes em [TASK-201-TESTES.md](TASK-201-TESTES.md).

## Próximos passos após a validação local da TASK-201

- Revisão local do responsável sobre o fluxo, a legibilidade e a experiência
  visual. As capturas e asserções automatizadas não representam esse aceite.
- TASK-202 autorizada em 31/10 e implementada localmente: revisar visualmente
  o catálogo e as limitações documentadas em [TASK-202.md](TASK-202.md).
- Homologação entre dois computadores/Radmin quando essa etapa for organizada;
  os testes locais não a substituem.

## Recursos e limites mantidos

- Aetheri/Spirit não têm atlas próprio; continuam usando aparência Human
  provisória indicada na interface.
- Reaper conserva os PNGs existentes e ainda não possui acessórios compatíveis.
- Contas reais, persistência pessoal online, novos bancos e autenticação
  pública não fazem parte desta tarefa.
- Não houve nova homologação entre dois computadores ou Radmin físico.
- O catálogo da TASK-202 está implementado; faltam atlas próprios Aetheri/Spirit
  e arte Run/Attack/Hit/Death. Não são declarados como recursos existentes.
- O Gate A tem implementação local 201/202; ainda requer revisão humana.

## Distribuição

`dist/SpiritGate-GitHub-0.201.0` conserva sua entrega anterior. A seleção e o
novo fluxo de janelas estão nos fontes locais e não chegaram ao EXE oficial.
Para usar os fontes atuais: `rodar-prototipo.cmd` e opções Radmin do Portal
de entrada. Não foi gerado preview ou novo pacote nesta TASK.

Publicação GitHub e dependências novas continuam exigindo autorização
específica. O empacotamento privado do ciclo permanece previsto para a
TASK-212; não executá-lo como consequência desta implementação.
