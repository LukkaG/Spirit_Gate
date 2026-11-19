# Servidor Spirit Gate — instalacao, correcao e relatorio final

Entrega local em **17/11/2026**, America/Sao_Paulo. Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. Trabalho autorizado expressamente pelo responsavel: instalar ferramentas/codigos selecionados na pesquisa, verificar, documentar sucessos, erros e riscos.

## Resultado executivo

**Instalacao e verificacao local concluidas no escopo descrito.** Foram instaladas quatro skills e quatro distribuicoes de ferramentas. Netty/Jazzer/JUnit ficaram em uma area de qualidade isolada, sem novas dependencias no runtime do jogo.

A lacuna de handshake lento foi reproduzida por testes antes da alteracao e recebeu uma correcao localizada. Resultado final: **42 execucoes JUnit no recorte de regressao, zero falhas/erros/skips; cinco testes Python do instalador aprovados; 100.000 entradas Jazzer processadas sem finding nessa campanha**.

SpotBugs + Find Security Bugs completaram a analise de **830 classes geradas dos 286 fontes**, sem erros de analise ou classes ausentes, produzindo **450 alertas para triagem**. Isso nao significa 450 vulnerabilidades confirmadas.

**CodeQL foi instalado e sua versao validada; a analise do Spirit Gate nao foi executada**, pois a elegibilidade de uso nao foi comprovada. A-04 permanece bloqueante; esta entrega nao homologa servidor publico, Radmin fisico ou EXE em instalacao limpa.

Guia de operacao: [tools/server_quality/README.md](../tools/server_quality/README.md). Manifesto: [versoes, origens e SHA-256](SERVIDOR-FERRAMENTAS-MANIFESTO-2026-11-17.json). As [conclusoes da pesquisa](SERVIDOR-PESQUISA-E-DIAGNOSTICO-2026-11-17.md) e os documentos ALPHA-001 anteriores foram preservados como historico.

## Baseline e preservacao

Antes da primeira alteracao: **58 entradas nao rastreadas**, sem modificacoes rastreadas; 3.831 arquivos existentes inventariados por SHA-256. Esses totais incluem trabalho local anterior de ORG-03 e os oito documentos de pesquisa/auditoria ja entregues. Nenhuma entrada anterior foi apagada, movida, sobrescrita ou adicionada ao Git.

Checkpoint novo: [.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe](../.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe), com baseline.json, copia do fonte de producao anterior, diff de producao e evidencias de cinco rodadas. Cada rodada conserva logs, XMLs e hashes dos fontes efetivamente copiados/executados. Ignorados e arquivos inacessiveis nao estao cobertos pelo inventario geral.

AGENTS.md, skill-installer, skill local de engenharia do servidor, POM e relatorios da pesquisa foram consultados. O monitor local continuava com registro antigo e listas vazias; isso nao foi interpretado como processo ativo nem correcao dos TXT antigos. Nenhum atendimento corretivo de World Studio foi iniciado.

## O que foi instalado

| Item | Versao/revisao | Instalacao e verificacao | Resultado |
|---|---|---|---|
| audit-context-building | Trail of Bits, commit 149d7b21f4067765b82e67029730b42d228ef4b4 | Skill-installer oficial do Codex; SKILL.md e recursos presentes | INSTALADO |
| property-based-testing | Mesmo commit | Skill-installer; orientacao de roundtrip/invariantes aplicada aos testes novos | INSTALADO |
| variant-analysis | Mesmo commit | Skill-installer; recursos presentes para futuras buscas de variantes | INSTALADO |
| codeql (skill) | Mesmo commit | Skill-installer; nao equivale a executar uma analise CodeQL | INSTALADO |
| SpotBugs | 4.10.5 | Release oficial, SHA-256 fixado, CLI -version e analise real | OPERACIONAL |
| Find Security Bugs | 1.14.0 | Release oficial; plugin JAR 1.14.0 carregado pelo SpotBugs 4.10.5 | OPERACIONAL |
| Jazzer | 0.30.0 | Distribuicao Windows oficial + dependencia JUnit isolada; CLI e fuzzing real | OPERACIONAL |
| CodeQL CLI | 2.27.4 | Release oficial, SHA-256 fixado, version --format=json | INSTALADO; ANALISE PENDENTE |
| Netty codec | 4.1.141.Final | Maven Central; somente dependencia de teste; EmbeddedChannel exercitado | OPERACIONAL EM TESTE |
| JUnit | BOM 5.11.4 | Alinhamento de Jupiter/commons/launcher no POM das ferramentas | OPERACIONAL EM TESTE |

