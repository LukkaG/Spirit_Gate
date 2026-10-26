# Spirit Gate — Atualização automática

Integracao local do launcher Radmin TASK-177: veja [LAUNCHER-RADMIN-ATUALIZAVEL.md](LAUNCHER-RADMIN-ATUALIZAVEL.md). Essa entrega ainda nao foi publicada no canal.

Implementado por solicitação do responsável. Inicialmente preparado localmente; após nova autorização, o canal foi publicado na branch `updates` do repositório público atual `LukkaG/Spirit_Gate`. A branch de trabalho do jogo não foi alterada pela publicação.

## Pacote para instalar uma vez

[Baixar pacote configurado](https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/SpiritGate-Instalar-Windows64.zip).

Cópia local: `dist/SpiritGate-Atualizacoes-GitHub-0.78.1-rc1/SpiritGate-Atualizavel-0.78.1-rc1-Windows64.zip`.

Extraia tudo e abra `SpiritGate-Atualizavel/Jogar-SpiritGate.cmd`. Java integrado; Windows 64 bits com Windows PowerShell. Não precisa instalar Python, Maven ou Java. O jogo vem com as sprites experimentais.

O pacote novo já configura `atualizacoes.json` com `feedUrl=https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json`. O iniciador verifica esse endereço ao abrir e recebe os arquivos que mudaram. A configuração da instalação local anteriormente preparada em `dist/SpiritGate-Atualizacoes-0.78.1-rc1/SpiritGate-Atualizavel/` também foi atualizada, sem tocar em saves.

Quem já extraiu o pacote local anterior pode editar apenas `atualizacoes.json` na pasta do iniciador e definir esse mesmo `feedUrl`, sem baixar novamente o jogo inteiro. Os ZIPs históricos anteriores continuam inalterados.

Saves, controles e preferências ficam em `data/saves/`, fora das versões substituíveis. Para continuar um personagem de pacote anterior, feche ambos os jogos, mantenha backup da origem e copie a pasta antiga `saves` para `data/saves`, preservando a estrutura. Não é feita migração automática nem conversão de formato. Não execute o EXE dentro de `versions`: o iniciador é responsável por selecionar a pasta estável de dados.

## Funcionamento

1. O iniciador adquire um lock exclusivo, mantido até o jogo fechar. Uma segunda abertura na mesma pasta é recusada.
2. Valida o manifesto e os hashes da versão instalada; se inválida, tenta a anterior.
3. Se houver canal, baixa um manifesto pequeno. Downloads remotos aceitam somente HTTPS; credenciais embutidas no endereço são recusadas. Nenhuma credencial foi incluída.
4. Compara hashes e reutiliza arquivos locais inalterados. Baixa individualmente somente arquivos diferentes; não baixa o ZIP inteiro em cada atualização.
5. Monta uma pasta temporária e confere tamanho e SHA-256 de todos os arquivos. Impõe limites, recusa caminhos externos/reservados e colisões de capitalização.
6. Só após validar o conjunto inteiro, move-o para uma nova pasta de versão e substitui a referência ativa por operação atômica. Não sobrescreve arquivos do jogo em execução, saves ou o iniciador.
7. Falha de rede, corrupção ou manifesto inválido mantém a versão válida anterior e permite jogar. A tentativa temporária é limpa. Sem qualquer versão válida, orienta reextrair o pacote inicial.

Pasta `versions/` usa o SHA-256 do manifesto como identificação interna. Versões anteriores ficam disponíveis para recuperação e não são apagadas automaticamente; há consumo adicional de disco por versão. `previous.txt` registra a referência anterior. O mecanismo verifica integridade; a confiança na atualização remota depende do controle do canal HTTPS configurado. Não há assinatura de publicação independente ou infraestrutura de contas implementada nesta entrega.

## Preparar a próxima versão

Depois de alterar e testar o projeto, compilar/empacotar uma versão nova, sem substituir artefatos anteriores:

```powershell
python tools/qualidade.py --offline --task proxima-versao
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-rpg-sprites-portable.ps1 -Version 0.78.2-rc1
python tools/preparar_atualizacao.py CAMINHO_DO_NOVO_ZIP dist/ATUALIZACOES-NOVAS --version 0.78.2-rc1
```

O último comando cria:

- `canal/latest.json`: manifesto da versão;
- `canal/release-.../`: arquivos individuais para hospedagem estática;
- `SpiritGate-Atualizavel/` e ZIP: pacote inicial para novos testadores.

