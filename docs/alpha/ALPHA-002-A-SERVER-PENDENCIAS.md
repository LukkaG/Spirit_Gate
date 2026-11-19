# ALPHA-002-A-SERVER — Pendências, riscos e reversão

## Situação

A correção de servidor substitui a necessidade de materialização de `thunder_5`: corpo permanece sólido e fixo, imunidade/controle têm prazo de 32 ticks, encerramento não remove a sessão. Nenhum timeout de 92 ticks ou recuperação alternativa B foi integrado.

Isso resolve a causa autoritativa da espera A-04 para esta skill. Não equivale a homologação completa da alpha, do renderer ou do servidor comercial.

## Pendências explícitas

| Item | Estado/impacto | Próximo trabalho necessário |
| --- | --- | --- |
| Frente visual | Não executada; cliente antigo ainda possui fusível de cinco segundos. | Aplicar o [contrato visual](ALPHA-002-A-CONTRATO-VISUAL.md) em tarefa própria, com autorização específica para arte quando necessária. |
| Cancelamento explícito/geração no wire | Não existem. Sem alteração das aplicações 6/7. | Avaliar extensão negociada somente se os dados atuais não atenderem ao aceite visual. |
| Reconexão e recargas/progresso | A sessão corrente conserva recursos e cooldown; reconectar continua criando personagem efêmero. Não é possível prometer continuidade entre conexões com o contrato atual. | Escopo próprio de identidade e continuidade, sem conectar automaticamente ORG-03, D33 ou persistência 193–196. A proibição de reset entre reconexões permanece não atendida globalmente. |
| Recargas em troca de revisão | O fluxo preexistente chama `skills.clear()`; esta entrega confirma que a fase não vaza, mas não implementa transferência de cooldowns entre mundos. | Decisão e contrato específicos caso seja exigida preservação nessa transição. |
| Resistências | Reutiliza elegibilidade real (`exactTarget`), região, HP, bloqueio do chefe e visibilidade. O caminho observado de STUN não possui sistema genérico de resistência. | Não anunciar resistência percentual ou imunidade inexistente; revisar se o catálogo de inimigos adquirir esse contrato. |
| Carga e desempenho | A técnica deixou de executar busca global de materialização. Controle consulta no máximo as participações do encontro por inimigo. | Medir tick completo sob carga representativa antes de definir SLA. |
| Físico/Radmin/launcher nativo | Não executados. | Dois PCs, sessão prolongada e validação de distribuição em tarefa própria. |
| Offline/Coop e interface | Cobertura headless relevante; não houve homologação manual desses modos ou da GUI. | Smoke manual dirigido sem transportar o redesign para esses modos. |
| Ensaios históricos R1/R2/R3 | Planos e laboratórios de retirada representam o contrato anterior; não foram reescritos. | Usar como histórico, sem inferir gate de 92 ticks ou recuperação implementada. |

## Incidente na seleção de testes

A primeira rodada ampliada incluiu indevidamente `NetworkSkillPresentationTest`, que exporta galerias de imagens. Foram gerados **12 PNGs exclusivamente no diretório temporário de testes**. A rodada foi interrompida, os 12 arquivos temporários removidos após verificar os caminhos, e seus nomes/hashes registrados em `.task-checkpoints/ALPHA-002-A-SERVER/galeria-temporaria-removida.json`. Nenhum asset do checkout foi editado/substituído. A suíte final exclui essa classe; nenhuma frente visual foi implementada. O incidente não é apresentado como cumprimento da proibição de gerar imagens.

## Rollback restrito

1. Encerrar apenas a instância local de ensaio antes de trocar o código; não realizar reversão em uma simulação viva com execução pendente.
2. Conferir os hashes/diffs atuais contra o baseline `.task-checkpoints/ALPHA-002-A-SERVER/baseline.json` e inspecionar alterações posteriores do desenvolvedor.
3. Reverter manualmente somente os hunks desta entrega em `RpgWorld`, `RpgSkillEngine`, `SharedRpgEncounter`, `RpgSkillCatalog`, `RpgSessionWorld` e nos dois testes antigos atualizados; remover os três testes ALPHA novos somente se não tiverem alterações posteriores.
4. Preservar integralmente o patch preexistente em `LocalSimulationServer`, ferramentas, documentos, ORG-03 e demais trabalho local. Não usar reset/clean, sobrescrita em massa nem apagar evidências.
5. Recompilar em cópia isolada. O rollback restaura também o comportamento defeituoso anterior de espera; A-04 volta a bloquear os gates correspondentes.

Não existe migração de wire, banco ou save nesta entrega. Não foi realizado commit, push, release ou publicação. A próxima frente visual não deve ser iniciada automaticamente.
