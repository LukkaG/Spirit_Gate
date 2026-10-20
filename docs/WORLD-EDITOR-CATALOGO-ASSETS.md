# Catálogo do World Studio

Raízes autorizadas: assets e server/src/main/resources, sob a raiz do projeto selecionado. PNGs são lidos, nunca executados ou modificados. Links e caminhos que saem das raízes são recusados. Limites: 32 MB por PNG, oito milhões de pixels, 8192 por eixo e 4096 imagens indexadas.

ID é SHA-256 do caminho relativo; revisão é SHA-256 dos bytes. A revisão do documento deve coincidir com a biblioteca e o arquivo no momento da exportação. Reindexar não aprova arte nem atualiza silenciosamente referências salvas. Categorias são manuais. Tilesets são recortes virtuais com grade, margens, espaçamento e região; não se presume qualquer layout universal.

Miniaturas 64×64 ficam em memória. Imagens usadas no canvas são carregadas em worker e limitadas por cache LRU de 64 MiB. A pintura não lê arquivos. Avisos de arquivos inválidos aparecem no painel, preservando os demais assets.