Binarios em `%LOCALAPPDATA%/SpiritGate/quality-tools`; skills em `~/.codex/skills`. Nao houve instalacao de servico, alteracao de PATH, registro, firewall ou configuracao do launcher. A CLI e o JUnit Jazzer coexistem para permitir uso direto e testes por Maven. Netty nao foi integrado ao transporte de producao.

As quatro skills receberam tambem o aviso de licenca do repositorio de origem na revisao instalada. Nao foram substituidas skills anteriores do usuario. As skills novas ficam disponiveis para selecao normal no **proximo turno**; os arquivos instalados ja foram verificados localmente.

Downloads de ferramentas foram verificados pelos SHA-256 fixados em [toolchain.json](../tools/server_quality/toolchain.json), obtidos dos metadados das releases oficiais. O manifesto da entrega registra origem, versao, destino e checksum; nao e uma assinatura de release do jogo.

A segunda chamada do instalador identificou as quatro instalacoes anteriores sem rebaixar, sobrescrever ou baixar novamente.

## Correcao de producao

Arquivo: [LocalSimulationServer.java](../server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java).

Antes, `socket.setSoTimeout(2000)` limitava cada leitura bloqueante. Um emissor de pequenas parcelas podia manter um frame incompleto ativo alem dos 2 s e ocupar uma das duas vagas antes de concluir a negociacao.

Foi acrescentado `NegotiationInput`, que calcula o tempo restante do mesmo deadline antes de cada leitura do prefixo/corpo. O prazo e iniciado ao reservar o peer e compartilhado entre Hello, CharacterCreate e espera da admissao pelo owner. A leitura normal da partida volta ao timeout existente de 5 s.

O patch **nao muda bytes, opcodes, serializacao, negociacao 6/7, capacidade dois, dano, HP, energia, skills, inventario, saves, banco, launcher ou publicacao**. A mudanca de comportamento intencional e recusar negociacoes que excedam o prazo total existente de 2 s, mesmo que cheguem fragmentos frequentes.

Limites: o patch nao implementa timeout de escrita, recuperacao de personagem, politica nova de duracao da sessao ou autenticacao de contas. Um cliente/rede legitimo que precise de mais de 2 s para enviar a negociacao completa pode ser recusado; o teste fisico continua necessario para avaliar esse limite no ambiente alvo.

## Codigo de qualidade acrescentado

- [install_tools.py](../tools/server_quality/install_tools.py): instalador de releases fixadas, checksums, extracao em pasta nova, recusa de escapes/links/nomes ambiguos e preservacao de instalacao anterior.
- [run_quality.py](../tools/server_quality/run_quality.py): copia publica temporaria, home/logs isolados, testes selecionados, scanner/fuzzer e resumo de resultados.
- [pom.xml de qualidade](../tools/server_quality/pom.xml): dependencias de teste separadas; o POM principal do servidor ficou intacto.
- [ServerHandshakeDeadlineTest](../server/src/test/java/com/projetok/multiplayer/ServerHandshakeDeadlineTest.java): teste integrado ao conjunto normal do servidor, usando JUnit ja existente.
- [ProtocolPropertiesTest](../tools/server_quality/src/test/java/com/projetok/multiplayer/ProtocolPropertiesTest.java): 2.000 comandos gerados dentro de um teste de roundtrip, cauda invalida e nao finitos; sem inflar contagem JUnit.
- [NettyFrameCompatibilityTest](../tools/server_quality/src/test/java/com/projetok/multiplayer/NettyFrameCompatibilityTest.java): comparacao do framing 6/7 em cada corte do frame, com buffers liberados.
- [ProtocolFuzzTest](../tools/server_quality/src/test/java/com/projetok/multiplayer/ProtocolFuzzTest.java): corpus valido/invalido e serializacao canonica estavel.
- [test_install_tools.py](../tools/server_quality/test_install_tools.py): cinco testes sem downloads, protegendo extracao e nao sobrescrita.

Os testes da area de qualidade sao mesclados apenas na copia temporaria; nenhum deles e empacotado no cliente. A copia inclui recursos e assets publicos, mas nao saves, dist, dados pessoais ou checkpoints. Os testes escolhidos nao acessam banco nem servico de jogo externo.

## Testes, comandos e contagem sem duplicacao

