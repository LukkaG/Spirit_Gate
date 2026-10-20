# Formato World Studio V1

Arquivos `.sgmap` são Java Properties em UTF-8 com `format=spirit-gate-world-studio` e `schema=1`. Não são saves de personagem nem substituem LocalArenaMap. O leitor recusa versões desconhecidas, campos desconhecidos, duplicações, contagens excessivas e UTF-8 inválido.

Cabeçalho: id, name, region (catálogo village/forest/ruins), width/height em pixels e grid. Registros indexados layer, element, hitbox, portal e trigger possuem contagem explícita. Campos são delimitados por `|`; nomes/textos livres usam Base64URL. Mapas de propriedades codificam pares chave/valor, sem código executável. Assets usam ID estável do caminho e SHA-256 da revisão, além do recorte de origem.

Limites: 8192 por eixo, 100.000 elementos, 32 camadas, 4.096 hitboxes, 64 portais, 256 eventos e 32 MB por arquivo. PNGs não são incorporados ao documento; a exportação confere e copia as revisões necessárias. O editor não atribui aprovação artística automaticamente.

Salvar valida um temporário, conserva backup `.bak` apenas de uma versão anterior íntegra e substitui o destino atomicamente quando disponível. Um arquivo corrompido não é sobrescrito silenciosamente: recuperar backup ou Salvar como. Não há promessa de recuperação após qualquer queda elétrica nem migração automática de formatos legados.

A autoria aceita dimensões maiores; o primeiro adaptador RPG é opt-in e deve informar os limites 960×640 e três regiões de conteúdo do runtime atual.
