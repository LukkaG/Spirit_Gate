# Spirit Gate — experimento de navegador

Adaptação isolada para testar os sprites e coletar avaliações sem instalar o cliente desktop. Branch GitHub: `browser-sprites-preview`. O checkout original e o canal `updates` não foram modificados.

## Uso

Abra o site, escolha uma das quatro configurações e clique em **Iniciar teste**. Um visitante novo entra no RPG. WASD movimenta, F interage e F5 salva no armazenamento virtual do navegador. O primeiro carregamento baixa o runtime Java e pode demorar; computador com teclado é o alvo deste teste. Não é multiplayer.

Os dados ficam somente no navegador utilizado, separados dos saves desktop. As avaliações são copiadas para compartilhar ou abertas como uma issue pré-preenchida no GitHub (exige conta). Não existe coleta silenciosa, banco central de feedback ou envio automático de avaliação.

## Build e preview

Java 17 e Python: `python build.py` e `python check.py`. Servidor local com suporte a Range: `node serve.mjs`, em `127.0.0.1:8789`. `node qa-game.mjs` executa QA com Playwright e o Edge instalado. `npm install` prepara apenas a ferramenta de teste. O servidor Python padrão não oferece os Range requests necessários ao CheerpJ.

O JAR contém somente domínio/apresentação/RPG e recursos necessários, sem JDBC, credenciais, servidor Coop ou saves pessoais. Os 12 PNGs são cópias byte a byte dos originais.

## Diferenças da cópia desktop

1. `BrowserLauncher` entra diretamente com personagem anônimo para facilitar ensaios de sprites.
2. O loader usa a lista explícita dos metadados para recursos sob `/app/`, pois o filesystem HTTP do CheerpJ não enumera diretórios. Hashes e dimensões continuam sendo conferidos antes de ativar as imagens.
3. O template de apresentação usa `DebugWorld.demo(new java.util.Random())`; o runtime web não fornece o provider LXM selecionado por `RandomGenerator.getDefault()` do JDK desktop. As regras de RPG não foram modificadas.
4. Propriedades da página direcionam assets para `/app/` (HTTP) e dados para `/files/spirit-gate-web` (filesystem virtual persistente). F5 é encaminhado ao jogo.

Fontes originais preservadas por hash em `snapshot.json`. `game-src` e `game-resources` são uma cópia explícita e independente; não são links para a produção. Nenhuma mudança será mesclada automaticamente na branch principal.

## Runtime e licença

CheerpJ 4.3, carregado do CDN oficial `https://cjrtnc.leaningtech.com/4.3/loader.js`, Java 17. Créditos visíveis no site. Runtime proprietário não foi redistribuído/self-hosted. Uso configurado para este projeto pessoal, compatível com a Community License indicada na documentação atual. Se o contexto passar a uma organização fora do escopo da licença, revisar as condições.

Referências oficiais: [Java application](https://cheerpj.com/docs/getting-started/Java-app.html), [Java 17/propriedades](https://cheerpj.com/docs/reference/cheerpjInit), [filesystem virtual](https://cheerpj.com/docs/guides/filesystem), [licença](https://cheerpj.com/docs/licensing).

## Testes

Compilação Java 17 da cópia isolada; hashes dos 12 PNGs; ausência de DB/Coop/testes no JAR; comparação dos hashes da produção original. QA real no Edge headless: janela do RPG, personagem com sprite, quatro teclas de movimento, geração de feedback e diálogo “Progresso salvo” em F5 sem reload. O desempenho e a apresentação ainda precisam de feedback humano em outros computadores. Teste local é separado da verificação do deploy público.
