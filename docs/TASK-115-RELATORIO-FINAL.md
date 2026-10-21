# TASK-115 — Relatório final

Implementação concluída em 20/10/2026, disponível para avaliação humana.

- Entrada offline integrada ao catálogo local existente, sem senha ou recuperação indisponível. Lembrar perfil local persiste somente o identificador público.
- Seleção com nome, classe, nível, preview frontal, respiração visual, fallback geométrico, Jogar, Criar, Voltar e Arena histórica preservada.
- Carregamento, vazio, arquivo inválido e backup recuperável apresentados. Criação e carregamento continuam usando os serviços existentes.
- Retirada de personagem por arquivamento reversível, com confirmação textual explícita. Backups não são apagados.
- Navegação por ações de teclado, atalhos e layouts flexíveis. Retorno seguro reutiliza o salvamento e a pausa já existentes no launcher.

Validação final: **384 testes, zero falhas, zero erros, zero ignorados**. Resultado consolidado em [test-summary.json](task-115/test-summary.json). O probe das janelas reais passou pelo fluxo de criação, jogo, salvamento, retorno, reabertura, perfil lembrado, corrompido, backup e fallback. Consulte [testes e capturas](TASK-115-TESTES.md) e [execução e auditoria](TASK-115.md).

Checkpoints locais recuperáveis:

- `.task-checkpoints/TASK-115/before/source.zip`: fonte inicial, inclusive arquivos ainda não rastreados.
- `.task-checkpoints/TASK-115/corujao-ii/before.zip` e `before.json`: estado completo inicial de fonte/assets/docs/tools e hashes de saves.
- `.task-checkpoints/TASK-115/corujao-ii/after.zip` e `after.json`: estado final.
- `isolated.diff` e `preservation.json`: comparação isolada em relação ao estado inicial desta tarefa.

Os quatro arquivos existentes alterados nesta tarefa são `RpgMenu`, `CharacterSaves`, `SpiritGateEntry` e `SpiritFlameButton`. Os novos arquivos são a seleção visual, seus testes, o probe e a documentação/evidências. As mudanças anteriores em launcher, scripts, Studio e login foram preservadas. Não houve mudança de protocolo multiplayer, formato de save, Coop, implementação da arena histórica ou assets originais. Os saves pessoais não foram usados nos testes de interface.

Pendências de avaliação visual:

- O pacote atual tem apenas um frame Idle por direção: a respiração é movimento de apresentação sobre o frame frontal original. Uma sequência Idle desenhada depende de arte futura.
- O sprite base é compartilhado pelas classes; não há arte individualizada por personagem, classe ou equipamento no contrato atual.
- Em 640×480, textos longos podem exigir rolagem horizontal ou tooltip. Revisar em DPI alto e outras resoluções físicas.
- Capturas dos componentes Swing reais foram registradas e inspecionadas. Robot devolveu desktop preto neste ambiente; captura física, foco por teclado e avaliação estética final continuam pendentes de revisão humana.
- A exclusão definitiva não é oferecida: o arquivamento preserva recuperação e backups. A restauração manual está descrita na especificação.

A TASK-115 encerra aqui. TASK-116 não foi iniciada.
