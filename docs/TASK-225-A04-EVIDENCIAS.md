# TASK-225 — Investigação controlada do risco A-04

Referência: 03/11/2026, branch `beco`, HEAD `6d72c65`. Nenhuma regra de imunidade, duração ou materialização foi modificada. Os testes usam mundos efêmeros, sem banco, saves pessoais ou publicação.

## Resultado e limite da evidência

**A espera de materialização com imunidade persistente foi reproduzida em cenário artificial sem apoio válido.** Aos 180 ticks (seis segundos simulados), Quatro relâmpagos ainda conserva `skillIntangible` e `skillInDash`, recusa novas ações e ignora dano. Os quatro cortes foram aplicados uma única vez: 56 de dano no fixture, quatro eventos HIT, sem pulsos repetidos na espera e sem evento terminal.

A fonte não contém prazo máximo para essa espera. O teste observa uma janela finita de seis segundos e não pretende demonstrar tempo infinito por execução. A ausência de limite decorre também da inspeção de `RpgSkillEngine.LightningField.advance` e `RpgWorld.skillReappear`.

O cliente, exercitado separadamente por `Task225PhasePresentationTest`, deixa de ocultar o lançador aos 5.000 ms se o evento terminal não chegar; retransmitir o mesmo início não reinicia a fase. Portanto, o fusível visual pode terminar enquanto o servidor continua em fase e imune. **Não é prova de exploração no mapa distribuído:** a geometria de falta total de apoio é sintética e a ocupação foi introduzida pela fixture durante a fase.

## Reprodução isolada e limites externos

Arquivo: `server/src/test/java/com/projetok/rpg/Task225MaterializationTest.java`.

1. Criar mapa 960 × 640 com quatro obstáculos que deixam apenas uma câmara bruta `x=110..170`, `y=280..320`; usar o perfil existente `original-ground`, versão 1, raio 12 e meia largura de cenário 22.
2. Criar lançador de raio, nível 15, no apoio `(140,300)` e iniciar a skill SELF do slot 5.
3. Introduzir um aliado no mesmo apoio durante a intangibilidade. A câmara é pequena o suficiente para que o bloco de apoio do aliado cubra todos os pontos onde o lançador poderia materializar. Um mob durável no interior permite contar os quatro cortes.
4. Avançar exatamente 180 ticks, sem dormir, ler relógio de parede para gameplay ou criar timer. Assertar os estados e danos descritos acima.
5. Retirar o aliado do encontro e matar o mob pelo ledger real de `skillDamage`. No tick seguinte, verificar a reentrada no apoio agora livre, limpeza dos flags, evento terminal único e retorno da recepção de dano.

Os métodos de teste possuem limite JUnit de 20 segundos; os laços de avanço têm contagens fixas. Não há espera aberta por materialização. A execução Maven é controlada pelo processo externo da validação da TASK-225.

## Cenários cobertos

| Cenário | Verificação isolada |
|---|---|
| Área livre | Fase mantida até tick 31; reentrada no tick 32, mesma posição e apoio válido. |
| Parede próxima | Mesmo término normal; cápsula de cenário preservada. |
| Borda do mapa | Apoio `(18,300)`, sem sair dos limites wire históricos. |
| Obstáculos próximos | Reentrada sem alterar raio/hitbox ou atravessar cenário. |
| Aliado sobre a origem | Busca local encontra outro apoio, preserva a posição do aliado e a parede próxima. |
| Ausência artificial de apoio | Imunidade e fase persistem após seis segundos, com somente quatro cortes. |
| Cenário volta a permitir | Liberação de ocupantes seguida de materialização no próximo tick; regressão dedicada. |
| Remoção explícita e clear | Flags e eventos do lançador antigo são descartados. |
| Perda do participante | `engine.tick` sem o mundo no mapa de participantes limpa a fase antiga. |
| Substituição de sessão | O mundo antigo é limpo e o novo não herda a fase ou as recargas. |
| Mudança de região | Portal real do mundo sintético e próximo tick do engine encerram a execução antiga. |
| Evento terminal perdido | Fase visual expira em cinco segundos e não é reativada por início duplicado. |
| Revisão, região, desconexão, ausência/morte do ator no cliente | Limpeza imediata da fase visual. |

São testes do engine e da apresentação com objetos reais. A perda/substituição de sessão acima não representa uma queda física de rede; a validação TCP da TASK-225 é registrada separadamente. O caso extremo sem apoio não foi induzido em rede/Radmin nem em mapa de produção.

## Custo da busca

`skillReappear` testa primeiro a origem; depois, 32 direções para cada um dos 32 raios entre 4 e 128 (1.024 sondas); por fim, a grade do mapa em passos de quatro unidades. Com raio 12 e mapa 960 × 640, a grade tem 235 × 155 = **36.425 pontos**. No caso totalmente bloqueado, cada tentativa percorre todos eles, além da origem e das sondas radiais: **37.450 consultas de posição por tentativa**. Essa contagem é derivada dos limites dos laços atuais, não de instrumentação de produção.

A primeira rodada focalizada produziu, depois de três aquecimentos e 15 amostras:

