# Atualização do launcher — 0.200.1

Entrega recompilada do HEAD 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3, com recursos atuais e 36 PNGs de assets/dev/reaper. Runtime e executável nativo reutilizados do pacote TASK-198 após conferência SHA-256 de todas as entradas; JAR recompilado e substituído. Preparador atualizado para aceitar o pacote Anfitrião Corujão VI.

## Como atualizar

Feche o jogo e abra Jogar-SpiritGate.cmd. O canal configurado deve ser https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/latest.json. Os dois participantes devem atualizar antes de conectar. Saves permanecem em data/saves.

Se atualizacoes.json tem feedUrl vazio, configure o endereço acima, preservando role (convidado para o amigo, anfitriao para quem hospeda). Pacotes antigos sem Jogar-SpiritGate.cmd precisam do pacote inicial do canal; extraia em pasta nova e preserve a instalação anterior.

Após atualizar, anfitrião usa Hospedar via Radmin VPN e amigo usa Entrar em partida, na mesma rede Radmin, com IP/porta exibidos pelo anfitrião.

## Sprites

As novas sprites Reaper (idle/walk) estão incluídas e integradas ao RPG offline por raça. O cliente de rede RpgNetworkClientLauncher continua carregando assets/dev/player-base: a nova Reaper ainda não é selecionada pelo renderizador multiplayer. Distribuir os arquivos não muda essa integração. Não houve implantação de servidor dedicado; a sessão é hospedada pelo anfitrião. Teste físico entre dois computadores via Radmin continua pendente.

## Validação

9 testes Python do preparador e 32 verificações PowerShell do atualizador passaram. Evidência de compilação e pacotes: dist/SpiritGate-Atualizavel-Radmin-0.200.1. Smoke com instaladores reais em .task-checkpoints/radmin-atualizavel (migração do launcher 0.78.1, saves sentinela, papéis, UI, download incremental, falha de integridade e dois clientes locais).
