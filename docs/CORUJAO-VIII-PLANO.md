# SPIRIT GATE — CORUJÃO VIII

> Continuação autorizada explicitamente pelo responsável: avançar nas TASKs
> restantes. TASKs 214–222 executadas e validadas localmente em 31/10/2026.
> Estado atual: [progresso](CORUJAO-VIII-PROGRESSO.md),
> [relatório final](CORUJAO-VIII-RELATORIO-FINAL.md) e
> [PDF completo](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf).
> A etapa inicial abaixo fica preservada como histórico. Canais separados de
> áudio são a ressalva de escopo da TASK-216; homologação física/visual pendente.

> Adendo visual posterior obrigatório: [CORUJAO-VIII-ADENDO-HUD.md](CORUJAO-VIII-ADENDO-HUD.md).
> Aplicado ao refinamento imediato da HUD e integrante dos requisitos das
> TASKs 214 e 217: três blocos, hotbar ampliada com reservas sem ações fictícias,
> centro limpo, barras alinhadas e utilitários consistentes.
## REFORMULAÇÃO DA HUD E DAS INTERFACES DO JOGO
### TASK-213 até TASK-222

**Projeto:** Spirit Gate / Projeto-K  
**Stack:** Java 17, Swing/Java2D, cliente + servidor autoritativo  
**Base funcional:** Corujão VII homologado e funcional  
**Objetivo principal:** reformular a experiência visual da interface do jogador, mantendo a lógica atual do jogo e usando como referência os wireframes enviados pelo responsável.

---

# 1. MISSÃO

O Spirit Gate já possui sistemas jogáveis e homologados.

Agora o foco é **reformular a interface do usuário** para torná-la mais próxima de um MMORPG consistente, legível e estilizado.

O responsável enviou **quatro exemplos visuais** que devem ser usados como **wireframes base**:

## Referência A — Inventário
Características:
- Janela retangular grande.
- Título “Inventário”.
- Grid de slots.
- Categorias na parte inferior.
- Pequenos indicadores/ações.

## Referência B — HUD principal
Características:
- Minimap no canto superior direito.
- Barra de HP e barra de energia na parte inferior central.
- Logotipo/nome do jogo no centro inferior.
- Skill bar na base.
- Botões laterais de personagem, livro/menu e configurações.

## Referência C — Lista de missões
Características:
- Painel vertical simples.
- Título.
- Missões listadas em ordem.
- Fundo transparente / sobreposição leve.

## Referência D — Configurações
Características:
- Painel central simples.
- Keybinds.
- Configurações gerais.
- Música do jogo.
- Sons do jogo.
- Sliders.
- Fundo transparente / sobreposição leve.

**Essas referências NÃO devem ser copiadas literalmente como arte final bruta.**  
Elas representam a **estrutura e a posição dos elementos**.

A arte final deve seguir a **identidade visual da tela de login já aprovada no Spirit Gate**.

---

# 2. ESTILO VISUAL OBRIGATÓRIO

Usar a mesma linguagem visual da tela de login do projeto.

## Direção estética
- Tema escuro.
- Azul profundo / navy.
- Ciano neon / azul energético.
- Bordas brilhantes.
- Painéis translúcidos.
- Destaques em glow suave.
- Tipografia clara e legível.
- Aparência espiritual/fantasia sombria.
- Interface elegante e limpa.

## Linguagem visual esperada
- Painéis com fundo azul-escuro semi-transparente.
- Contornos ciano/azul luminoso.
- Botões com glow suave.
- Realces ao passar mouse / foco.
- Barras de status em estilo refinado, não blocos crus.
- Hotbar e slots com aparência consistente com o login.
- Elementos “mágicos/espirituais”, mas sem poluir a tela.

## Não fazer
- Não manter o visual cinza cru dos wireframes.
- Não usar cores aleatórias desconectadas da identidade do login.
- Não transformar a UI em algo cartunesco genérico.
- Não quebrar a legibilidade por excesso de brilho.
- Não refazer a lógica de gameplay sem necessidade.

---

# 3. REGRAS GERAIS

## Preservar obrigatoriamente
- Multiplayer homologado.
- HUD e controles existentes enquanto a substituição não estiver pronta.
- Inventário e lógica de itens.
- Sistema de missões.
- Configurações já existentes.
- Minimap atual, se houver base funcional.
- Hotbar, habilidades e cooldowns.
- Protocolo multiplayer vigente.
- Saves pessoais.
- Sistemas de combate e progressão.
- Launcher e atualizador.
- World Studio e ferramentas já prontas.

## Não autorizado
- Reescrever o motor do jogo.
- Quebrar compatibilidade do multiplayer.
- Alterar lógica de inventário para além do necessário à UI.
- Alterar balanceamento de combate como efeito colateral.
- Instalar dependências novas sem autorização.
- Fazer push/publicação automática.
- Remover janelas existentes antes de existir substituição funcional.

