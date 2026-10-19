# TASK-036 - Release candidate consolidada

Objetivo: candidata portátil Windows x64 construída dos fontes consolidados.
Base: TASK-035 aprovada, 241 JUnit e 25 Python.
Versão 0.36.0-rc1 sem conflito anterior no diretório dist.
Situação: AUTOMAÇÃO LOCAL APROVADA; homologação humana pendente.

Implementação: versão do diagnóstico consolidada; README/changelog e aviso do
pacote atualizados; kit novo com roteiro por tarefa, modelo de bug, notas e mapa
V2 opcional. Build limpo real com Maven offline, sem SkipMaven; JAR novo criado
somente de domain/prototype; jdeps, jlink, jpackage e JVM Java 17 integrada.
Não foram incluídas artes pendentes, fontes de teste ou configurações privadas.
Revisão final corrigiu o comando do mapa opcional no kit e tornou os caminhos
dos previews HTML relativos, para portabilidade do pacote documental.

Artefato: dist/SpiritGate-0.36.0-rc1-20261019T042237Z-a006aab1-portable.zip
Bytes: 29401846
SHA-256: 511d59412598be73395391e21a6cfb2315f4855c714059086abc4d02e46cff31
Auditoria oficial PASS: ZIP original, inventário, whitelist, hashes, PE x64 e
bytecode de todas as classes atuais. Controle negativo e distribuição visual
sintética testados em cópias, sem promoção de arte. Ver logs/JSON TASK-036.
Smokes reais do EXE padrão e --map em extrações distintas fora de Projeto-K,
PATH sem Java externo, JVM integrada comprovada, fechamento normal e códigos 0.
Capturas TASK-036-exe.png e TASK-036-mapa-exe.png são reais, não simulações.
Inspeção de sockets por CIM pode estar indisponível: ver JSON, sem afirmar o
que esse controle não conseguiu medir. Não houve teste em outro computador.

Preservação: 82 fontes/testes históricos de domínio idênticos;
228 PNGs idênticos ao inventário histórico; ZIP 0.21 original intacto.
Todos os releases anteriores mantidos. Sem commit, push, publicação ou upload.
Arquivos modificados/adicionados e inventário estão no relatório final e diff.

Reprodução: python tools/qualidade.py --offline --task 036
Build: tools/build-windows-portable.ps1 -Version 0.36.0-rc1 -MavenRepository
.maven-repository -TesterKitDirectory docs/kit-homologacao-022-036
Auditoria: python tools/auditar_rc.py --zip dist/SpiritGate-0.36.0-rc1-20261019T042237Z-a006aab1-portable.zip --report docs/re-auditoria.json
Smoke: tools/test-windows-portable.ps1 -ZipPath dist/SpiritGate-0.36.0-rc1-20261019T042237Z-a006aab1-portable.zip

Limites: sem assinatura do executável, sem arte final aprovada, sem homologação
humana em outro Windows. Não é lançamento público. Próximo passo humano: kit de
homologação por tarefa. Continuidade em docs/CONTINUIDADE-TASK-022_036.md.

## Resultado final observado
Validação local aprovada: 241 JUnit e 25 Python; zero falhas.
