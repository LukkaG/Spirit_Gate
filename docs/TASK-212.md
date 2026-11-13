# TASK-212 — integração final local

31/10/2026. Autorização do usuário: todas as TASKs do prompt, com relatório PDF.
Fontes no branch beco, HEAD 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3.
Sem commit, troca de branch ou publicação.

Integração: criação/raça (201), catálogo (202), feedback de combate (203), HUD
(204), alvo por inimigo (205), loot real (206), inventário V6 (207), progressão
(208), hotbar/domínio (209), diário/jornada de missão (210), mundo nativo (211).

A fábrica LauncherSession.start conserva o baseline histórico; startExpanded
é usada pelo novo fluxo Radmin e pela entrada de servidor de desenvolvimento.
Conteúdo publicado existente continua preservado. O mundo novo exige cliente
capaz de sincronizar conteúdo; V1–V3 não são admitidos nessa revisão, mantendo
a regra de segurança histórica. V1–V5 continuam funcionando nos seus caminhos.

Foram gerados dois pacotes privados novos em
`dist/SpiritGate-CorujaoVII-0.212.0-private`, Anfitriao e Convidado. Contêm JAR
recompilado, runtime e assets anteriores verificados, além do launcher/atualizador
atuais. Sem feed remoto; sem push/release. O conteúdo novo vive no JAR e nos
documentos nativos gerados para a sessão, sem modificar PNGs ou saves.

Usar o SpiritGate-Launcher.exe da raiz do pacote extraído. Anfitrião hospeda;
convidado conecta ao endereço Radmin do anfitrião. I abre inventário; 1 usa ação
selecionada; SPACE ataca; SHIFT/Q Shunpo (nível 20); F/E/C missão; P portal;
R retorna após morte; F2 abre diagnóstico. Execute em duas pastas distintas.
Progresso é efêmero e reconexão cria outra sessão, conforme contrato anterior.

Ferramentas novas: corujao7_verify.py, CorujaoSevenPackageProbe.java,
smoke_corujao7_package.py e gerar_relatorio_corujao7.py. Evidências nos checkpoints
individuais e TASK-212; não são distribuídas como arquivos do jogador.