| Chamada | Mínimo | Mediana | Máximo |
|---|---:|---:|---:|
| Sem apoio válido, busca completa | 0,794 ms | 0,943 ms | 3,411 ms |
| Origem já livre | 0,004 ms | 0,007 ms | 0,025 ms |

Na repetição após corrigir a fixture, a busca completa apresentou **mediana de 1,138 ms e máximo de 31,664 ms**, enquanto a origem livre teve **mediana de 0,025 ms**. A diferença entre as rodadas demonstra a variância da medição; o máximo observado ficou próximo do intervalo nominal de um tick a 30 Hz (33,333 ms), sem estabelecer que esse atraso ocorrerá sob carga real.

Os tempos incluem a chamada e a asserção de teste; dependem de JIT, máquina e ordem da suíte. Não são percentis de produção nem benchmark de carga concorrente. A espera repete a busca por tick, sem cache ou orçamento temporal explícito. Não extrapolar esses valores como garantia de desempenho para todos os mapas ou número de obstáculos.

Dados brutos da rodada mais recente: [a04-measurements.txt](skills-preview-task225/a04-measurements.txt). As duas rodadas descritas acima estão preservadas em [focused-first.log](../.task-checkpoints/CORUJAO-IX/TASK-225/focused-first.log) e [a04-fixture-fixed.log](../.task-checkpoints/CORUJAO-IX/TASK-225/a04-fixture-fixed.log), com os XML no checkpoint. O arquivo de medições recebe novos tempos quando a suíte é repetida; os números da tabela identificam especificamente a primeira rodada.

## Ocorrência de fixture durante a validação

Na primeira rodada focalizada, o cenário comprovou todos os estados da espera e a contagem única de dano, mas falhou na asserção de reentrada após a liberação. A fixture havia atribuído `enemy.hp=0` diretamente, deixando `readyTick=0`; o encontro respawnava o mob no tick 181 e ocupava novamente a câmara. A correção de teste usa `world.skillDamage` para registrar a morte e agendar o respawn pelo ledger real. Não houve alteração do engine para fazer o teste passar. **A repetição específica passou os 12 casos de `Task225MaterializationTest`, sem falhas, erros ou skips**, conforme `a04-fixture-fixed.log`, incluindo a reentrada no tick seguinte à liberação. A suíte final fica registrada no relatório geral de testes da TASK-225.

## Decisão e proposta de encerramento seguro

**A investigação está concluída; a mudança de comportamento do A-04 permanece bloqueada para autorização específica do desenvolvedor.** O pedido da TASK-225 proíbe alterar automaticamente imunidade, duração ou materialização. Não iniciar a TASK-226 nem promover etapas que dependam de o risco estar resolvido. A espera atual e seu custo permanecem registrados.

Proposta concreta para aprovação posterior:

1. Propor **60 ticks adicionais de espera** após os 32 ticks existentes: até dois segundos de tolerância para ocupação temporária, com teto total de 92 ticks (aproximadamente 3,067 segundos) desde a conjuração. Esse valor é uma proposta de regra de produto para aprovação posterior, **não foi implementado**. O custo possível são até 60 novas buscas completas por execução bloqueada, além da busca inicial de término; não multiplique a mediana isolada como previsão confiável de carga.
2. Até o limite, manter a procura de apoio com as regras atuais, sem repetição de dano, empurrão, teleporte ou redução de hitbox. Avaliar orçamento de busca por tick em alteração separada, com medições equivalentes.
3. Se o limite vencer e não houver apoio válido, **encerrar a participação do lançador na sessão e remover sua execução/corpo** pelos caminhos existentes de saída; permitir retorno somente por admissão em spawn validado. Isso evita tornar um corpo vulnerável materializado dentro de obstáculo ou manter imunidade indefinida. Deve haver mensagem clara ao jogador.
4. Caso seja exigido um motivo terminal novo no cliente ou uma descrição explícita da fase em snapshot, preparar extensão negociada do protocolo antes de implementá-la. A aplicação 7 existente não fornece o estado autoritativo de intangibilidade nem um identificador de execução por skill.
5. Testar o limite, saída/retorno, cooldowns, ledger, evento terminal perdido e dois clientes; considerar como alternativa uma área de recuperação previamente validada somente se essa regra for expressamente aprovada. Não escolher coordenadas arbitrárias para materializar.

A proposta tem custo de experiência: pode retirar o participante de uma sessão em condição extrema. Preserva a validade física e oferece um término finito. A alternativa de aguardar indefinidamente conserva a regra atual, mas mantém o risco comprovado. Nenhuma dessas mudanças foi implementada nesta TASK.

## Reversão e compatibilidade

Esta parte da entrega adiciona somente dois arquivos de teste, esta análise e o TXT de medições. A reversão é remover esses artefatos específicos após comparar mudanças posteriores. Não restaurar globalmente o checkout nem apagar checkpoints anteriores. Não houve edição de `RpgWorld`, `RpgSkillEngine`, catálogo, protocolo, sprites, HUD, saves, offline/Coop, launcher ou World Studio nesta investigação.
