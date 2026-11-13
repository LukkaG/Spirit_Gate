# ORG-03 — Relatório final da entrega inicial

## Resultado

Entregue a arquitetura proposta de Player/estado recuperável, com auditoria
do checkout e sem ligação ao runtime. **87 testes dirigidos aprovados em 20
classes, zero falhas/erros/skips.** A entrega conclui o escopo inicial de
auditoria/documentação; decisões futuras são propostas, não políticas ativadas.

## Principais constatações

1. `domain/entity/Player` com componentes e `rpg/RpgWorld` são modelos distintos.
   `RpgSessionWorld` usa RpgWorld/proxy; PlayerFactory não é sua fábrica de estado.
2. O UUID online deriva da instância do mundo e do peer. Reconectar refaz criação
   efêmera, sem prova de dono ou importação persistente; mesmo nome não recupera
   o personagem anterior. Não há geração de admissão durável no runtime atual.
3. RpgSave V2 cobre campanha local, mas não atributos manuais/pontos, recargas
   das cinco skills ou ledger completo de missões autoradas. Energia normalizada
   não recupera seu valor absoluto com precisão. Fork de revisão não é readmissão.
4. Flags/recibos atuais dão deduplicação nos seus escopos; não equivalem a uma
   transação durável online entre recompensa, inventário, XP, moedas e conclusão.
5. O repositório sintético e o laboratório R3 são referências úteis para CAS,
   posse local e retry, sem autenticação de rede ou recuperação multiplayer pronta.

## Contratos e decisões propostas

Persistir identidade estável, XP/domínio exatos, investimentos/pontos, inventário,
equipamentos, moedas, objetivos/entregas/recibos, direitos de loot pendentes,
recursos, deadlines e localização candidata com versão/revisão/contexto.
Recalcular nível/desbloqueios/máximos usando regras existentes; não aceitar
valores de autoridade do cliente nem reconceder recompensas na restauração.

Não restaurar conexão, peer/seat, filas, seleção/input, eventos, animações,
projéteis, dash, execução de efeitos, marcas ou intangibilidade. Restrições
negativas precisam de política explícita para não incentivar logout; excluir
execução não significa apagar penalidade ou reiniciar recarga.

Recomendar identidade independente de conexão, admissão exclusiva por geração/
fence, CAS de revisão, recibos atômicos e reconciliação após commit incerto.
Cooldown transcorreria desconectado por tempo autoritativo, sem cura/refill
automático; precisão/restart/clock e status adversos exigem aprovação e testes.
Gravação fora do tick, fila limitada e confirmação durável não são implementadas
por este plano. Banco e ativação do mundo precisam de coordenação/reparo próprios.

## Relação com A-04

Usadas R1/R2 e também a ADR R3 presente no checkout, mais recente. O futuro
snapshot deve ser capturado antes de remover execução/participação e recuperar
progresso permitido sem fase intangível. Sem apoio físico, reter estado sem
corpo ativo. **A-04 continua sem correção de runtime:** não houve prazo de 92
ticks, desconexão automática, alteração de imunidade ou recuperação automática.

## Arquivos entregues

- [Escopo e plano](ORG-03.md).
- [Matriz de estados por campo](ORG-03-MATRIZ-ESTADO-PLAYER.md).
- [ADR de identidade/reconexão](ORG-03-ADR-IDENTIDADE-E-RECONEXAO.md).
- [Contrato de snapshot](ORG-03-CONTRATO-SNAPSHOT.md).
- [Plano de persistência](ORG-03-PLANO-PERSISTENCIA.md).
- [Testes e limites](ORG-03-TESTES.md).
- Este relatório e adendo em [organização principal](ORGANIZACAO-JOGO-2026-11-09.md).

## Gate da entrega inicial

| Critério solicitado | Evidência |
| --- | --- |
| Inventário confiável | Matriz com nomes/tipos reais, donos, origem, validação, mutabilidade, escopo, persistência, recuperação, duplicação e dependências |
| Personagem/sessão/conexão separados | Auditoria das duas estruturas e ADR, com IDs/épocas/gerações distintos |
| Estado recuperável proposto | Contrato versionado, blocos obrigatórios/opcionais, validação e exclusões |
| Identidade/concorrência | Claim/fence, CAS, recibos e reconciliação; laboratório separado de runtime |
| Plano de persistência | Agregados/relacionamentos, transações, fila/worker e contingência sem SQL |
| Evidência de preservação | Git inicial, 2336 hashes anteriores, verificação final e 87 testes dirigidos |

## Compatibilidade, limites e riscos pendentes

Nenhum fonte Java/teste/POM, asset, recurso ou SQL foi criado/alterado nesta
entrega. Só documentação nova e adendo ao principal. Preservados skills,
balanceamento/combate, protocolo 6/7, mobs/IA/colisões, HUD/hotkeys, offline/Coop,
World Studio, launcher, saves e banco. Sem commit, push, release, migration,
instalação ou publicação. ORG-04/TASK-226 não iniciadas.

Inventário/diferenças em `temp/org03/before-sha256.json` e `validation.json`;
lista Git anterior em `git-status-before.txt`. Nenhum arquivo anterior deve
estar ausente por ação desta entrega. O resultado automatizado da verificação
é a evidência correspondente, sem confundir o trabalho local anterior com o diff
documental da ORG-03.

Não se executou suíte geral nesta etapa documental; os 1068 verdes da R2 são
baseline anterior. Não há validação PostgreSQL ou de contas, saves pessoais,
teclado físico/Radmin/launcher. A intermitência de foco continua sem causa.
O monitor local tinha status antigo e sem pendências listadas; relatórios TXT
anteriores de World Studio foram preservados e ficam fora desta tarefa.

Gates futuros: aprovar políticas, DTO completo e validador/importador sem
duplicar regras, relógio durável, orçamento/retenção de recibos, autorização/
proteção de canal, claim/lease e falhas entre commit/ativação, contingência de
banco e testes descartáveis. Não anunciar progresso salvo antes de confirmação
durável quando essa garantia for oferecida.

## Reversão e próximo passo recomendado

Remover somente os sete novos documentos e o adendo ORG-03; preservar fontes,
demais diffs e evidências. O principal anterior está copiado em
`temp/org03/ORGANIZACAO-before.md`; não restaurar o checkout globalmente.

Próximo recorte recomendado, mediante autorização própria: aprovar o contrato
e implementar DTO/validadores puros com testes dos casos faltantes, **sem runtime
ou banco**. Não foi iniciado automaticamente.
