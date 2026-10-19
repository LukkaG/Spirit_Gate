# PROMPT COMPLETO - SPIRIT GATE TASK-017 A TASK-021

---

## ARQUIVO: AGENTE-PROMPT-UNICO.md

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


---

## ARQUIVO: CONTRATO-COMUM.md

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


---

## ARQUIVO: TASK-017.md

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


---

## ARQUIVO: TASK-018.md

# TASK-018 - Ferramentas de produção e revisão de sprites 64x64

**Objetivo:** remover gargalos de autoria sem falsificar aprovação das imagens existentes. Produzir fluxo claro Aseprite -> frames -> preview -> validação -> revisão humana -> manifest aprovado.

## Escopo

- Revisar os schemas e validadores de TASK-013 e o atlas da TASK-014. Trabalhar por extensão, não por segundo pipeline paralelo.
- Gerar um **template transparente vazio** rigorosamente alinhado a células 64x64, com 59 posições previstas: IDLE 4 (1 por direção), WALK 16, RUN 16, PUNCH 12, HIT 8, KO frontal 3. Produzir também arquivo de índice (CSV/JSON/Properties conforme ferramentas existentes) que descreva ação, direção, frame, duração e pivot. Não desenhar o personagem.
- Implementar ferramenta CLI de pré-visualização para revisão: grade, índice de frames, checagem de bordas, pivot consistente, contagem, fundo/alfa, tamanho e SHA-256. A saída de revisão deve ficar fora dos assets de produção.
- Se compatível com o código existente, aceitar exportações Aseprite na forma documentada (atlas PNG + metadados) sem quebrar os PNGs isolados existentes. Rejeitar ou sinalizar qualquer incompatibilidade em vez de autocorrigir.
- Gerar relatório por arquivo e prévia para os PNGs existentes; NÃO tentar rescalar em lote os 228 itens não validados. Preservar originais e registrar porque falham.
- Criar testes para template 59, inversão de direção, frame ausente, ordem incoerente, pivots, pixels semitransparentes, sobreposição e mudanças de hash após revisão.

## Aceite

- Template/metadata/preview reproduzíveis, 59 frames descritos exatamente e zero falsa aprovação de arte.
- A saída de produção permanece vazia enquanto o usuário não fornecer uma arte aprovada. Mecanismo de fallback existente continua funcionando.


---

## ARQUIVO: TASK-019.md

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


---

## ARQUIVO: TASK-020.md

# TASK-020 - Câmera, viewport e coordenadas de entrada

**Objetivo:** preparar a arena para mapas maiores preservando colisão, mira por mouse, pixel art e comportamento padrão de 960x640.

## Escopo

- Auditar coordenadas de mundo, tela, origem da câmera, conversão do mouse, escala do Java2D e o desenho do HUD. Apresentar plano antes de editar.
- Introduzir transformação determinística mundo <-> tela: câmera e zoom inteiro onde viável, limites e recorte, com API pura e testes de ida/volta. Resolução padrão deve manter visão atual e coordenadas equivalentes às anteriores.
- Caso o mapa local aceite dimensões superiores à viewport, seguir o jogador sem revelar regiões fora dos limites; caso não aceite, habilitar apenas a transformação/câmera e um cenário sintético nos testes, sem mudar schema de produção de forma incompatível.
- Preservar a mira: Shift + mouse e Q + mouse devem usar coordenadas de mundo corretas após deslocamento da câmera/escala. Revisar bordas, diagonais e janela redimensionada.
- Renderer continua puro, sem modificar mundo; Hitboxes (`H`), trail, HUD, soft/solid obstacle e sprites opcionais devem manter alinhamento visual. Nenhuma mudança em alcance/custo/dano/física.
- Cobrir por testes: viewport padrão, câmera deslocada, zoom, redimensionamento, coordenadas de ataque e colisões, render repetido sem mudança no snapshot e reinício R.

## Aceite

- Baselines antigos verdes; screenshot(s) de cenário sintético opcional e testes determinísticos confirmam alinhamento e mira.
- Nenhuma nova mecânica de corrida, dash ou combate; saída padrão mantém aparência e controle anteriores.


---

## ARQUIVO: TASK-021.md

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

