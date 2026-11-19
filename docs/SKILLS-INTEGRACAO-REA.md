# Integração das sprites REA — 18–19/11/2026

As 40 habilidades existentes passaram a usar os VFX dos pacotes `Skill_Collection_v1` e `WATER_Pack_v1`, fornecidos em `C:/Users/lucas/OneDrive/Desktop/REA/Spirit_Gate_VFX`. Integração local na branch `beco`, sobre HEAD `c3d484e`, preservando as alterações de combate e documentos que já estavam no checkout. Não houve commit, publicação ou atualização de pacote distribuído.

## Revisões selecionadas e conteúdo

| Habilidades | Revisão |
| --- | --- |
| FIRE_5 | v3 |
| MARTIAL_1–5 | v3 |
| SPIRIT_1–5 | v2 |
| AETHERI_1–5, FIRE_1–4, LIGHT_1–5, THUNDER_1–5, WIND_1–5 | v1 |
| WATER_1–5, pacote separado | v1 |

O importador escolhe a maior revisão **numérica por habilidade**. Importou 219 fases, com 5.766 frames, para `assets/skills/rea-v1`. Os PNGs finais foram copiados sem repintura, recorte, interpolação ou alteração de pixels. As pastas de origem e os assets anteriores do jogo foram preservados.

Seis camadas marciais contêm corpos/poses, não apenas efeitos: `body_action`, `body_combo`, `body_stomp`, `attacker` e `target`. Foram excluídas da camada de VFX para não desenhar um segundo personagem sobre o herói. Não se alterou o atlas aprovado do personagem nem se integraram os estudos de corpos, cabelos e `Character_Actions_v1` como substituições de aparência.

O [manifesto de proveniência](../assets/skills/rea-v1/provenance.json) registra revisão, fase, origem, SHA-256, geometria, pivô e tempos. A [folha de contato](../assets/skills/rea-v1/contact.png) permite conferir as famílias.

## Formato e ordem verificados

- Recorte de cada frame comparado ao respectivo PNG individual; hashes RGBA conferidos quando presentes na fonte.
- Retângulos, tempos e ordem comparados aos metadados Aseprite; nenhuma divergência detectada na importação.
- Linhas direcionais preservadas: **N, NE, E, SE, S, SW, W, NW**. Fases radiais têm uma única linha. O cliente seleciona a linha desenhada, sem girar ou espelhar a arte.
- Tempos variáveis por quadro, pivôs, alfa e dimensões nativas preservados. Entrada e saída não reiniciam como loops.
- Existem **260 frames totalmente transparentes nas próprias fontes**, principalmente contatos e dissipações. Foram preservados e relacionados na proveniência; não foram tratados como arquivos ausentes nem removidos para acelerar a animação.
- Ondas e contatos são numerados por lançador/habilidade. O contador global de eventos e o tick absoluto do mundo não escolhem a primeira onda.
- A erupção e o impacto sísmico reservam duração visual para preparação/abertura, ápice/acomodação e dissipação, evitando suprimir uma fase inteira.

## Integração e prioridade de tela

`NetworkSkillPresentation` carrega o overlay REA depois dos assets anteriores. O novo `ReaSkillAssets` interpreta as fases e seus metadados. Se o pacote opcional não existir, o carregamento histórico continua disponível. Pacotes presentes com caminhos, geometrias ou metadados inválidos são recusados e seguem o tratamento de erro de assets já existente no cliente; não há fallback silencioso para corrupção.

A ordem efetiva em `DebugPanel` já desenhava atores antes do overlay de mundo. Ela foi preservada: **mundo → personagens → VFX das habilidades → HUD**. Os efeitos usam o PNG completo sobre os atores, com alfa real; a transparência permite ver partes do personagem, mas um pixel opaco do efeito o cobre. Nenhuma camada foi promovida sobre a HUD.

As investidas usam posições já observadas do personagem para seus rastros. Chuvas nativas são composições completas de área: não são repetidas cinco vezes nem rotacionadas como se fossem um projétil isolado. As cinco ondas de fogo e os contatos da chuva de flechas continuam sendo somente apresentação dos eventos existentes.

Os 219 recortes estão catalogados. Fases auxiliares que exigem informação não transmitida pelos eventos atuais — por exemplo, ativação/cancelamento da postura Sentinela para observadores e certos sockets/status individuais — não são disparadas por suposição. Os disparos da Sentinela usam as novas flechas; a integração não acrescenta mensagens ao protocolo para exibir essas camadas auxiliares. Isso é uma limitação desta entrega visual.

## Arquivos e compatibilidade

Alterações desta entrega:

