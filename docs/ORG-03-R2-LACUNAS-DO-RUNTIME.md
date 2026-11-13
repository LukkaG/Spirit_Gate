# ORG-03-R2 — Lacunas do runtime e proposta mínima de API

## Conclusão de escopo

Nenhum accessor de produção foi criado ou modificado. O protótipo ficou em
`src/test`, porque captura integral exigiria novos contratos de leitura e
informações que ainda não existem. Esta proposta fica disponível para **autorização
específica antes de qualquer edição de produção**; não inicia uma implementação.

Não é suficiente chamar RpgSave.save, SyntheticOnlineRepository ou o laboratório
R3. O primeiro não é snapshot multiplayer completo e monta flags; os outros
têm autoridade/escopo sintéticos distintos, não histórico econômico deste ator.

## Bloqueios demonstrados

| Lacuna | Evidência no código | Consequência |
| --- | --- | --- |
| Identidade estável/propriedade | UUID online deriva da instância/peer, sem dono autenticado | UUID efêmero não vira CharacterId permanente |
| Revisão de personagem/época confiável | Não existem como metadados duráveis por personagem | Não usar tick, sequence ou pedaço do UUID como revisão/fence |
| Atributos brutos | manualAttributes habilita modo e chama applyLevelUp até o nível atual | Exportador não pode usá-lo nem recalcular pontos |
| Fração de energia | energyRegenerationRemainder é privado; normalização arredonda | HP/energia observáveis não completam Resources |
| Cadência fracionária | preciseAttackReady é privado; view usa inteiro ceil/clamp | Não reconstruir deadline de AGI por remainingTicks |
| Cinco recargas | ready[5] privado; state ausente produz zeros | Capturar antes de remove com presença/binding comprovados |
| Missões builtin/autoradas | Mapas/flags privados e ledgers distintos; View só tem total/texto | Não reconstruir vetor de objetivos nem entrega pelo texto |
| Recibos | Flags locais de conclusão/derrota não são recibos completos | Nenhum getter pode fabricar pagamento/confirmedRevision/resultHash |
| Direitos pendentes | pending:item:quantity não carrega concessão completa | Quantidade exige accessor; identidade de direito exige contrato econômico |
| Status negativos | Timers privados em RW, efeitos também no engine; bool blocked é misto | Não afirmar que ausência de export significa limpo |
| Vínculo ao owner/participação | LS.commands e RS.Session são internos | Laboratório não prova captura de peer admitido em coop-world |

## Menor proposta de leitura, não aplicada

Os nomes são candidatos para revisão e não declarações de APIs existentes.
Preferir read models internos com valores imutáveis, sem fazer RpgWorld depender
do DTO de persistência. O adaptador faz a conversão depois da cópia coerente.

### 1. RpgWorld: read model sem inicialização tardia

Propor `readCharacterState()` ou equivalente package-private, invocado apenas
no owner, retornando cópias de:

- Identidade atual, região, posição, tick, XP e DomainProgression.
- StatsComponent bruto **e** manualStatsEnabled, sem manualAttributes/applyLevelUp.
- HP, energia absoluta, máximo observacional e acumulador fracionário.
- Inventário, equipamento, moedas e ledgers builtin, sem save/snapshot/writeTo.
- Prazos básicos: attackReadyTick, preciseAttackReady e seu estado inicial
  declarado, abilityReadyTick, potionReadyTick e portalReadyTick.
- Evidência de restrições com tipo/prazo/origem disponíveis; dado ausente
  continua explícito, não substituído por lista vazia.

Não exportar corpo, movimento, cenário, NPCs, população, socket, RNG, fila ou
execução. Não reescrever o objeto inteiro nem criar getter público por campo.
Estado inicial de cadência ainda não usado (NaN interno) precisa de representação
explícita; não o converter silenciosamente em um deadline inventado.

### 2. RpgSkillEngine: timing ligado à sessão real

Propor leitura `readTiming(participantId, expectedWorld)` com resultado de
presença distinto de estado com todas as skills prontas. Verificar o vínculo
ao mesmo objeto/mundo, região e domínio no mesmo owner; retornar cópia dos
cinco ready absolutos e carimbos de relógio, não pendências/eventos/checkpoints.
Não chamar o helper que cria Session lazy. Não usar state(id) como fonte de
presença nem fazer remove/clear antes de ler. Captura após remoção deve falhar
explicitamente, sem obter uma lista de zeros como fallback.

Não inventar geração de admissão onde hoje ela não existe. O vínculo atual
ajuda a leitura na mesma instância; retomada durável precisa do contrato de
identidade/geração próprio da ORG-03, ainda não implementado.

### 3. AuthoredQuestSession e builtin: ledger read-only

