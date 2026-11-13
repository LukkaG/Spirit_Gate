# TASK-209 — hotbar, habilidades e domínios

31/10/2026. Auditoria: ataque racial, Shunpo ofensivo/defensivo, poção e
DomainProgression já existem. Técnicas descritas em Domains são previews,
não skills executáveis. A integração não inventa magia ou assets.

NetworkHotbar permite selecionar e usar quatro ações (tecla 1), mantendo SPACE,
SHIFT e Q. A ficha V6 publica recargas reais de ataque, Shunpo e poção, alcance
básico e mensagem do mundo. Custo Shunpo: 25 energia; nível 20; recarga comum
de 90 ticks/3s; ofensivo exige alvo visível e percurso livre. Poção usa 60 ticks.
Ataque usa 12 ticks. O servidor valida custo, vida, alcance, geometria e nível.

Escolha de domínio reutiliza a regra real: nível 15, compatibilidade racial,
sem domínio anterior. Hotbar exibe o estado e não promete técnicas ainda sem
implementação. Progresso/domínio seguem efêmeros na sessão. UI foi distribuída
em linhas para evitar ocultar controles no topo.

Arquivos: NetworkHotbar, GameplayProtocol, RpgWorld, RpgSessionWorld,
RpgNetworkClientLauncher, NetworkInventoryPanel e Task209AbilitiesTest.
