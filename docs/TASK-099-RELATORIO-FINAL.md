# TASK-099 - Corujao II

Estado: **VALIDADA**.

Formato .sgmap Properties UTF-8 V1, registros estruturados com Base64URL para textos, limites 32MB e contagens, recusa de versoes/chaves duplicadas/desconhecidas. Novo/Abrir/Salvar/Salvar como/recuperar backup e confirmacao de descarte em UI; IO em worker. Temporario validado e backup integro sem sobrescrever arquivo corrompido. Testes round-trip, corrupcao, backup e UTF-8 aprovados; sem migracao de mapas legados.

Checkpoints recuperaveis, hashes e diff isolado: `.task-checkpoints/TASK-099/corujao-ii/`. Logs da suite: `docs/TASK-studio-099-*`. A aprovacao artistica permanece humana; os testes nao equivalem a homologacao perceptiva.
