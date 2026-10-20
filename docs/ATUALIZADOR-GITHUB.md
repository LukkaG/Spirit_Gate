# Canal de atualizações no GitHub

Publicado após autorização explícita para usar o repositório atual, substituindo a preferência anterior de preparar somente localmente.

- Repositório público: `https://github.com/LukkaG/Spirit_Gate`.
- Branch de distribuição: [updates](https://github.com/LukkaG/Spirit_Gate/tree/updates).
- Commit inicial: `3384757659699d8b2f8315344162aa39364a9f1f`.
- Manifesto fixo: `https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json`.
- [Download para instalar uma vez](https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/SpiritGate-Instalar-Windows64.zip).
- Versão inicial: `0.78.1-rc1`; Windows 64 bits; Java integrado; sprites experimentais.

Extrair todo o ZIP e abrir `Jogar-SpiritGate.cmd`. A partir desse pacote, o canal já está configurado. As atualizações são verificadas ao abrir o iniciador; o jogo aberto não é alterado em tempo real.

## Instalações anteriores

Se o amigo já tem o pacote com iniciador preparado localmente, pode alterar somente `atualizacoes.json` ao lado de `Jogar-SpiritGate.cmd`:

```json
{
  "feedUrl": "https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json"
}
```

O arquivo fica fora de `versions`. Saves em `data/saves` são preservados. O portátil antigo sem iniciador ainda requer a instalação inicial do novo pacote e cópia consciente dos saves, conforme `ATUALIZADOR.md`.

## O que foi publicado

Uma branch separada, com histórico independente, contendo somente os 161 arquivos da distribuição auditada, manifesto, documentação do canal e ZIPs iniciais. Nenhum save, credencial ou arquivo de trabalho foi enviado. O checkout principal e sua branch `beco` não receberam commit, checkout, reset ou push; alterações locais anteriores foram preservadas. A identidade Git já configurada foi usada apenas no repositório temporário de publicação, sem alterar a configuração global.

O repositório já era público antes desta operação; sua visibilidade não foi alterada. A autorização permite esta publicação de teste, não significa aprovação artística final ou implantação de servidor multiplayer.

## Validação online

1. Git confirmou a publicação da nova branch sem force-push.
2. O manifesto foi obtido por HTTPS sem autenticação e comparado byte a byte com o preparado localmente.
3. O ZIP do link público foi baixado e comparado integralmente com o ZIP local: 30.185.312 bytes, SHA-256 `ffcfcf6f7cbe016e87d5298f461ed284cb857f358fe983248b569e951701599c`.
4. Uma instalação isolada com versão anterior de teste verificável recebeu a atualização pelo endereço público: **1 arquivo baixado e 160 reutilizados**. A referência ativa passou a coincidir com o SHA-256 do manifesto publicado e o sentinela de save permaneceu intacto.
5. A primeira montagem do teste já continha a pasta da versão-alvo e o atualizador a recusou por segurança; esse log foi preservado. O teste final usou apenas a versão anterior instalada, sem apagar dados ou enfraquecer essa proteção.
6. Os cinco testes Python do preparador foram repetidos e passaram após configurar o README do pacote para distinguir canal local e publicado. O código do jogo e o atualizador PowerShell não foram alterados nesta publicação.

Evidências: `docs/ATUALIZADOR-GITHUB-publicacao.log`, `ATUALIZADOR-GITHUB-teste-online-final.log`, `ATUALIZADOR-GITHUB-verificacao.json` e `.task-checkpoints/github-update-live-check/public-download.json`. Este teste não substitui homologação em outro computador.

## Publicar funcionalidades futuras

É necessário criar e validar um novo pacote do jogo; salvar código ou fazer push da branch de trabalho não recompila o jogo dos testadores. Depois de gerar um ZIP com versão nova:

```powershell
python tools/preparar_atualizacao.py CAMINHO_DO_NOVO_ZIP dist/ENTREGA-NOVA --version NOVA-VERSAO --feed-url https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json
python tools/publicar_atualizacoes_git.py dist/ENTREGA-NOVA .task-checkpoints/PUBLICACAO-NOVA
```

Use nomes de pastas novos. O publicador verifica os hashes, recusa saves no ZIP, usa o Git autenticado localmente e prepara um repositório temporário independente. Se `updates` já existir, parte dela e preserva as releases anteriores. Envia um commit normal sem force-push; todos os arquivos e `latest.json` mudam no mesmo commit. O endereço do canal e o link inicial permanecem fixos.

Uma atualização para versão que já exista na instalação é recusada pelo atualizador atual; não há comando de downgrade/reinstalação por cima implementado. O canal depende da disponibilidade e dos limites do GitHub, podendo ter cache. Em indisponibilidade, o iniciador mantém a versão validada instalada.
