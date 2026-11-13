# Refinamento V3 — Martial Artist, projéteis e Sentinela

## Escopo e decisões

Pedido atendido sobre a revisão V2, preservando o checkout local da branch `beco`, HEAD `6d72c65`. O responsável também confirmou que o básico do Aetheri deve ser skillshot fora da Sentinela e target somente durante o estado.

- Caminhada da aplicação 7 e eixo de smartcast usam quatro direções. Duas direções simultâneas conservam o eixo dominante, com preferência vertical no empate, igual à pose do atlas. Leques mantêm a abertura ao redor desse eixo; projéteis target continuam seguindo seu destinatário.
- Os efeitos viajantes recebem elevação visual de 28 unidades: canvas 64 e pivô dos pés 60 colocam o centro em 32. Trajetória e impacto recebem a mesma elevação, sem deslocar hitboxes para cima no plano do mapa. Fissuras, crateras e áreas permanecem no chão.
- Impacto sísmico passa de 240×240 para 320×320 na apresentação, preservando raio de dano 110. Combo marcial passa de alcance 100/abertura 70 para alcance 170/abertura 95, com seis impactos e animação própria de punhos/chutes.
- Aetheri recebe energia azul nas cinco técnicas, preservando os efeitos de gelo anteriores em V2. A técnica 1 continua target independente; o básico frontal usa o dano, alcance e recarga básicos, sem energia ou recarga da técnica 1. O disparo pode errar mesmo com inimigo selecionado.

## Sentinela

A segunda técnica pode ser ativada sem alvo. Imobiliza o usuário por **120 ticks/4 segundos**, mas não dispara automaticamente. SPACE usa o inimigo selecionado como alvo durante a janela; alvo, geração, alcance, obstáculos e intervalo são revalidados no servidor. Sem alvo válido, não há fallback para básico livre.

Não há cinco munições fixas. A cadência provisória é `10 / (1 + min(AGI,100)/100)` ticks, usando a regra de atributos existente e conservando frações entre disparos. Assim, ataques contínuos permitem 12 tiros com AGI 0 e até 24 com AGI 100 durante os mesmos quatro segundos. A agilidade vem da ficha autoritativa.

Conjurar novamente cancela mesmo durante recarga ou stun, sem novo custo e sem remover a recarga de cinco segundos contada desde a ativação. O cancelamento libera somente a imobilidade voluntária; não limpa stun/root independentes. Expiração, morte, saída, viagem, troca de domínio e revisão também descartam o estado. A UI mantém o botão de cancelamento habilitado e mostra segundos restantes.

## Arquivos e compatibilidade

Execução: `rpg/RpgSkillEngine.java`, `RpgWorld.java`, `RpgSkillCatalog.java` e `multiplayer/RpgSessionWorld.java`. Entrada/apresentação: `NetworkSkillControls.java`, `NetworkSkillPresentation.java`. `SkillProtocol.java` recebeu somente documentação do campo legado: o mesmo byte `sentryShots` agora transporta segundos restantes 4..1, com zero inativo. Não houve adição de campos ou mudança da serialização.

Os caminhos de ataque e movimento novos são condicionados à aplicação 7. A reserva de recarga básica do arco e os estados são efêmeros; nenhum save, banco, modo offline/Coop, launcher ou publicação foi alterado. Cliente e servidor desta revisão devem ser usados juntos para a semântica nova da Sentinela.

## Arte e reprodução

O pacote independente `assets/skills/refined-v3/` sobrepõe V1/V2 sem sobrescrever seus PNGs. Duas folhas foram geradas/editadas pela ferramenta nativa **image_gen**: energia azul a partir da flecha V2 e novo combo marcial. Prompts, variantes e referências ficam em `art/prompts.json`; modelo/custo não foram informados pela ferramenta.

`tools/prepare_skill_assets_v3.py` faz somente recorte, registro da ponta, escala nearest e composição das flechas em chuva/leque. Não repinta nem recolore por Python. São sete atlas e 63 frames, mais a escala de apresentação herdada de Martial 3. QA sem frames vazios, falhas de grade ou avisos. A reprodução em pasta separada confirmou os sete PNGs, manifesto e proveniência idênticos.

```powershell
python tools/prepare_skill_assets_v3.py
```

