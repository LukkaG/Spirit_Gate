# Spirit Gate — cliente desktop

Abra `rodar-prototipo.cmd` ou `jogar-rpg-offline.cmd`. O launcher compila as fontes atuais quando necessário e abre a entrada RPG. Para executar a partir do repositório, instale um JDK 17 ou superior e configure `JAVA_HOME` ou `javac` no `PATH`. Maven, PostgreSQL e rede não são necessários para o cliente local.

A raça é escolhida na criação: Reaper, Human, Aetheri ou Spirit. O personagem começa sem domínio. Reaper inicia com espada pequena, Human e Aetheri com socos, Spirit com feixe espiritual básico. A iniciação permite escolher um domínio compatível a partir do nível 15. Shunpo exige nível 20 de personagem para todas as raças.

A progressão usa quests e mobs; um mob de nível 1 recompensa 3 XP geral e 1,5 XP de domínio após a escolha. Outros valores e curvas finais permanecem sujeitos a balanceamento. Veja as regras completas em [Raças e domínios](docs/SISTEMA-DE-RACAS-E-DOMINIOS.md).

WASD move; Espaço ataca; F interage; I abre o inventário; J abre o diário; M abre o mapa; F5 salva. Os atalhos e ações disponíveis aparecem na interface. Saves pessoais ficam em `saves/` e não são incluídos na distribuição.

`world-studio.cmd` abre o editor. `testar-mapa-rpg.cmd "pasta-exportada"` valida uma exportação no cliente. `testar-rpg-sprites.cmd` mantém a prévia opcional dos sprites; `--geometrico` usa a representação básica.

Para conferir somente a compilação: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/launch-local.ps1 -BuildOnly`. A saída isolada fica em `server/target/launcher/classes`; fontes, recursos e arquivos compilados são verificados por SHA-256 antes da reutilização.

Para gerar uma distribuição Windows com Java integrado: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/build-rpg-portable.ps1 -Version 1.0.0-rc1`. Esse comando exige um JDK completo com `jar`, `jdeps`, `jlink` e `jpackage`. O ZIP e seus hashes ficam em `dist/`. Trata-se de um candidato local; o serviço de compra de Spirit Coins e o reset comercial ainda não estão integrados.