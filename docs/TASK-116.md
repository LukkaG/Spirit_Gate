# TASK-116 — Seleção de personagens do Spirit Gate

Data: 20/10/2026. Escopo encerrado nesta tarefa, aguardando avaliação humana.

## Objetivo e implementação

Reformular a apresentação de `CharacterSelection`, preservando o catálogo e os eventos da TASK-115. A arte aprovada da entrada é reutilizada, escurecida para preservar o contraste. Seu carregamento opcional acontece no worker existente do catálogo; a composição desenhada em Java2D continua disponível se a imagem faltar.

- Cabeçalho temático, painéis escuros, contornos ciano e botões com estados de foco, hover, pressão e desabilitado.
- Cards com miniatura frontal ou geométrica, nome, classe, nível, seleção e aviso de backup/arquivo inválido. Rolagem vertical e largura limitada ao viewport; nomes longos têm tooltip.
- Prévia com portal, plataforma espiritual, respiração discreta e escala adaptada à altura. O timer inicia e termina com a presença do componente na janela.
- Resumo com nome, classe, nível, HP, vigor e estilo de combate. HP/vigor vêm de `RpgCombatStatus.from(RpgWorld.restore(save).snapshot())`, como no HUD. Vigor usa a mesma conversão para 0–100. Não foram criados atributos novos.
- Jogar recebe destaque principal; Criar personagem fica ao lado. Arquivar, Arena histórica e Voltar ficam abaixo. Setas, Tab/Shift+Tab, Enter, Escape e mnemônicos Swing são preservados.

Os componentes visuais estão isolados em helpers de painel/texto e nas classes `SpiritButton`, `CharacterCard` e `Preview`. São reutilizáveis dentro da apresentação sem introduzir dependências externas.

## Preservação

O catálogo, a criação, o carregamento, a confirmação de arquivamento e a recuperação continuam nos serviços/eventos existentes. A única adição desta tarefa em `RpgMenu` é carregar o fundo no worker. Saves pessoais, formato de persistência, backups, assets originais, Coop, protocolo multiplayer e arena histórica não foram alterados pela implementação.

O workspace já continha alterações da TASK-115 e do Studio. Foram preservadas. Checkpoints locais: `.task-checkpoints/TASK-116/before/` (fonte original da seleção, patch e status inicial) e `after/` (fontes finais, documentação e hashes). Os logs de validação estão no mesmo diretório da tarefa.

## Aceitação técnica

- [x] Tela reformulada, lista com estados distintos e prévia temática.
- [x] Identidade visual alinhada à entrada, com arte existente preservada.
- [x] Hierarquia de ações, foco visível e dados do domínio.
- [x] Layout verificado em 1000×620, 800×600 e 640×480.
- [x] Probe adicional com escala Java2D de 150%.
- [x] Fluxos da TASK-115, recuperação e arquivamento cobertos pelos testes descritos abaixo.
- [x] Suíte completa: 386 testes, sem falhas, erros ou ignorados.
- [x] Relatórios e capturas produzidos.
- [ ] Aprovação visual humana e verificação de teclado físico/desktop em DPI real.

Veja [testes](TASK-116-TESTES.md) e [relatório final](TASK-116-RELATORIO-FINAL.md). TASK-117 não foi iniciada.
