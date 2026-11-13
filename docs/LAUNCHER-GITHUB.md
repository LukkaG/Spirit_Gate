# Atualizacoes pelo GitHub durante o desenvolvimento

Versao **0.201.0 publicada e verificada por HTTPS em 27/10/2026**, no commit
[`097cdbb`](https://github.com/LukkaG/Spirit_Gate/commit/097cdbbaafcd9f902f6ce5b9f7dee46d63eb9780)
da branch `updates`.

Downloads oficiais:

- [Ativar GitHub na instalacao existente (aproximadamente 17 KB)](https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/SpiritGate-Ativar-GitHub.zip).
- [Instalacao nova Anfitriao](https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/SpiritGate-Anfitriao-Windows64.zip).
- [Instalacao nova Convidado](https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/SpiritGate-Convidado-Windows64.zip).

O canal escolhido para esta fase e o GitHub. O launcher consulta:

`https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json`

Os arquivos do jogo, a interface e o motor do atualizador sao baixados dessa
mesma branch `updates`. O jogador nao precisa instalar Git, Python ou Node.
O transporte continua HTTPS com verificacao SHA-256 e aproveita os arquivos
que ja estao instalados. O plano Cloudflare/CDN fica reservado para uma fase
futura; esta distribuicao nao usa R2 nem Pages.

## Para quem ja tem o jogo

1. Feche o jogo e o launcher.
2. Extraia `SpiritGate-Ativar-GitHub.zip` e abra `Ativar-SpiritGate.exe`.
3. Selecione a pasta atual que contem `SpiritGate-Launcher.exe`,
   `atualizacoes.json` e `versions`.
4. Continue abrindo `SpiritGate-Launcher.exe` dessa mesma pasta.

O kit tem aproximadamente 17 KB e copia somente os arquivos pequenos do
iniciador. Preserva o papel Anfitriao/Convidado, saves e Java, e guarda backup
em `launcher-backups`. Depois de ativado, o canal ja fica configurado para o
GitHub; nao e preciso informar URL nem credenciais.

O amigo usa a propria instalacao Convidado. O anfitriao usa sua instalacao
Anfitriao. As atualizacoes seguintes passam pelo mesmo executavel; os dois
devem atualizar antes de entrar na partida. Se a internet falhar, o launcher
oferece a versao local validada.

## Instalacao nova

Use os pacotes de `dist/SpiritGate-GitHub-0.201.0`:

- Anfitriao: `Anfitriao/SpiritGate-Atualizavel-0.201.0-Windows64.zip`.
- Amigo: `Convidado/SpiritGate-Atualizavel-0.201.0-Convidado-Windows64.zip`.
- Migracao sem reinstalar: `Ativador/SpiritGate-Ativar-GitHub.zip`.

Extraia o pacote completo e abra o `SpiritGate-Launcher.exe` na pasta
`SpiritGate-Atualizavel`. As pastas `canal` e os arquivos de integracao sao
ferramentas de publicacao do desenvolvedor.

## Publicacao de novas versoes

O repositorio de distribuicao atual e publico. O launcher nao usa tokens ou
senhas. Um repositorio privado exigiria outra estrategia de autenticacao;
nao inclua tokens pessoais no cliente.

Prepare os dois papeis com o mesmo feed, usando o ZIP Anfitriao completo:

```powershell
python tools/integrar_launcher_radmin.py CAMINHO_DO_HOST_ZIP dist/SpiritGate-GitHub-NOVA_VERSAO --version NOVA_VERSAO --feed-url https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json
python tools/preparar_ativacao_cdn.py dist/SpiritGate-GitHub-NOVA_VERSAO/Ativador --provider github --feed-url https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json
```

Para compilar as proximas mudancas do jogo e gerar os dois pacotes a partir
do canal local anterior, use este comando no lugar da primeira linha:

```powershell
python tools/build-multiplayer-update.py dist/SpiritGate-GitHub-0.201.0/Anfitriao/canal .task-checkpoints/build-NOVA_VERSAO dist/SpiritGate-GitHub-NOVA_VERSAO --version NOVA_VERSAO --feed-url https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json
```

O argumento `--provider github` gera o kit GitHub e aceita somente o
endereco raw do manifesto. A implementacao compartilhada tambem conserva a
preparacao CDN para o futuro, sem acionamento de servicos externos.

O publicador Git possui uma etapa de preparacao local antes de publicar:

```powershell
python tools/publicar_atualizacoes_git.py dist/SpiritGate-GitHub-NOVA_VERSAO .task-checkpoints/git-NOVA_VERSAO --activation-zip dist/SpiritGate-GitHub-NOVA_VERSAO/Ativador/SpiritGate-Ativar-GitHub.zip
```

Revise o plano e repita com `--publish`. O publicador usa um checkout isolado,
preserva as releases anteriores e envia um commit normal somente para
`updates`. O checkout de desenvolvimento permanece na sua branch atual.
O plano enumera os arquivos, tamanhos e hashes que serao publicados.

Confira o manifesto remoto e os downloads depois do push. O GitHub pode
levar algum tempo para propagar a nova resposta raw. A ferramenta de
preparacao nao implica publicacao online.

## Validacao desta entrega

Foram conferidos por HTTPS o manifesto, os dois instaladores, o ativador e
seu checksum. Uma copia isolada do Convidado 0.200.8 foi ativada e atualizada
pela URL real do GitHub: 3 arquivos baixados, 388 reutilizados, saves e papel
preservados, Java reaproveitado e executavel fixo inalterado durante o update.
As interfaces versionadas de ambos os papeis abriram pelo bootstrap em smoke
test com o canal remoto configurado.

O recibo e os resultados da verificacao permanecem em
`dist/SpiritGate-GitHub-0.201.0/publicacao.json`. O historico de testes foi
consolidado em [RELATORIO-GERAL-LIMPEZA-2026-10-27.md](RELATORIO-GERAL-LIMPEZA-2026-10-27.md);
as copias temporarias antigas foram removidas.
