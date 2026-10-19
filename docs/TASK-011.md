# TASK-011 - Release candidate e kit de homologação

18/10/2026. Candidata 0.11.0-rc1, Windows x64. Não é publicação, release pública, instalador, multiplayer ou homologação externa concluída.

## Preparação

TASK-007 a TASK-010 conferidas: código, PDFs, resumos, logs e diffs. Nenhum bloqueio automatizado permanece. Base inicial da TASK-011: mvn clean test, 202 testes, zero falhas/erros/ignorados. Java/jpackage Temurin 17.0.20.1 e Maven 3.10.0 no Windows 10 amd64.

O pacote foi reconstruído a partir dos fontes consolidados com o build limpo e a suíte completa. Não foi reaproveitado o JAR da TASK-007. O build mantém runtime integrado, lista permitida de produção, jdk.random, inventário e SHA-256, sem substituir a versão anterior.

O kit contém roteiro de homologação por task, modelo de relato de bug e changelog resumido. A cópia para o app-image permite apenas esses três nomes, antes de gerar inventário e manifest. LEIA-ME-TESTER.txt e VERSION.txt são gerados pelo próprio build.

## Evidências reais

AUTOMATIZADO APROVADO: mvn clean test inicial e novamente no build final, 202 testes, zero falhas/erros/ignorados. O log completo do build e da suíte está em TASK-011-final.log; resumo em TASK-011-testes.txt. Nenhum teste novo foi necessário para mecânicas nesta etapa; foram validados os scripts e os artefatos reais.

tools/auditar_rc.py verificou ZIP/manifest/inventário/hashes, PE x64, versões, ponto de entrada, runtime 17 com jdk.random e todas as 122 classes do JAR, byte a byte contra a compilação atual. São 149 arquivos no ZIP; o manifest cobre os outros 148 e o inventário textual cobre 147, evitando circularidade. O staging só contém o conteúdo permitido, sem teste, servidor/banco legado ou padrão concreto de credencial detectado nas classes do jogo. Relatório: TASK-011-auditoria.json.

Controle negativo: uma cópia do ZIP em saída de build recebeu um byte alterado no JAR e um SHA externo recalculado. O verificador recusou essa cópia por divergência no manifest, antes de qualquer execução; exit code 1 era o resultado esperado do controle. Evidência: TASK-011-auditoria-negativa.json. O original e seu hash foram preservados.

EXECUCAO LOCAL CONFERIDA: EXE extraído em pasta com espaços fora de Projeto-K, com PATH sem Java/Maven e JAVA_HOME inexistente. A janela mostrou os três objetos leves e o HUD atualizado. A DLL jvm.dll efetivamente carregada fica sob SpiritGate/runtime; bundledJvmVerified=true. Launcher e processo filho fecharam normalmente com código 0. Evidência TASK-011-portable-smoke.json e captura real TASK-011-exe.png; TASK-011-hud.png mostra quatro estados renderizados a partir da compilação final. Não houve homologação humana externa.

## Distribuição e reprodução

ZIP: dist/SpiritGate-0.11.0-rc1-20261018T220040Z-b2e659ed-portable.zip. Tamanho 29.359.239 bytes. SHA-256: 7484f90f5de5ca0475c8d73a918a8868b20a089d5a3fb3a12dd2e40f5fc19d01. Sidecar .sha256 e manifest externo estão junto ao ZIP. A versão anterior 0.7.0-task007 foi preservada.

Reconstruir na pasta Projeto-K: tools\build-windows-portable.cmd -Version 0.11.0-rc1 -TesterKitDirectory docs\kit-homologacao. O build padrão executa a suíte limpa. Novos horários/identificadores geram nomes e hashes próprios, sem sobrescrita. Comandos completos da auditoria e do smoke estão em CHANGELOG-RC.md.

Entregas: relatório/PDF, resumo/log, diff/checkpoint, ZIP/SHA/manifest, CHANGELOG-RC.md, LEIA-ME-TESTER.txt, modelo de bug e roteiro de homologação por task no kit, scripts de build/smoke/auditoria e RELATORIO-FINAL-RODADA.md/PDF. Nenhum arquivo Java ou teste foi modificado na TASK-011. README atualizado para o estado consolidado.

Limites: a análise de arquivos e padrões não é prova absoluta de ausência de qualquer segredo. Runtime inclui os arquivos normais do JDK, como certificados públicos e avisos legais. Não se inclui banco, conexão JDBC, configuração privada, teste ou bibliotecas legadas da aplicação. Nenhuma migração, rede/protocolo ou dependência externa nova foi adicionada.

HOMOLOGACAO HUMANA PENDENTE: outro Windows x64 sem Java/Maven, usabilidade e roteiro por task. Inspeção de sockets por CIM indisponível por restrição do ambiente, explicitamente registrada. EXE não assinado e espera de saída totalmente ocupada continuam limitações conhecidas. Não há bloqueio automatizado em aberto. Rodada encerrada na TASK-011, sem push/publicação ou trabalho além do escopo.
