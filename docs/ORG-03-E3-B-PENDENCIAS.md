# ORG-03-E3-B — Pendências e decisões necessárias

16/11/2026. **Pronta para revisão documental; não pronta nem autorizada para
implementação operacional.** Nenhum novo cluster, backup, restore, SQL,
failover ou processo operacional foi iniciado. Responsável: Lucas.

## Pendências de escopo e contrato

| ID | Pendência / decisão solicitada para etapa futura | Responsável proposto | Bloqueia |
| --- | --- | --- | --- |
| P01 | Ratificar o recorte E3-B como decomposição da E3/R6. Não foi encontrada especificação detalhada anterior desse rótulo | Lucas | Implementação do desenho como requisito aprovado |
| P02 | Escolher versão/topologia, método de backup, alvo de recuperação, fronteira de confirmação, perfil WAL, RPO/RTO e o que constitui perda aceitável ou reprovação; nenhuma garantia numérica de produção proposta | Lucas + infraestrutura; D12 | Backup/restore e conclusão de durabilidade |
| P03 | Definir cobertura consistente de DB, roles/metadados, intenções, manifestos, archive/índice e dependências; tratar confirmação S1 ausente em R | Lucas + revisão técnica; D26 | Reconciliação econômica completa |
| P04 | Aprovar fonte de verdade e revogação que não retrocedam junto ao backup; não foi escolhido mecanismo de era, eleição, quorum, lease ou promoção | Lucas + arquitetura/infraestrutura; D33/D07 | B06/B08 positivos, admissão/escrita após restore e recuperação de liderança |
| P05 | Aprovar contrato experimental de intenções/manifestos pendentes e limites de reexecução; manter payload/resultados originais e não repetir RNG | Lucas; D03/D19/D26 | B03/B05/B07 e qualquer correção automática |
| P06 | Definir quais cenários são obrigatórios e quais exigem fase/rodada própria; se D33 não estiver aprovado, identificar negativos e positivos bloqueados | Lucas | Fechamento futuro da E3-B |

## Pendências de implementação e operação

| ID | Condição ainda não atendida | Evidência futura exigida |
| --- | --- | --- |
| P07 | Autorização para implementar runtime E3-B isolado | Aceite B-A2; cópias revisadas, diferenças frente ao R3, testes estáticos/simulados rotulados e hashes antes/depois; nenhuma edição do R3 |
| P08 | Ficha operacional final | B-A3: responsável/data, novo run-id, raiz/subcaminhos absolutos, endpoints e quotas; proposta no plano não está reservada nem sondada |
| P09 | Autorização de criação/backup/restore e consultas | B-A4: binários existentes, método, fontes/destinos, objetos, número de clusters/cópias/starts, SQL experimental permitido e limites; sem instalação |
| P10 | Revisão de isolamento/encerramento do novo ambiente | Identidade SO+SQL por instância, handles originais, ACL de segredos, ausência de port fallback, verificação de links/raiz, quotas incluindo WAL, stop fast e ausência antes de descarte; B-A6 |
| P11 | Orçamento real de disco/tempo/conexões | Demonstrar viabilidade do backup escolhido; confirmar espaço livre e disponibilidade somente após autorização operacional; sem elevar quotas automaticamente |
| P12 | Evidência de ensaio real | Todos B01–B10 estão NÃO EXECUTADOS; nenhum resultado estático desta entrega substitui o ensaio. Sem prova do mecanismo aprovado, D33 permanece bloqueado |

Ficha candidata, sujeita a substituição e aceite explícito:

- Run-id: `<AAAAMMDD>-lucas-e3b-<nonce-novo>`, ainda inexistente.
- Raiz: `C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3b/<run-id-novo>`;
  origem `source-data`, alvo `restored-data`, backup/history/evidence/secrets.
- Portas candidatas: 127.0.0.1:55437 e :55438; :55439 somente com relay aprovado.
  Nenhuma disponibilidade verificada; nenhuma porta antiga será reaproveitada.
- Proposta máxima: dois clusters, um backup/restore, quatro starts totais,
  dois reinícios limpos, zero crashes/failovers; oito personagens/dois owners,
  64 intenções, três workers simultâneos/32 totais, seis conexões agregadas,
  três tentativas/intenção, payload 16 KiB, 120 s/caso e 20 min totais,
  1 GiB total/64 MiB logs, pelo menos 4 GiB livres. **Não autorizada.**

## Limitações que não se resolvem nesta etapa

O aceite sintético E3-A-R3 não resolve identidade/posse reais, retirada do ator,
clock durável, gameplay, scripts de combate, projeções de sprites ou dados legados.
Nenhuma proposta deve mudar D33 ou escolher política econômica sem decisão própria.
Um oráculo externo do ensaio não se torna serviço de autoridade por conveniência.
Não há implementação produtiva de restore/eleição/failover homologada por esta
revisão. C08 testou disponibilidade de arquivo, não recuperação de backup.

Os incidentes e relatórios das rodadas E3-A/R1/R2 continuam históricos. Esta
entrega não reabre nem modifica os run-ids anteriores. O run-id E3-A-R3 foi
consumido, encerrado e descartado; não há data a reutilizar.

## Próximo passo submetido à revisão

Revisar [plano](ORG-03-E3-B-PLANO.md),
[matriz de riscos](ORG-03-E3-B-MATRIZ-RISCOS.md) e
[critérios/aceites B-A1 a B-A6](ORG-03-E3-B-CRITERIOS-ACEITE.md).
Depois das decisões pertinentes, uma ordem específica poderá autorizar apenas
a implementação isolada ou uma operação precisamente delimitada. A ausência de
resposta não constitui aprovação. Até lá, não iniciar cluster, SQL, restore,
failover, integração E4/E5, ORG-04, TASK-226, A-04, commit/push ou publicação.
