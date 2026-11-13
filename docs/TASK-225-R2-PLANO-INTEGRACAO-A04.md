# TASK-225-R2 — Plano de integração A-04 (não aplicado)

Referência: 03/11/2026. O laboratório demonstra um caminho compatível com os mecanismos atuais; não autoriza ativação. Nenhuma classe de produção foi modificada na R2.

## Diagnóstico e resultado da fronteira

O prazo finito pode terminar pela retirada existente. Só remover a skill deixaria um corpo sem apoio válido; só remover o mundo sem avisar o transporte permitiria consulta privada para sessão inexistente. O adaptador de teste retirou o mundo e conservou um pedido separado de desconexão; `LocalSimulationServer` fechou o peer antes de solicitar snapshots privados. O callback `departures -> leave` posterior foi inofensivo.

O TCP usa dois clientes, lançador 7 e observador 6 ou 7. O prazo exato é exercitado separadamente no modelo: engine real primeiro tenta reaparecer, coordenador candidato só retira aos 92 se ainda houver fase. A condição TCP é uma retirada equivalente injetada durante fase real; não constitui uma integração de `LightningField` com o timeout.

## Classes e métodos propostos

| Classe/método atual | Mudança futura para revisão |
|---|---|
| `RpgSkillEngine.LightningField.advance` | Após a tentativa real aos 92, produzir pedido interno de falta definitiva de apoio, uma vez por execução. Preservar os quatro cortes e sua guarda `resolved`. |
| `RpgSkillEngine.tick`, `remove`, `clear` | Expor/coletar pedidos imutáveis; invalidar por sessão, mundo, região, revisão e geração ativa; não chamar o coordenador enquanto itera `pending`. Preservar sequência monotônica. |
| `RpgWorld.skillReappear`, `skillDashing`, `skillClearControls` | Reutilizar validação real de apoio e limpeza. Não inventar posição terminal nem simplesmente tornar sólido um personagem obstruído. Nenhuma alteração geométrica prevista. |
| `RpgSessionWorld.step`, `skill`, `leave`, `disconnectRequested`, `installRevision` | Consumir pedidos no owner, retirar lógica e idempotentemente, manter pedido de transporte fora de `sessions`, invalidar pedidos antigos em revisão/substituição. Impedir novos comandos de sessão encerrada. |
| `SharedRpgEncounter.leave`, `hit` | Reutilizar saída; conferir ausência no mapa de participantes vivos e que contribuições antigas não premiam o personagem novo. Não alterar ledger/recompensas sem evidência de necessidade. |
| `SessionSimulation.disconnectRequested` | Pode ser reutilizado sem novo método wire. Documentar semântica interna de pedido persistente até fechamento e consumo/expiração limitados. |
| `LocalSimulationServer.start`, `Peer.closePeer` | Manter checagem de pedido antes de qualquer snapshot privado; fechamento idempotente e liberação de capacidade. Definir confirmação de consumo do tombstone, se necessária, sem chamada ao socket pelo engine. |
| `LocalClient` reader/`close`/`reconnect` | Fluxo genérico atual funciona para desconexão e readmissão efêmera. Mensagem específica exige trabalho autorizado e compatibilidade explícita; não inseri-la silenciosamente em 6/7. |
| `NetworkSkillPresentation.actors`/`update` | Reutilizar limpeza por ausência do ator, desconexão e revisão. Preservar correção visual da TASK-225. |

## Contrato interno candidato

Pedido imutável com: ID de participação, UUID de personagem ou referência ao mundo admitido, geração de admissão, geração única da execução ativa, região, revisão/época do mundo, tick inicial/limite e motivo interno `NO_VALID_SUPPORT`. Campos internos não são nova mensagem de protocolo.

O owner compara todos os campos com a execução **ainda ativa**. Presença de evento histórico no buffer não basta. O laboratório usa sequência do evento inicial como prova de vínculo durante uma única fase; o modelo usa geração explícita. Isso não implementa a geração de execução necessária em produção. Nome/IP não são identidade. Não reutilizar o ID antigo na readmissão.

Estados propostos: ativo → retirada lógica concluída/pedido de transporte pendente → peer fechado/pedido consumido. Duplicatas não refazem impactos, recompensa, cancelamento de outra geração ou liberação de vaga. Uma desconexão espontânea vence um pedido que chega atrasado. Região/revisão/substituição invalidam o pedido anterior.

## Ordem e prazos

1. Tick relativo 31: fase normal, sem resolver cortes por antecipação.
2. Tick 32: quatro cortes uma vez; primeira tentativa real de materialização.
3. Ticks 33..91: tolerância, tentando apoio pelas regras atuais.
4. Tick 92: última tentativa primeiro. Se tiver apoio, termina normalmente. Se falhar, registra resultado interno sem tornar o corpo sólido.
5. Depois da iteração do engine, no mesmo passo do owner e antes do snapshot público/privado: revalidar token, cancelar execução/flags/pendências, retirar sessão/corpo/encontro pela rotina existente e registrar tombstone de transporte. A implementação deve tornar essa transição observável como única retirada lógica.
6. Transporte consulta pedido, fecha apenas o peer correspondente e libera capacidade uma vez. Hoje `closePeer` já é idempotente e agenda `departures`; preservar esse mecanismo. O comentário da classe sugere ausência de sockets no owner, mas a checagem atual chama `closePeer` do owner: é comportamento existente, não uma nova garantia de fechamento assíncrono. O engine nunca toca sockets.
7. O callback posterior de `leave` não recria sessão nem recompensa. Tick 93 não faz nova busca/retirada para aquela execução.

