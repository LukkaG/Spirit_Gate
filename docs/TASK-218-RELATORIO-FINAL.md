# TASK-218 - Feedback de combate

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Valores de HP e energia continuam exatos; apresentação suavizada sem mudar o modelo. Hotbar exibe recarga, seleção, bloqueio por nível, falta de energia e falta de poção, com descrições acessíveis. Ataque básico continua possível com energia zero. Arcos de ataque e alvo usam ciano; dano flutuante tem contorno escuro e contraste. Redução de movimento mantém os números estáveis. Estados indisponíveis limpam indicadores antigos.

Arquivos principais: NetworkHotbar.java, NetworkCombatHud.java, NetworkCombatPresentation.java, RpgNetworkClientLauncher.java.

Validação: Task214To220UiTest: energia zero, recarga real e poções. GamePanelsIntegrationTest: ataque com foco em outros botões e hits observados pelos dois clientes. Task213HudTest e regressões de combate completas.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).


## Evolução posterior: alvo de combate — 01/11/2026

Seleção PvE por TAB e ataque dirigido implementados na build privada 0.223.0-private. O ataque básico vira sem caminhar; o Shunpo ofensivo aproxima do alvo dentro de 240 unidades ou avança 70 na direção atual quando não há alvo. Nenhuma habilidade atual exige alvo obrigatório, conforme confirmação do responsável. TAB conserva navegação dentro dos painéis. [Regras, testes e pacotes](TARGET-COMBATE.md) · [PDF](Spirit_Gate_Target_Combate_Relatorio.pdf). Esta evolução é posterior à consolidação 0.222.0-private registrada acima.

