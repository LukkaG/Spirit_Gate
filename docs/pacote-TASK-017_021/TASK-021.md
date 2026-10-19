# TASK-021 - Consolidação, distribuição e kit de homologação

**Objetivo:** reconstruir release candidate a partir das alterações validadas 017-020, sem reaproveitar JAR anterior, com arte apenas se aprovada por usuário e SHA.

## Escopo

- Executar clean build e suíte Java completa, testes Python, auditor de arquivos, verificações do atlas e do pacote visual. Repetir offline quando possível; documentar qualquer indisponibilidade.
- Gerar versão **0.21.0-rc1** ou outra identificação coerente com o projeto, em nome único, nunca sobrescrevendo a 0.16.0-rc1. Incluir Java 17 integrado e pacotes liberados por whitelist.
- Validar ZIP original diretamente, SHA-256 externo, manifest, inventário, classes do JAR, runtime, PE x64 e controle negativo em cópia descartável. Registrar hash exato produzido em vez de presumir um valor.
- Smoke Windows em pasta com espaços fora do checkout, com PATH sem Java/Maven e verificação da JVM integrada, padrão e modo `--map` se suportado. Não declarar máquina externa testada sem evidência externa.
- Atualizar `LEIA-ME-TESTER`, changelog, notas de limitações, instruções para assets, mapa e câmera, roteiro de homologação humana e modelo de relato de bug.
- Preparar diff/checkpoint por task, PDFs, inventário de entregas e relatório consolidado. Sem push, publicação, instalador, assinatura simulada, banco ou multiplayer.

## Aceite

- Todos os testes disponíveis passando e arte não aprovada fora da distribuição final.
- Smoke real do EXE, quando possível, com pendências manuais claramente identificadas. Parar após a entrega.
