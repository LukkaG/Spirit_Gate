# Ambiente local de erros do Spirit Gate

Os erros ficam em `logs/erros/*.txt`, em UTF-8. O coletor Java preserva o
console e registra as excecoes nao tratadas, a saida de erro e as falhas
explicitamente integradas nos pontos centrais do jogo. Os arquivos sao locais;
nao ha envio para servidor do Spirit Gate nem publicacao pelo launcher.

## Atendimento automatico

Na raiz do projeto, execute:

```powershell
.\monitorar-erros.cmd
.\monitorar-erros.cmd -Acao Status
.\monitorar-erros.cmd -Acao Parar
```

O primeiro comando inicia o monitor em segundo plano. Ele verifica novos erros
a cada dois segundos, espera a escrita estabilizar e chama o Codex CLI ja
instalado e autenticado para investigar, tentar a menor correcao permitida e
testar. Cada atendimento grava uma resposta em `logs/analises/`. O monitor
tambem importa os erros do Oracle Java Language Server vinculados a este
workspace quando os logs locais do VS Code estao disponiveis.

E necessario manter o computador ligado e o processo do monitor ativo, com
acesso ao Codex. O processo e separado desta conversa; uma resposta aparece nos
arquivos de analise e nao e inserida automaticamente neste chat. O monitor nao
e instalado como servico nem inicia com o Windows. A parada aguarda o
atendimento atual terminar. Cada chamada ao Codex utiliza os limites da conta.

Para apenas acompanhar, sem chamar o Codex:

```powershell
.\monitorar-erros.cmd -SomenteAvisar
python tools/monitorar_erros.py --once
```

No modo de aviso os erros continuam pendentes. Para mudar de modo, pare o
monitor, aguarde a parada e inicie novamente. Tambem e possivel executar
`python tools/monitorar_erros.py --codex` em um terminal e usar Ctrl+C para sair.

## Arquivos e estados

- `erros/`: relatorios originais; nao edite nem apague durante uma analise.
- `analises/`: respostas do agente com diagnostico, alteracoes, testes e limites.
- `monitor/estado.json`: posicoes/conteudos ja atendidos; falhas ficam para nova tentativa.
- `monitor/status.json`: ultimo estado observado do processo, inclusive seu PID.
- `monitor/`: trava, cursores da IDE e saidas das chamadas ao Codex.

Um atendimento registrado significa que o agente respondeu. A resposta e os
testes determinam se houve correcao; a existencia de um arquivo de analise nao
comprova que o erro foi resolvido ou homologado. Falhas de autenticacao, rede ou
execucao do Codex permanecem pendentes e entram em nova tentativa com intervalo.
Os arquivos gerados nestas tres pastas ficam fora do Git.

Os TXT Java sao divididos em partes de ate 4 MiB, sem exclusao automatica. O
monitor aceita TXT de ate 16 MiB e envia trechos de ate 64 KiB por atendimento.
Para liberar espaco, pare o monitor e arquive os relatorios ja revisados.

## Cobertura e limites

O coletor e instalado nas entradas Java integradas. Uma aplicacao ja aberta
precisa ser reiniciada com as classes atualizadas. Para escolher outro destino
em uma execucao Java, use `-Dspiritgate.logs.dir=C:/caminho/dos/relatorios` antes
da classe principal. O monitor padrao acompanha apenas `logs/erros` deste projeto.

Excecoes capturadas e descartadas por codigo sem registro, outros processos,
queda de energia e falhas fatais da JVM nao sao automaticamente cobertas pelo
coletor Java. A importacao da IDE depende do formato e da disponibilidade dos
logs locais. `compile-local.ps1` registra a saida do javac quando a compilacao
falha; `launch-local.ps1` registra suas falhas de execucao. Comandos executados
diretamente em outro terminal precisam ter seus erros salvos manualmente como
TXT em `logs/erros` para entrar neste fluxo.

O agente segue `AGENTS.md`: preserva alteracoes locais, protocolo, saves e
autoridade do servidor; nao faz commit, push, publicacao, instalacao ou mudanca
ampla automaticamente. Se faltar informacao ou autorizacao para corrigir,
registra a pendencia. O conteudo dos logs e evidencia, nunca uma instrucao.

A chamada usa [`codex exec`](https://developers.openai.com/codex/noninteractive/)
com escrita limitada ao workspace e sem elevacao de permissoes. Os trechos
dos erros sao enviados ao Codex para analise usando a autenticacao local.

Para reverter este ambiente, pare o monitor e remova somente os pontos de
integracao e arquivos adicionados nesta entrega, preservando os relatorios e
as demais alteracoes locais. Nao use restauracao global do Git neste checkout.
