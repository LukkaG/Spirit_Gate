# Spirit Gate - Rodada autônoma TASK-017 a TASK-021

Você é o agente local de desenvolvimento do Spirit Gate. Estou autorizando a execução sequencial e autônoma das cinco tarefas anexas. Use somente o checkout mais recente resultante da TASK-016. Não reserve exercícios para mim e não aguarde minha presença após uma etapa aprovada automaticamente.

**Leitura obrigatória, nesta ordem:** `REVISAO-TASK-012_016.md`, `CONTRATO-COMUM.md` e `TASK-017.md` a `TASK-021.md`.

## Ponto de partida

O relatório anterior DECLAROU 216 testes JUnit, 12 Python e um ZIP portátil `0.16.0-rc1`. Esses são números a verificar, não resultados que você pode assumir sem reexecutar. O SHA documentado do ZIP é `9bc425107d23ccfd9a4d36279be4da23c7f016a74c92fe03a9bb1f0d36eb79c1`.

## Ordem executiva

1. TASK-017: confirmar repositório, testes, pacote e riscos; interromper se a base estiver incompleta ou com falha não resolvida.
2. TASK-018: gerar ferramentas de autoria/revisão e template vazio para 59 frames 64x64; jamais autovalidar arte.
3. TASK-019: fechar lacunas reais de integração de pacotes visuais aprovados e fallback; usar fixtures sintéticas.
4. TASK-020: trabalhar câmera/viewport, preservando controles e física.
5. TASK-021: consolidar o release candidate e produzir o kit completo de homologação.

## Política obrigatória

- Após cada etapa: testes completos, checkpoint, diff e relatório PDF em português.
- Nunca substituir sucesso real por alteração de asserção ou baseline. Não remover testes antigos.
- Códigos e scripts existentes são fonte de verdade: confira assinaturas reais antes de editar.
- Não acesse ou modifique servidor remoto, PostgreSQL, credenciais ou funções multiplayer.
- Não aprove nenhuma das 228 imagens reprovadas automaticamente nem altere fontes artísticas sem controle de revisão. Preserve fallback até existir arte aprovada por SHA.
- Não altere tempos, custo, dano, colisões, dash, congelamento, FIFO ou replays salvo correção mínima de um bug reproduzido e documentado.
- Se uma tarefa exigir decisão humana, complete o trabalho preparatório seguro, registre essa dependência e prossiga apenas se a próxima tarefa puder ser feita sem ela.
- Ao acabar TASK-021, pare e entregue relatório consolidado, ZIP real produzido, hashes, logs e artefatos individuais. Não começar TASK-022.

**Não afirme homologação em outro Windows, auditoria Linux ou validação de artes se essas ações não ocorreram de fato.**
