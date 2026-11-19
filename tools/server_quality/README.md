# Qualidade isolada do servidor Spirit Gate

Esta area instala e executa ferramentas sem acrescentar dependencias ao runtime do jogo. Requer Windows x64, Python 3.12+, JDK 17 e Maven no PATH. As versoes fixadas estao em [toolchain.json](toolchain.json).

## Instalacao

```powershell
python -B tools/server_quality/install_tools.py
```

Destino padrao: `%LOCALAPPDATA%/SpiritGate/quality-tools`. O instalador verifica SHA-256 fixado das releases oficiais e preserva instalacoes existentes. Nao altera PATH, registro, firewall ou launcher. ZIP/TAR com escape de caminho, nomes reservados, links e colisoes ambiguas sao recusados.

Skills Trail of Bits foram instaladas separadamente pelo skill-installer do Codex, na revisao registrada no manifesto; ficam em `~/.codex/skills`.

## Execucao

A partir da raiz do repositorio:

```powershell
# Regressao focalizada; a primeira resolucao Maven pode baixar dependencias de teste.
python -B tools/server_quality/run_quality.py --mode test

# Depois de preencher o cache, permite verificacao sem downloads.
python -B tools/server_quality/run_quality.py --mode test --offline

# Compila, testa, analisa bytecode/fontes e executa uma campanha Jazzer limitada.
python -B tools/server_quality/run_quality.py --mode all --offline

# Modos individuais.
python -B tools/server_quality/run_quality.py --mode compile --offline
python -B tools/server_quality/run_quality.py --mode spotbugs --offline
python -B tools/server_quality/run_quality.py --mode pmd --offline
python -B tools/server_quality/run_quality.py --mode fuzz --offline
python -B tools/server_quality/run_quality.py --mode codeql

# Testes de seguranca do instalador e da politica de projeto fechado, sem rede.
python -B -m unittest discover -s tools/server_quality -p 'test_*.py' -v
```

Cada chamada cria um diretorio temporario novo `spiritgate-quality-*`. `--output CAMINHO_NOVO` permite escolher outra pasta, desde que ainda nao exista. Fontes/recursos de teste e assets publicos sao copiados; saves, dist, arquivos pessoais, checkpoints e configuracoes de banco nao sao copiados. Links/junctions nas arvores de entrada sao recusados.

A JVM usa home e logs dentro da copia. Testes existentes que produzem imagens/documentos relativos escrevem nessa copia, nao no historico original. O conjunto padrao seleciona testes puros/TCP especificos; nao representa autorizacao para chamar toda a suite GUI ou laboratorios de persistencia.

Resultados: `summary.json`, `test.log`, XMLs em `test-reports/`, `spotbugs.xml`, `spotbugs.log`, `pmd.xml`, `pmd.log`, `fuzz.log` e `fuzz-reports/`. O wrapper limita processos a 360 s e a JVM de teste a 120 s. O alvo Jazzer limita a campanha a 20 s ou 100.000 entradas.

## Ferramentas e limites

- SpotBugs 4.10.5 + plugin Find Security Bugs 1.14.0 analisam classes de producao. Codigo de saida 1 do SpotBugs significa achados; 2 indica classes ausentes e 4 erro de analise. O wrapper conserva contagens e interrompe problemas operacionais.
- Jazzer 0.30.0 roda em regressao durante `test` e fuzzing durante `fuzz`. Corpus e sementes ficam na copia temporaria; nao e executado contra rede externa.
- Netty 4.1.141.Final aparece somente como dependencia de teste: EmbeddedChannel compara framing/fragmentacao das aplicacoes 6/7. O servidor continua usando sockets do JDK.
- JUnit BOM 5.11.4 alinha Jupiter, commons e launcher, inclusive transitivos de Jazzer. Nao altera o POM principal do jogo.
- PMD 7.29.0 analisa os fontes Java 17 com regras de seguranca, multithreading e error-prone em [pmd-ruleset.xml](pmd-ruleset.xml). Codigo 4 significa alertas; erros de configuracao/processamento continuam impeditivos. Nao equivale a toda a analise interprocedural do CodeQL.
- **Projeto fechado:** CodeQL esta desativado, fora de `all` e da instalacao padrao. O modo legado `codeql` apenas grava estado DESATIVADO, sem copiar fontes ou chamar o executavel; `--codeql-licensed` nao reativa a analise. O binario instalado anteriormente foi preservado, sem uso neste fluxo.

O fluxo ativo usa analisadores e testes locais: SpotBugs/Find Security Bugs, PMD e Jazzer. Nenhum fonte e publicado, nenhuma licenca do jogo e alterada e nenhuma ferramenta e empacotada no runtime por esta configuracao. PMD/Find Security Bugs sao complementos, sem promessa de equivalencia total ao CodeQL. Licencas e avisos dos binarios continuam em seus diretorios de instalacao; nao foram removidos.

## Correcao e testes adicionados

[ServerHandshakeDeadlineTest](../../server/src/test/java/com/projetok/multiplayer/ServerHandshakeDeadlineTest.java) cobre Hello lento, escolha incompleta com prazo compartilhado, escolha normal e Hello valido fragmentado. A correcao aplica o prazo existente de 2 s a toda a negociacao de entrada e espera de admissao, sem mudar os bytes do protocolo.

Os testes desta pasta acrescentam propriedades de roundtrip, rejeicao e comparacao com Netty, alem do alvo Jazzer. Eles sao mesclados somente na copia temporaria de testes, com recusa de colisao de nomes.

Nao executar Maven diretamente sobre este POM como alternativa ao wrapper: o padrao `game.root` aponta para uma pasta inexistente de proposito. O wrapper e quem prepara e informa a arvore isolada.

## Reversao

Nenhuma ferramenta altera o runtime por instalacao. Para reverter, preservar evidencias e retirar apenas os diretorios novos enumerados no manifesto/relatorio, apos validar os caminhos absolutos. O patch Java possui copia anterior no checkpoint da entrega. Nao usar reset/clean, apagar caches inteiros ou remover outras skills.

