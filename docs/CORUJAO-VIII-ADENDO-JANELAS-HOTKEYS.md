# SPIRIT GATE — CORUJÃO VIII
## ADENDO OBRIGATÓRIO: JANELAS INTEGRADAS, TRANSPARÊNCIA E PRIORIDADE DAS HOTKEYS

**Referência:** TASK-213 implementada; TASKs 214–220 em planejamento/execução.

**Origem:** novas observações do responsável sobre a HUD e o painel "Opções do Spirit Gate".

## 1. OBJETIVO

Refinar todas as interfaces visuais do Spirit Gate para que sejam parte integrante da experiência de MMORPG, abandonando a aparência convencional das janelas Swing.

Também corrigir um comportamento de teclado: atualmente, ao navegar com TAB e deixar um botão selecionado, pressionar SPACE pode ativar esse botão, em vez de executar o ataque básico.

As teclas configuradas para ações de gameplay deverão ter prioridade sobre o foco visual de navegação quando a partida estiver ativa.

Preservar o comportamento funcional existente e a arquitetura Java 17/Swing/Java2D.

---

## 2. REMOVER AS DECORAÇÕES NATIVAS DAS JANELAS

As interfaces do jogo deverão deixar de apresentar:

- Barra de título branca do Windows.
- Ícone padrão do Java, descrito pelo responsável como o "pinguim".
- Bordas nativas brancas.
- Botões convencionais de minimizar/maximizar/fechar nas janelas internas.
- Espaços externos brancos.
- Aparência padrão de JFrame/JDialog.

Esta mudança se aplica prioritariamente a:

1. Inventário.
2. Configurações.
3. Missões.
4. Perfil do personagem.
5. Menus internos da partida.
6. Painéis adicionais de jogo que utilizem janelas Swing convencionais.

Preferir painéis internos sobrepostos ao mundo utilizando o JLayeredPane já presente na TASK-213, quando isso preservar melhor o ciclo de vida e a usabilidade.

Quando um JDialog independente ainda for necessário, avaliar uma janela undecorated com controles próprios, proprietário correto e comportamento modal preservado.

Não aplicar mudanças indiscriminadas nas janelas externas do launcher, World Studio ou ferramentas de desenvolvimento.

### Controles personalizados

Toda janela interna deverá oferecer:

- Botão de fechar próprio.
- Título estilizado.
- Comportamento correto de ESC, quando apropriado.
- Navegação por teclado.
- Foco acessível.
- Feedback visual de hover e seleção.
- Posicionamento correto.
- Limites de arraste, se o painel for móvel.
- Encerramento dos recursos ao fechar a partida.

Não eliminar funcionalidades de acessibilidade simplesmente para remover a decoração nativa.

---

## 3. TRANSPARÊNCIA E IDENTIDADE VISUAL

O responsável exige que as interfaces mantenham fundo translúcido, permitindo perceber o mundo ao fundo.

A transparência não significa tornar textos ou botões invisíveis.

### Estilo obrigatório

- Navy escuro como base.
- Ciano/azul luminoso nas bordas.
- Transparência parcial nos painéis.
- Texto claro com contraste adequado.
- Botões integrados ao tema.
- Contornos refinados.
- Glow discreto.
- Visual de fantasia espiritual.
- Mesmo estilo da tela de login e da TASK-213.

Usar a paleta e os componentes reais de SpiritTheme e GameHudStyle, evitando criar estilos conflitantes.

### Comportamento visual

O mundo deverá permanecer visível por trás das áreas translúcidas, mas sem comprometer a leitura.

O título, a lista de itens, as descrições e os botões deverão permanecer legíveis, inclusive sobre mapas claros.

Avaliar o uso de Java2D AlphaComposite e cores RGBA nos componentes internos.

Não depender de transparência nativa do sistema operacional se a plataforma não oferecer suporte confiável.

Priorizar pintura translúcida dentro da própria janela do jogo.

---

## 4. CORREÇÃO CRÍTICA — HOTKEYS E FOCO DO TAB

### Problema identificado

Atualmente, TAB permite navegar entre os controles da HUD.

Porém, ao deixar uma opção selecionada e pressionar SPACE, o Swing pode ativar o botão que possui foco, em vez de executar o ataque básico.

Isso não é o comportamento desejado.