O preparador recusa PNGs V3 que tenham sido alterados manualmente em relação à proveniência. [Prancha dos efeitos](skills-preview-v3/contact.png), [QA](skills-preview-v3/qa.json), [reprodução](skills-preview-v3/reproduction.json) e capturas em `skills-preview-v3/runtime/`.

O diagnóstico da ferramenta de arte foi repetido com UTF-8 após detectar stdout cp1252. Ficou o aviso `python.path` sobre outro `python.exe` no PATH; foi usado Python 3.14.5 em `C:/Python314`. Ferramentas opcionais de vídeo/SVG não instaladas não foram necessárias para esse pacote PNG.

## Testes e evidências

Logs em `.task-checkpoints/SKILLS40/refinamento-v3/`; rodadas sobrepostas não devem ser somadas.

- `core-first.log`: 91 execuções aprovadas na etapa inicial.
- `sentry-first.log`: 85 execuções aprovadas, incluindo o teste TCP real de ativação sem alvo, imobilidade, ataque target, cancelamento, retorno ao básico frontal e caminhada sem diagonal.
- `presentation-first.log`: 21 aprovados e dois casos GUI abortados no modo headless. Inclui os quatro lados do lançamento no centro visual, trajetória, preservação da direção no impacto e fases de Fogo.
- `gui-first.log`: 22 aprovados e uma falha no teste de reset de teclado `SkillAimInputRoutingTest`, após alternância entre janelas. O novo teste gráfico do Sentry/Marcial passou, assim como input de área e apresentação. O mesmo teste de reset passou isoladamente (`gui-focus-isolated.log`), sem modificar o roteador. A complementação gráfica final registra a repetição da sequência.
- `full-headless.log`: **862 casos, 828 aprovados, 34 abortados por depender de janelas, zero falhas/erros**, em 1min41s. Inclui 85 testes do executor após o caso adicional de cancelamento sob stun, mais as regressões de protocolo, combate e suporte.

- `gui-final.log`: **60 execuções aprovadas, zero falhas/erros/abortos**, em 1min14s. Inclui todos os 34 casos gráficos abortados na suíte headless e a repetição do caso de foco que falhou na primeira tentativa. A intermitência anterior permanece registrada, sem alteração no roteador para escondê-la.
- Na revisão final foi alinhado o empate de direção à pose vertical já usada pelo atlas, evitando que um personagem olhando para frente disparasse para o lado após um alvo diagonal. `cardinal-final.log` registrou um erro na fixture nova: o ator de teste recebeu facing `(1,1)` sem normalização e foi corretamente recusado pelo protocolo. Foi corrigida somente a fixture para um vetor unitário. `cardinal-green.log` aprovou **112 execuções**, incluindo executor, TCP, mira, projéteis, animação, alvos e atributos após esse ajuste final.
- A ferramenta `SkillsLocalPreview.java` foi recompilada com `javac --release 17 -encoding UTF-8` em `refinamento-v3/preview-classes`.
- `sentry-facing-final.log`: 112 aprovados após fazer o arqueiro virar para o destinatário ao disparar na Sentinela. O TCP também verifica que, após cancelar e mudar a direção, o básico volta a sair frontal, mesmo com outro inimigo selecionado.

Os relatórios XML das rodadas gerais, gráficas e finais foram arquivados. O agregado mais recente por classe contém **862 casos aprovados, sem falhas, erros ou abortos restantes**, em `verification-final.json`; não é uma única invocação Maven. Três capturas históricas regeneradas pelos testes foram arquivadas em `suite-generated/`, conservando as 2.432 exclusões locais anteriores.

A [verificação de integridade](skills-preview-v3/integrity.json) confirmou 54 arquivos do pacote V1 e 18 PNGs V2 sem modificações. Testes com teclado físico, Radmin entre máquinas, balanceamento jogado e launcher nativo continuam pendentes.

## Revisão e reversão

Os fontes centrais anteriores estão em `.task-checkpoints/SKILLS40/refinamento-v3/baseline/`. Para reverter, revisar apenas os hunks V3 e desativar sua sobreposição de assets, preservando as revisões anteriores e as demais alterações locais. Não usar restauração global do Git. Não houve commit, push ou publicação.

Para testar a versão atual, fechar a prévia anterior e executar `tools/testar-skills.cmd`; controles em [SKILLS-COMO-TESTAR.md](SKILLS-COMO-TESTAR.md).
