# Canal de atualizações — Cloudflare Pages + R2

Esta pasta contém somente o endpoint de leitura da distribuição. O bucket R2 fica **privado**, sem `r2.dev` público. A Pages Function lê os objetos permitidos e publica os downloads por HTTPS. Nenhuma credencial da Cloudflare entra no launcher ou nos arquivos do jogo.

O endereço `*.pages.dev` permite começar sem comprar um domínio. A Cache API funciona em Pages Functions nesse endereço; o exemplo de cache R2 documenta que `*.workers.dev` não é equivalente para esse uso. Por isso este endpoint usa Pages em modo avançado. [Cache API](https://developers.cloudflare.com/workers/runtime-apis/cache/), [R2 e cache](https://developers.cloudflare.com/r2/examples/cache-api/).

## Antes de publicar

A implementação e os testes locais não criam uma conta nem publicam arquivos. É necessário entrar na **sua conta Cloudflare**, habilitar R2 pelo painel e autenticar o Wrangler. Qualquer aceite de termos, plano ou cobrança deve ser feito pelo titular da conta no painel da Cloudflare. Não envie chaves ou senhas pelo chat.

Estado desta preparação: as tentativas de provisionamento foram recusadas porque o e-mail da conta ainda precisa ser verificado e o R2 não está habilitado. **Não há endpoint de produção confirmado nem canal publicado.** Conclua essas duas ações no painel antes de repetir o provisionamento; autenticar o CLI não substitui essa habilitação.

O projeto Pages e o bucket estão configurados inicialmente como `spiritgate-updates`. Se esse nome de projeto não estiver disponível, escolha outro e ajuste `name` em `wrangler.toml`. Confirme o endereço de produção realmente atribuído ao projeto antes de preparar os instaladores; não presuma que um endereço de exemplo pertence à sua conta.

Os comandos abaixo foram conferidos no help e no código local do **Wrangler 4.153.0**. Use o CLI local do projeto e a mesma pasta de autenticação ignorada pelo Git. No PowerShell, a partir da **raiz do repositório**:

```powershell
$spiritGateRoot = (Resolve-Path -LiteralPath '.').Path
$env:XDG_CONFIG_HOME = Join-Path $spiritGateRoot '.task-checkpoints/cloudflare-auth'
$env:WRANGLER_LOG_PATH = Join-Path $spiritGateRoot '.task-checkpoints/cloudflare-tools/logs'
$env:WRANGLER_SEND_METRICS = 'false'
$spiritGateWrangler = Join-Path $spiritGateRoot '.task-checkpoints/cloudflare-tools/node_modules/wrangler/bin/wrangler.js'
```

Se o CLI local não existir em um checkout novo, instale a versão conferida dentro da área ignorada, sem instalação global:

```powershell
npm install --prefix .task-checkpoints/cloudflare-tools --cache .task-checkpoints/cloudflare-tools/npm-cache --no-audit --no-fund wrangler@4.153.0
```

`XDG_CONFIG_HOME` mantém a autenticação em `.task-checkpoints/cloudflare-auth`; não copie essa pasta, logs ou tokens para a entrega CDN. Preserve essa variável nos próximos comandos para usar a mesma sessão autenticada.

Confira a sessão com `whoami`. Se ainda não estiver autenticado, execute `login` e conclua a autenticação no navegador:

```powershell
node $spiritGateWrangler whoami
node $spiritGateWrangler login
```

Após verificar o e-mail e habilitar R2, crie somente os recursos que ainda não existirem:

```powershell
node $spiritGateWrangler r2 bucket create spiritgate-updates
node $spiritGateWrangler --cwd deploy/cdn-cloudflare pages project create spiritgate-updates --production-branch main --force
```

Nesta versão do CLI, a criação de um projeto Pages novo em uma sessão de agente pode ser automaticamente delegada a Workers. O `--force` **dessa criação** escolhe Pages diretamente; não apaga nem sobrescreve recursos. Essa opção é oculta no help, mas foi conferida no handler local de `pages project create` da versão 4.153.0. A escolha preserva a arquitetura Pages/R2 e o cache em `*.pages.dev` descritos acima. Depois que o projeto Pages existir, os deploys normais não precisam dessa opção.

Publique o endpoint pelo comando Pages, usando a pasta que contém `wrangler.toml`:

```powershell
node $spiritGateWrangler --cwd deploy/cdn-cloudflare pages deploy public --project-name spiritgate-updates --branch main
```

`pages deploy` aceita `--project-name` e `--branch`; não use `wrangler deploy` para este projeto e não troque essa operação por um deploy Workers. Use `--cwd`, em vez de apontar `--config` para outra pasta. O `--skip-caching` mostrado no help controla o cache do upload de assets, não a política HTTP dos downloads; ele não é necessário aqui.

Esses comandos criam recursos ou publicam quando executados com uma conta habilitada. Não são executados pelos testes. O `wrangler.toml` liga o bucket ao binding `RELEASES`; não é necessário tornar o bucket público. Use a URL **de produção realmente retornada**, sem prefixo de pasta, como base do canal. O launcher deve apontar para `<URL_REAL>/latest.json`. [Bindings R2 em Pages](https://developers.cloudflare.com/pages/functions/bindings/#r2-buckets), [Direct Upload com Wrangler](https://developers.cloudflare.com/pages/get-started/direct-upload/).

## Arquivos e cache

O `_worker.js` atende todas as rotas. Não expõe listagem, painel administrativo, fonte do endpoint ou upload HTTP. São permitidos GET e HEAD somente para:

- `latest.json`;
- `release-<24 caracteres hex>/<arquivo do payload>` com caminho seguro;
- `downloads/<SHA-256>/SpiritGate-Atualizavel-<versão>[-Convidado]-Windows64.zip` e hash opcional;
- aliases `SpiritGate-Anfitriao-Windows64.zip`, `SpiritGate-Convidado-Windows64.zip`, `SpiritGate-Instalar-Windows64.zip` e `SpiritGate-Ativar-CDN.zip`, com seus `.sha256`.

`latest.json` e aliases usam `no-store, max-age=0, must-revalidate`, sem consultar ou preencher o cache do Worker. Release e downloads com hash usam `public, max-age=31536000, immutable`. O cache usa caminho canônico sem query string. HEAD devolve metadados e nenhum corpo. Range é recusado com 416; o atualizador baixa cada arquivo completo e confere seu SHA-256.

Publique primeiro os objetos e downloads imutáveis, depois **`latest.json`** e por último os aliases dos instaladores. Assim um instalador novo nunca consulta um manifesto antigo durante a promoção. O plano produzido por `tools/preparar_cdn.py` define essa ordem e os metadados. Não envie a pasta do repositório ao R2: envie somente o conteúdo auditado de `public` da entrega CDN. A pasta `deploy/cdn-cloudflare/public` contém o código do endpoint e não é a pasta dos binários.

O publicador `tools/publicar_cdn_cloudflare.py` usa o CLI local e `XDG_CONFIG_HOME` acima, valida o plano antes de enviar, e chama `r2 object put <bucket>/<chave> --file <arquivo> --remote --cache-control <política> --content-type <tipo>`. Esses argumentos foram conferidos no help local. Sem `--remote`, uma operação R2 pode atingir o armazenamento de desenvolvimento, sem publicar o objeto real. O `--force` de `r2 object put` tem outro significado e não é necessário para este bucket.

Primeiro execute a validação, que não faz escritas remotas:

```powershell
python tools/publicar_cdn_cloudflare.py CAMINHO_DA_ENTREGA_CDN
```

Somente depois de preparar a entrega com a URL real e confirmar a conta habilitada, o envio é feito com `--publish`; ao final, o publicador baixa manifesto e instaladores pela URL pública e compara os hashes. A opção `--verify-only` repete apenas essa leitura remota. O projeto Pages vazio não significa que a versão do jogo já foi enviada ao R2. Não modifique conteúdo em uma chave de release já publicada; gere outra release com hash.

A Cache API é local a cada data center e não oferece tiered caching. Falha do cache não deve impedir uma leitura válida do R2. A validação final deve consultar o endpoint publicado, baixar os pacotes e verificar hashes; testes locais não comprovam propagação ou disponibilidade da CDN.

## Verificação local

Node.js com Fetch/Request/Response globais é suficiente; os testes não instalam dependências ou acessam uma conta:

```powershell
node --test deploy/cdn-cloudflare/test/worker.test.js
```

As dez verificações cobrem imutabilidade do cache, frescor de `latest`, aliases dos dois papéis e ativação, HEAD, queries, bypass de cache, caminhos privados e codificados, métodos de escrita, Range, erros da origem, limites de tamanho e falha do cache. O R2 e a Cache API são simulados; não há transferência remota.

Referências de implementação: [Pages em modo avançado](https://developers.cloudflare.com/pages/functions/advanced-mode/), [configuração Wrangler de Pages](https://developers.cloudflare.com/pages/functions/wrangler-configuration/).
