# TASK-224 — Proposta de feedback autoritativo (não implementada)

Esta proposta cobre a limitação de A-01. **Não integra o protocolo atual e não está autorizada para implementação.** Aplicações 6 e 7 permanecem byte a byte como estavam.

## Necessidade

Os eventos visuais e cooldowns atuais descrevem efeitos/estado, mas não identificam o resultado de cada `SkillProtocol.Request.sequence`. Ausência de evento pode ser recusa, atraso, evento descartado pelo buffer ou uma ação sem evento visual próprio. Stun/execução também não são descritos como estado completo do personagem no contrato atual.

## Alternativas sem extensão

1. Manter o feedback desta TASK: prévias locais, envio/espera, dado observado e resultado desconhecido. Não promete rejeição ou aceite por intenção.
2. Mostrar mensagens privadas existentes como texto genérico, sem atribuí-las a uma skill específica. Não analisar texto para inventar códigos de erro; operações concorrentes tornam a atribuição insegura.
3. Inferir por custo/cooldown não resolve cancelamentos, ações concorrentes, perda de eventos ou igualdade de valores. Essa inferência não deve virar ACK.

## Contrato candidato, sujeito a aprovação

Resultado privado e estruturado contendo:

| Campo | Tipo/limite proposto | Finalidade |
|---|---|---|
| Revisão ativa | SHA-256 ASCII de 64 caracteres | Impedir atribuição após troca de conteúdo |
| Sequência da intenção | long, mesmos limites da request atual | Correlação exata com o envio |
| Slot | byte 1–5 | Identificação da ação sem nome livre |
| Tick do servidor | long limitado | Momento em que a decisão foi tomada |
| Resultado | enum pequeno ACCEPTED/REJECTED | Aceite do lançamento, não promessa de impacto |
| Motivo | enum limitado, sem texto arbitrário | Nível, energia, cooldown, alvo, geração, região, alcance, LOS, controle ou estado incompatível |

Um segundo estado privado opcional poderia informar flags/intervalos de controles do próprio jogador e fase de execução. Deve diferenciar incapacidade de mover/atacar/conjurar e preservar exceções como cancelamento da Sentinela. A lista de inimigos marcados por Vento é outro requisito, de TASK-227, e não deve ser incluída por conveniência sem escopo/privacidade definidos.

ACK de lançamento não aplica dano; o executor continua único responsável por contato e efeitos posteriores. Resultados de transporte e resultados do gameplay são distintos. Repetição de sequência não pode gastar energia ou conjurar novamente; a política de cache/reenvio do resultado precisa ser limitada e definida no servidor.

## Negociação e compatibilidade

Não acrescentar bytes/opcodes silenciosamente a uma sessão Hello de aplicação 6 ou 7. Propor uma nova capacidade/aplicação negociada explicitamente, com identificador a aprovar antes de implementação. Servidor sem essa capacidade deve recusar a negociação com mensagem clara; uso do modo 7, caso oferecido, precisa ser uma escolha explícita de compatibilidade e conservar as limitações documentadas.

Clientes 6 e 7 continuam recebendo apenas seus frames originais. Nem o wrapper de revisão nem a versão de conteúdo devem ser reinterpretados para ativar ACK. As opções de negociação e coexistência precisam passar por revisão arquitetural.

## Frames, serialização e limites

Manter `Protocol.MAX_FRAME=4096`. O núcleo proposto ocupa aproximadamente 85 bytes antes do envelope: hash UTF limitado (66), sequence (8), slot (1), tick (8), resultado (1) e motivo (1). Deve permanecer abaixo de 256 bytes com o envelope/wrapper definidos, com teste de tamanho exato quando o formato for aprovado. Não serializar stack trace, nomes ou texto livre do servidor nesse frame.

Fila de resultados privada e limitada; ACK não deve competir sem limite com snapshots nem bloquear o tick por cliente lento. Definir ordem com estado/cooldown e usar revisão/sequence para deduplicação. Validar contagem, enum, limites, wrapper e revisão antes de construir objetos/alocar buffers.

## Riscos

Confundir aceite com hit; mensagens de revisão antiga; duplicação de ações por retransmissão; cache de resultados ilimitado; cliente antigo recebendo opcode novo; vazamento de estados de outro jogador; mudança acidental de regras de controle. A decisão deve continuar no servidor e não depender do relógio ou pixels do cliente.

## Testes antes de liberar

- Golden bytes 6/7 intactos e matriz de negociação compatível/incompatível.
- Round-trip, tamanho máximo, enums/contagens inválidos e wrappers malformados.
- Aceite/recusa por motivo sem consumo em recusa; ACK não reaplica dano.
- Duas intenções simultâneas, ordem invertida, duplicação e replay, revisão/viagem/reconexão.
- Cliente lento, fila cheia, socket perdido e encerramento; nenhuma espera ilimitada no owner.
- Sentinela liga/desliga, área cancelada antes do envio, controles ativos e especial de raio sem alterar suas regras.

## Rollback

Desabilitar a capacidade nova e recusar novas negociações correspondentes. Clientes antigos 6/7 continuam no fluxo anterior. Cliente novo pode oferecer reconexão explícita em modo compatível com feedback limitado; não reinterpretar frames da sessão em andamento. Sem migração de save/banco. A estratégia exata de descarte de cache e encerramento de sessões novas deve acompanhar a implementação aprovada.
