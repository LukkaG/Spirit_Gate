# Refinamento V5 — nascimento e dissipação dos efeitos

Revisão local de 02/11/2026, branch `beco`, HEAD `6d72c65`. Escopo solicitado: melhorar a erupção de Fogo 4 e os vórtices de Água/Fogo, com crescimento até o ápice e dissipação. O checkout já continha amplo trabalho local; nenhuma exclusão ou alteração alheia foi descartada.

## Resultado e arquivos

Três fontes novas foram geradas por `image_gen` nativo, com as imagens anteriores como referência visual. A erupção recebeu uma coluna explosiva de fogo; os vórtices receberam espirais detalhadas e estágios próprios de formação e colapso. Cada sequência tem 12 frames, totalizando 36. São formas desenhadas para cada estágio, não apenas cópias ampliadas da mesma imagem.

- `assets/skills/refined-v5/`: três atlas, manifesto opcional e proveniência. `art/` conserva fontes e prompts; modelo/custo não foram retornados pela ferramenta e estão registrados como nulos.
- `tools/prepare_skill_assets_v5.py`: recorte de regiões explícitas, registro de apoios, composição e redução nearest. Uma escala comum por sequência conserva o tamanho relativo de cada fase. O script recusa substituir atlas editados manualmente e publica o manifesto por último.
- `server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java`: carrega a sobreposição V5 depois da V4 e distribui os frames dos vórtices pela vida do efeito, sem loop. O modo de movimento reduzido mantém o ápice estável e conserva entrada/saída. A erupção continua condicionada ao evento de término da fissura.
- `server/src/test/java/com/projetok/multiplayer/NetworkSkillPresentationTest.java`: regressões de duração, repetição de snapshots e cobertura visível dos três efeitos reais.
- `server/src/test/java/com/projetok/multiplayer/SkillLifecycleGuiTest.java`: três cenários com servidor local e dois clientes, capturando início/ápice/fim e verificando expiração, conexões e ausência de falha do servidor.

O vórtice permanece com os 40 ticks existentes. A animação visual da erupção dura 1.200 ms, antes eram 1.280 ms; dano e duração das áreas não foram alterados. A erupção desenha em canvas de 240 × 280; os dois vórtices em 192 × 240, com apoio no chão. O tamanho visual não redefine hitboxes.

## Verificação e evidências

A [prancha, GIFs e capturas](skills-preview-v5/README.md) foram inspecionados. QA dos atlas: 36 frames, zero vazios, zero pixels nas bordas, zero ampliações e nenhum aviso. A reprodução em pasta separada gerou três PNGs e dois metadados idênticos. Os hashes dos **111 PNGs anteriores** e dos **415 originais** da pasta fornecida permaneceram iguais, registrados em `skills-preview-v5/verification.json`.

Comandos executados em `server/`, usando o cache existente e sem instalar dependências:

```powershell
mvn test -o "-Dmaven.repo.local=../.maven-repository" "-Dtest=NetworkSkillPresentationTest,RpgSkillEngineTest,RpgSkillCatalogTest,SkillProtocolTest"
mvn test -o "-Dmaven.repo.local=../.maven-repository" "-Djava.awt.headless=false" "-Dtest=SkillLifecycleGuiTest,NetworkSkillPresentationTest,SkillAimInputRoutingTest,SkillInputRefinementGuiTest,SentryRefinementGuiTest"
```

Ambos usaram `-Dspiritgate.logs.dir` apontando para `refinamento-v5/test-errors` no checkpoint. Resultados: **128 casos aprovados** na rodada focalizada e **31 aprovados** na rodada gráfica, sem falhas, erros ou ignorados. Como a apresentação aparece nas duas rodadas, são **135 casos distintos em oito classes**, não 159 casos únicos. Logs: `.task-checkpoints/SKILLS40/refinamento-v5/focused.log` e `gui.log`. A suíte geral não foi repetida nesta revisão localizada.

O primeiro empacotamento encontrou um acento em comentário de manifesto escrito como ASCII. O comentário foi corrigido antes de publicar o manifesto; o empacotamento e a reprodução posteriores passaram. O diagnóstico de ferramentas foi repetido com saída UTF-8: a rota nativa de imagem está disponível; aviso de outro Python no PATH e ferramentas opcionais ausentes não foram tratados como falhas de arte. Não houve instalação.

## Compatibilidade, limites e reversão

Nenhum executor de combate, regra de dano, recarga, movimento, protocolo, save, banco ou fluxo de publicação foi modificado nesta rodada. O seletor de domínios da prévia permanece. Os testes de combate, protocolo, input e Sentinela passaram. Offline/Coop não receberam mudanças.

Para carregar as classes e os atlas novos, encerre a prévia anterior uma vez e execute `tools/testar-skills.cmd`; depois continue trocando domínios na mesma janela. A revisão humana de estética continua pendente. Testes físicos/Radmin e launcher nativo não foram executados.

A reversão é localizada: comparar a apresentação e seu teste com `.task-checkpoints/SKILLS40/refinamento-v5/baseline/`, retirar somente os hunks V5 e a referência ao overlay. As versões anteriores continuam intactas. Não usar reset global; não houve commit, push ou publicação.
