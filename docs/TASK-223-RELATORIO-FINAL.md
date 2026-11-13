# TASK-223 — Relatório final da auditoria

Referência: 02/11/2026, `beco`, HEAD `6d72c65`. **Auditoria encerrada; nenhuma implementação da TASK-224 iniciada.**

## Resultado

Foram identificadas 40 habilidades executáveis, oito kits e dez domínios/aliases. A matriz classifica 24 como **funcionais e testadas** e 16 como **funcionais com ressalvas**. Não foram encontrados IDs desse conjunto apenas documentados, indisponíveis ou sem executor. Essa classificação considera a implementação local e a cobertura indicada; não representa aprovação de todos os requisitos futuros do Corujão IX.

Foram carregados 71 clips: 40 principais e 31 componentes. O loader resolve manifesto base e overlays até V8, valida caminhos/geometria e decodifica fora da pintura. O PDF tem 42 páginas; seu texto foi extraído para conferência, além da comparação do catálogo auxiliar com as definições compiladas. Nomes, descrições, custos, recargas, alcance, raio, dano/cura e demais campos comparáveis não apresentaram diferenças. A semântica dos contadores exige ressalvas abaixo.

Testes: suíte geral de 927 casos, zero falhas/erros, 51 ignorados em headless; complemento gráfico de 103 execuções, sem falhas/erros/skips. O agregado mais recente por classe reúne 927 casos aprovados em 248 classes. [Evidências](TASK-223-TESTES.md).

## Arquitetura reaproveitável

| Responsabilidade | Código confirmado | Observação |
|---|---|---|
| Catálogo e texto | RpgSkillCatalog / RpgSkillDescriptions | Imutáveis; fonte dos tooltips e do relatório. Domains contém previews históricos diferentes. |
| Regra autoritativa | RpgSessionWorld.skill / RpgSkillEngine.cast | Verifica aplicação, sequência, domínio, nível 15, alvo/geração, custo e recarga; cliente não envia dano. |
| Energia | RpgWorld.skillSpend / EnergyRules | Catálogo usa EN; stamina interna usa décimos. Regeneração segue o tick, não a animação. |
| Dano e recompensa | RpgWorld.skillDamage / SharedRpgEncounter | Reutiliza ledger de morte/contribuição, sem loot paralelo. |
| Colisão | CollisionService / MovementService / RpgCollisionProfile | Segmentos contínuos, limite do mapa e geometria própria de projéteis, mobs e jogadores. |
| Estado e efeitos | RpgSkillEngine Pending/Zone/Dot e campos efêmeros em RpgWorld/EnemyService | Root/stun/slow têm expiração; DoTs têm identidade do lançador e geração da vítima. |
| Protocolo | SkillProtocol / Protocol / LocalClient | Aplicação 7 negociada; comandos e snapshots sujeitos à revisão e limites. Mensagens antigas preservadas nos testes. |
| Entrada e HUD | GameInputRouter / NetworkSkillControls / GamePanels / GameHudStyle | Reutilizar; não criar novo roteador. Hotbar mantém quatro ações anteriores e cinco slots de domínio. |
| Apresentação | NetworkSkillPresentation / RpgNetworkClientLauncher / RemoteTimeline | Deduplicação por sequência, tempos de eventos, clips cacheados, histórico real dos dashes e ocultação da fase. |
| Ferramenta de treino | tools/SkillsLocalPreview.java | Dez escolhas de domínio, servidor sintético nível 20, troca de kit na janela. Não é campanha persistente. |

## Achados confirmados e riscos

### A-01 — Disponibilidade e rejeição ainda têm feedback incompleto

**Confirmado por inspeção; prioridade alta para TASK-224/230.** `NetworkSkillControls.refresh` considera nível, energia, cooldown, seleção e combo, mas não mede alcance/LOS nem recebe estado completo de stun/dash. Assim, um botão pode estar “Pronto” e o cast ser recusado corretamente pelo servidor. `send` informa envio/espera; `SkillProtocol.State` não inclui ACK/rejeição específico da intenção. Não confundir envio TCP com conjuração aceita.

Seleção distingue classes de ID e há integração visual/recipient, mas a linha de destinatário não mostra HP numérico do aliado. O retículo de área é limitado por alcance/bordas; não conhece toda a geometria autorada para pintar validade. Smartcast sai imediato, sem retículo pendente. É necessário definir feedback contextual dentro do roteador/HUD atual, mantendo o servidor como autoridade. Qualquer extensão de estado/mensagem precisa de proposta de compatibilidade antes de implementação.

