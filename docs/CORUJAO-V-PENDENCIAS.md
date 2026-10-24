# Pendências explícitas

- TASK-163: ampliar testes de rede de vigor insuficiente, parede, destino inválido e esquiva observada em dois clientes; as regras são reutilizadas do offline.
- TASK-165: harness completo de latência/jitter/perda/reordenação no transporte e correção visual sob essas condições. RTT básico está implementado.
- TASK-169: carregamento online de `.sgmap` personalizado e mapas maiores; DTO/save legados continuam 960×640.
- TASK-171: bind real em adaptador Radmin e teste remoto com dois computadores. Não há evidência ambiental para aprová-los.
- TASK-172: associações item→arte exigem aprovação explícita. Adaptador começa sem associações inferidas; sprites temporários preservados.
- TASK-173: ensaio prolongado, CPU total/renderização sob carga e análise de pausas máximas de tick. Sem afirmação de segurança de produção.
- TASK-174/175: jornada nativa completa quest→portal→reconexão e homologação remota permanecem parciais. Capturas e teste integrado independente não substituem esse roteiro.
- Checkpoints posteriores são consolidações cumulativas de uma implementação integrada; não representam fronteiras temporais isoladas e retroativas de cada tarefa/marco.

Persistência online integrada, contas, credenciais duráveis, retomada por token, PvP e escalabilidade massiva não foram implementados. A reconexão nova é a alternativa permitida ao não haver retomada autenticada comprovada.
