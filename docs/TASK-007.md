# TASK-007 - Windows portátil com Java integrado

18/10/2026. Base: checkout local TASK-006.1, incluindo o teste de parede do usuário. Nenhum ZIP anterior foi usado como fonte. Windows 10 amd64, Temurin JDK 17.0.20.1+1, Maven 3.10.0 e jpackage 17.0.20.1.

## Implementação

tools/build-windows-portable.cmd encaminha os argumentos ao script PowerShell. O wrapper usa política de execução restrita ao seu processo; não modifica a configuração permanente do Windows. O script confere JDK 17 x64, executa mvn clean test por padrão, gera um JAR com as classes de produção domain/prototype e confirma a dependência apenas do JDK com jdeps. Não é um fat JAR e não precisa das bibliotecas externas declaradas no POM para o servidor legado. Não houve alteração do POM nem novas dependências.

jlink gera o runtime reduzido e jpackage --type app-image cria SpiritGate.exe, app/ e runtime/. O módulo jdk.random é incluído explicitamente: seus provedores RandomGenerator são carregados por serviço e não aparecem na análise estática de jdeps. O staging é uma lista permitida de pacotes, sem classes legadas de banco/servidor, recursos privados, JUnit ou arquivos de teste. A arena atual não carrega assets externos. Recursos novos fazem o script falhar até revisão explícita da lista permitida.

O pacote inclui LEIA-ME-TESTER.txt, VERSION.txt, INVENTARIO-SHA256.txt e manifest.json com hashes individuais. O manifest exclui a si próprio; o inventário exclui a si próprio e o manifest para evitar referências circulares. Um arquivo .sha256 externo cobre o ZIP inteiro. O script cria saídas únicas, não remove diretórios e não sobrescreve distribuições anteriores; -SkipMaven exige uma compilação/teste anterior.

Reconstrução, na pasta Projeto-K: tools\build-windows-portable.cmd -Version 0.7.0-task007. O argumento -JavaHome permite selecionar um JDK 17. A execução real desta entrega usou -SkipMaven depois do teste limpo registrado abaixo.

## Testes e evidências

AUTOMATIZADO APROVADO: mvn clean test inicial com 170 testes e final com 171, zero falhas/erros/ignorados. DebugLauncherEntrypointTest verifica por reflexão public static void main(String[]) sem abrir JFrame. Os 170 testes e baselines anteriores foram preservados. Log integral: TASK-007-final.log. Resumo: TASK-007-testes.txt.

O script tools/test-windows-portable.ps1 valida ZIP, manifest, hashes, PE x64, runtime Java 17 com jdk.random e configuração de main. Extrai em uma pasta com espaços fora de Projeto-K, remove Java/Maven do PATH somente no processo de verificação, aponta JAVA_HOME para um caminho inexistente e executa o EXE. Captura sua janela e fecha apenas esse processo por WM_CLOSE. A observação de sockets pertence ao intervalo do teste e não substitui auditoria de tráfego histórico.

Pacote aprovado da etapa: dist/SpiritGate-0.7.0-task007-20261018T212519Z-bccb553f-portable.zip, 29.341.071 bytes. SHA-256: c786f095445747111270ae7dccff791944a58cc3b689d4e40898f358824326b2.

EXECUCAO LOCAL CONFERIDA: EXE extraído fora dos fontes em caminho com espaços, PATH sem Java/Maven e JAVA_HOME inexistente. Janela real capturada em TASK-007-exe.png e processos encerrados normalmente. Evidência: TASK-007-portable-smoke.json. O launcher nativo usa processo filho; o validador foi corrigido para acompanhar essa árvore e manter o handle antes do encerramento. As tentativas de validação anteriores foram guardadas, sem atribuir a falha do harness ao jogo. A inspeção de sockets ficou indisponível por restrição do ambiente e não é declarada como aprovada; o staging e o código do launcher foram inspecionados sem inicialização de banco/rede.

Uma primeira construção, anterior à inclusão de jdk.random, foi identificada antes da homologação local e movida para dist/rascunhos-nao-entregar. Ela não é o pacote aprovado nem a candidata final. A versão da TASK-011 será reconstruída com o código consolidado, sem substituir esta versão.

A limpeza inicial da etapa seguinte detectou arquivos somente leitura criados por jpackage em server/target. Atributos foram normalizados somente nas pastas de saída deste build, e o script passou a fazê-lo após empacotar; o JDK instalado não é alterado. A falha e a repetição do clean estão registradas na evidência da TASK-008. Isso não alterou o código ou os hashes do pacote aprovado.

## Limitações e roteiro manual

HOMOLOGACAO HUMANA PENDENTE: extrair tudo em outro Windows x64 sem Java/Maven, abrir SpiritGate.exe e conferir WASD, Espaço, E, Q, Shift, H e R. Manter app/ e runtime/ ao lado do EXE. Não há instalador, WiX, assinatura, auto-update, login ou multiplayer. O EXE não é assinado; não é recomendado desativar proteções do Windows.

Arquivos: scripts de build/smoke, teste de entrada, .gitignore para dist/checkpoints, ferramenta local de checkpoints, este relatório/PDF, resumo/log, diff e evidência JSON/PNG. Checkpoints usam snapshots e patches porque esta pasta não é um repositório Git. Nada foi enviado ou publicado.
