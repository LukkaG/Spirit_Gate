# TASK-176 - Launcher integrado ao Radmin VPN

Entrada principal existente: RpgLauncher -> SpiritGateEntry -> RpgMenu offline. Hospedar e Entrar usam RadminSessionPanel, LocalClient e RpgNetworkClientLauncher. NetworkClientLauncher permanece como Coop historico. LocalSimulationServer executa SessionSimulation/RpgSessionWorld com combate V3 e cosmeticos existentes.

Auditoria incluiu RadminAccess, configuracao de conexao, scripts Servidor-Radmin.cmd, Cliente-Radmin.cmd, Servidor-local.cmd e Iniciar-dois-clientes.cmd gerados por build-corujao-v.py, empacotamento por papel e documentacao Corujao V/TASK-175.

Servidor gerenciado em processo pela API AutoCloseable existente; utiliza o Java do launcher, inclusive o runtime integrado. Bind explicito ao IPv4 validado do adaptador Radmin. Nao anuncia prontidao antes do listener. Nao controla processos externos.

Sem alteracao de protocolo, saves, assets originais, firewall ou instalacao de VPN. Sem push, publicacao ou TASK-177. Checkpoint: .task-checkpoints/TASK-176/launcher-radmin, com HEAD, branch, hashes, diff e evidencias.