Para um canal já definido, pode acrescentar `--feed-url https://HOST/CANAL/latest.json` ao preparador. Ele configura o pacote inicial, mas não faz upload.

Quando for autorizado hospedar: enviar primeiro a pasta `release-...` nova inteira; publicar `latest.json` por último no endereço fixo. Preservar a versão anterior enquanto houver testadores usando-a. Configurar o mesmo endereço HTTPS em `atualizacoes.json` de cada instalação. A hospedagem deve servir os arquivos com seus caminhos e nomes exatos, sem transformar conteúdo ou substituir respostas por páginas HTML.

Não basta anexar um ZIP a uma release: este formato usa arquivos individuais sob uma árvore de URLs. A hospedagem atual usa os arquivos raw da branch pública `updates`; não depende de GitHub Pages nem de Releases. Para publicar outra entrega preparada no mesmo formato, usar `tools/publicar_atualizacoes_git.py` com uma pasta de staging nova. Ver `ATUALIZADOR-GITHUB.md`. Atualizar código na branch de trabalho, sozinho, não distribui um novo build aos jogadores.

Para ensaio sem servidor, com cópia de instalação e canal locais:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File CAMINHO/Atualizar-SpiritGate.ps1 -LocalFeed CAMINHO/canal/latest.json -NoLaunch
```

`-LocalFeed` é uma opção explícita de desenvolvimento; o fluxo normal usa o HTTPS de `atualizacoes.json`. `-NoLaunch` instala/verifica sem iniciar o jogo. O script inicial não se substitui automaticamente; mudanças no próprio mecanismo de atualização podem exigir nova versão do pacote inicial.

## Implementação

- `tools/updater/Atualizar-SpiritGate.ps1`: instalação incremental, validação, isolamento, fallback e execução pelo runtime integrado.
- `tools/updater/Jogar-SpiritGate.cmd`: ponto de entrada do usuário.
- `tools/preparar_atualizacao.py`: canal estático e pacote inicial, sem rede/publicação; recusa inclusão de saves e sobrescrita de entrega existente.
- `RpgMenu.dataHome()`: usa `spiritgate.rpg.dataDir` quando informado pelo iniciador. `RpgMenu.home()` permanece apontando à instalação para localizar assets. Fluxo normal fora do iniciador mantém o comportamento anterior.
- `RpgLauncher`: saves/preferências/controles passaram a usar `dataHome()`, sem alteração do formato ou das regras do mundo.

## Validação realizada

- 308 testes Java aprovados, zero falhas/erros/ignorados. Inclui teste novo separando dados e assets. Log: `docs/TASK-auto-update-java.log`.
- 30 testes Python aprovados: 25 anteriores e 5 novos para canal/pacote inicial. A suíte Java/Python conjunta terminou antes da adição dos cinco testes Python; a descoberta Python completa foi repetida e passou em 30. Logs: `docs/TASK-auto-update-qualidade.json` e `docs/ATUALIZADOR-python-final.log`.
- 25 verificações do atualizador Windows: instalação inicial, troca incremental, no-op, hash incorreto, staging, travessia de diretórios, caminhos reservados, duplicação de nomes, HTTP recusado sem rede, canal ausente, recuperação de referência ativa corrompida, lock e preservação de saves. `tools/updater/Test-Atualizador.ps1`; evidências em `.task-checkpoints/auto-update-tests-final/`.
- Duas falhas iniciais do próprio atualizador foram encontradas e corrigidas antes da entrega: criação da pasta de versões antes do move e substituição atômica compatível com Windows PowerShell 5.1 (backup explícito em vez da coerção de `$null`). Logs iniciais preservados, sem alterar testes antigos para encobrir falhas.
- Pacote base 0.78.1-rc1 auditado por `auditar_rpg.py`: runtime, classes, recursos, ZIP e hashes conferidos.
- Pacote inicial extraído e iniciado pelo atualizador, canal vazio, jogo real com sprites, 61 ticks, captura de janela e saída 0. Nenhum save pessoal incluído. Evidências em `.task-checkpoints/updater-real-smoke/`.
- Atualização local sobre esse pacote real: **1 arquivo transferido e 160 reutilizados**; referência anterior preservada e arquivo sentinela de save intacto. Esse ensaio altera somente cópia de teste do canal, não o ZIP de entrega.
- Nenhum serviço público, conexão remota de atualização, upload ou publicação foi usado para esses testes. Transferência HTTPS real e comportamento em outro Windows dependem da futura hospedagem e homologação no computador do testador.