### A-02 — Combo coletivo funciona; readiness e marcas visuais são limitadas

**Confirmado; prioridade alta para TASK-227.** Vento 4 marca todos os afetados com ID/geração e apoio; Vento 5 já é SELF, sem target, revalida alcance/visibilidade e consome o conjunto uma vez. Testes cobrem separados, não marcados e geração antiga. `state()` informa presença de marcas até expirar, sem filtrá-las todas por morte/alcance naquele instante; o cast filtra corretamente. Pode aparecer disponível e recusar quando não restar vítima elegível, sem gasto.

O snapshot expõe apenas booleano de checkpoint, sem lista de inimigos marcados. Não há marcação visual individual autoritativa. Não inferir IDs por proximidade ou por pixels; planejar representação compatível/negociada se esse requisito exigir dados novos.

### A-03 — Projétil dirigido e desenho linear podem divergir com alvo móvel

**Diferença confirmada no código; impacto visual em alvos móveis requer captura específica.** `Projectile.advance` recalcula a direção para a posição atual do alvo e testa o primeiro corpo real; o cliente interpola o evento inicial `from→to` até o impacto confirmado. O servidor está coberto por `targetedProjectileFollowsTheMovingEnemyAndPublishesItsRealContact`, mas o caminho intermediário curvo não é transmitido. Afeta cortes target, flecha target, Vento 2 e Sentinela. Não é evidência de dano por pixels nem de invasão de autoridade. Avaliar na TASK-225/229 antes de mudar trajetória ou protocolo.

### A-04 — Fase de raio: espera de materialização sem limite e fusível visual de cinco segundos

**Risco condicional de prioridade alta; não reproduzido nesta suíte.** O especial espera 32 ticks, aplica quatro pulsos uma única vez e só remove a intangibilidade quando `skillReappear()` encontra apoio livre. Se não encontrar, repete a espera sem repetir o dano; não existe limite explícito para essa espera. O cliente expira a representação após cinco segundos. A fonte comprova essa diferença de contrato; não comprova que o mapa atual permita forçar um bloqueio permanente.

Busca de reentrada pode chegar à varredura de uma grade do mapa e exige segmento reto livre de cenário. Não há medição dedicada desse pior caso. Tests existentes cobrem aliado sobre a origem, borda, parede, imunidade normal, término e clear, mas não toda a combinação de fase + portal + perda definitiva de evento + falta total de apoio.

**Gate:** exigir reprodução controlada, decisão sobre encerramento seguro e testes de ciclo de vida na TASK-227, com revisão de colisão na TASK-225/226. Se invulnerabilidade indevida, travessia ou estado persistente forem reproduzidos, bloquear progressão até correção autorizada. A TASK-223 não altera a imunidade já aprovada.

### A-05 — Sentinela não tem cinco tiros nem munição fixa

**Contrato confirmado, não regressão.** A janela é de 120 ticks; AGI altera a cadência e o ataque básico produz flechas target. Recast cancela sem novo custo. `mechanics.hits=1` representa ativação, e `sentryShots` é um campo wire legado que informa segundos restantes. O PDF explica a regra atual, mas contadores não podem ser somados como dano fixo por ativação. Cobertura específica inclui AGI, quatro segundos, imobilidade, cancelamento e liberação da trava.

### A-06 — Quantidade declarada não é sempre dano multiplicável

**Confirmado; prioridade para documentação e métricas da TASK-231.** Leque dispara sete raios com um `sharedHits`, portanto uma vítima/geração recebe no máximo um acerto por ativação. Chuva de flechas são cinco ondas; as cinco flechas visuais de cada onda não multiplicam dano. O texto genérico de múltiplos impactos do PDF não explicita suficientemente essa diferença do leque.

Fogo 2 tem contato de frente e trilha que renova queimadura sem novo dano direto; Fogo 4 pode ter contato da frente e dois pulsos de erupção. `hits=2` não resume sozinho todos os contatos de Fogo 4. Reaplicação de queimadura pode prolongar a janela efetiva além de uma duração isolada do status. Essas observações vêm do executor/testes, não são propostas de mudar números ou medições de DPS.

### A-07 — Corrente tem quatro impactos condicionais

