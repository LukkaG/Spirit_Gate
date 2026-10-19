# TASK-013 - Pipeline de validacao de assets pixel art

**Prioridade P1 | Sem alterar gameplay**

## Objetivo
Criar ferramentas de inventario e validacao para tilesets e sprites recebidos para Spirit Gate antes de integra-los ao renderizador. As imagens de Karakura existentes sao candidatas, **nao** tiles garantidamente validos.

## Implementacao autorizada
1. Inspecione `assets/README.md`, `assets/tilesets/`, `assets/sprites/` e utilitarios de recorte atuais. Nao reescreva nem descarte imagens-fonte.
2. Introduza validacao de PNGs em ferramenta de desenvolvimento e manifest de assets versionavel: dimensoes, grid pretendido (32x32 tiles, 64x64 personagem), numero de frames, quatro direcoes, pivô/offset, camada, transparencia/alfa, origem, status `VALIDADO`/`PENDENTE`/`REPROVADO`.
3. Detecte arquivos fora da grade, arte transparente com fundo acidental ou linhas de corte, sprites cortadas ou sobrepostas e coordenadas inconsistentes. Reporte informacoes verificaveis; nao afirme que uma imagem e pixel-perfect apenas pela extensao ou por ter sido gerada com IA.
4. Para sprites de personagem, prepare contrato formal: IDLE 1x4, WALK 4x4, RUN 4x4, PUNCH 3x4, HIT 2x4, KO 3 front = **59 quadros** ao total, tamanho exato 64x64 por frame; sprites alinhados com escala/pivô consistentes.
5. Gere relatório de integridade, hashes e visualizacao de atlas **somente para os assets validados**. Nao introduza Pillow ou outra dependencia de producao sem justificativa; ferramentas de build podem ter dependencia documentada.
6. Nao importe automaticamente assets gerados na conversa, que podem nao estar no checkout. Nao invente nomes de arquivos. Se faltar sprite validada, registre pendencia e continue usando fallback de geometria.

## Aceite
- Exemplos de PNG valido e casos negativos (dimensoes incorretas, frame ausente, fundo irregular) possuem testes automatizados e resultado explicito.
- Repeticao deterministica gera mesmo inventario de hashes com mesmos inputs.
- Jogo e gameplay existentes nao mudam. Suite Maven permanece verde.
- Entregar relatorio, PDF, diff, tests e checklist para Aseprite.
