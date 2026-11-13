# TASK-201 — relatório de implementação

Fechamento local: 28/10/2026. Auditoria e aprovação: 27/10/2026.
Branch `beco`, HEAD
`6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`.

**Estado: implementada e validada localmente; fontes congeladas.**
A TASK-202 não foi iniciada.

## Resultado jogável

Depois da auditoria, o usuário aprovou a arquitetura da TASK-201 e acrescentou
o ajuste das janelas de entrada. O painel de personagem agora fica dentro
da conexão modal Radmin e permite escolher nome, Reaper, Human, Aetheri ou
Spirit, visualizar o corpo disponível e confirmar cosméticos compatíveis.

A aplicação negociada 5 envia o perfil antes de o servidor admitir o jogador.
O servidor valida os dados e cria o mundo com a raça escolhida. Dois jogadores
podem usar a mesma raça e o mesmo nome com identidades distintas. A reconexão
mantém a escolha e cria outra sessão. O caminho legado V1–V4 continua com seu
comportamento histórico, e o cliente novo orienta atualizar um servidor sem
a capacidade necessária, sem trocar silenciosamente a raça escolhida.

Reaper conserva seus PNGs originais e não oferece acessórios incompatíveis.
Aetheri/Spirit usam aparência Human provisória, identificada na interface.
A ausência de um atlas necessário impede confirmar a escolha; não foi
gerada arte nova nem alterada a hitbox.

## Fluxo de janelas

- A UI WinForms fecha após o processo do jogo confirmar que iniciou e permanece
  vivo na checagem inicial. Falhas imediatas mantêm a UI recuperável.
- A conexão Radmin é um `JDialog` modal com dono, não outra janela independente.
- Após a entrada no jogo, as telas de navegação identificadas são descartadas.
- O anfitrião continua vivo depois desse descarte e durante reconexões.
  O encerramento confirmado da janela final encerra o servidor correspondente.
- A prévia, o jogo e a reconexão usam a mesma apresentação já carregada.
  Isso evita escolher outro atlas por causa do diretório de execução.

A confirmação WinForms indica processo vivo, não o fim do carregamento do
mundo. A revisão final corrigiu e verificou quatro pontos: apresentação
compartilhada por prévia/jogo/reconexão; conclusão do worker durante a decisão
de cancelar a conexão Radmin; reconexão durante confirmação de encerramento
da partida, incluindo erro tardio sem popup; e falha ao gravar o ACK após
iniciar Java. Nesse último caso, o engine encerra somente o processo
recém-criado e mantém supervisão e lock até confirmar a saída.

## Validações registradas

- Java completo: **671 executados, 651 aprovados, 20 abortados por GUI,
  zero falhas/skips**; 243 fontes e 209 arquivos de testes compilados.
- Complemento gráfico consolidado: **28 aprovados**, cobrindo os 20 abortos
  e repetindo oito casos já aprovados; zero abortos/falhas/skips.
- Python: **97 aprovados**, incluindo os três testes de handoff.
- PowerShell: **39 verificações do atualizador e 21 do transporte GitHub** aprovadas.
- Compilação de entrada local: 243 fontes atualizadas; fontes estáveis durante
  os testes finais.
- Integridade: 705 assets e oito arquivos pessoais preservados. Dos 1.242
  arquivos do baseline, 14 têm alterações previstas, 1.228 estão intactos e
  nenhum está ausente. Há 12 arquivos novos de código/testes; as 2.432 exclusões
  anteriores foram preservadas.
- Distribuição oficial e fonte de empacotamento conferidas e preservadas.

Essas contagens correspondem às rodadas finais registradas. Não somar
repetições gráficas como testes únicos. Detalhes em
[TASK-201-TESTES.md](TASK-201-TESTES.md), com evidências em
`implementation/review-cancel/`, `lifecycle-reconnect/` e `launcher-handoff/`
sob `.task-checkpoints/TASK-201/corujao-vii/`.

O patch de revisão `implementation/task201-source.patch` e o inventário
`source-changes.json` registram 26 arquivos de código/testes contra o baseline:
14 existentes alterados e 12 novos. As mudanças anteriores foram preservadas.

## Distribuição e limites

As mudanças estão nos fontes locais. `dist/SpiritGate-GitHub-0.201.0` conserva
seus executáveis e scripts anteriores, sem receber a seleção ou o handoff novo.
Nenhum novo pacote ou preview foi gerado e nada foi publicado no GitHub.
Para executar o código novo localmente, usar `rodar-prototipo.cmd` e as opções
Radmin do Portal de entrada; `jogar-rpg-offline.cmd` também abre esse caminho.

Não houve teste novo em dois computadores, aprovação artística humana,
contas reais, persistência pessoal online, dependência nova, commit ou push.
O aceite informado do Corujão VI permanece apenas como contexto histórico.

## Próximo passo

Submeter a experiência local à revisão do responsável. A TASK-202 depende
de nova instrução; o Gate A
não é encerrado pela TASK-201 isoladamente. O empacotamento privado do ciclo
continua reservado à TASK-212, quando autorizado e com qualidade suficiente.
