# Refinamento V4 — impacto, fissuras e arte das habilidades

Implementação local de 02/11/2026, na branch `beco`, sobre HEAD `6d72c65`, preservando as alterações anteriores do checkout. A revisão visual e de balanceamento pelo responsável continua pendente.

## Comportamento entregue

- Projéteis skillshot terminam no primeiro corpo elegível ou parede, com animação de impacto única. Sem contato, seguem até o alcance máximo e desaparecem sem impacto falso. A varredura autoritativa do segmento impede atravessar uma hitbox entre atualizações. Projéteis target também respeitam corpos elegíveis no caminho.
- Fogo 2/4 abre a fissura progressivamente desde o personagem. Cada trecho nasce quando a frente chega e depois continua queimando no chão. O corte inicial acompanha a origem; a erupção ocorre no término. Como área persistente, a fissura atravessa os inimigos atingidos, mas respeita paredes.
- Vento 2 lança três projéteis reais nos ticks 0, 3 e 6: intervalos de 0,1 segundo. Cada corte parte da posição atual do personagem e recebe seu próprio contato/impacto.
- Aetheri 3 mostra cinco flechas de energia por onda descendo até os pontos do chão selecionado. Cada chegada produz dissipação; o dano ocorre na chegada, não no início da queda. São cinco ondas nos ticks 12, 24, 36, 48 e 60. As cinco flechas visuais não multiplicam o dano da área.
- Martial Artist recebe nova fonte detalhada para o impacto sísmico, sem ampliar pixels da fonte antiga, e novos efeitos dos socos primários. O multi-hit apresenta cada contato real. O raio de dano sísmico continua 110; sua apresentação permanece 320 × 320.
- Água recebe novas folhas para corte, ataque em área, prisão, vórtice e dash, com registro de bases e pontas para reduzir saltos de posição.

## Arquivos e arte

O executor e o catálogo ficam em `server/src/main/java/com/projetok/rpg/RpgSkillEngine.java` e `RpgSkillCatalog.java`. A apresentação fica em `server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java`. O overlay opcional `assets/skills/refined-v4/manifest.properties` complementa as camadas anteriores.

Foram examinados novamente os 415 PNGs originais de `C:/Users/lucas/Downloads/animations`. As referências de fissura 53/343 e a referência de impacto 6 orientaram arte nova, sem substituir os originais. Oito folhas foram criadas/editadas por geração nativa de imagem, com três ajustes posteriores de margem. Prompts, referências e fontes permanecem em `assets/skills/refined-v4/art/`.

`tools/prepare_skill_assets_v4.py` apenas recorta, registra, compõe e reduz com nearest. Não repinta, recolore ou amplia as fontes. Nove atlas totalizam 75 frames; o efeito de impacto é compartilhado por aliases de metadados. A reprodução independente produziu nove PNGs e dois metadados idênticos. Os 91 PNGs anteriores e os 415 originais mantiveram seus hashes. QA não encontrou frames vazios, pixels de borda ou avisos.

A [prancha e capturas de dois clientes reais](skills-preview-v4/README.md) documentam os resultados. Nenhuma animação corporal nova foi acrescentada aos personagens.

## Verificação

Evidências em `.task-checkpoints/SKILLS40/refinamento-v4/`:

- Suíte geral Maven offline: **876 casos, zero falhas/erros, 39 testes gráficos ignorados em headless**, em `full-headless.log`.
- Complemento com AWT habilitado: **69 execuções aprovadas, zero falhas/erros/ignorados**, em `gui-green.log`. Inclui cinco cenários novos com servidor TCP e dois clientes, além de controles, Sentinela, HUD, ciclo de vida e apresentação.
- Agregado do último XML por classe: **876 casos aprovados em 243 classes**, em `verification-final.json`. É uma composição das duas rodadas, não uma única invocação Maven.
- A ferramenta `tools/SkillsLocalPreview.java` compilou com `javac --release 17` sobre as classes atuais.

Os comandos foram executados em `server/`, com `mvn test -o`, cache `../.maven-repository` e logs isolados no checkpoint. O complemento usou `-Djava.awt.headless=false` e a seleção de classes registrada no log. As regressões verificam primeiro contato, parede versus alcance vazio, cadência do vento, dano apenas na chegada da chuva, substituição da trajetória pelo impacto e crescimento da fissura.

As primeiras rodadas expuseram expectativas antigas de pré-limitação por parede e do evento antecipado do multi-hit; os testes foram alinhados às novas fases e complementados por regressões. Na sequência gráfica houve uma falha na fotografia inicial de listeners: a janela anterior ainda tinha `windowClosed` pendente. A fixture de `GameInputRoutingTest` passou a drenar a fila AWT antes dessa fotografia, mantendo intacta a comparação final contra vazamentos. A repetição integral das 69 execuções passou. Nenhuma regra de produção de input foi alterada para essa correção.

Três capturas históricas recriadas pela suíte foram arquivadas em `suite-generated/`, com hashes, restaurando suas exclusões preexistentes no checkout. Não foram descartadas alterações alheias.

## Compatibilidade, limites e reversão

Não foram alterados mensagens, serialização, negociação, saves, banco, publicação ou launcher. Os eventos da aplicação 7 existente carregam origem, destino e fase; cliente e servidor atualizados em conjunto apresentam as novas fases. A compatibilidade anterior V6 permanece. Colisão, dano, temporização e cancelamento continuam autoritativos. Offline/Coop não receberam essas alterações de apresentação multiplayer.

O seletor de domínios da prévia continua disponível para trocar kits sem fechar a janela. Para carregar esta revisão dos fontes e atlas, encerre a prévia antiga uma vez e execute `tools/testar-skills.cmd` novamente.

Permanecem pendentes a avaliação humana da estética e do balanceamento, testes físicos/Radmin e launcher nativo. As capturas e testes locais não constituem homologação desses ambientes.

Para reverter, revisar somente os hunks desta rodada contra `baseline/` do checkpoint, incluindo executor, catálogo, apresentação e fixture, e remover a referência ao overlay V4. Os atlas anteriores permanecem disponíveis. Não usar reset global: o checkout contém amplo trabalho preexistente. Não houve commit, push ou publicação.
