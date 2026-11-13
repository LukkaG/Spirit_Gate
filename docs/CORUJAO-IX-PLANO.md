# Corujão IX — plano técnico e gates

Plano inicial de 02/11/2026 derivado do documento do responsável, preservado integralmente em `.task-checkpoints/CORUJAO-IX/TASK-223/user-directive.txt`. Somente a TASK-223 foi autorizada para esta primeira execução; este plano não inicia as próximas tarefas.

## Objetivo e limites

Refinar as 40 skills existentes sem substituir sua identidade, motor, roteador, pipeline ou protocolo por conveniência. Preservar raças/domínios, quatro ações antigas da hotbar, progressão, básico, Shunpo, inventário, missões, IA, colisão, World Studio, saves, launcher e offline/Coop.

Sem balanceamento numérico aplicado, arte nova, dependência instalada, protocolo incompatível, build privada ou publicação sem as autorizações correspondentes. Somente o responsável pode aprovar valores novos; a TASK-231 produzirá proposta, não alteração automática.

## Sequência proposta a partir da auditoria

| Etapa | Escopo técnico proposto | Evidência/aceite antes de avançar |
|---|---|---|
| TASK-223 | Congelar baseline, matriz, contratos, testes e riscos | Entrega atual; revisão do responsável antes de TASK-224. |
| TASK-224 | Mapear política contextual no GameInputRouter; seleção e feedback de alcance/LOS; marcadores de área e atalhos atuais | Testes TAB/SHIFT+TAB, SPACE, ENTER, ESC e remapeamento em gameplay, mira, texto e menus; dois clientes. Propor dados adicionais antes de mudar contrato. |
| TASK-225 | Matriz de trajetórias, primeiro contato, múltiplos corpos, parede fina, fronteira e exceções de dash | Dano uma vez por contato/geração, sem dano por sprite; prova TCP; investigar A-03 e reentrada de A-04. |
| TASK-226 | Contrato de root/stun/slow/DoT, reaplicação, múltiplos lançadores, morte/região/desconexão e purificação | Testes simultâneos e limpeza, sem inventar resistência/imunidade; documentar diferenças do escopo PvE. |
| TASK-227 | Combo coletivo e marcas inválidas/visuais; corrente; Sentinela; fase de raio e múltiplos impactos | Cinco segundos/gerações corretos; plano de representação de marcas; prova de encerramento seguro da fase extrema e dos pulsos. |
| TASK-228 | Seleção/HP de aliados, cura própria, área, dano ofensivo a mobs, purificação e revive | Dois clientes com HP máximo, aliado desconectado, corpo obstruído, região e duplicação/repetição. |
| TASK-229 | Reusar sprites aprovadas; alinhar projéteis dirigidos, corpo, pivô, fases e descarte | Capturas/vídeos de casos móveis; clareza sobre efeito versus animação corporal; sem decodificação por frame ou partículas ilimitadas. Arte nova requer autorização/definição. |
| TASK-230 | Disponibilidade real, estado impeditivo, retorno da rejeição e tooltips do catálogo; unificar previews antigos | Nove ações continuam acessíveis, navy/ciano, teclas remapeadas, foco, resize; avaliar versão/negociação se forem necessários novos dados. |
| TASK-231 | Medir oito estilos em cenários comparáveis e analisar suporte separadamente | Dano/ativação e janela, EN/dano, HPS/EN/cura, controle, alcances, oportunidades e custos de CPU/render. Proposta de valores e riscos para aprovação; não aplicar. |
| TASK-232 | Integrar melhorias aprovadas e repetir regressões de todos os sistemas | TCP local de dois clientes, reconexão, região, saves/assets, protocolo, offline/Coop; build privada somente com qualidade e autorização. |

Cada tarefa deverá receber seu próprio plano, testes e relatório antes/depois de executar. Estes resumos não afirmam conclusão das próximas etapas.

## Gates

- **A (223/224):** auditoria está entregue; conjuração/seleção a validar após aprovação da TASK-224. Gate ainda não fechado.
- **B (225/226):** contatos e estados seguros; cenários extremos não podem ser substituídos por capturas estáticas.
- **C (227/228):** combos, suporte e fase especial coerentes. A-04 exige decisão de encerramento seguro se o cenário extremo confirmar a condição.
- **D (229/230):** arte existente e HUD integradas com input preservado.
- **E (231/232):** métricas, aprovação numérica quando aplicável e regressões finais. Não equivale a publicação.

Falha crítica reproduzida de dano duplicado, invulnerabilidade indevida, travessia de obstáculo, estado inválido persistente ou corrupção bloqueia progressão. Registrar evidência e pedir decisão quando a correção exigir regra nova/protocolo/escopo maior.

## Checkpoints e reversão

Criar um diretório próprio por tarefa sob `.task-checkpoints/CORUJAO-IX/`, capturar Git/hash/diff e preservar arquivos não rastreados. Não sobrescrever Corujões VII/VIII. Reverter somente o diff daquela etapa, sem reset/clean. Toda ampliação incompatível de protocolo, migração ou publicação exige autorização específica.
