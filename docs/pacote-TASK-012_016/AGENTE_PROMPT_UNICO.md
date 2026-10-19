# SPIRIT GATE - PROMPT UNICO PARA AGENTE LOCAL (TASK-012 A TASK-016)

Execute no checkout **mais recente** recebido do usuario, atualmente 0.11.0-rc1, sem depender de novas mensagens dele. Leia o arquivo `AUDITORIA_INDEPENDENTE_RC.md` e as cinco TASKs deste pacote antes de alterar qualquer fonte.

## Diagnostico inicial confirmado
- Base do upload inclui fontes `domain`/`prototype` que compilam com `javac --release 17` e 202 testes nos relatórios Surefire.
- O ZIP 0.11.0-rc1 possui SHA correto, mas grava nomes internos com `\`; o verificador rodando no Linux recusa-o. Isto **nao e problema de hash**. Corrigir a origem do empacotamento na TASK-012 antes das novas entregas.
- Auditoria de conteúdo de uma copia temporaria normalizada passou; esta copia **nao substitui** o ZIP oficial.
- Smoke independente do dominio Java passou (arbusto, dash, HUD, reinicio).
- Arte gerada de Karakura ainda pode violar grade/transparencia; nao tratar como validada por padrao.

## Autonomia e metodo
1. Implementar **uma tarefa por vez**, na ordem 012, 013, 014, 015, 016. Nao pedir que o usuario escreva testes; sua participacao esta suspensa nesta rodada.
2. Antes de cada tarefa, registrar o baseline exato `mvn clean test`, arquivos potencialmente afetados e risco. Nao chutar assinaturas ou criar classes duplicadas.
3. Fazer alteracoes pequenas com Javadoc em portugues. Preservar regras de movimento, politica de colisao, consumo de vigor, Criostase, Freeze, dano, controle FIFO, replay e pontos de entrada anteriores.
4. Criar testes para cada mudanca. Nao editar testes antigos/baselines para gerar falso sucesso. Executar suite completa apos cada task; registrar contagem verdadeira e logs reais. Registrar falhas intermediarias.
5. Salvar checkpoint/diff de cada etapa e arquivos docs/TASK-012.md ... TASK-016.md, PDFs individuais e um relatorio final consolidado com pass/fail e pendencias. Use o formato de documentacao existente.
6. Se uma task encontrar erro que nao consiga corrigir dentro do seu escopo, **PARAR**. Nao seguir com base vermelha. Nunca publicar, fazer push, instalar servicos ou modificar credenciais/servidor remoto.
7. Recursos graficos so entram na build se validados. A ausencia de sprite 64x64 de personagem aprovada nao impede criar o pipeline e o fallback, mas impede declarar que o visual final foi integrado.
8. Homologacao externa em Windows e aprovacao visual humana ficam para depois; nao marcar como concluidas automaticamente.

## Entrega final
- Um indice de TASK-012 a TASK-016 com commits/checkpoints locais, suites, logs, diffs, PDFs, hashes, capturas e ZIP de release candidato.
- Uma secao obrigatoria: `O QUE FOI REALMENTE EXECUTADO` versus `O QUE AINDA EXIGE O USUARIO`.
- Pare ao concluir a TASK-016; nenhum plano de TASK-017 sem nova aprovacao.

Leia as especificacoes detalhadas do pacote e execute. Nao tratar este prompt como aprovacao para mudar escopo.
