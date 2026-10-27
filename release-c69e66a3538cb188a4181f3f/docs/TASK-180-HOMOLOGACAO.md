# Homologação local e Radmin

Task180PublishingProbe inicia a mesma fábrica do launcher com RpgSessionWorld, duas conexões TCP V4 e janelas de RpgNetworkClientLauncher. Abre WorldStudio autorizado e PreviewServer em outra porta, com entidades/estado próprios. Não são duas janelas de mockup nem o destino sintético usado como Game Server.

Roteiro automatizado: confirmar baseline nos três participantes; editar obstáculo cinza e meia largura física 22→26 no preview; verificar que Game Server/clientes principais permanecem no baseline; salvar rascunho sem ativar; clicar Salvar; verificar READY/ACTIVE e hashes; conferir geometria/perfil nas duas cenas de produção; mover personagens, acertar inimigo pelo combate autoritativo e atravessar portal; restaurar documento inicial e publicar rollback; conferir ponteiro, física e hashes novamente.

As capturas combinam componentes reais de editor, preview e dois clientes. São evidências de renderização de componentes, não aprovação humana ou fotografia de monitores. Os projetos de prova ficam no checkpoint e usam cópias de sprites originais, sem saves pessoais.

As métricas separam descritor, payload para os clientes, bytes de conteúdo enfileirados, validação, preparo de servidor/clientes, espera READY, ativação, transição, rollback, tick, paint e heap. Bytes enfileirados incluem overhead dos frames de conteúdo, não todos os snapshots/comandos da sessão. Imagens transferidas: zero. Heap é observação sem controle de GC; não demonstra consumo permanente. Sem comparação controlada com uma versão anterior, não se afirma ganho de performance.

Para homologar Radmin fisicamente:

1. Extrair os dois pacotes privados novos em computadores separados e preservar os anteriores.
2. Hospedar pelo Anfitrião na interface Radmin explicitamente selecionada; Convidado entra no mesmo IP/porta.
3. Conferir conteúdo sincronizado, sprites e conectividade. Não alterar firewall/roteador por conta deste roteiro.
4. Autorizar o Studio no host e repetir a edição de obstáculo/perfil, confirmando que nada muda na partida antes de Salvar.
5. Observar transição/erros, hashes, movimento, PvE e portal nos dois computadores.
6. Executar rollback pelo histórico e conferir ambas as telas e a física.
7. Repetir perda de conexão e tentativa com cliente antigo; preservar logs e relatar rejeições.

Esse teste físico permanece pendente. Nenhum comando desta tarefa publicou servidor na internet, release ou feed remoto. Os ZIPs privados têm manifesto/SHA-256 e Java integrado. A distribuição de executáveis continua independente do conteúdo de sessão.
