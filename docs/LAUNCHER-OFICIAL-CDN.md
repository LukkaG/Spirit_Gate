# Launcher oficial e atualizacoes pela CDN

> Em 27/10/2026, foi escolhido manter o canal GitHub durante o desenvolvimento.
> Esta preparacao CDN fica reservada para o futuro. Consulte o fluxo atual em
> [LAUNCHER-GITHUB.md](LAUNCHER-GITHUB.md).

## Estado da entrega 0.201.0

A entrega em uso esta em `dist/SpiritGate-GitHub-0.201.0`, publicada pelo
GitHub. A antiga copia de preparacao CDN foi removida na limpeza. O codigo,
os testes e as instrucoes da CDN permanecem para uma migracao futura, com
publicacao separada e uma URL confirmada da Cloudflare.

## Como cada jogador vai usar

Depois da publicacao, abra sempre `SpiritGate-Launcher.exe` dentro da sua
instalacao atual. O anfitriao conserva o papel Anfitriao; seu amigo conserva
Convidado. A pasta `versions` e administrada pelo atualizador.

Para quem ja tem o jogo:

1. Baixe e extraia `SpiritGate-Ativar-CDN.zip` quando o canal estiver publicado.
2. Abra `Ativar-SpiritGate.exe` e selecione a pasta que contem o
   `SpiritGate-Launcher.exe` existente.
3. Volte a usar o mesmo launcher nas proximas vezes.

O kit tem aproximadamente 16 KB e atualiza somente os arquivos pequenos do
launcher. Cria backup, configura o canal por ultimo e preserva saves, papel,
versoes instaladas e Java. A atualizacao seguinte baixa apenas os arquivos do
jogo que mudaram. Uma nova interface do launcher passa a ser usada na proxima
abertura.

Para uma instalacao nova, o canal oferece os downloads separados
`SpiritGate-Anfitriao-Windows64.zip` e `SpiritGate-Convidado-Windows64.zip`.
Seu amigo recebe Convidado. Os arquivos de integracao e ferramentas de
publicacao ficam com o desenvolvedor.

A CDN distribui atualizacoes. A conexao da partida continua usando a
configuracao atual do servidor e do Radmin.

## Hospedagem e custos

Os arquivos publicaveis ficam em um bucket R2 privado. Um endpoint Pages
Functions entrega os caminhos permitidos por HTTPS, usando a Cache API para
os arquivos imutaveis. `latest.json` e os aliases de download usam `no-store`.
O endereco fornecido pelo Pages dispensa a compra de dominio. O repositorio
pode continuar privado, e nenhuma credencial Cloudflare entra no jogo.

Em 27/10/2026, o R2 Standard inclui mensalmente 10 GB-mes de armazenamento,
1 milhao de operacoes Classe A e 10 milhoes Classe B. A saida de dados e
gratuita; excedentes de armazenamento e operacoes sao cobrados. A ativacao
passa pelo cadastro de faturamento. O plano Free das Pages Functions permite
100 mil requisicoes por dia, compartilhadas com Workers Free. A estimativa
para os testes de dois jogadores e permanecer nessas franquias; acompanhe o
uso no painel.

Referencias: [precos do R2](https://developers.cloudflare.com/r2/pricing/),
[ativacao do R2](https://developers.cloudflare.com/r2/get-started/),
[precos das Pages Functions](https://developers.cloudflare.com/pages/functions/pricing/)
e [Cache API](https://developers.cloudflare.com/workers/runtime-apis/cache/).

## Publicacao pelo desenvolvedor

O endpoint e suas instrucoes estao em `deploy/cdn-cloudflare/README.md`.
Primeiro confirme o e-mail, habilite o R2, crie o bucket privado e publique o
endpoint. Use somente o endereco retornado e confirmado pela Cloudflare.

Com `$cdnBaseUrl` definido para esse endereco real e `$feedUrl` para
`$cdnBaseUrl/latest.json`, prepare novas pastas de saida:

```powershell
python tools/integrar_launcher_radmin.py dist/SpiritGate-GitHub-0.201.0/Integracao/Fonte-Anfitriao-0.201.0.zip .task-checkpoints/distribuicao-cdn-0.201.0 --version 0.201.0 --feed-url $feedUrl
python tools/preparar_cdn.py .task-checkpoints/distribuicao-cdn-0.201.0 .task-checkpoints/cdn-0.201.0 --base-url $cdnBaseUrl
python tools/preparar_ativacao_cdn.py .task-checkpoints/ativacao-0.201.0 --feed-url $feedUrl
python tools/publicar_cdn_cloudflare.py .task-checkpoints/cdn-0.201.0 --activation-zip .task-checkpoints/ativacao-0.201.0/SpiritGate-Ativar-CDN.zip
```

O ultimo comando apenas audita, sem escrever no provedor. Depois de revisar
o plano, acrescente `--publish` para publicar e verificar os downloads por
HTTPS. `--resume` reaproveita os envios imutaveis ja confirmados no mesmo
bucket. Credenciais e evidencias ficam em `.task-checkpoints`, que e ignorado
pelo Git e nao faz parte dos artefatos publicos.

O publicador audita a distribuicao e todos os hashes antes de escrever.
Envia payloads imutaveis, instaladores com hash, `latest.json`, aliases de
instaladores e o kit de ativacao, nessa ordem. O resultado online deve ser
confirmado em `online-verification.json` e `publication.json`; ter um plano
local nao significa que houve publicacao.

Os ZIPs com a URL real sao produzidos na pasta preparada da CDN. Os ZIPs da
integracao local original nao sao modificados automaticamente.

Para a proxima versao, recompile usando o ultimo canal local validado. Por
exemplo, para preparar 0.201.1 depois de 0.201.0:

```powershell
python tools/build-multiplayer-update.py dist/SpiritGate-GitHub-0.201.0/Anfitriao/canal .task-checkpoints/build-0.201.1 .task-checkpoints/distribuicao-cdn-0.201.1 --version 0.201.1 --feed-url $feedUrl
```

Depois, repita a preparacao e publicacao para a nova pasta e a mesma URL de
producao. O empacotador substitui o JAR antigo pelo recem-compilado tanto no
formato legado quanto no formato oficial. Os jogadores continuam abrindo o
launcher que ja possuem.

## Evidencias locais

A migracao real 0.200.8 -> 0.201.0 baixou 3 arquivos e reutilizou 388.
O executavel fixo permaneceu igual durante a atualizacao do jogo, e os
sentinelas de save, papel Convidado e Java foram preservados. As interfaces
versionadas de Anfitriao e Convidado abriram pelo bootstrap no modo de smoke
test. O historico foi consolidado em `docs/RELATORIO-GERAL-LIMPEZA-2026-10-27.md`.

Os testes cobrem a verificacao de hashes, retomada, rollback, cache HTTP,
redirecionamentos, protecao do endpoint e ativacao leve. A verificacao de
rede em producao permanece pendente ate a conclusao da conta Cloudflare.
