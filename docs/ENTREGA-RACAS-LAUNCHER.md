# Entrega: raças, domínios e launcher desktop

Data: 21/10/2026. Candidato local de desenvolvimento; não representa lançamento
comercial, integração financeira ou homologação multiplayer.

## Fluxo atual

- `rodar-prototipo.cmd` e `jogar-rpg-offline.cmd` abrem o mesmo cliente atual.
  Um compilador compartilhado verifica hashes de fontes, recursos e saídas,
  recompila quando necessário e recusa usar classes antigas após uma falha.
- A criação oferece Reaper, Human, Aetheri e Spirit. O personagem começa como
  aprendiz sem domínio: espada pequena para Reaper, socos para Human/Aetheri,
  feixes luminosos básicos para Spirit. Todos podem optar por socos básicos.
- Dez caminhos estão cadastrados e possuem prévias animadas de conceito.
  A escolha é racial e só é aceita a partir do nível 15. Não existe troca normal
  nem armazenamento de vários domínios para alternância.
- XP geral e de domínio são independentes. Mob de nível 1 concede 3 XP geral e
  1,5 XP de domínio após a escolha. O armazenamento usa meias unidades inteiras.
  Quests também concedem XP; a proporção de metade no domínio é provisória.
- Shunpo ofensivo e defensivo exigem nível 20 para todas as raças, compartilham
  recarga e preservam seu estado ao salvar. Ofensivo exige alvo visível e destino
  livre; defensivo executa um recuo com uma janela curta de esquiva.
- A regra de reset exige 35 Spirit Coins (equivalentes a R$ 35), quest e intervalo
  de um mês. A regra preserva nível geral, identidade, inventário e equipamento,
  e zera o domínio. A operação comercial não está disponível no cliente local.
- Saves V2 usam raça explícita. Saves V1 de Guardian/Mystic são lidos como
  Reaper/Human, sem domínio adquirido e preservando a base e o progresso.
  Arquivos pessoais não foram removidos nem regravados nesta entrega.

## Limpeza e correções

Removidos o login legado `KServer`/`Kclient`, protocolo `Network`, conexão
`DatabaseManager`, entrada vazia `Main`, catálogo antigo `Archetypes` e recurso
`archetypes.properties`. As dependências de produção Kryonet e PostgreSQL
também foram retiradas. O menu atual não oferece mais a arena histórica.

O motor compartilhado, os testes de regressão e o World Studio foram preservados,
pois ainda sustentam renderização, movimento, colisão e autoria de mapas. Os
relatórios HTML de revisão de arte não são um cliente web substituto do launcher.

Corrigidos três defeitos da auditoria: repetição de evento de morte ao atacar um
alvo morto, perda de XP ao aplicar recompensa a um estado válido e seleção de
inimigo oculto impedindo ataque a outro visível. A atribuição de XP geral/domínio
também valida ambas as parcelas antes de alterar o personagem.

O laboratório web anterior foi conferido via Sites e teve o acesso público
retirado. A política retornada é `custom`, apenas proprietário, sem visitantes
externos. O conector não oferece exclusão definitiva; não foi feito novo deploy.

## Verificação

- **191 fontes Java e 144 arquivos de teste compilados** com alvo Java 17.
- **413 testes Java aprovados, 6 ignorados/abortados, zero falhas** na execução
  completa pelo motor JUnit Jupiter. Log em
  `server/target/local-tests/run-g1bqk1zk/test-results.log`.
- **30 testes Python aprovados**, via `python -m unittest discover -s tools -p 'test_*.py'`.
- Criação de personagem e as dez prévias renderizadas sem janela e inspecionadas.
  Capturas em `server/target/ui-validation/`. Foi corrigida uma falha de layout
  que podia ocultar a cena da prévia no primeiro desenho.
- `git diff --check` sem erros de whitespace. Métricas históricas que os testes
  reescrevem foram preservadas, sem substituir relatórios anteriores.
- O executável do pacote portátil foi verificado após extração nova do ZIP em
  caminho com espaços: retorno 0 e relatório `ok=true`, quatro raças, dez domínios,
  save V2 e pagamentos desativados. Foram conferidas as 148 entradas do pacote
  e seus hashes. Caminhos, hashes e resultado detalhado ficam em
  `docs/VALIDACAO-LAUNCHER-RACAS.json`.

O ambiente usa JDK 24, compilando para Java 17. A execução dos testes nesta sessão
precisou ocorrer fora das restrições do sandbox por falha de leitura de classpath
do compilador nesse ambiente. O executor `tools/run-local-tests.py` usa JUnit já
presente no cache local; não baixa dependências nem altera configuração global.
Isso não substitui executar o pacote em outra máquina Windows.

## O que continua em desenvolvimento

O percurso jogável da pequena alma, as árvores e skills finais dos domínios,
conteúdo da quest de reset, carteira/pagamentos reais, balanceamento, instalador,
atualizador e homologação de lançamento ainda precisam ser produzidos ou validados.
As prévias são demonstrações visuais, não a implementação final dessas skills.

O cliente atual é local. Controles poderem atingir aliados da mesma party está
registrado como regra, mas ainda depende das skills de controle e da integração
multiplayer; não foi simulado como uma funcionalidade online pronta.

Custos e alcance do Shunpo são provisórios: 25 de vigor, 90 ticks de recarga,
240 pixels de alcance ofensivo, recuo defensivo de 70 pixels e janela de 6 ticks
em simulação de 30 Hz. Apenas o requisito de nível 20 está fechado pelo design.
Os mobs existentes são tratados como nível 1 até haver regra de recompensa para
outros níveis. A curva de nível geral permanece provisória, sem teto de design;
o limite de 1.000.000 XP é uma proteção técnica do formato de save.

Até decisão diferente, a implementação considera intervalo mensal por personagem,
medido por um mês de calendário, e não acumula XP de domínio antes da iniciação.
Essas interpretações estão documentadas para revisão posterior.

Documento de regras atualizado: [Raças e domínios](SISTEMA-DE-RACAS-E-DOMINIOS.md).
Versão para leitura: [PDF](Spirit_Gate_Racas_e_Dominios.pdf).
