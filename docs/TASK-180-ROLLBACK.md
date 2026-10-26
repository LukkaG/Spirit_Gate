# Histórico, rollback e recuperação

LivePublishing foi estendido por PublicationTarget; o caminho sintético da TASK-179 continua funcionando. O destino real participa das mesmas fases de reserva, staging, ativação, confirmação e abort.

O namespace do host é `studio-projects/game-revisions-<porta>`, relativo à raiz do pacote ou ao `spiritgate.rpg.dataDir` quando fornecido pelo atualizador. Ele é independente de saves pessoais e dos manifestos de executáveis. Não use cópia manual de active.txt como rollback de uma partida aberta.

Na primeira publicação real, a revisão inicial do servidor também é arquivada. Cada versão preserva documentos canônicos, perfil, data UTC, hash, origem, compatibilidade, validação e manifesto. Assets usados são copiados por hash para images/ e conferidos. A publicação continua exigindo os mesmos recursos preinstalados nos clientes; o arquivo de histórico não é um canal de download.

Staging e versões têm nomes separados. Ponteiros usam escrita em temporário, force de arquivo e atomic rename. Journal guarda before/after. Erros antes do commit abortam os mundos candidatos e restauram os renderizadores antes de retomar. Falhas de confirmação tardia consultam o destino, evitando colocar o ponteiro atrás de um commit já confirmado.

**Revisoes → Historico e rollback validado** restaura conteúdo ao rascunho. Aplique com Salvar: rollback é nova transação, com a mesma validação de assets, perfil, posições, clientes e hash-base. Uma parede antiga sobre uma posição atual também pode impedir rollback; afaste os jogadores/inimigos por gameplay legítimo e tente novamente. Não há teleporte corretivo silencioso.

Se houver desconexão durante uma transação, o conjunto é recuperado. Participantes que não confirmem a cena anterior em oito segundos são isolados/desconectados; o restante só retoma com hashes compatíveis. Isso não é expulsar um cliente V1/V2/V3 para liberar publicação: esses clientes bloqueiam antes da preparação.

Reinício carrega somente revisão confirmada, com documentos/hash/assets validados. Journal pendente bloqueia hospedagem e informa recuperação necessária. Com o servidor desligado, use o botão **Recuperar publicacao interrompida** no anfitrião; o journal é preservado como recovered-*.properties. Depois hospede novamente e confira o hash.

Arquivo ausente, hash adulterado ou asset incompatível interrompem carregamento/publicação; não há fallback considerado sincronizado. O histórico tem limite de 200 revisões para impedir crescimento sem controle; arquive cópias de forma explícita antes de atingir esse limite, preservando ativa/anterior e evidências. Nenhuma revisão é apagada automaticamente.

Limites: filesystem local e instância de servidor experimental. Não é consenso distribuído, nem recuperação homologada de queda física de energia. Uma falha fatal da JVM encerra suas janelas; journal/versão confirmada orientam a recuperação no próximo início.