| Etapa | Comando principal | Resultado | Evidencia |
|---|---|---|---|
| Instalacao das skills | install-skill-from-github.py com repo, quatro paths e commit fixado | 4 skills instaladas | Manifesto da entrega e arquivos locais |
| Instalacao dos binarios | python -B tools/server_quality/install_tools.py | 4 distribuicoes instaladas e verificadas | Recibos locais e manifesto |
| Regressao vermelha, fonte anterior | run_quality.py --mode test --tests ServerHandshakeDeadlineTest --offline | 3 casos: 2 falhas esperadas de Hello/escolha incompletos; 0 erros | [02-before-fix](../.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe/02-before-fix) |
| Correcao inicial | Mesmo recorte em copia nova | 3 aprovados, 0 falhas/erros/skips | Teste intermediario registrado na coleta temporaria |
| Qualidade integrada | run_quality.py --mode all --offline | 41 execucoes JUnit aprovadas; scanner e fuzzer executados | [04-quality-final](../.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe/04-quality-final) |
| Regressao final reforcada | run_quality.py --mode test --offline | **42 execucoes JUnit aprovadas**, 0 falhas/erros/skips | [05-regression-final](../.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe/05-regression-final) |
| Instalador | python -B -m unittest discover -s tools/server_quality -p test_install_tools.py -v | **5 aprovados** | Saida registrada nesta entrega |
| SpotBugs + Find Security Bugs | java -jar spotbugs.jar -textui -pluginList ... -effort:max -low -exitcode -xml:withMessages ... | **450 alertas**, zero erros e classes ausentes | [spotbugs.xml](../.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe/04-quality-final/spotbugs.xml) |
| Jazzer | run_quality.py --mode fuzz, executado pela etapa all | **100.000 entradas**, sem finding; cov=180 no instrumento desta campanha | [fuzz.log](../.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe/04-quality-final/fuzz.log) |
| CodeQL | codeql version --format=json | 2.27.4 validado; sem base/queries do projeto | [codeql.log](../.task-checkpoints/SERVIDOR-FERRAMENTAS-20261117-e8b91dfe/04-quality-final/codeql.log) |

O conjunto final inclui testes existentes de parser, protocolo de skills, servidor local, robustez, snapshot forjado, TCP real 6/7, reconexao, XP e jornada de missao. A fixture de missao/progressao e a ja existente no repositorio, nao um ajuste de HP de producao.

Os quatro casos finais do prazo cobrem: Hello incompleto com parcelas continuas; escolha incompleta que consome o mesmo prazo do Hello; escolha valida; Hello valido fragmentado dentro do prazo. **4/4 aprovados**. O segundo caso consome parte do prazo antes da segunda mensagem para detectar renovacao indevida.

Nao somar 3 + 41 + 42 como total unico: sao reexecucoes e evolucoes do mesmo recorte. O modo fuzz registrou oito execucoes JUnit em sua propria rodada (incluindo sementes/execucao do alvo), que tambem nao devem ser somadas ao total da regressao. Entradas do fuzzer sao medidas separadamente de casos JUnit.

A compilacao Java 17 dos 286 fontes ocorreu na copia. Sucesso de compilacao nao certifica todas as funcionalidades. **Nao foi executada a suite completa, GUI, Radmin, dois PCs, benchmark de carga ou instalacao EXE limpa.**

## Erros encontrados durante a instalacao/configuracao

| Ocorrencia real | Causa identificada | Tratamento / estado |
|---|---|---|
| JUnit falhou antes da descoberta de testes | Jazzer transitivamente trouxe commons/launcher 1.9.0 enquanto o Jupiter era 5.11.4; NoSuchMethodError | Corrigido por BOM JUnit 5.11.4 apenas no POM de qualidade; rodada sem casos nao foi contada como aprovacao |
| Agente Jazzer recusou configuracao inicial | Glob usava separador/padrao nao suportado | Corrigido para com.projetok.multiplayer.**; regressao e campanha reais passaram |
| Dois testes de handshake falharam antes do patch | Defeito reproduzido do timeout por leitura | Fonte corrigido; testes antigos e novos preservados |
| Jazzer imprimiu aviso da JVM sobre bootstrap/class sharing | Instrumentacao do agente altera o bootstrap classpath | Aviso preservado, sem falha de teste; CLI tambem avisa sobre opcao CriticalJNINatives depreciada no JDK17 |
| SpotBugs retornou codigo 1 | Codigo documentado pelo proprio binario como BUGS_FOUND_FLAG | Tratado como analise concluida **com alertas**, sem esconder erro de infraestrutura; bits 2/4 continuam impeditivos |
| CodeQL nao foi usado para analisar o projeto | Elegibilidade de uso ainda nao demonstrada | Instalacao e versao validadas; analise marcada PENDENTE, sem modificar licenca ou publicar codigo |