### Regra funcional obrigatória

**Durante a partida ativa, um atalho de gameplay explícito deverá executar sua ação correspondente, independentemente do botão destacado pelo TAB.**

Exemplo:

1. O jogador pressiona TAB.
2. Um botão de inventário, menu ou habilidade recebe destaque visual.
3. O jogador pressiona SPACE.
4. O personagem executa o ataque básico.
5. O botão destacado NÃO deve ser acionado.

### Comportamento esperado por tecla

| Tecla | Comportamento |
|---|---|
| TAB | Percorrer os controles da HUD |
| SPACE | Executar ataque básico |
| I | Abrir/fechar inventário |
| J | Abrir/fechar missões |
| ESC | Abrir menu ou fechar painel conforme contexto |
| SHIFT | Shunpo ofensivo, quando permitido |
| Q | Shunpo defensivo, quando permitido |
| 1 | Usar a ação selecionada da hotbar, conforme contrato existente |
| WASD | Movimentar o personagem |
| ENTER | Ativar o controle destacado pelo TAB, quando apropriado |

Preservar também F, E, C, P, R, M e demais atalhos existentes.

Não mudar a função das teclas para resolver o conflito.

### Prioridade contextual

Implementar uma política centralizada de entrada:

**Contexto 1 — Partida ativa**

- Hotkeys de gameplay têm prioridade.
- TAB pode navegar entre controles.
- SPACE continua sendo ataque básico.
- O destaque por TAB não altera a ação de SPACE.
- ENTER pode ativar o botão destacado.
- Um clique no botão continua executando sua função.
- Atalhos não devem gerar ações duplicadas.

**Contexto 2 — Formulário ou modal com edição de texto**

- Digitação normal tem prioridade.
- SPACE insere espaço no campo.
- ENTER confirma quando apropriado.
- TAB navega entre campos.
- Atalhos de ataque e movimento não devem disparar enquanto o jogador digita.

**Contexto 3 — Configurações e remapeamento de controles**

- Captura de tecla deve funcionar sem executar gameplay.
- SPACE deve poder ser configurado sem atacar.
- A ação de Aplicar deve respeitar confirmação explícita.
- Não executar atalhos escondidos durante a captura.

**Contexto 4 — Desconectado, reconectando ou personagem indisponível**

- Não enviar ataques inválidos.
- Não reter teclas pressionadas.
- Não repetir comandos após a recuperação.
- Manter controles de janela disponíveis.

### Implementação técnica

Auditar:

- RpgNetworkClientLauncher.
- NetworkGameHud.
- NetworkHotbar.
- RpgControls.
- InputMap.
- ActionMap.
- FocusTraversalPolicy.
- Key Bindings.
- Eventuais KeyEventDispatcher existentes.

Investigar o consumo de SPACE pelos JButton e a precedência de key bindings Swing.

Não resolver o problema usando requestFocusInWindow repetidamente a cada frame.

Não desabilitar completamente TAB.

Não implementar um KeyEventDispatcher global sem controle de escopo, ciclo de vida e remoção do listener.

Criar uma solução pequena, centralizada e testável para roteamento das hotkeys, respeitando o contexto ativo e as regras existentes do jogo.

---

## 5. REFORMULAR ESPECIFICAMENTE AS CONFIGURAÇÕES

A captura fornecida mostra um painel escuro com conteúdo relativamente correto, porém ainda preso às decorações tradicionais do sistema operacional.

Transformá-lo em uma interface integrada ao Spirit Gate.

### Elementos

- Cabeçalho personalizado.
- Título "Configurações".
- Botão X estilizado.
- Seções de áudio, acessibilidade, vídeo e controles.
- Slider de volume com estilo navy/ciano.
- Checkbox personalizado.
- Resolução de janela.
- Botões Aplicar e Cancelar.
- Rolagem personalizada, quando necessária.
- Fundo translúcido.

Não inventar controles de música/efeitos separados se o motor só oferecer volume geral.

Se houver suporte real a volumes separados, reutilizá-lo.

Manter semântica de Aplicar/Cancelar e persistência existente.

A janela deve preservar a configuração anterior quando o jogador cancelar.

### Resoluções

Testar ao menos:

- 1366×768.
- 1000×700.
- 640×480.

Nenhum botão essencial pode ficar inacessível.

---