---

# 4. DIRETRIZ DE IMPLEMENTAÇÃO

Primeiro auditar:
- classes da HUD atual;
- inventário;
- painel de missões;
- configurações;
- minimapa;
- hotbar;
- renderização Swing/Java2D;
- sistema de layout e input.

Identificar:
- o que já existe;
- o que precisa ser reformulado;
- o que precisa ser encapsulado;
- quais componentes podem ser reaproveitados.

## Regra central
**A reformulação será primeiramente visual e estrutural.**  
Só alterar lógica quando isso for indispensável para viabilizar a nova interface.

---

# 5. TASK-213 — HUD PRINCIPAL

## Objetivo
Refazer a HUD principal do jogo tomando como base a Referência B.

## Implementar
- Minimap no canto superior direito.
- Barra de HP.
- Barra de energia.
- Área central inferior com nome/logo do jogo ou identidade do jogador, conforme viável.
- Skill bar na parte inferior.
- Botão/perfil do personagem no lado esquerdo inferior.
- Botões rápidos de menu/livro/configurações no lado direito inferior.
- Layout coerente com a tela de login.

## Direção visual
- Fundo escuro translúcido.
- Contornos ciano neon.
- Barras polidas com preenchimento animável.
- Slots de skill com brilho leve e destaque de seleção.
- Ícones com borda consistente.

## Critérios de pronto
- HUD nova substitui ou convive temporariamente com a antiga sem quebrar o jogo.
- HP e energia exibem valores corretos.
- Hotbar continua funcional.
- Minimap fica posicionado corretamente.
- Botões são clicáveis e legíveis.

---

# 6. TASK-214 — INVENTÁRIO

## Objetivo
Reformular o inventário com base na Referência A.

## Implementar
- Janela de inventário com grid visual.
- Slots organizados.
- Categorias na parte inferior:
  - Armadura
  - Armas
  - Itens de drop
  - Todos
  - Organizar inventário
- Título “Inventário”.
- Painel com estilo da tela de login.
- Melhor visualização de itens, quantidades e seleção.
- Ações existentes (equipar, consumir, etc.) mantidas ou reorganizadas.

## Direção visual
- Painel escuro translúcido.
- Barra superior azul/ciano.
- Slots com moldura iluminada.
- Categorias com estados ativo/inativo.
- Feedback visual ao selecionar item.

## Critérios de pronto
- Inventário funcional.
- Itens continuam corretos.
- Categorias visíveis.
- Operações existentes não quebradas.
- Janela visualmente alinhada ao Spirit Gate.

---

# 7. TASK-215 — PAINEL DE MISSÕES

## Objetivo
Criar ou reformular o painel/lista de missões tomando como base a Referência C.

## Implementar
- Janela/painel vertical.
- Título de missões.
- Lista de missões ativas.
- Destaque para missão selecionada.
- Suporte a descrição resumida ou detalhes, quando já houver dados disponíveis.
- Fundo transparente ou semitransparente.
- Possibilidade de abrir/fechar painel.

## Direção visual
- Painel leve, flutuante.
- Visual integrado à fantasia sombria do jogo.
- Sem poluir a tela.
- Elemento lateral opcional decorativo (ícone/ornamento) compatível com o estilo espiritual.

## Critérios de pronto
- Missões aparecem corretamente.
- Seleção/abertura não quebra fluxo do jogo.
- Interface clara e estilizada.
- Sem perder dados atuais de progresso.

---

# 8. TASK-216 — CONFIGURAÇÕES

## Objetivo
Reformular o painel de configurações com base na Referência D.

## Implementar
- Painel de configurações central.
- Seção de keybinds.
- Configurações gerais.
- Controle de música.
- Controle de efeitos sonoros.
- Sliders visuais.
- Fundo transparente/semitransparente.
- Fechar/voltar.
- Estados persistentes, se a base atual já suportar.

## Direção visual
- Painel elegante, escuro, translúcido.
- Sliders com destaque em azul/ciano.
- Labels claros.
- Layout organizado em blocos.

## Critérios de pronto
- Música e som refletem estados corretos.
- Keybinds e opções acessíveis.
- Layout consistente com o login.
- Sem quebra do fluxo do jogo.

---

# 9. TASK-217 — INTEGRAÇÃO VISUAL GERAL

## Objetivo
Garantir consistência entre HUD, inventário, painel de missões e configurações.

## Implementar
- Biblioteca visual compartilhada ou componentes comuns, quando viável.
- Padronização de:
  - bordas;
  - painéis;
  - cores;
  - botões;
  - títulos;
  - slots;
  - espaçamentos;
  - transparências.

