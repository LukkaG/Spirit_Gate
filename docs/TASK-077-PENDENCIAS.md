# TASK-077 — Pendências classificadas

Nenhuma falha crítica ou alta foi comprovada na suíte atual. Cada item abaixo registra limite, risco ou divergência; não houve correção automática.

## P077-01 — Future pendente após pausa e encerramento

- Severidade: Média. Arquivo: `server/src/main/java/com/projetok/prototype/rpg/RpgRuntime.java`, métodos command/action/advanceOneTick/close.
- Descrição: runtime pausado aceita action, mas o tick pausado não consome a fila; close não resolve/cancela futures enfileirados. Integração que aguarde esse future sem timeout pode ficar esperando indefinidamente.
- Reprodução: iniciar runtime; esperar saveAndPause em arquivo temporário; chamar action; close; consultar isDone. Código completo em AuditProbe.java.
- Esperado: recusa explícita ou término excepcional/cancelamento da operação no encerramento. Encontrado: `FUTURE_APOS_PAUSA_E_CLOSE_COMPLETO=false`.
- Evidência: probe-second.log, exit 0 da sonda confirma reprodução. Não foi comprovada perda de save nem travamento no fluxo normal da janela atual, que desabilita o frame durante save/saída.
- Recomendação: em tarefa futura autorizada, definir contrato de pausa/encerramento e concluir/cancelar operações pendentes; manter teste de regressão específico.

## P077-02 — Consolidação sobrescreve histórico

- Severidade: Baixa. Arquivo: `tools/consolidar_expansao.py`, reports; `docs/CONTINUIDADE-TASK-037_076.md`.
- Reprodução: inspecionar writes de reports e o comando de continuidade. Executá-lo literalmente na raiz reescreve inventário, relatório, PDF, continuidade e roadmap anteriores.
- Esperado para TASK-077: novos registros próprios, histórico preservado. Encontrado: destinos fixos anteriores, contornados por DOCS isolado.
- Evidência: código original e consolidacao.log/output no checkpoint. Não foi feita a sobrescrita para demonstrar o risco.
- Recomendação: futuramente oferecer destino explícito ou modo de verificação sem escrita.

## P077-03 — Homologação física e humana pendente

- Severidade: Média (validação com contorno por testes locais, sem liberação pública). Arquivos: `docs/TASK-071.md`, `docs/TASK-074.md`, roteiros RPG/Coop e LocalSimulationServer.
- Reprodução: comparar testes TwoProcessTest/NetworkFightIntegrationTest (loopback) com roteiro de duas máquinas; inspecionar CoopWorld, que tem só um mapa e não tem travel online.
- Esperado para alegar LAN homologada: duas máquinas privadas e avaliação humana real. Encontrado: somente automação local; transições online inexistentes, sem persistência/autenticação/TLS de produção.
- Evidência: XMLs multiplayer e código auditado. TASK-071: homologação física LAN pendente. TASK-074: transições online inexistentes, além dos limites já conhecidos de rede.
- Recomendação: executar roteiro humano em tarefa autorizada; não declarar MMORPG completo ou expor esse servidor publicamente.

## P077-04 — Contrato artístico limitado ao atlas atual

- Severidade: Baixa. Arquivos: ValidatedSpriteAtlas, DirectionalAnimation, RpgLauncher, NetworkClientLauncher, RpgMaps.
- Reprodução: seguir chamadas setPresentation; apenas entrada da arena conecta atlas. Conferir recortes fixos 64×64/quatro direções e mapas sintéticos.
- Esperado para integrar arte variada: contrato por recurso e integração explícita por entidade. Encontrado: contrato específico sem integração automática no RPG/Coop e sem importador genérico de tilesets.
- Evidência: TASK-077-CONTRATO-SPRITES.md e testes VisualPackage/DirectionalAnimation/Python.
- Recomendação: combinar dimensões/direções/pivôs com responsável antes de integrar; aprovar assets por hash. Não converter toda arte para mesma grade por presunção.

## P077-05 — Divergência de representação no checkpoint histórico

- Severidade: Baixa. Arquivos: 178 entradas do TASK-076-after.json, lista completa em checkpoint-076-comparison.json.
- Reprodução: calcular SHA-256 bruto e repetir com CRLF→LF; em TASK-016-reproduzir-smoke.ps1 remover também BOM UTF-8.
- Esperado: comparação documentada sem presumir corrupção. Encontrado: 177 diferenças somente CRLF e uma BOM+CRLF; nenhum conteúdo restante divergente. 102 hashes do inventário de alterações coincidem em bytes.
- Evidência: before.json, comparação histórica; core.autocrlf=true. Narrativa “sem Git” diverge da existência de Git neste checkout.
- Recomendação: manter comparação bruta e normalizada identificadas; não regravar fontes só para fazer hash antigo coincidir.

## P077-06 — Falhas do tick pouco visíveis — interface

- Severidade: Média; risco por inspeção, sem ocorrência espontânea nesta execução. Arquivos: RpgRuntime.start/failure e RpgLauncher timer/fechamento.
- Reprodução por inspeção: uma RuntimeException no tick define failure e encerra loop; procurar consulta a runtime.failure no launcher (ausente).
- Esperado: interface informar interrupção e oferecer ação coerente de recuperação. Encontrado: apresentação pode ficar congelada; salvamento seguinte — recusado como simulação encerrada/fila cheia. A suíte normal passou.
- Evidência: caminhos de código, sem alegar teste de falha injetada ou regressão comprovada.
- Recomendação: definir e testar UX de falha em tarefa futura. Não alterar runtime nesta auditoria.

Limites adicionais de arquitetura documentados em TASK-077-ARQUITETURA: lock de save por instância (sem coordenação multiprocesso), ausência de fsync explícito, geometria fixa e domínio agregando regras. São restrições de uso/projeto; não houve teste de queda elétrica ou disputa de dois processos sobre o mesmo save, nem se afirma corrupção comprovada.