Nenhuma falha foi mascarada alterando assercoes de testes anteriores, gameplay, protocolo ou assets. Logs das rodadas com erro foram conservados.

## Alertas estaticos: resultado e triagem inicial

| Categoria do scanner | Quantidade |
|---|---:|
| SECURITY | 58 |
| STYLE | 96 |
| BAD_PRACTICE | 146 |
| MALICIOUS_CODE | 60 |
| PERFORMANCE | 49 |
| MT_CORRECTNESS | 37 |
| CORRECTNESS | 4 |
| **Total** | **450** |

Prioridades internas do scanner: 8 de prioridade 1, 180 de prioridade 2 e 262 de prioridade 3. **Essas prioridades nao equivalem automaticamente a P0/P1/P2 do Alpha.** A categoria MALICIOUS_CODE inclui exposicao de representacao/colecoes e nao demonstra que exista malware no projeto.

Triagem manual limitada aos principais grupos, com leitura do contexto:

| Grupo / local | Interpretacao atual | Acao |
|---|---|---|
| UNENCRYPTED_SOCKET, LocalClient/LocalSimulationServer | Confirma uso do TCP sem TLS no caminho inspecionado. Lacuna conhecida para exposicao publica; nao houve captura de dados ou invasao | Planejar identidade/transporte protegido em recorte proprio |
| VO_VOLATILE_INCREMENT, contadores de NetworkDiagnostics | Incremento de volatile nao e atomico, mas cada alerta exige verificar quantos writers existem; alguns contadores do servidor sao escritos apenas pelo owner | Revisar uso/concorrrencia e medir; nao tratar como dano duplicado comprovado |
| RC_REF_COMPARISON, RpgSessionWorld.prepareRevision | Comparacao de Integer por referencia; o trecho atual percorre o mesmo mapa, cujas chaves armazenadas sao as mesmas referencias. Nao foi demonstrada falha para IDs acima do cache | Registrar fragilidade de expressao, sem patch de publicacao por alarme automatico |
| DE_MIGHT_IGNORE, preferencias/preview/poll da UI | Existem catches que evitam interromper a UI; alguns sao intencionais e comentados. Podem prejudicar diagnostico | Revisar logging com contexto, sem modificar foco/UX nesta entrega |
| UNSAFE_HASH_EQUALS, LivePublishing/revisoes/assets | Comparacoes de hash de conteudo publico/integridade foram sinalizadas; nao sao automaticamente comparacao de segredo de autenticacao | Nao trocar comparacoes em massa; diferenciar hash publico de credencial |
| COMMAND_INJECTION, RadminSessionPanel | Trecho usa ProcessBuilder com executavel encontrado nos caminhos conhecidos; o alerta nao demonstra entrada remota concatenada em shell | Nao executado nesta tarefa; revisar a origem/canonicalizacao do executavel em tarefa propria |
| PATH_TRAVERSAL_IN, launchers | Entradas de CLI/captura local exigem analise de fronteira; scanner nao demonstra alcance remoto | Revisar separadamente; nao executar CLI de caminho arbitrario |
| PREDICTABLE_RANDOM, loot/combate/fixtures | Aleatoriedade de gameplay nao deve ser presumida token de seguranca. Previsibilidade pode ser relevante ao design, sem exploit confirmado | Manter regra existente ate avaliacao de uso e balanceamento |

Os 450 alertas completos estao no XML; **nao foram todos homologados ou corrigidos**. A entrega prioriza a falha reproduzida e a instalacao do fluxo de verificacao. Analise estatica nao substitui testes de lifecycle, seguranca e operacao.

## Pendencias e possiveis erros

- **A-04 / P0 permanece aberto.** Nao foi ativado timeout de 92 ticks, removida skill ou integrada recuperacao.
- A-03 visual, jornada de 20–30 min com 10.000 HP, foco e pacote EXE continuam com os limites da ALPHA-001.
- Prazo total de 2 s precisa de avaliacao em rede fisica autorizada; ele passou em TCP loopback.
- Timeout de escrita/cliente que deixa de ler e corrida de admissao/close continuam hipoteses a ensaiar.
- O limite de sessao de 30 minutos continua no transporte, sem mudanca de politica nesta entrega.
- Identidade duravel, autenticacao de contas, TLS, banco e ORG-03/D33 nao foram implementados.
- CodeQL: validar elegibilidade antes de analise; o modo padrao confere somente a versao.
- Os alertas estaticos precisam de triagem progressiva e testes antes de novos patches.
- A selecao de regressao nao representa cobertura completa nem certifica seguranca em producao.

