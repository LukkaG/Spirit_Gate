# Corujão VIII — adendo visual obrigatório para a HUD

Recebido e aplicado ao refinamento imediato da TASK-213 em 31/10/2026.
Estas diretrizes também são obrigatórias nas TASKs 214 e 217.

## Diretrizes recebidas

1. Organizar a faixa inferior em três blocos visualmente separados, pertencentes
   à mesma barra: perfil/inventário à esquerda, hotbar ao centro e livro/missões,
   menu/configurações à direita.
2. Ampliar a composição central mantendo as quatro ações instaladas. Preparar
   espaços laterais para expansão futura, sem inventar ações ou habilidades.
3. Reduzir/remover o elemento decorativo entre HP e energia, priorizando leitura
   e evitando um bloco central pesado.
4. Alinhar as barras à área da hotbar: HP à esquerda e energia à direita,
   mantendo equilíbrio, legibilidade e valores textuais corretos.
5. Uniformizar alinhamento, espaçamento e agrupamento dos utilitários, mantendo
   click, hover, foco e acessibilidade.

Identidade preservada: navy escuro, ciano neon, brilho leve, painéis translúcidos,
bordas refinadas e fantasia espiritual sombria. Restrições: sem mudança de
protocolo, gameplay ou ações existentes; preservar multiplayer, offline e Coop.

## Refinamento imediato entregue

- Três painéis com mesma altura e base, acabamento comum e separação explícita.
- Hotbar central mais ampla: quatro slots funcionais e quatro reservas laterais
  em telas maiores; em modo compacto, duas reservas laterais. Reservas têm
  contorno discreto, descrição acessível, sem foco de teclado nem ação associada.
- Logo/emblema/subtítulo retirados do espaço entre as barras; o centro fica livre.
- HP/energia com intervalo central pequeno e margens alinhadas ao painel de ações.
- Utilitários centralizados nos painéis laterais e com dimensões uniformes por
  resolução. Callbacks, tooltips e nomes acessíveis anteriores preservados.

Arquivos alterados: NetworkGameHud, NetworkCombatHud, NetworkHotbar e a prova
gráfica Task213HudSwingTest. Sem alterações no servidor, protocolo, inventário,
habilidades ou controles de jogo. O modo de HUD anterior continua preservado.

## Validação desta revisão

Cinco execuções aprovadas: Task213HudTest (2), Task213HudSwingTest (1),
Task212PlayableUiTest (1) e Task204CombatHudTest (1). Zero falhas/abortos/skips.
Servidor e dois clientes TCP reais, ataque, movimento observado pelo outro
cliente, abertura/fechamento de painéis, configurações sem gravação ao fechar,
ocultar/reabrir minimapa e slots desabilitados ao desconectar.

Em 1366×768, 1000×700 e 640×480: três blocos sem interseção, barras alinhadas,
botões dentro da janela e reservas não interativas. Capturas inspecionadas.
Esta revisão usa testes dirigidos; a suite completa de 692 testes e complemento
de 30 execuções gráficas são o baseline anterior da TASK-213, não nova execução.

Evidências: `.task-checkpoints/TASK-213-ADENDO/corujao-viii/`, incluindo
verification-gui.json, ui-regression/, baseline e patch incremental.

[HUD refinada 1366×768](TASK-213-ADENDO-HUD-1366.png) ·
[HUD refinada 640×480](TASK-213-ADENDO-HUD-640.png).
Capturas originais da TASK-213 mantidas para comparação.

## Aplicação nas próximas tarefas

**TASK-214:** o inventário deve continuar acessível pelo botão do bloco esquerdo
e por I. Compatibilizar abertura/fechamento e foco com a nova faixa; não ocupar
reservas da hotbar com funções fictícias nem remover operações existentes.

**TASK-217:** consolidar os três blocos, margens, molduras, transparências, ícones
e cores com as demais interfaces. Manter o centro inferior limpo e as reservas
claramente distintas de habilidades instaladas. Este adendo é requisito visual,
não autorização para implementar funções ainda não suportadas.

TASKs 214 e 217 não foram iniciadas neste refinamento. A revisão permanece nos
fontes locais, sem novo pacote ou publicação; avaliação visual humana pendente.
