# Spirit Gate - Relatorio final TASK-012 a TASK-016

18/10/2026, America/Sao_Paulo. Trabalho aplicado em Projeto-K do workspace recebido. Documentos do pacote foram usados como especificacoes das cinco tarefas autorizadas; nenhuma publicacao, trabalho remoto ou expansao de escopo foi executada.

**AUTOMATIZADA APROVADA. HOMOLOGACAO HUMANA PENDENTE.**

## Indice de tarefas e checkpoints

| Etapa | Resultado local | JUnit final | Entregas |
| --- | --- | --- | --- |
| TASK-012 | APROVADA | 202, 0 falhas/erros/ignorados | [Relatorio](TASK-012.md), [PDF](Spirit_Gate_TASK-012.pdf), [diff](TASK-012.diff), [suite](TASK-012-testes.txt) |
| TASK-013 | APROVADA | 202, 0 falhas/erros/ignorados | [Relatorio](TASK-013.md), [PDF](Spirit_Gate_TASK-013.pdf), [diff](TASK-013.diff), [suite](TASK-013-testes.txt) |
| TASK-014 | APROVADA | 210, 0 falhas/erros/ignorados | [Relatorio](TASK-014.md), [PDF](Spirit_Gate_TASK-014.pdf), [diff](TASK-014.diff), [suite](TASK-014-testes.txt) |
| TASK-015 | APROVADA | 216, 0 falhas/erros/ignorados | [Relatorio](TASK-015.md), [PDF](Spirit_Gate_TASK-015.pdf), [diff](TASK-015.diff), [suite](TASK-015-testes.txt) |
| TASK-016 | APROVADA | 216, 0 falhas/erros/ignorados | [Relatorio](TASK-016.md), [PDF](Spirit_Gate_TASK-016.pdf), [diff](TASK-016.diff), [suite](TASK-016-testes.txt) |

Checkout recebido sem repositorio Git: nao foram inventados commits. Para cada numero NNN, .task-checkpoints/TASK-NNN-before.json conserva a base exata e TASK-NNN-after.json lista alteracoes/hashes; docs/TASK-NNN.diff fornece o checkpoint revisavel. Logs TASK-NNN-inicial.log e TASK-NNN-final.log, com repeticoes/falhas preservadas onde aplicavel. PDF individual por etapa e este consolidado. [Hashes de todas as evidencias](TASK-012_016-integridade.json).

## O que foi feito e ajustado

1. TASK-012: trocada a origem do ZIP que usava barras do Windows por nomes POSIX explicitos. Auditor usa orig_filename e smoke verifica FullName antes de substituicoes. Rejeicao de traversal, paths absolutos, streams, duplicatas sem diferenciar caixa, dispositivos Windows e symlinks. Manifest, runtime, PE, JAR, inventarios e hashes preservados. Novo pacote interno 0.12.0-task012 passou diretamente; historico 0.11.0-rc1 nao foi sobrescrito.
2. TASK-013: pipeline PNG sem bibliotecas externas, inventario deterministico, dimensoes/grade/alfa/hashes/origem/camada/pivot/offset/recortes, revisao vinculada ao SHA e contrato de 59 frames de personagem 64x64. Exporta atlas e ponte Properties somente para aprovados. Inspecionados 228 PNGs: todos fora da grade declarada de 32, zero validados. Isso reprova integracao como tiles prontos, nao descarta fontes artisticas. Originais PNG/Aseprite preservados. Checklist Aseprite no relatorio da tarefa.
3. TASK-014: estados/direcoes e frames deterministas por dt na simulacao, publicacao visual atomica/imutavel, sinais reais de ataque e dano, KO por morte real, pausa por Freeze e reset por R. RUN nunca e produzido sem corrida real. Renderer por manifest verificado e SHA, nearest-neighbor, pivot constante e fallback geometrico. Nenhuma arte final integrada.
4. TASK-015: mapa local Properties, aparencia separada do descritor de colisao, limites WORLD_BOUNDARY e obstaculos SOLID/SOFT explicitos. Valida limites, schema, IDs/chaves, contagens, spawn e sobreposicoes. Variante sintetica opcional --map com reinicio deterministico; demo e arena padrao preservados. Corrigida a amostragem visual do spawn apos R, detectada pelo novo teste.
5. TASK-016: revisao, cache Maven local configuravel, auditoria obrigatoria no build, kit atualizado com mapa opcional, reforcos dos validadores, suites normal/offline, novo RC e smoke EXE real padrao/mapa. Todas as mudancas foram locais.

## Evidencias executadas

