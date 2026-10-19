# TASK-012 - ZIP canonico e auditoria multiplataforma

**Prioridade P0 | Pre-requisito das demais entregas distribuidas**

## Objetivo
Corrigir a incompatibilidade real detectada na auditoria: os nomes internos do ZIP oficial usam `\` e impedem a verificacao direta no Linux. Garantir entradas canonicas com `/` em todas as plataformas e preservar a distribuicao historica.

## Implementacao autorizada
1. Inspecione `tools/build-windows-portable.ps1`, `tools/auditar_rc.py`, `tools/test-windows-portable.ps1` e os sidecars. Localize exatamente o ponto que gravou entradas `SpiritGate\...`.
2. Substitua a geracao por um ZIP com nomes de arquivo **sempre POSIX (`/`)**, inclusive diretórios. Evite usar formatos de path do host diretamente como `arcname`.
3. O validador deve analisar o **nome cru de cada entrada**, p.ex. `ZipInfo.orig_filename` no Python, e recusar `\`, paths absolutos, traversal, streams alternativos, duplicatas case-insensitive, symlinks e nomes reservados Windows. Nao normalize silenciosamente um ZIP inseguro antes de aprova-lo.
4. Mantenha o manifest, inventario, versionamento unico, hashes por arquivo, sidecars e verificacoes de runtime/PE/JAR sem enfraquecer regras. Teste controle negativo adulterando copia nao distribuida.
5. Gere novo pacote versionado sem substituir `0.11.0-rc1`. A versao nova pode ser `0.12.0-task012`, reservada a teste interno.
6. Teste a leitura real desse ZIP por Python `zipfile` e descompactacao no Windows. Acrescente verificacao do nome original executavel tambem em Windows, pois alguns leitores normalizam `\` durante a leitura.

## Qualidade / aceite
- `mvn clean test` passa, sem editar baselines para mascarar erro.
- Auditoria oficial roda no ZIP **original gerado** sem transformacoes intermediarias. Verificadores aceitam tambem em Linux, quando disponivel.
- Hash e manifest verificados; SHA do ZIP anterior preservado historicamente.
- Teste do EXE em Windows fora do repositorio, sem Java/Maven no PATH; homologacao em outro PC segue pendente.
- PDF, diff, logs, JSON de auditoria, comando reproduzivel.

**Gate**: se novo ZIP ainda tiver `\` nos nomes internos, **PARAR** a rodada e corrigir antes de TASK-013.