## 6. INTEGRAÇÃO COM AS TASKS

Este adendo deve ser incorporado ao ciclo atual sem descartar o trabalho já validado.

### TASK-213 — Refinamento da HUD
Corrigir prioridade das hotkeys, foco e apresentação de botões internos quando o problema se originar da HUD.

### TASK-214 — Inventário
Aplicar o novo padrão de painel translúcido e sem decoração nativa.

### TASK-215 — Missões
Substituir a janela convencional por painel integrado e sem barra branca.

### TASK-216 — Configurações
Reformular integralmente a apresentação visual de acordo com a referência recebida.

### TASK-217 — Padronização
Consolidar componentes compartilhados, transparência, títulos, botões, foco e decoração.

### TASK-220 — Polimento
Validar navegação por TAB, atalhos de gameplay, mouse, hover, resize, abertura/fechamento e acessibilidade.

Não adiar a correção de SPACE até a TASK-220 se ela já estiver interferindo no combate.

---

## 7. TESTES OBRIGATÓRIOS

### Teclado

Testar:

1. TAB seleciona Inventário, SPACE ataca e não abre inventário.
2. TAB seleciona Configurações, SPACE ataca e não abre configurações.
3. TAB seleciona outra habilidade, SPACE usa ataque básico.
4. ENTER ativa o botão destacado pelo TAB.
5. I abre inventário independentemente da seleção anterior.
6. WASD funciona após fechar painéis.
7. Atalho pressionado e solto não fica preso.
8. Texto digitado em campos não dispara ataque.
9. Rebinding captura teclas sem executar habilidades.
10. Dois clientes continuam observando os eventos de combate confirmados pelo servidor.

### Interfaces

Testar:

- Ausência de decoração branca nas janelas internas.
- Ausência do ícone padrão de Java nessas janelas.
- Fundo navy translúcido real.
- Legibilidade em mapas claros e escuros.
- Abrir e fechar painéis repetidamente.
- Sem vazamento de listeners, timers ou janelas.
- Sem perda de foco após fechar inventário/configurações.
- Compatibilidade com diferentes resoluções.
- Nenhuma regressão no launcher, offline e Coop.

Capturar imagens reais das novas interfaces e estados de foco.

---

## 8. RESTRIÇÕES

Não alterar:

- Protocolo V6.
- Movimento autoritativo.
- Cálculo de dano.
- Cooldowns.
- Regras de habilidades.
- Inventário no servidor.
- Progressão.
- Missões.
- Colisão.
- Saves.
- World Studio.
- Distribuição anterior.

Não criar novas dependências sem autorização.

Não executar commit, push, release ou publicação automática.

---

## 9. ENTREGÁVEIS

Registrar as alterações na TASK responsável e produzir evidências específicas para:

- Decoração removida.
- Transparência navy.
- Foco pelo TAB.
- Prioridade de SPACE.
- Hotkeys com foco em outros botões.
- Comportamento de entrada de texto.
- Regressões multiplayer.

Produzir um relatório com arquivos alterados, testes aprovados, limitações e capturas reais.

## RESULTADO ESPERADO

As interfaces deverão parecer componentes nativos do Spirit Gate, e não janelas padrão do Windows.

Ao mesmo tempo, TAB continuará permitindo navegar visualmente, mas SPACE sempre executará o ataque básico durante a partida ativa, sem acionar acidentalmente o botão atualmente destacado.

**Priorizar a correção de teclado e integrar a estilização ao desenvolvimento das TASKs 214–217, preservando a TASK-213 já homologada tecnicamente.**

## Adendo obrigatório de janelas e teclado - 31/10/2026

Implementado no V6: cinco painéis internos translúcidos, roteamento contextual de teclado e configurações com Aplicar/Cancelar. Base das TASKs 214-217 adiantada por determinação do novo adendo; seus escopos completos continuam pendentes. TASK-216 tem implementação parcial de apresentação e controles. Regressão: 695 testes Java (670 aprovados, 25 abortos gráficos), complementada por 33 execuções GUI aprovadas, sem falhas. Teclado físico e revisão visual humana permanecem pendentes. Ver [relatório do adendo](ADENDO-JANELAS-HOTKEYS-RELATORIO.md) e [PDF](Spirit_Gate_Adendo_Janelas_Hotkeys.pdf).
