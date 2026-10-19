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