- Maven inicial da rodada: 202 testes. Final: 216, zero falhas, erros ou ignorados. Sao 14 testes JUnit adicionais, sem editar testes/baselines historicos. Tres replays historicos continuam verdes; nova comparacao da variante com demo por tick tambem passou.
- Suite final normal no build e repeticao offline registradas em [log final](TASK-016-final.log) e [offline](TASK-016-offline.log). [Resumo por classe](TASK-016-testes.txt).
- Ferramentas: 12 testes Python aprovados (7 de assets, 5 de ZIP). [Log](TASK-016-python.log). Inclui fixtures validas/negativas, CRC, contrato/exportacao de frames e pivot.
- Inventario final: [integridade de assets](TASK-016-assets.json) e [repeticao deterministica](TASK-016-determinismo.json). Zero atlas de producao gerados; fixtures sinteticas de testes nao entram no JAR.
- Auditoria oficial no proprio ZIP: [PASS](TASK-016-auditoria.json), seis controles, 150 arquivos, 149 entradas no manifest, 148 no inventario textual, 138 classes JAR comparadas byte a byte com classes atuais. [Controle negativo](TASK-016-auditoria-negativa.json) recusou copia adulterada.
- EXE fora de Projeto-K, em caminho com espacos e PATH sem Java/Maven: [smoke padrao](TASK-016-portable-smoke.json) e [mapa](TASK-016-map-smoke.json). Ambos comprovaram JVM integrada e fechamento com codigo 0, sem fechamento forcado. [Captura real padrao](TASK-016-exe.png), [captura real mapa sintetico](TASK-016-mapa-exe.png).
- Capturas de testes: [personagem sintetico magenta](TASK-014-sintetico.png) e [mapa placeholder](TASK-015-placeholders.png). Identificadas como sinteticas; nao sao aprovacao artistica.

## Pacote local para testes

[ZIP Windows x64 0.16.0-rc1](../dist/SpiritGate-0.16.0-rc1-20261018T230252Z-a6150a66-portable.zip)

Tamanho: 29.386.343 bytes. SHA-256:
9bc425107d23ccfd9a4d36279be4da23c7f016a74c92fe03a9bb1f0d36eb79c1

[Sidecar SHA](../dist/SpiritGate-0.16.0-rc1-20261018T230252Z-a6150a66-portable.zip.sha256), [manifest externo](../dist/SpiritGate-0.16.0-rc1-20261018T230252Z-a6150a66-portable.zip.manifest.json) e [auditoria gerada pelo build](../dist/SpiritGate-0.16.0-rc1-20261018T230252Z-a6150a66-portable.zip.audit.json).

Extrair inteiro e abrir SpiritGate/SpiritGate.exe. Para a variante sintetica, abrir PowerShell na pasta SpiritGate e executar .\SpiritGate.exe --map mapa-demo.properties. Sem argumentos abre o padrao. O pacote contem runtime Java 17 integrado, roteiro por task, changelog, modelo de bug e mapa demonstrativo opcional. Nao requer Java/Maven/Python no PC do tester.

Historico 0.11.0-rc1 preservado com SHA 7484f90f5de5ca0475c8d73a918a8868b20a089d5a3fb3a12dd2e40f5fc19d01. Ele conserva a incompatibilidade original de nomes; nao foi renomeado nem falsamente declarado aprovado no Linux. Candidato intermediario 0.12.0-task012 tem SHA befe84b9d22eb19b7e2b8b2986ccd028da881a34fbee71af3e7d64a16caa6b63.

## Falhas intermediarias e ajustes

Primeiro Maven tentou cache C:/.m2 sem acesso; copiado cache existente para .maven-repository do workspace, sem trocar dependencias. Primeiro build TASK-012 exigiu carregar assembly Compression explicitamente; tentativa vazia sem sidecars nao e release valido. Testes de ZIP precisaram simular nome cru porque o escritor Windows normaliza barras/NUL. TASK-014 teve erro de compilacao em fixture lambda, corrigido. TASK-015 detectou um negativo com spawn valido e diferenca visual no primeiro tick apos R; corrigidos e suite repetida verde. Smoke final direto encontrou ExecutionPolicy restrita; repetido com Bypass somente no processo, sem alterar politica global. Logs de falha que continham resultados foram preservados; erros operacionais sem log constam aqui.

## O QUE FOI REALMENTE EXECUTADO

Implementacao local das cinco tarefas em ordem, baselines antes das etapas, testes completos apos mudancas, validadores e controles negativos, inventarios/determinismo, capturas sinteticas, rebuild atual, auditoria do ZIP original, execucao Windows local de padrao e mapa com JVM integrada e encerramento normal, documentacao/PDFs/diffs/checkpoints/hashes e kit de homologacao. Mecanicas de movimento, colisao seletiva, vigor, Criostase, Freeze, dano, FIFO e replays preservadas pelos testes. Sem banco, rede, autenticacao, migracoes, protocolo, instalacao de servicos, alteracao de credenciais, push ou publicacao.

## O QUE AINDA EXIGE O USUARIO

- Jogar e homologar em outro Windows x64 sem Java/Maven; conferir usabilidade e roteiro do kit. Smoke local nao substitui esse teste independente.
- Revisar fontes no Aseprite, corrigir grades/recortes, produzir personagem 64x64 com 59 frames, aprovar visualmente e registrar SHA. Nenhum visual final de Karakura/personagem foi integrado.
- Auditoria Linux independente nao executada: tentativa de consultar WSL retornou ajuda, sem ambiente Linux utilizavel nesta sessao. Verificacao Python do ZIP original foi executada no Windows, inclusive orig_filename.
- EXE continua sem assinatura e sem multiplayer. Espera de pouso com rota totalmente ocupada permanece comportamento conhecido. Busca de segredos por padroes nao comprova ausencia absoluta; hashes nao autenticam origem.

[Kit/roteiro completo](kit-homologacao-012-016/ROTEIRO-HOMOLOGACAO.md). Rodada encerrada na TASK-016, sem iniciar outra tarefa.
