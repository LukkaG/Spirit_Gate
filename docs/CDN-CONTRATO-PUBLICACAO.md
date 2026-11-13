# Contrato do pacote de CDN

`tools/preparar_cdn.py` prepara uma árvore estática a partir da saída de `tools/integrar_launcher_radmin.py`. O preparador não autentica contas, não envia arquivos e não muda configurações da instalação pessoal. Recebe a URL HTTPS real da CDN, sem token, query ou credenciais no endereço.

```powershell
python tools/preparar_cdn.py CAMINHO_DA_ENTREGA PASTA_NOVA_DE_STAGING --base-url https://ENDPOINT-REAL-DA-CDN
```

Substitua o endereço acima pelo endpoint atribuído à conta escolhida antes de distribuir o pacote. Um endereço de exemplo não torna o canal disponível.

## Arquivos publicados

Somente o conteúdo de `PASTA_NOVA_DE_STAGING/public` vai à CDN. `deployment-plan.json` fica fora dessa árvore e contém o inventário legível por ferramenta, SHA-256, tipo de conteúdo, cache e ordem de envio. O plano permanece com `published=false`: preparar ou executar um callback não prova que os arquivos foram verificados publicamente.

| Caminho | Política |
| --- | --- |
| `release-<hash>/...` | Imutável, cache de um ano |
| `downloads/<sha256>/<nome-do-zip>` | Imutável, cache de um ano |
| `SpiritGate-Anfitriao-Windows64.zip` | Alias estável, sem cache |
| `SpiritGate-Convidado-Windows64.zip` | Alias estável, sem cache |
| `SpiritGate-Instalar-Windows64.zip` | Alias compatível para Anfitrião, sem cache |
| `latest.json` | Manifesto estável, sem cache; ativado após todos os imutáveis |
| `SpiritGate-Ativar-CDN.zip` e `.sha256` | Kit pequeno de ativação opcional, sem cache, enviado após o plano principal |

Os aliases ZIP possuem sidecars `.sha256`. O `feedUrl` dos dois bootstraps aponta para `<base-url>/latest.json`, preservando o papel Anfitrião/Convidado. O preparador aceita um canal vazio ou já igual ao destino; recusa um canal diferente para evitar redirecionar uma entrega configurada sem intenção.

O manifesto do jogo e o payload instalado são preservados byte a byte. O SHA-256 do manifesto identifica `versions/<hash>` e `active.txt`. O EXE/dispatcher inicial permanece fora das versões; a entrega oficial pode colocar `launcher/Launcher-SpiritGate.ps1` e `launcher/Atualizar-SpiritGate.ps1` dentro do payload versionado. Nesse caso, o ZIP deve conter `Iniciar-SpiritGate.ps1` na raiz.

## Auditoria

O preparador confere `integracao.json`, SHA-256 dos dois ZIPs, igualdade dos manifestos, inventário exato das releases, tamanhos e hashes dos arquivos, payload dentro dos ZIPs e respectivos ponteiros. Recusa traversal, links, nomes reservados, duplicatas, arquivos adicionais fora do inventário, diretórios de saves/dados/projetos/fontes e arquivos de credenciais conhecidos. Somente arquivos definidos do bootstrap são aceitos fora de `versions`.

Não há cópia da raiz do repositório ou enumeração automática de arquivos pessoais. Destinos existentes nunca são sobrescritos pelo preparador. A autenticação de publicação pertence à máquina administrativa e nunca deve ser incluída no launcher distribuído.

## Ordem e adaptador de publicação

`execute_plan(staging, upload)` audita novamente todos os artefatos antes da primeira escrita remota. O callback recebe `(arquivo_local, metadados)`; os metadados têm `key`, `bytes`, `sha256`, `contentType`, `cacheControl`, `immutable` e `phase`.

As fases são: 0 — payload da release; 1 — downloads imutáveis; 2 — `latest.json` (ponto de ativação); 3 — aliases ZIP e hashes. Falha no callback interrompe a sequência. Se falhar antes da fase 2, o canal anterior permanece ativo; se falhar na promoção dos aliases, o canal novo já está disponível e os instaladores anteriores continuam podendo atualizar. O uploader deve tratar objetos imutáveis como create-if-absent ou verificar que o conteúdo remoto já existente tem o mesmo SHA-256. Não deve sobrescrever uma release imutável com conteúdo diferente.

A ordem evita ativar uma release cujo upload não terminou e evita promover um instalador novo enquanto o canal ainda aponta para a versão anterior. Ela não substitui a política de cache do provedor nem fornece transação entre objetos: clientes não devem fazer downgrade automático quando receberem um manifesto antigo durante a propagação de cache. A validação final deve obter o manifesto e os dois ZIPs pelos endereços públicos e comparar hashes com o plano local, antes de declarar a publicação concluída.

`tools/publicar_cdn_cloudflare.py` aceita `--activation-zip CAMINHO_DO_KIT`. Antes da primeira escrita no R2, ele confere a árvore exata do kit, o canal igual ao do plano, os hashes do payload, os scripts correspondentes às fontes administrativas e as entradas dos executáveis. Um ZIP arbitrário ou um ativador para outro canal é recusado. Os dois aliases de ativação são enviados na fase 4, após concluir o plano principal. Quando o kit é fornecido, a verificação HTTP também compara o ZIP e seu sidecar antes de marcar `publication.json` como publicado e verificado.

Cache de imutáveis: `public, max-age=31536000, immutable`. Cache de mutáveis: `no-store, max-age=0, must-revalidate`. O provedor/Worker deve respeitar essa separação; não deve aplicar uma regra geral de cache prolongado a `latest.json` ou aos aliases ZIP.

## Testes locais

```powershell
python -m unittest discover -s tools -p test_preparar_cdn.py -v
```

As verificações exercitam migração dos dois papéis, dispatcher oficial, inventários e hashes, JSON malformado, caminhos privados, cache, ordem de ativação, interrupção do upload e adulteração do staging. O uploader dos testes é simulado; nenhum teste envia arquivos à internet.
