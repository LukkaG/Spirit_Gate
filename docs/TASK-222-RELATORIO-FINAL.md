# TASK-222 - Pacote privado

31/10/2026. Autorização explícita para avançar nas tarefas restantes.

Estado: Implementada e validada localmente; aceite visual/físico pendente.

Dois pacotes novos 0.222.0-private, Anfitrião e Convidado, com JAR recompilado e runtime/assets anteriores verificados. Canal comum local, sem feed remoto. Manifest e ZIPs verificados, servidor e dois clientes em três processos com os runtimes distribuídos; interface também aberta a partir do JAR empacotado. Distribuições anteriores preservadas.

Arquivos principais: CorujaoEightUiProbe.java, gerar_relatorio_corujao8.py; ferramentas de build/smoke existentes reutilizadas.

Validação: package-smoke/result.json e package-ui/result.json; hashes no integracao.json. Não equivale a executar o iniciador EXE ou homologar duas máquinas via Radmin.

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Limites: revisão visual humana, teclado físico e Radmin entre máquinas pendentes. Escopo visual moderno na partida V6; offline/Coop preservados.

[Relatório consolidado](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro de uso](CORUJAO-VIII-COMO-TESTAR.md).
