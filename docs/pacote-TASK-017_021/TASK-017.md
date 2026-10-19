# TASK-017 - Auditoria reproduzível da versão 0.16.0-rc1

**Objetivo:** confirmar, no checkout consolidado, as afirmações da rodada 012-016 antes de novas mudanças. Qualquer incompatibilidade real encontrada deve ser corrigida minimamente e coberta por teste.

## Escopo

- Inspecionar fontes, pom.xml, scripts `tools`, testes, manifest, assets e diretório dist. Confirmar que o ZIP 0.16.0-rc1 existe no ambiente do agente e calcular SHA-256; o hash documentado é `9bc425107d23ccfd9a4d36279be4da23c7f016a74c92fe03a9bb1f0d36eb79c1`.
- Executar suíte Java completa (esperados 216 testes na base documentada), controles Python e auditor oficial sobre o ZIP **original, sem alterar separadores**. Comparar inventário, manifest, JAR e `runtime` com a compilação corrente.
- Reexecutar controle negativo em cópia descartável. Conferir segurança de caminhos ZIP, duplicatas, links, traversal e regras de seleção de conteúdo.
- Executar smoke Windows de arena padrão e mapa opcional quando Windows estiver disponível. Se Linux estiver disponível, auditar e extrair o ZIP em Linux; caso contrário registrar `NAO EXECUTADO`, nunca declarar como aprovado.
- Examinar diferença entre resultados esperados e observados; corrigir apenas defeitos reproduzidos, com teste regressivo novo.

## Aceite

- Evidência de hashes, contagem real de testes, auditor original PASS, controle negativo rejeitado, relatório de limites e ausência de regressões.
- Se o ZIP citado não existir, interromper a tarefa e registrar bloqueio. Não fabricar um ZIP com nome idêntico para fingir validação de artefato anterior.
- Sem mudanças de gameplay.
