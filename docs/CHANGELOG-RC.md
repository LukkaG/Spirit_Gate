# Spirit Gate 0.11.0-rc1

18/10/2026. CANDIDATO A HOMOLOGACAO LOCAL. Não é release pública ou homologação em outra máquina.

## Conteúdo desde a TASK-006.1

- TASK-007: executável Windows x64 com runtime Java integrado, build reproduzível por script, inventário e hashes.
- TASK-008: dois arbustos e um poste SOFT_OBSTACLE no demo usado pela arena; mapa histórico e replay preservados.
- TASK-009: estados/contexto de Q e Shift, vigor insuficiente, recarga, atividade, espera e bloqueio sólido/borda medido pela física.
- TASK-010: treze testes de sequências e limites; tick real acessível internamente a testes, sem mudar agendador. Corrigido texto que arredondava recurso insuficiente para saldo igual ao custo.
- TASK-011: build novo consolidado, kit, auditoria de arquivos e conferência do EXE fora dos fontes.

Base: 170 testes. Consolidado: 202 testes, zero falhas/erros/ignorados. As duas execuções da TASK-010, uma offline, concordaram por classe. O build limpo final da RC repetiu os 202 testes. Os 26 arquivos de teste originais e baselines estão intactos.

## Pacote e reprodução

Arquivo: dist/SpiritGate-0.11.0-rc1-20261018T220040Z-b2e659ed-portable.zip.

Tamanho: 29.359.239 bytes (aproximadamente 28 MiB).

SHA-256: 7484f90f5de5ca0475c8d73a918a8868b20a089d5a3fb3a12dd2e40f5fc19d01.

Gerado em 2026-10-18T22:00 UTC. Java/jpackage Temurin 17.0.20.1+1; Maven 3.10.0; Windows 10 amd64. Versão PE numérica: 0.11.0; identificação completa no manifest/VERSION/README: 0.11.0-rc1. Identificadores de build novos evitam sobrescrever versões anteriores.

Na pasta Projeto-K, executar: tools\build-windows-portable.cmd -Version 0.11.0-rc1 -TesterKitDirectory docs\kit-homologacao. O padrão executa mvn clean test antes de gerar o pacote. O JDK 17 pode ser selecionado com -JavaHome. Os hashes de builds separados podem variar com metadados/horários; cada distribuição recebe o próprio sidecar SHA-256.

Auditoria: python -B tools/auditar_rc.py --zip dist/SpiritGate-0.11.0-rc1-20261018T220040Z-b2e659ed-portable.zip --report docs/TASK-011-auditoria.json. Smoke do EXE: powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/test-windows-portable.ps1 -ZipPath dist/SpiritGate-0.11.0-rc1-20261018T220040Z-b2e659ed-portable.zip -EvidencePath docs/TASK-011-portable-smoke.json.

## Inventário e validação

O ZIP contém uma pasta SpiritGate com SpiritGate.exe, app/SpiritGate.cfg, app/spirit-gate-arena.jar, runtime/, VERSION.txt, LEIA-ME-TESTER.txt, INVENTARIO-SHA256.txt, manifest.json e três documentos do kit. O manifest cobre 148 arquivos; exclui a si próprio por circularidade. O inventário textual exclui a si e o manifest; o SHA externo cobre o ZIP inteiro. Manifest e SHA também têm cópias externas junto ao ZIP.

O JAR contém 122 classes de produção domain/prototype, comparadas byte a byte com target/classes atual. Sem classes de teste, servidor legado, DatabaseManager, dependências JDBC/Kryonet ou configurações locais. O runtime inclui java.base/java.desktop/jdk.random e suas dependências transitivas. A análise de padrões é limitada e não prova ausência absoluta de qualquer dado sensível; nenhum padrão concreto proibido foi encontrado.

EXECUCAO LOCAL CONFERIDA: EXE extraído em pasta com espaços fora de Projeto-K, PATH sem Java/Maven e JAVA_HOME inexistente. jvm.dll carregada do runtime integrado, confirmada pelos módulos do processo. Janela e processo launcher encerraram com código 0. Captura real: TASK-011-exe.png; captura dos estados do HUD recompilado: TASK-011-hud.png. Evidências JSON em TASK-011-portable-smoke.json e TASK-011-auditoria.json.

## Pendências

HOMOLOGACAO HUMANA PENDENTE: outro Windows x64 sem Java/Maven e avaliação jogando por tarefa, conforme kit-homologacao/ROTEIRO-HOMOLOGACAO.md. Inspeção de sockets por CIM ficou indisponível neste ambiente; código e pacote não iniciam servidor/banco, mas não se alega auditoria dinâmica de rede concluída.

Limitações conhecidas: EXE sem assinatura; espera de saída pode persistir enquanto toda a trajetória estiver ocupada; mapa histórico e demo são variantes deliberadas. Relatar problemas usando o modelo do kit. Nenhuma TASK-012 foi iniciada, nem houve push ou publicação.