Propor `readLedger()` ou equivalente interno com progress/flags/contadores e
referência fiel da definição. Não fazer refresh/complete/event, conceder prêmio,
ler UI ou parsear texto de objetivo para reconstruir seu progresso.
Separar formato do QuestBook, revisão do conteúdo e versão semântica da missão.
Não assumir que QuestBook.version=1 prova versão de cada entrega.

Mesmo uma cópia perfeita do ledger antigo não contém recibo durável novo.
O exportador deverá continuar devolvendo ausência até existir política de
migração/registro econômico, aprovada separadamente.

### 4. RpgSessionWorld/owner: envelope coerente de participação

Propor entrada interna de leitura no owner, ligada ao participante e contexto
esperados, agregando as três leituras anteriores e cosméticos. Recusar sessão
ausente, revisão incompatível, congelamento/transição não capturável ou binding
do engine divergente; não chamar snapshots de atributos que materializam pontos.
Retornar snapshot interno imutável ou diagnóstico, sem instalar ou publicar.

O encaminhamento até coop-world deve usar comando interno limitado/ordenado,
sem bloquear tick com I/O. Não expor inventário/identidade por endpoint de debug
ou mensagem TCP não autenticada. A interface atual SessionSimulation não oferece
essa exportação completa; revisar a menor fronteira Java necessária, sem mudar
wire6/7 nesta proposta. Limites de fila e resultado de remoção concorrente exigem
teste próprio antes de conectar ao servidor.

## Informações que getters não resolvem

Recibos e direitos econômicos precisam nascer na operação autoritativa que
concede/consome o direito, com identidade de origem e deduplicação. Não se pode
emitir recibo novo apenas porque se encontrou quest-done, defeat ou pending.
Exigir decisão sobre estado legado sem histórico completo: recusar exportação
integral, reter como legado explicitamente não confirmado ou migrar por política
aprovada. Nenhuma alternativa foi implementada.

Revisão de personagem, identidade estável, prova de posse e época/clock são
contratos de autoridade. Não derivar nomes/IP/peer/tick para preencher seus campos.
Metadados sintéticos do laboratório não podem ser enviados a uma conta real.

## Políticas pendentes

- Continuidade de cooldown desconectado e conversão de precisão/unidade.
- Tratamento de clock/época após restart, falhas e rollback/avanço de horário.
- Recuperação de restrições adversas por tipo, sem purga vantajosa nem replay
  de DoT. O protótipo preserva o status ativo no mundo e não o instala no DTO.
- Retenção/migração de históricos, recibos e loot pendente sem prova completa.
- Identidade/propriedade, estado de morte, seleção de spawn e compatibilidade
  de regras/conteúdo no momento de futura instalação.

APIs read-only e políticas de recuperação devem ser aprovadas separadamente.
Esta etapa não escolhe como curar, regenerar, expirar efeito ou materializar.

## Limitações das provas desta etapa

O executor `org03-r2-owner` é da fixture, não é coop-world de um servidor TCP.
Todos os escritores do laboratório estão confinados a ele. Comparar NetworkState
antes/depois é guarda adicional, não detecção completa de corrida/ABA no mesmo
tick. Não há callback de mutação durante capture. Mudança real durante ativação/
revisão e saída de peer em produção continuam pendentes.

O teste de Source.retire prova somente a recusa da participação declarada pela
fixture após retirada do encontro/engine local. Não prova que uma referência RW
retida detecta sozinha RS.leave. O protótipo não consulta recargas do engine e
por isso não apresenta recargas pós-remove zeradas como estado recuperável.

O teste de atributos prova observação de AGI/Energy e ausência de chamadas lazy
no capturador, não exportação dos quatro investimentos/pontos/checkpoint.
Resumos autorados não demonstram round-trip de objetivos/recibos. A fração real
e deadlines exatos estão marcados como não capturados, não como sucesso simulado.

## Gate para futura importação

Somente considerar um candidato completo após existir:

1. Leitura integral do estado permitido no owner, com presença/contexto verificáveis.
2. Todas as referências econômicas necessárias ou política explícita de legado.
3. Identidade autorizada e revisão/geração/época sem aliases de peer/tick.
4. Precisão de energia/cadência/recargas e política de efeitos/clock aprovada.
5. Validação R1 com blocos completos, mais guardas reais de mundo/spawn e posse.
6. Testes de corrida, remoção, revisão, erro e ausência de mutação no backend real.

Isso ainda não autoriza instalar. Importação, persistência, reconexão, protocolo
e eventual ativação de A-04 exigem autorização própria e novas provas. Não há
timeout de 92 ticks nem mudança de Quatro relâmpagos nesta R2.
