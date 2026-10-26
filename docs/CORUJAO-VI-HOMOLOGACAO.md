# Corujão VI — homologação

Homologação automatizada local deve ser lida com o resultado final da TASK-200. Publicação/rollback e recuperação negativa são testes reais de Game Server/TCP; Studio e clientes gráficos da TASK-199 são janelas de produção. Pacotes da TASK-198 foram extraídos em pastas novas e executados com Java integrado; self-checks, manifests e recusa administrativa do convidado passaram. Nada foi publicado.

**Homologação física Radmin: PENDENTE.** Preencher em dois computadores reais; não converter os PASS locais abaixo em aprovação remota.

| Caso | Evidência automatizada local | Evidência A/B pela VPN |
| --- | --- | --- |
| Mesmo jar, assets e revisão inicial | 197/198, manifests e hashes | PENDENTE |
| Preview separado, DRAFT preserva partida | 199, capturas e métricas | PENDENTE |
| Save → READY/ACTIVE/RESUME e commit | 186/198/199 | PENDENTE |
| Confirmação negativa não publica | 186, repetida na 200 | PENDENTE |
| AFTER_STAGE/AFTER_ACTIVATION/BEFORE_POINTER | 186; falha ativação também no pacote 198 | PENDENTE |
| Movimento/PvE/portal após publish | 199, servidor e renderizadores reais | PENDENTE |
| Rollback/ponteiro/reconexão coerentes | 185/186/191/198/199 | PENDENTE |
| Cliente antigo bloqueia Save sem expulsão | 188 e regressão final | PENDENTE |
| Convidado sem administração | 192/198 | PENDENTE |
| Identidade/retry/restart de personagem sintético | 194–196; sem integração ao launcher | Não integra teste de contas reais |

Procedimento humano: obter os dois ZIPs pareados do inventário CORUJAO-VI-PACOTES.md; conferir SHA, extrair integralmente em pastas novas, anotar versões/IP/adaptador selecionado. Máquina A usa Anfitrião, B usa Convidado. Seguir TASK-184-RADMIN-ROTEIRO.md para ingresso pelo adaptador explicitamente selecionado, publicação e rollback, inputs, portal, queda/retorno e cliente legado. Não alterar firewall/VPN por efeito colateral nem mover active.txt manualmente.

Registrar data, operador A/B, SHA dos ZIPs e jar, endpoint escolhido, hashes antes/candidata/depois, logs dos dois lados e resultado de cada caso. Não incluir credenciais ou personagens pessoais. Até esses registros existirem, manter PENDENTE. Capturas por printAll mostram componentes reais, mas não constituem aceite humano nem teste de energia.
