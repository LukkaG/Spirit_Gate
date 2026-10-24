# Inventário Corujão V

Base recuperável: `.task-checkpoints/TASK-160/corujao-v/before.zip`, 4678 arquivos versionados elegíveis, sem saves pessoais/projetos/releases. `before.json` contém HEAD, branch, hashes de fontes e inventário protegido de assets, saves, studio-projects e dist existentes.

Mudanças: encontro compartilhado e ligação sintética em RpgWorld; opções em RpgSessionWorld; novos DTOs negociados em Protocol; distribuição por peer em LocalSimulationServer; cliente/RTT/apresentação; política RadminAccess; protótipo SyntheticOnlineRepository; EquipmentAppearanceAdapter; fixture `rpg/corujao-v.sgquest`; testes e ferramentas privadas.

Checkpoint final mantém ZIP de delta, diff contra a base e SHA-256 por tarefa, além de inventário final. Por serem mudanças integradas, os diffs posteriores são cumulativos, não provas de fronteiras de implementação independentes. Os quatro marcos recebem índices de consolidação com essa limitação declarada.

Pacotes privados são gerados por allowlist em novos nomes `SpiritGate-CorujaoV-0.175.0-dev-{Anfitriao,Convidado}-*.zip`, com manifesto interno e SHA-256 externo. Runtime é produzido por jlink; jar contém somente fontes de produção/recursos; sprites-base e catálogo cosmético temporário são copiados nos seus caminhos originais. Sem JUnit, fontes de desenvolvimento, saves, credenciais, backups pessoais ou releases antigos dentro dos pacotes. Arquivos da runtime incluem avisos legais necessários.
