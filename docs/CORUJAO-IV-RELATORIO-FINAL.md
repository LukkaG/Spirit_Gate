# Corujão IV — encerramento técnico experimental TASK-159

Rodada encerrada na TASK-159. TASK-134 permanece VALIDADA COM RESSALVAS; relatórios individuais e pacote intermediário da TASK-149 foram preservados. Não houve push, publicação ou alteração de saves pessoais. Não iniciar TASK-160.

Entrega privada: `dist/SpiritGate-CorujaoIV-studio-0.159.0-rc2-20261023T033925Z-1d5d45ef.zip`, com manifesto SHA-256 separado, Java integrado, Studio, RPG offline, servidor experimental e dois clientes. Abra `Iniciar-multiplayer-local.cmd` após extrair. Roteiros em `CORUJAO-IV-HOMOLOGACAO.md` e `CORUJAO-IV-HOMOLOGACAO-MULTIPLAYER.md`.

| Tarefa da expansão | Estado técnico |
| --- | --- |
| 150 | Arquitetura e contratos auditados, Coop preservado |
| 151 | Backend RPG sintético autoritativo e servidor local |
| 152 | Transporte, mensagens e validações compatíveis |
| 153 | Dois jogadores, movimento, direção e renderização remota |
| 154 | Parcial explícito: combate RPG compartilhado indisponível; ações recusadas |
| 155 | Desconexão, limpeza e reconexão como sessão nova |
| 156 | 180 camadas PNG temporárias, catálogo e hashes |
| 157 | Composição visual e preview, sidecar sintético separado |
| 158 | Aparência sincronizada por IDs e negociação com peer legado |
| 159 | Regressões, executáveis privados, inventário e encerramento |

Validação final: 465 testes Java aprovados em ambiente gráfico, zero abortados/falhos/ignorados. Headless: 457 aprovados, oito abortados ambientais, zero falhos/ignorados. Python: 30 aprovados. Servidor e dois clientes nativos observaram dois jogadores e aparências distintas; três saídas normais. Studio nativo abriu/encerrou e o Java integrado passou o self-check. Fontes recompiladas após corrigir encerramento prematuro durante carregamento assíncrono do cliente. Logs e tentativas incompletas preservados na TASK-159.

Localhost foi validado. LAN física permanece pendente. Combate RPG compartilhado, contas e persistência online não foram implementados; segurança de produção não homologada. As sprites são provisórias e não concedem atributos. Os 486 arquivos originais de assets permanecem idênticos por SHA-256. Protocolo mantém envelope V1 e mensagens históricas; extensão cosmética negociada pela aplicação 2 envia State V1 aos peers legados.

Os resultados de Studio/Quest Builder e desempenho abaixo registram o marco intermediário. Contagens históricas não substituem as contagens finais acima.

World Studio recebeu tema escuro, contraste de biblioteca/camadas, preferências de favoritos e recentes, miniaturas ajustáveis, preview ampliado, prévia de pincel e cancelamento de gestos. Comandos e documentos V1 foram preservados.

Quest Builder usa os catálogos reais, documento `.sgquest` V1, dependências e condições de raça, nível e domínio. Missões são executadas pelo QuestService e RpgWorld reais, em sessão sintética. Recompensas são integralmente preparadas antes da publicação, com XP fracionária exata e conclusão única. O save V2 não recebeu flags ou IDs autorados.

Mapas compatíveis receberam validação explícita de spawn, minimapa autorado e culling. Mapas maiores continuam limitados conforme CORUJAO-IV-PENDENCIAS.md. Shunpo ofensivo valida o trajeto com o raio físico existente; velocidade, alcance, custo e níveis não foram alterados.

Primeiro salvamento do cenário local de 254 assets/12.000 elementos teve mediana de 8228,93 ms antes e 243,00 ms depois, com validação, backup e atomicidade preservados. A metodologia e as demais métricas, incluindo variações para cima, estão documentadas em CORUJAO-IV-OTIMIZACOES.md.

Na TASK-148: 447 testes Java integralmente aprovados em execução gráfica; headless teve 439 aprovados e oito abortados ambientais, zero falhas e zero ignorados. Os 30 testes Python passaram. Launcher recompilado e `--verify-install` confirmou quatro raças, dez domínios e schema V2. Resultados finais da TASK-149 e do pacote constam no relatório individual e nos arquivos de evidência.

Cada tarefa possui documentos, logs, diffs e checkpoints anteriores/posteriores. A TASK-134 permanece validada com ressalvas. Não houve push, publicação, alteração de schema de personagem, pagamento, restauração de dependências removidas ou substituição de imagens originais. Homologação artística e física permanecem humanas.
