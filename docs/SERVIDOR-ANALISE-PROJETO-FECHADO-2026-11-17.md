# Servidor Spirit Gate — ferramentas para projeto fechado

Entrega em **17/11/2026**, America/Sao_Paulo. Branch `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.

## Resultado

A diretriz do responsavel e manter o jogo **fechado e comercial**. O fluxo foi adaptado para usar **SpotBugs + Find Security Bugs, PMD e Jazzer**, localmente, sem publicar fontes ou alterar a licenca do Spirit Gate.

**CodeQL esta desativado no runner e fora da instalacao/execucao padrao.** A antiga opcao --codeql-licensed nao o reativa. O executavel instalado na rodada anterior permanece no perfil local, sem ser chamado por este fluxo.

Foi instalado **PMD 7.29.0**, verificado por SHA-256 e executado com Java 17. O conjunto integrado terminou com **42 execucoes JUnit aprovadas, sete testes Python aprovados e 100.000 entradas de fuzzing sem finding na campanha**. As analises estaticas concluiram com alertas, conforme descrito abaixo.

Esta rodada nao alterou o servidor de producao, o patch de handshake anterior, gameplay, protocolo, banco, assets ou saves. Tambem nao resolveu A-04 nem homologa rede fisica/EXE.

## Instalacao e adequacao de uso

PMD foi baixado da [release oficial 7.29.0](https://github.com/pmd/pmd/releases/tag/pmd_releases/7.29.0):

- Arquivo: pmd-dist-7.29.0-bin.zip.
- SHA-256 fixado e verificado: `d27177c185b837fa57f097e482d1253468aa035379c5eb2671fa0a279109b179`.
- Destino: `%LOCALAPPDATA%/SpiritGate/quality-tools/pmd-7.29.0`.
- CLI confirmou PMD 7.29.0, revisao `47b3135b2cdd4afa0b4447da91786497b58bc10b`, sobre Java 17.0.20.1.
- O [LICENSE da versao](https://github.com/pmd/pmd/blob/pmd_releases/7.29.0/LICENSE) apresenta os termos BSD-style e avisos dos componentes. Os avisos da distribuicao foram preservados.

PMD e os demais analisadores sao ferramentas externas de desenvolvimento; nao foram ligados ou empacotados no executavel do jogo. Nenhuma licenca nova foi criada para o projeto. O manifesto de downloads atual esta em [toolchain.json](../tools/server_quality/toolchain.json), com politica project=closed e CodeQL enabled=false.

O instalador padrao foi novamente exercitado: reconheceu os componentes existentes, ignorou CodeQL pela politica e conservou a instalacao PMD. Nao modificou PATH, registro, firewall ou servicos.

## Funcao de cada componente

| Componente ativo | O que cobre | Limite |
|---|---|---|
| SpotBugs | Padroes de defeitos no bytecode Java, incluindo concorrencia e corretude | Alertas exigem contexto e reproducao; nao cobre toda a logica de negocio |
| Find Security Bugs | Detectores de seguranca complementares ao SpotBugs | O transporte/protocolo customizado ainda exige testes e revisao manual |
| PMD | Regras de seguranca, multithreading e error-prone sobre fontes Java 17 | Nao substitui integralmente queries interprocedurais de CodeQL |
| Jazzer | Fuzzing do parser real e estabilidade da serializacao canonica | Campanha limitada; ausencia de finding nao demonstra ausencia de vulnerabilidades |
| JUnit/Netty em testes | Regressao TCP, contratos, propriedades e framing 6/7 | Netty continua restrito a teste; nenhum transporte de producao foi substituido |

Essa combinacao oferece cobertura complementar sobre fontes, bytecode e entradas executadas. Nao foi anunciada equivalencia total a CodeQL nem eliminacao de todos os riscos de seguranca.

Referencias tecnicas: [documentacao PMD](https://docs.pmd-code.org/pmd-doc-7.29.0/pmd_userdocs_installation.html), [regras Java](https://docs.pmd-code.org/pmd-doc-7.29.0/pmd_rules_java.html), [Find Security Bugs](https://github.com/find-sec-bugs/find-sec-bugs).

## Alteracoes de configuracao e codigo de ferramentas

1. [toolchain.json](../tools/server_quality/toolchain.json): politica de projeto fechado, desativacao de CodeQL e versao/checksum PMD.
2. [install_tools.py](../tools/server_quality/install_tools.py): ignora ferramentas desativadas, mesmo se explicitamente selecionadas.
3. [run_quality.py](../tools/server_quality/run_quality.py): `all` passou a executar test → spotbugs → pmd → fuzz. O modo CodeQL apenas grava DESATIVADO, sem copiar o jogo nem iniciar executavel.
4. [pmd-ruleset.xml](../tools/server_quality/pmd-ruleset.xml): categorias Java security, multithreading e error-prone.
5. [test_install_tools.py](../tools/server_quality/test_install_tools.py): acrescentado teste de ferramenta desativada.
6. [test_run_quality.py](../tools/server_quality/test_run_quality.py): prova que a opcao legada nao executa CodeQL nem copia fontes.
7. [README](../tools/server_quality/README.md): comandos e politica atualizados.

A ferramenta PMD recebe explicitamente o jrt-fs.jar do mesmo JDK usado na execucao e as classes compiladas da copia. A primeira rodada emitia aviso sobre inferencia do JDK; a configuracao foi corrigida e o PMD repetido isoladamente, mantendo os mesmos resultados, sem o aviso. Nenhum erro de processamento foi ignorado para obter aprovacao.

## Verificacoes realmente executadas

| Comando / etapa | Resultado | Limite |
|---|---|---|
| install_tools.py --tool pmd | Instalacao e SHA-256 aprovados | Ferramenta de desenvolvimento externa |
| CLI PMD --version | Versao e JDK confirmados | Nao e teste de gameplay |
| run_quality.py --mode all --offline | Fluxo integrado concluido sem falha operacional | Analises estaticas continuam com alertas |
| Regressao JUnit do modo all | **42 execucoes, zero falhas/erros/skips** | Mesmo recorte da rodada anterior, nao suite completa |
| Testes Python, discover test_*.py | **7 aprovados** | Inclui extracao segura e desativacao CodeQL; nao usa rede |
| SpotBugs + Find Security Bugs | **450 alertas, zero erros de analise/classes ausentes** | Nao sao 450 vulnerabilidades confirmadas |
| PMD, repeticao final com JDK explicito | **1.174 alertas, zero erros de processamento/configuracao** | Alertas de regras; precisam de triagem |
| Jazzer | **100.000 entradas**, sem finding; cov=194 nesta campanha | Nao somar entradas a casos JUnit ou campanhas anteriores |
| run_quality.py --mode codeql --codeql-licensed | DESATIVADO; analysisExecuted=false; executableInvoked=false | Nenhuma base/query CodeQL criada |

O modo PMD retorna **4** quando encontra alertas; o runner reconhece esse resultado como analise concluida com achados. Codigos de erro de uso/processamento continuam impeditivos. O SpotBugs retorna **1** para achados, como ja documentado.

Os oito casos/execucoes JUnit internos da rodada de fuzzing incluem sementes e alvo; nao foram somados ao total da regressao. Repeticoes do PMD e da suite nao foram somadas como novos casos unicos.

Nao houve teste em dois PCs, Radmin, GUI, EXE limpo, banco, contas, benchmark do jogo ou publicacao. A compilacao/execucao ocorreu em copia temporaria com home/logs isolados.

## Resultado PMD e triagem de contexto

| Grupo | Quantidade |
|---|---:|
| Multithreading | 288 |
| Error Prone | 886 |
| **Total** | **1.174** |

O grupo Security selecionado nao produziu alerta nesta configuracao. Isso nao invalida os alertas de seguranca do Find Security Bugs nem prova seguranca do projeto.

Prioridades internas PMD: **1 alerta de prioridade 1, 53 de prioridade 2, 1.120 de prioridade 3**. A escala e especifica da ferramenta e nao equivale automaticamente a P0/P1/P2 do Alpha.

Principais padroes:

| Regra | Quantidade | Leitura inicial |
|---|---:|---|
| AvoidLiteralsInIfCondition | 159 | Constantes/legibilidade; nao explorar como vulnerabilidade por contagem |
| AvoidFieldNameMatchingMethodName | 147 | Nomenclatura; nao justifica renomeacao ampla |
| AvoidCatchingGenericException | 123 | Revisar tratamento/contexto; alguns catches de UI sao intencionais |
| UseConcurrentHashMap | 106 | Nao substituir mapas do owner por estruturas concorrentes sem analisar confinamento |
| NullAssignment | 90 | Revisar lifecycle/sentinelas antes de mudar comportamento |
| AvoidDuplicateLiterals | 90 | Manutencao; nao implica erro funcional |
| NonSerializableClass | 80 | Conferir contratos de serializacao efetivamente utilizados |
| CompareObjectsWithEquals | 66 | Verificar tipos e semantica de identidade no trecho concreto |
| AssignmentInOperand | 62 | Legibilidade e risco contextual |
| AvoidSynchronizedAtMethodLevel | 61 | Recomendacao generica; nao remover sincronizacao por automacao |
| DoNotUseThreads | 56 | Regra generica que nao considera, por si, a necessidade de threads em um servidor de jogo |
| AvoidUsingVolatile | 53 | Nao retirar visibilidade de memoria sem prova e testes |

O unico alerta PMD de prioridade 1 foi `ReturnEmptyCollectionRatherThanNull` em RecoverableCharacterSnapshot.java:187. Nao foi tratado como P0 nem alterado: pode refletir um contrato/sentinela que exige contexto, e pertence a recuperacao fora do recorte atual.

Nenhuma supressao de regra foi adicionada para reduzir numeros. Os XMLs completos permanecem no checkpoint, e nao se somam 450 + 1.174 como total de vulnerabilidades distintas: as ferramentas podem sinalizar o mesmo trecho por motivos diferentes.

## Evidencias, integridade e pendencias

Checkpoint novo: [.task-checkpoints/SERVIDOR-FECHADO-20261117-10a41c3d](../.task-checkpoints/SERVIDOR-FECHADO-20261117-10a41c3d), com baseline de hashes, execucao integrada, repeticao PMD, recibo da instalacao e demonstracao de CodeQL desativado. Os relatorios anteriores continuam preservados como historico.

Baseline desta rodada: **3.843 arquivos existentes**, incluindo as alteracoes nao commitadas da rodada anterior. O HEAD continua o mesmo; esta entrega nao assume um checkout limpo. O patch Java anterior foi preservado, sem nova alteracao de producao.

Permanecem: A-04 bloqueante, avaliacao A-03, politica de duracao de sessao de 30 minutos, timeout de escrita/cliente lento, triagem dos alertas e homologacao fisica. Usar mais ferramentas nao substitui essas decisoes e testes.

Reversao: comparar o diff desta rodada e restaurar somente os cinco arquivos de ferramentas alterados, se solicitado; retirar apenas os tres arquivos novos desta rodada depois de conferir trabalho posterior. Os diretorios anteriores de instalacao e historicos nao devem ser apagados globalmente. Nenhuma mudanca de banco, save ou wire precisa de reversao.

Comparacao final: **3.838 dos 3.843 arquivos anteriores preservados por SHA-256**; apenas os cinco arquivos de ferramentas enumerados acima foram alterados, e tres arquivos foram acrescentados. Nenhum fonte de producao, teste do servidor, asset, save, POM principal ou relatorio historico mudou nesta rodada. HEAD inalterado.

Python, XML e JSON foram validados; links locais existentes; git diff --check sem apontamentos. O aviso LF/CRLF do fonte Java corresponde a alteracao herdada da rodada anterior, que permaneceu intacta. A repeticao final do PMD confirmou os 1.174 alertas sem o aviso anterior de inferencia do JDK.