São até 61 tentativas (32..92 inclusive). A R2 não muda frequência/custo da busca. Testes locais não demonstram prazo máximo de CPU nem carga concorrente. Definir limite e expiração do registro de encerramentos para não acumular IDs durante o processo; não apagar antes de o transporte deixar de consultar a sessão.

## Snapshots e comandos pendentes

Retirada deve anteceder snapshot público e filtro de eventos visíveis. Preservar verificação `disconnectRequested` antes de `skillsSnapshotFor` e `gameplaySnapshotFor`. Nenhum snapshot privado substituto/fictício deve esconder sessão ausente.

Comandos que o owner já executou antes da decisão seguem sua ordenação normal. Comandos restantes para o ID retirado devem retornar recusa sem efeitos; o teste colocou comandos na fila real antes da retirada, drenados no tick seguinte. A nova conexão recebe outro ID do transporte; frames de comando não oferecem escolha arbitrária do ID de origem. Sequências continuam vinculadas à sessão e revisão atuais. Respostas de consulta privada só podem ser solicitadas após comando aceito e sessão ainda válida.

## Decisão de experiência: A versus B

| Opção | Consequência e decisão desta entrega |
|---|---|
| A — aceitar desconexão e reinicialização experimental | Tecnicamente viável pelo fluxo efêmero atual. **Aceita apenas como premissa do laboratório**, sem autorização para aplicar aos jogadores. Perde XP/progressão adquirida, domínio, itens, moedas, missões e cooldowns; nível inicial depende de `syntheticLevel`. Reiniciar recarga pode permitir abuso de reconexão; não apresentar como recuperação. |
| B — exigir recuperação antes da ativação | **Recomendação de gate para qualquer ambiente que prometa conservar progresso ou recargas.** Exige identidade confiável, retenção/recuperação de estado e cooldown autoritativo, regras de expiração e prevenção de duplicação; não foi projetada nem implementada nesta tarefa. |

Decisão explícita: a R2 não autoriza perda de progresso fora do laboratório. A ativação continua bloqueada até o responsável aceitar expressamente A para o contrato experimental ou exigir B. A suíte demonstra novo UUID/ID, domínio `none` e recargas zero após readmissão; não testa uma restauração inexistente. Inventário/XP do personagem retirado são checados no modelo para detectar crédito tardio, sem serem transportados ao novo.

Readmissão sempre usa o handshake/admissão atual. Spawn ocupado pode recusar sem criar sessão parcial; depois de liberar o spawn, nova tentativa é possível. Não reconectar automaticamente em ciclo nem reaproveitar corpo/identidade anterior.

## Feedback sem alterar silenciosamente 6/7

No contrato atual, o cliente detecta encerramento do socket e segue o fluxo genérico de conexão/reconexão. Não há evento estruturado de causa A-04; o evento aplicável no ensaio é EOF/fechamento, não materialização fictícia. `Protocol.Error` não é tratado como razão de partida pelo reader atual e não garante entrega antes do fechamento.

Alternativa compatível para aprovação: aviso textual prévio fora da partida (documentação/tela de entrada experimental), informando possibilidade de encerramento e perda do personagem temporário, mantendo fechamento genérico. Mesmo essa alteração de produto não foi aplicada. Se a razão específica em jogo for obrigatória, exigir proposta separada de capacidade negociada, mensagem e fallback para clientes 6/7; não aprovar integração silenciosa nem reutilizar ACK/trajetória.

## Testes necessários antes de ativar

Reexecutar a matriz R2 contra o patch definitivo, retirando o adaptador candidato: início real do pedido no engine, apoio 31/32/91/92, ausência até 93, quatro cortes, morte/ledger/recompensa única, geração antiga após execução nova, revisão/região/substituição, duplicatas e desconexão simultânea. Integrar falta real de apoio até 92 com dois TCP, além da prova equivalente atual. Testar queries de energia/atributos pendentes, churn e liberação limitada de tombstones.

Manter regressão das 40 skills, colisões, aplicações 6/7, HUD/foco/hotkeys e correção visual. Medir owner completo e transporte sob carga limitada; validar feedback e readmissão conforme opção aprovada. Homologação física/Radmin/launcher é separada. Se B for escolhida, incluir testes de identidade, persistência/recuperação, duplicação e cooldown durante ausência antes da ativação.

## Rollback e autorizações

Implementar futuramente em patch localizado, com baseline e testes. Para desativar, reverter somente o patch de prazo/pedido no engine e consumo no coordenador, preservando retirada normal e revisão; confirmar que nenhum pedido candidato pendente permanece. Sem mudança wire/save, não há migração implícita. Se B exigir formatos persistentes, produzir plano próprio antes de aplicar.

Exigem autorização: prazo 32+60, retirada automática definitiva, consequências de A ou escopo de B, tratamento de recargas/progresso, feedback e eventual contrato negociado, integração de runtime e homologação/ativação. TASK-226, publicação, ACK e trajetória continuam fora desta entrega. A-04 permanece bloqueante.
