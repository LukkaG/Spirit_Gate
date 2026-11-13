# TASK-211 — expansão finita do mundo

31/10/2026. Usada a skill generate2dmap para planejamento e navegação, com
documentos nativos do World Studio (conforme a orientação de usar o schema
do projeto). Arte `art_source=existing`: recortes Karakura já instalados.
Sem geração de imagens, novas dependências ou edição dos PNGs originais.

Plano: duas regiões 960x640, origem superior esquerda, grade 32, câmera atual,
corpos físicos preservados e arte em camadas. Bosque: spawn histórico, slime,
clareira leste com lobo e árvores de copa sobreposta/tronco abaixo. Vila:
Anciã, mercado com NPC Nilo, edificação existente e jardim. Caminhos e duas
passagens recíprocas ligam áreas; chegadas conservam margem física. Portais
ganharam indicadores de interação P. Não se criou streaming ou terceira região.

CorujaoSevenWorld cria LiveRevision validada, com referências de asset por ID e
hash. LauncherSession.startExpanded usa essa revisão somente quando ainda não
existe revisão publicada; conteúdo já publicado não é substituído. A fábrica
start histórica conserva o baseline. Com catálogo de teste vazio
conserva marcadores/geometria sem inventar imagens.

Arquivos: CorujaoSevenWorld, LauncherSession, StudioMapRenderer.portalHints,
launcher e Task211WorldExpansionTest. Documentos .sgmap e imagens com colisões:
`.task-checkpoints/TASK-211/corujao-vii/`. A loja é ponto de interesse/NPC;
comércio online não foi acrescentado ao escopo de inventário.

Diagnóstico da skill: o primeiro doctor encontrou `encoding.stdout` cp1252;
reexecutado com PYTHONUTF8=1, esse erro foi resolvido. Persistem avisos de
múltiplos python.exe no PATH e ausência de ffmpeg, resvg-py, APIs de mídia e
Grok CLI. São ferramentas opcionais de geração/vídeo, não usadas nesta entrega.
Os JSONs originais do doctor registram os avisos integralmente.