- `assets/skills/rea-v1/`: PNGs, metadados, manifesto e proveniência.
- `tools/import_rea_skills.py`: importação e conferência reproduzíveis.
- `server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java`: carregamento, fases REA, numeração de ondas e apresentação de rastros.
- `server/src/main/java/com/projetok/multiplayer/ReaSkillAssets.java`: leitura e desenho de fases, com tempos e direções nativos.
- `server/src/test/java/com/projetok/multiplayer/ReaSkillAssetsTest.java`: cobertura do novo formato e da prioridade.
- Testes `NetworkSkillPresentationTest`, `SkillImpactRefinementGuiTest`, `SkillLifecycleGuiTest` e `SkillSlashRefinementGuiTest`: fallback histórico, novas condições de carregamento e destino das evidências.
- Este relatório e `docs/skills-preview-rea/`.

Nenhum arquivo de regras de combate, protocolo, persistência, offline/Coop, launcher ou World Studio foi alterado por esta entrega. As modificações preexistentes nesses sistemas foram preservadas. Não houve alteração de dano, alcance, hitboxes, recargas ou progressão. A compilação local incorpora o estado atual completo do checkout, incluindo essas alterações anteriores.

## Validação executada

JDK Temurin 17 e Maven 3.10.0 disponíveis; nenhuma dependência foi instalada. A importação foi executada com:

```powershell
python tools/import_rea_skills.py 'C:/Users/lucas/OneDrive/Desktop/REA/Spirit_Gate_VFX'
```

Foram validados **75 testes em nove classes**, sem falhas ou testes ignorados nos resultados finais de cada classe. A [consolidação JSON](skills-preview-rea/validacao.json) registra os resultados. O conjunto foi executado e os testes afetados foram repetidos após as correções finais:

```powershell
mvn -o -f server/pom.xml '-Dtest=ReaSkillAssetsTest,NetworkSkillPresentationTest,SkillTargetPresentationTest,SkillProtocolTest,SkillDisplacementServerTest,NetworkSkillGuiTest,SkillImpactRefinementGuiTest,SkillLifecycleGuiTest,SkillSlashRefinementGuiTest' test
mvn -o -f server/pom.xml '-Dtest=NetworkSkillPresentationTest' test
mvn -o -f server/pom.xml '-Dtest=ReaSkillAssetsTest,SkillLifecycleGuiTest,SkillSlashRefinementGuiTest' test
```

A primeira execução conjunta detectou uma expectativa histórica de `thunder_5.travel`; ela foi atualizada para a fase REA de área intangível. Os testes dos atlas antigos de 12 quadros continuam executados explicitamente contra o fallback histórico. Não se alterou a arte para satisfazer a geometria antiga.

Também foi executado `powershell -NoProfile -ExecutionPolicy Bypass -File tools/compile-local.ps1 -PassThru`: 287 fontes compiladas para Java 17, com sucesso, atualizando somente `server/target/launcher/classes` e seu manifesto local. `git diff --check` passou. Esse build local não é uma publicação nem valida um pacote nativo distribuído.

Cobertura nova: 40 habilidades em oito direções e múltiplos instantes, passagem pelo apresentador real com lançamento/impacto/replay/limpeza, tempos variáveis, ordem das ondas independente do tick global, sequência completa da erupção, rejeição de caminhos externos, pacote ausente, composição única das chuvas e sobreposição dos pixels da habilidade sobre o personagem.

Os testes gráficos abriram **duas janelas com clientes TCP locais** e verificaram conjurador/observador, input real, carregamento, expiração e conexão. Exemplos de capturas inspecionadas:

- [Vórtice de água](skills-preview-rea/runtime/SkillImpactRefinementGuiTest/vortex-agua-polido.png).
- [Chuva de flechas](skills-preview-rea/runtime/SkillImpactRefinementGuiTest/chuva-em-queda.png).
- [Chuva de fogo v3](skills-preview-rea/runtime/SkillSlashRefinementGuiTest/fire-apice.png).
- [Fissura](skills-preview-rea/runtime/SkillImpactRefinementGuiTest/fissura-erupcao.png).

As capturas antigas que os testes regeneraram foram preservadas em uma pasta nova de evidências e os arquivos históricos do Git foram restaurados. Não se descartou trabalho anterior do desenvolvedor.

## Pendências, reversão e próxima revisão

Não foram executados testes em Radmin, outra máquina ou launcher nativo distribuído. A aprovação artística das 40 habilidades em todas as situações reais de combate permanece uma revisão humana; a conferência automatizada não equivale a essa aprovação. Camadas auxiliares sem evento suficiente e animações corporais continuam separadas, como explicado acima.

Para desativar apenas o novo conjunto, renomeie localmente `assets/skills/rea-v1/manifest.properties` para um nome que o carregador não procure, com o jogo fechado. A ausência desse manifesto reativa os assets anteriores. Para remover a implementação, reverta somente os diffs desta entrega; não restaure globalmente os arquivos de combate que já estavam modificados.

Próximo passo sugerido: abrir `tools/testar-skills.cmd` e revisar os oito kits, sobretudo escala/pivô das áreas, leitura do personagem sob os VFX e as camadas auxiliares cuja ativação exigiria um contrato de eventos adicional. Não foi preparada publicação.
