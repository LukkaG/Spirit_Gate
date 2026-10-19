# TASK-019 - Integração controlada de pacotes visuais aprovados

**Objetivo:** fazer o runtime aceitar uma futura arte real com garantia de integridade, sem incorporar sprites não aprovados e sem dependência obrigatória de novos arquivos nesta rodada.

## Escopo

- Examinar `ValidatedSpriteAtlas`, o manifest da TASK-013, a lógica de fallback da TASK-014 e o staging da distribuição. NÃO reimplementar loader existente; trabalhar somente nos pontos faltantes.
- Definir pacote visual versionado com IDs estáveis, mapeamento de frames, direção, origem da revisão, SHA-256, pivots, offsets, dimensões e compatibilidade com mapa 32x32 e personagem 64x64.
- Testar carregamento e validação uma única vez na inicialização, mantendo renderização somente leitura. Corrupção, frame ausente, hash trocado, dimensão inválida e asset ausente devem produzir fallback controlado ou erro documentado, nunca tela quebrada ou exceção não tratada.
- Criar fixtures **sintéticas** localizadas exclusivamente nos testes. Validar a inclusão de um pacote aprovado fictício na distribuição de teste e a exclusão de arte pendente no pacote real.
- Exigir revisão do SHA para habilitar arte verdadeira; construir procedimento operacional claro para o dia em que o usuário aprovar o sprite. Não escrever `approved=true` para qualquer PNG existente.

## Aceite

- Testes independentes para um pacote sintético válido e casos inválidos, sem regressão dos 216 testes documentados.
- Release real sem arte aprovada permanece no fallback; nenhuma fonte artística alterada ou descartada.
