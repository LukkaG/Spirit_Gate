# Regras obrigatórias para o agente - TASK-017 a TASK-021

1. Trabalhe **apenas** no checkout mais recente do `Projeto-K`, resultante da TASK-016. Inspecione as classes, configurações e scripts reais antes de programar; não invente nomes de APIs. Registre a referência exata da base (checksum/checkpoint ou commit real, se existir).
2. Execute as tarefas **em ordem**, com checkpoint antes/depois, diffs de código e documentação técnica em português. Não faça push, publicação ou alteração de credenciais.
3. Após cada task, rode `mvn clean test` (ou o comando equivalente comprovado no repositório) e testes Python relevantes. Não apague nem reescreva baselines históricos para fazer uma falha desaparecer. Pare se qualquer regressão ficar sem explicação e resolução comprovada.
4. Preserve as regras atuais: movimento, colisão SOLID/SOFT, dash seletivo, retorno seguro, vigor, Q/Criostase, Freeze, ataques, cooldown, fila FIFO, morte, R e replays. Não acrescente servidor remoto, login, banco ou protocolo de rede neste ciclo.
5. Não declare arte aprovada nem gere um atlas de produção com material rejeitado. A aprovação visual deve estar vinculada ao SHA do arquivo exato. Ferramentas podem gerar **fixtures sintéticas** e **templates vazios**, identificados como tal.
6. Não redimensione, recorte ou substitua fontes originais silenciosamente; forneça preview e relatório para revisão. O pipeline existente da TASK-013 continua sendo a fonte de verdade da validação.
7. Preserve Java 17, orientação por tempo de simulação, snapshots imutáveis, renderização pura e escala nearest-neighbor. Não crie loops/timers concorrentes para animação ou câmera.
8. Evite duplicar arquitetura. Se uma task já estiver satisfeita por código existente, comprove isso por teste/inspeção e produza só as lacunas reais.
9. Para cada task, entregue `docs/TASK-NNN.md`, PDF, diff, resumo de testes, logs e evidências (imagens marcadas como sintéticas quando aplicável). Se não houver Git, mantenha checkpoints por hashes.
10. **Prossiga automaticamente** quando a validação automatizada for verde e o resultado estiver documentado. A homologação humana pode ficar pendente. Interrompa apenas por falhas não resolvidas, mudanças fora do escopo, ausência da base ou dependência obrigatória de aprovação humana.
11. Ao concluir a TASK-021, pare, entregue relatório consolidado e os arquivos reais/ZIP gerados. Não inicie TASK-022 nem outras funcionalidades.