**Contrato confirmado; ressalva de documentação.** Intervalo de oito ticks depois do primeiro contato, seleção estável de não visitados num raio de 130 e escala de repetição de 750/1000. Primeiro impacto inclui tempo de voo; alvo morto ou LOS inválida encerra a cadeia. O guia é compatível, mas “quatro” não significa garantia quando o alvo morre antes. Testes verificam quatro alvos e dano decrescente 20/15/11/8 no fixture sem bônus.

### A-08 — Purificação e DoTs têm escopos diferentes

**Escopo confirmado; matriz multijogador adicional pendente.** Purificação chama `skillClearControls()` no jogador e limpa root/stun/slow. Os DoTs desta extensão são agendados contra mobs, por chave lançador + vítima + geração + tipo; não há aplicação pública de Burn/Bleed de skills a jogadores nesse fluxo. Não anunciar purificação de qualquer efeito imaginável. Dois lançadores têm chaves distintas; reaplicação pelo mesmo lançador renova prazo, não empilha outro relógio. Cenários simultâneos dedicados e critérios de limpeza precisam ser consolidados na TASK-226/228.

### A-09 — Previews históricos de domínio divergem do catálogo executável

**Confirmado; prioridade documental para TASK-224/230.** `Domains.java` ainda diz que as técnicas são previews não executáveis. Traz textos como barreira/cura canalizada e possibilidade de Luz ferir qualquer raça ou Spirit atingir aliados. O executor atual das 40 skills é PvE, com suporte a jogadores e dano híbrido de Luz somente contra mobs; não implementa essas técnicas hipotéticas nem PvP a partir desses textos. Preservar IDs/raças, escolher o catálogo real como fonte e propor unificação de texto sem inventar habilidades.

### A-10 — Body slam separa fases visualmente; contato é imediato

**Confirmado; não é pose corporal completa.** Servidor desloca, aplica dano/stun e informa pegada/impacto no mesmo tick; cliente apresenta pegada por 200 ms e depois pancada. O efeito é de mãos/impacto sobre o personagem existente. Avaliar sincronização na TASK-229 sem afirmar que há animação completa de agarrar/lançar nem atrasar dano por conveniência gráfica.

## Comparação com o guia e regras atuais

O guia foi preservado. Possui os valores atuais e já descreve retorno de Vento sem alvo, imunidade do raio e Sentinela sem munição fixa. A matriz complementa o guia nos casos de quantidade, contato, status e cobertura. Campos comparáveis do XML auxiliar e do runtime: zero diferenças (`guide-vs-runtime.json`). Texto extraído diretamente do PDF em `guide-pages.json`/`guide-text.txt`; a extração é auxiliar e não substitui imagens/animação.

Não foram medidas eficácia de builds, DPS, HPS, FPS ou carga de servidor nesta TASK. Duração de teste não é benchmark. Resistência a status não foi adicionada. As regras de PvP, “party” e efeitos em aliados dos previews não foram assumidas como implementadas.

## Prioridades propostas

1. TASK-224: política contextual e critérios de feedback de alvo/alcance/validez, mantendo GameInputRouter; não mascarar recusas do servidor.
2. TASK-225/226: cenários de contato, estado e reentrada extrema, inclusive dois lançadores e reset por região/desconexão.
3. TASK-227/228: marcações autoritativas do combo, encerramento seguro da fase especial e suporte com HP/feedback de destinatário.
4. TASK-229/230: trajetória dirigida, sincronização de impacto e fonte única de texto, com arte existente e HUD preservada.
5. TASK-231/232: medir cenários equivalentes, apresentar proposta numérica separada e integrar somente o que for aprovado.

Detalhamento em [CORUJAO-IX-PLANO.md](CORUJAO-IX-PLANO.md). A execução dessas etapas depende da aprovação solicitada no plano diretor, não desta auditoria.

## Integridade, limites e reversão

Nenhuma fonte de produção, teste preexistente, ferramenta, asset ou PDF foi modificada pela auditoria. Baseline de 1.792 arquivos protegido por SHA-256, cópia dos fontes, diff anterior e Git inicial no checkpoint. Evidências produzidas por testes foram arquivadas e artefatos históricos restaurados. Não houve acesso a banco, migração, build privada, commit, push ou publicação. O status do monitor lido estava sem pendências/falhas listadas, mas com timestamp antigo; isso não comprova monitor ativo.

TASK-224 a TASK-232 permanecem não iniciadas. Radmin físico, hardware distinto, launcher nativo distribuído e homologação estética humana permanecem pendentes. Reversão desta entrega é documental, conforme TASK-223.md.