## Critérios de pronto
- Todas as interfaces parecem pertencer ao mesmo jogo.
- A tela de login e a HUD compartilham a mesma identidade.
- Sem divergências gritantes de estilo.

---

# 10. TASK-218 — FEEDBACK DE COMBATE

## Objetivo
Lapidar elementos visuais ligados ao combate para combinar com a nova HUD.

## Implementar
- Melhor exibição de HP do jogador.
- Melhor exibição de energia.
- Cooldowns visuais na hotbar.
- Destaque de skill ativa.
- Melhorias em dano flutuante e feedback de hit, se viável.
- Melhor feedback de alvo/seleção, quando disponível.

## Critérios de pronto
- Informações de combate mais claras.
- Nada quebra no multiplayer.
- Feedback visual coerente com o novo estilo.

---

# 11. TASK-219 — MINIMAP E UTILITÁRIOS

## Objetivo
Melhorar a experiência do minimapa e dos botões rápidos.

## Implementar
- Refinar container do minimapa.
- Melhor enquadramento.
- Botões de personagem/menu/configurações.
- Estados hover/foco/click.
- Navegação mais clara entre HUD e janelas.

## Critérios de pronto
- Minimap e atalhos funcionam.
- UI mais intuitiva.
- Ícones legíveis e consistentes.

---

# 12. TASK-220 — POLIMENTO DE UX

## Objetivo
Adicionar refinamento à experiência visual e de uso.

## Implementar
- Hover states.
- Focus states.
- Feedback ao clicar.
- Abertura/fechamento consistente de painéis.
- Tratamento de sobreposição entre janelas.
- Melhor legibilidade.
- Ajustes finos de espaçamento e alinhamento.

## Critérios de pronto
- Interface mais fluida e agradável.
- Menos aparência de protótipo.
- Sem atrapalhar a jogabilidade.

---

# 13. TASK-221 — TESTES E REGRESSÕES

## Objetivo
Garantir que a reformulação visual não quebre sistemas existentes.

## Testar
- Multiplayer com dois clientes.
- HUD durante movimentação.
- Inventário.
- Missões.
- Configurações.
- Hotbar.
- Combate.
- Minimap.
- Abrir/fechar janelas.
- Regressão offline, quando aplicável.

## Critérios de pronto
- Sem regressões críticas.
- Interface estável.
- Logs e relatórios produzidos.

---

# 14. TASK-222 — CONSOLIDAÇÃO DO PACOTE

## Objetivo
Fechar o Corujão VIII com uma build privada estável.

## Entregas
- Relatório final.
- Pacote privado atualizado.
- Evidências visuais.
- Lista de pendências.
- Próximas recomendações.

Não publicar automaticamente no canal oficial.

---

# 15. TESTES E EVIDÊNCIAS

Cada TASK deve gerar:
- `TASK-XXX.md`
- `TASK-XXX-TESTES.md`
- `TASK-XXX-RELATORIO-FINAL.md`

Manter:
- `CORUJAO-VIII-PLANO.md`
- `CORUJAO-VIII-PROGRESSO.md`
- `CORUJAO-VIII-PENDENCIAS.md`
- `CORUJAO-VIII-RELATORIO-FINAL.md`

Capturar imagens das interfaces resultantes.

---

# 16. PRIMEIRA AÇÃO

Iniciar somente a **TASK-213**.

## Antes de implementar
Auditar:
- HUD atual;
- minimap;
- hotbar;
- barras de HP/energia;
- componentes de botões e janelas;
- formas de renderização existentes.

Depois apresentar:
1. arquitetura atual;
2. classes afetadas;
3. estratégia de reformulação;
4. riscos;
5. testes planejados.

**Não iniciar automaticamente a TASK-214 antes de concluir e validar a TASK-213.**

**MISSÃO FINAL:** reformular a HUD e as interfaces do Spirit Gate seguindo os wireframes enviados pelo responsável, mas convertendo-os para a identidade visual dark/azul neon da tela de login, preservando toda a jogabilidade já homologada.


## Adendo obrigatório de janelas e teclado - 31/10/2026

Implementado no V6: cinco painéis internos translúcidos, roteamento contextual de teclado e configurações com Aplicar/Cancelar. Base das TASKs 214-217 adiantada por determinação do novo adendo; seus escopos completos continuam pendentes. TASK-216 tem implementação parcial de apresentação e controles. Regressão: 695 testes Java (670 aprovados, 25 abortos gráficos), complementada por 33 execuções GUI aprovadas, sem falhas. Teclado físico e revisão visual humana permanecem pendentes. Ver [relatório do adendo](ADENDO-JANELAS-HOTKEYS-RELATORIO.md) e [PDF](Spirit_Gate_Adendo_Janelas_Hotkeys.pdf).