## CodeQL: esclarecimento sobre uso

CodeQL e uma ferramenta de analise de codigo, nao uma dependencia necessaria para rodar o jogo. A instalacao funcionou. Os [termos do pacote](https://github.com/github/codeql-cli-binaries/blob/main/LICENSE.md) condicionam a analise de projetos ao escopo permitido; projetos fechados dependem de licenca comercial aplicavel. A [documentacao GitHub](https://docs.github.com/en/code-security/concepts/code-scanning/codeql/codeql-cli) descreve disponibilidade para repositorios publicos e organizacoes com GitHub Code Security, vinculada aos termos.

Nao foi comprovada uma licenca open source OSI do Spirit Gate nem uma licenca comercial aplicavel. Por isso nao foi criada base de analise do jogo, rodada query ou enviado resultado ao GitHub. Nao e necessario abrir o projeto ou comprar uma licenca para usar o restante do fluxo instalado.

A opcao --codeql-licensed do runner serve apenas para operador que ja tenha verificado essa condicao; cria base local, mas nao executa automaticamente queries. Nao foi usada. A licenca do jogo permaneceu intacta.

## Compatibilidade e limites de autorizacao

Foram mantidos transporte/framework existentes, autoridade do owner, mensagens 6/7, capacidade dois, gameplay, offline/Coop fora do efeito de negociacao TCP, launcher, World Studio, assets e formatos de saves. O patch modifica o transporte compartilhado, portanto seus consumidores TCP tambem passam pelo prazo total; os testes de servidor/Coop e TCP RPG foram incluidos.

Nenhum codigo de Nakama, Minestom, TFS ou jqwik foi importado. A pesquisa os havia classificado como referencias ou candidatos adiados, nao substitutos aprovados do motor. Netty foi exercitado somente em teste comparativo.

Sem commit/push/pull, alteracao de branch, release, publicacao no canal updates, banco, dados pessoais ou servico externo de jogo.

## Reversao segura

1. Preservar o checkpoint e o trabalho posterior.
2. Comparar o diff de LocalSimulationServer com a copia anterior e retirar somente o patch desta entrega se solicitado.
3. Retirar os novos testes/ferramentas/documentos apenas apos verificar referencias posteriores; nao usar reset/clean.
4. As ferramentas ficam em quatro diretorios versionados listados no manifesto; remover somente esses destinos absolutos apos valida-los. Nao apagar o perfil Codex, todas as skills ou caches Maven inteiros.
5. Nenhuma migracao de dados/wire/save precisa ser revertida. A reversao do patch reintroduz a lacuna reproduzida de handshake.

## Proximo passo sugerido

Triar os alertas de concorrencia/transporte com testes focados, decidir a politica dos 30 minutos e tratar A-04 em sua tarefa propria. Para homologacao, executar o roteiro fisico ALPHA-001 apos os gates pertinentes. Nenhum desses passos foi iniciado automaticamente.

## Conferencia final

Comparacao final dos **3.831 arquivos anteriores**: **3.830 preservados por SHA-256**; somente LocalSimulationServer.java mudou, pelo patch autorizado. Foram acrescentados **12 arquivos versionaveis**, alem das evidencias ignoradas e das instalacoes no perfil local. Git passou de 58 para 63 entradas agregadas; HEAD inalterado. Nenhum teste anterior, asset, POM principal ou documento historico teve divergencia.

O fonte de producao atual coincide por SHA-256 com a copia usada na regressao final de 42 execucoes. Links locais e tabelas dos novos manuais foram conferidos, sem inconsistencias; git diff --check sem apontamentos. O Git emite somente o aviso de normalizacao LF/CRLF conforme a configuracao existente; ela nao foi alterada.

A analise estatica registrou pico aproximado de 736 MB, e a campanha Jazzer registrou RSS de aproximadamente 1.167 MB. Sao medidas das ferramentas nesta rodada, nao desempenho do jogo ou garantia de consumo futuro. As repeticoes/rodadas com erro continuam no checkpoint e nao foram somadas como aprovacao.

