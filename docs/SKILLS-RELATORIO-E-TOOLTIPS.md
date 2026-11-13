# Relatório ilustrado e ajuda ao passar o mouse

Entrega local de 02/11/2026, branch `beco`, HEAD `6d72c65`.

## Arquivos para leitura

- [Guia ilustrado em PDF](Spirit_Gate_Guia_Ilustrado_das_40_Skills.pdf): 42 páginas, com capa, controles e uma ficha para cada uma das 40 habilidades.
- [Versão HTML](Spirit_Gate_Guia_Ilustrado_das_40_Skills.html): contém as imagens embutidas e pode ser aberta sem rede.
- `skills-report/catalog.xml` e PNGs: catálogo e recortes exportados pelo loader do jogo, sem alterar fontes de arte.

Cada ficha inclui descrição, entrada, custo, recarga, alcance, raio, efeitos e observações sobre dano/cura-base. As imagens são quadros dos atlas atuais; representam componentes visuais, não uma gravação ou hitbox. Raio 5 mostra a fonte dos cortes finais. Valores permanecem provisórios de balanceamento.

## Interface

Os cinco slots e os controles da janela Habilidades agora exibem descrições completas no tooltip, incluindo como conjurar, efeitos, custo, recarga, alcance e disponibilidade. O texto permanece informativo em recarga ou sem energia. Acessibilidade recebe uma descrição em texto simples. Slots sem habilidade não conservam tooltip do domínio anterior.

`RpgSkillDescriptions.java` compartilha comandos e efeitos entre relatório e interface. `NetworkSkillControls.java` monta e escapa o HTML Swing. A descrição de Raio 5 no catálogo explicita a imunidade aprovada. Não houve mudança de combate, protocolo, persistência ou publicação.

## Reprodução

Depois de compilar o projeto com JDK 17 e o Maven/cache já existentes:

```powershell
javac --release 17 -encoding UTF-8 -cp server/target/classes -d .task-checkpoints/SKILLS40/relatorio-ilustrado/export-classes tools/ExportSkillReport.java
java -cp ".task-checkpoints/SKILLS40/relatorio-ilustrado/export-classes;server/target/classes" ExportSkillReport .
python tools/gerar_relatorio_skills.py
```

O HTML é impresso em PDF A4 pelo Edge local em headless, com `--no-pdf-header-footer` e `--print-to-pdf=<caminho do PDF>`, usando perfil isolado no checkpoint. Não foram instaladas dependências. O script Python prepara HTML; a conversão final é feita pelo navegador.

## Verificação

- 15 testes, sem falhas, erros ou skips: `SkillTooltipTest`, `RpgSkillCatalogTest`, `SkillInputRefinementGuiTest`, `NetworkSkillGuiTest`, com Maven offline e `-Djava.awt.headless=false`.
- Os tooltips das 40 skills são montados pelo Swing com largura máxima de 420 px e altura máxima de 420 px nos testes; entidades HTML e estados indisponíveis também são verificados.
- PDF com assinatura válida e 42 páginas; 40 fichas exportadas diretamente do catálogo. Aferição DOM das 42 páginas sem sobreposição de conteúdo com rodapé. Inspeção visual da página de Vento 5.
- Evidências e logs em `.task-checkpoints/SKILLS40/relatorio-ilustrado/`. Capturas antigas da suíte são restauradas após arquivar as novas evidências.

Reabra o treino para carregar os tooltips. Revisão estética humana e teste físico permanecem pendentes. Para reverter a parte de interface, compare os dois arquivos anteriores com `baseline/` no checkpoint e remova apenas os novos auxiliares relacionados, preservando trabalho posterior. O PDF e HTML são documentos independentes. Sem commit, push ou distribuição.
