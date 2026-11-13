# Launcher gráfico do Spirit Gate

Abra **SpiritGate-Launcher.exe**, na raiz da instalação atualizável. A janela verifica as atualizações e libera **Jogar** quando há uma versão instalada válida. O botão **Verificar atualizações** repete a consulta. A barra animada indica trabalho em andamento, sem estimar porcentagem de download. Se o canal estiver indisponível, a versão instalada pode continuar sendo usada.

Você usa o pacote **Anfitrião** e escolhe **Hospedar via Radmin VPN** na tela do jogo. Seu amigo usa o pacote **Convidado** e escolhe **Entrar em partida**. Atualizem ambos antes de conectar. O executável da cópia de desenvolvimento e os executáveis dentro de `versions` não são a entrada da instalação atualizável.

Pacotes novos em `dist/SpiritGate-Launcher-Grafico-0.200.6`. Extraia o ZIP completo em uma pasta nova. Você pode criar um atalho do `SpiritGate-Launcher.exe` na área de trabalho. Java está incluído; Windows PowerShell e .NET Framework são usados pela interface. `Jogar-SpiritGate.cmd` também abre a mesma janela.

Para comparar câmera e nitidez, a entrega experimental `dist/SpiritGate-Teste-Camera-0.200.7` inclui seletor de zoom 1x/2x/3x. Consulte [o roteiro do teste](TESTE-CAMERA-0.200.7.md). A versão anterior foi preservada para comparação.

O teste mais recente, `dist/SpiritGate-Teste-Zoom-0.200.8`, acrescenta 0,75x e 1,25x e inicia em 1,25x. Consulte [o roteiro dos zooms fracionários](TESTE-ZOOM-0.200.8.md).

Os saves ficam em `data/saves`. Para migrar de outra instalação, feche o jogo, faça backup e copie os saves para essa pasta. O launcher não importa saves pessoais automaticamente. Feche o jogo antes de fechar a janela do launcher ou atualizar.

Esta entrega permanece local, com `feedUrl` vazio: a publicação remota anterior foi bloqueada pela revisão automática. O canal público ainda contém a versão antiga; não o ative nessas instalações antes da publicação da entrega nova. O launcher informa quando o canal não está configurado.

O pacote mantém o jogo recompilado da entrega 0.200.1 e inclui as sprites Reaper offline. Esta interface não altera a integração de sprites ao multiplayer.

Validação: 9 testes do preparador; 32 verificações do atualizador; teste real da janela com migração de instalação 0.78.1 para 0.200.2, save sentinela preservado e fallback para versão válida quando o canal falha. Evidências em `.task-checkpoints/launcher-grafico-0.200.2`.

O EXE inicializa a interface PowerShell sem terminal. A atualização roda em processo separado, mantendo a janela responsiva, e usa a validação SHA-256 e ativação atômica existentes. O botão Jogar chama o motor com `SkipUpdate`, que verifica a instalação e conserva o lock durante o jogo. Uma segunda janela na mesma instalação é recusada. Para recriar pacotes, use `tools/integrar_launcher_radmin.py`; ele compila o EXE de entrada automaticamente no Windows.
