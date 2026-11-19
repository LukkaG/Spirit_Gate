# ORG-03-E3-B-A1 — Comparativo de métodos de backup

Revisão iniciada em 16/11 e concluída em 17/11/2026. **Análise para decisão; nenhuma alternativa aprovada ou executada.**
Alvo possível: laboratório sintético PostgreSQL, sem produção e sem escritores
econômicos no destino recuperado. D12/D26/D33 permanecem independentes.

## A — Backup lógico

Uma exportação consistente por banco permite reconstruir schema/dados e testar
o conjunto lógico. Seleção parcial exige cuidado com dependências e objetos
omitidos. Não preserva identidade física, posições WAL ou uma imagem binária;
não permite recuperar commits posteriores mediante aplicação de WAL ao dump.
[Fonte primária: pg_dump, série 18](https://www.postgresql.org/docs/18/app-pgdump.html).

| Aspecto | Avaliação para a E3-B |
| --- | --- |
| Abrangência | Tabelas, schemas, funções, constraints, índices, sequências, owners/ACLs e demais objetos selecionados do banco; inventariar opções e exclusões. pg_dump sozinho não inclui roles/tablespaces globais |
| Globais | Exigem material complementar aprovado. pg_dumpall contempla bancos e globais, mas chamadas por banco não estabelecem por si só um corte global simultâneo; congelar alterações relevantes no ensaio |
| Histórico externo | Archive/índice externo e manifestos precisam de corte coordenado e inventário próprio; não aparecem automaticamente no dump |
| WAL | Não precisa de arquivo WAL como entrada para restaurar o dump; o servidor ainda usa WAL internamente. R não ganha S1 por replay do WAL da origem |
| S0/S1/R | S0 é o snapshot lógico escolhido, não o fim do arquivo de dump; S1 tem confirmações adicionais; R deve equivaler semanticamente a S0 e aos complementos aprovados |
| Complexidade | Exportação/restauração de objetos e ordem de dependências, privilégios e erros SQL; maior facilidade para examinar apenas dados sintéticos, maior risco de omissão seletiva |
| Evidência | Inventário de objetos/roles/ACLs/versões, corte e barreiras; stderr e erros de restauração; comparação lógica completa e histórico externo, não igualdade binária de data |
| Não demonstra | Recuperação física/WAL/PITR, failover, autoridade global ou RPO zero até S1; migração entre versões não é objetivo desta rodada |

[Fonte dos objetos globais: pg_dumpall](https://www.postgresql.org/docs/18/app-pg-dumpall.html).
Credenciais exportadas seriam segredos novos do laboratório: não devem entrar
em evidência pública. A política de cópia/reemissão deve ser explícita e
registrar alterações, sem importar roles ou senhas do PostgreSQL :5432.

## B — Backup físico com parada consistente

Copiar o cluster inteiro depois de parada comprovada oferece um corte físico
delimitado. Bloquear novas conexões sem parar não torna uma cópia comum
consistente. Não se restauram tabelas isoladas por cópia de arquivos. Todos os
tablespaces e caminhos externos usados devem estar abrangidos; a proposta
inicial recomenda não usar tablespaces externos.
[Fonte primária: backup de arquivos, série 18](https://www.postgresql.org/docs/18/backup-file.html).

| Aspecto | Avaliação para a E3-B |
| --- | --- |
| Abrangência | Cluster completo, bancos/schemas/dados/catálogos/roles, controles transacionais e pg_wal necessários ao conjunto; configuração fora de data continua exigindo inventário |
| Histórico externo | Arquivo de recibos e manifestos devem estar quiescentes e selados no mesmo corte lógico; parada do PostgreSQL não para um arquivador externo por si só |
| WAL | Preservar os arquivos próprios do conjunto físico. Não depender de WAL posterior a S0 nem prometer PITR nesta variante |
| S0/S1/R | S0 é o corte limpo após parada e quiescência; copiar S0, reiniciar origem para produzir S1; R deriva apenas da cópia imutável S0 e de seus complementos |
| Complexidade | Uma parada limpa, cópia integral e identidade/portas/configuração do clone; mais espaço que dump pequeno, menos mecanismos de recuperação do que C |
| Evidência | Parada/ausência comprovadas, inventário de todos os arquivos e dependências, hashes da cópia imutável, mapa das mudanças de configuração do clone e leitura lógica de R |
| Não demonstra | Backup online, recuperação temporal, retenção contínua de WAL, sobrevivência de S1 não copiado ou autoridade pós-restore |

O clone pode conservar o identificador físico do sistema da origem; ele não é
prova suficiente de endpoint ou autoridade. Validar também raiz/data, porta,
processo/criação/handle e identidade lógica. Antes de iniciar o destino, impedir
autostart de escritores/rotas do jogo e revisar a configuração copiada. Mudanças
de porta/HBA e acesso para inspeção são diferenças autorizáveis no alvo; a cópia
de backup original permanece imutável, com hashes anteriores preservados.

## C — Base backup com WAL e recuperação temporal

Base backup cobre o cluster e requer material WAL suficiente para atingir um
estado recuperável; avançar até um alvo posterior exige cadeia contínua e
timeline correta. Um arquivo faltante pode impedir atingir o alvo. A origem
não precisa parar para a obtenção de base backup.
[Fonte primária: arquivamento/PITR, série 18](https://www.postgresql.org/docs/18/continuous-archiving.html).

| Aspecto | Avaliação para a E3-B |
| --- | --- |
| Abrangência | Cluster inteiro e globais do catálogo; não cobre automaticamente archive econômico externo, arquivos fora do conjunto ou manifestos da aplicação |
| Dependências | Versão/formato compatíveis, início/fim de backup, WAL requerido, timeline/histórico, alvo de replay e política ao não atingir alvo; sem eleição ou promoção de serviço por associação |
| S0/S1/R | S0 deve identificar o ponto consistente de base, não apenas hora inicial da cópia; R pode ficar em S0 ou avançar ao alvo T aprovado, que deve ser comparado às confirmações de S1 |
| Complexidade | Privilégios/canal de replicação, transferência/arquivo WAL, espaço/retenção e verificação do alvo; mais caminhos de erro e subprocessos que B |
| Evidência | Manifesto da base, inventário WAL/timelines, intervalos/LSNs, replay e alvo atingido, logs completos e consulta lógica independente; separar integridade de arquivo de completude econômica |
| Não demonstra | RPO zero só por usar WAL; perdas fora da cadeia disponível, história externa perdida, revogação do antigo primário, eleição ou produção |

pg_basebackup usa conexão de replicação e requer permissões/configuração
próprias; a disponibilidade dos binários não constitui autorização para usá-los.
[Fonte primária: pg_basebackup](https://www.postgresql.org/docs/18/app-pgbasebackup.html).
pg_verifybackup verifica manifesto/arquivos e parte das condições WAL, mas não
substitui um teste de recuperação e conferência dos dados corretos.
[Fonte primária: pg_verifybackup](https://www.postgresql.org/docs/18/app-pgverifybackup.html).
Nenhuma dessas ferramentas foi executada nesta A1.

## Compatibilidade com as quotas: estimativa, não medição

A proposta anterior é 1 GiB total, incluindo até 64 MiB de evidências, 4 GiB
livres, dois clusters, um backup/restore, 120 s/caso e 20 min totais. Não foi
aceita e não foi aferida nesta tarefa. A viabilidade depende do tamanho real de
clusters recém-criados e de todos os arquivos simultâneos; poucos personagens
não tornam pg_wal/catalogs pequenos por definição.

Definições: Ds/Dr = árvores origem/alvo incluindo seu WAL ordinário; B = backup;
H = histórico externo/oráculo; E = evidências; T = temporários adicionais;
W = WAL adicional fora dessas árvores; M = margem proposta. Não contar um
mesmo arquivo duas vezes, nem ignorar cópias adicionais. Não pressupor compressão,
hardlink ou deduplicação para caber na quota.

| Método | Fórmula de pico para planejamento | Exemplo hipotético em MiB | Parecer sobre 1 GiB |
| --- | --- | --- | --- |
| A | Ds + Dr + dump + H + E + T + M | 192 + 192 + 64 + 32 + 64 + 64 + 128 = 736 | Pode caber se esses tetos forem verificados; dump/temporários podem ser maiores |
| B | Ds + Dr + B + H + E + T + M | 192 + 192 + 192 + 32 + 64 + 32 + 128 = 832 | Condicionalmente compatível; reservar três cópias físicas e margem antes do aceite |
| C | Ds + Dr + B + W + H + E + T + M | Mesma base B, mais 256 de WAL adicional = 1.088 | Excede nesse exemplo; pode caber com árvores/WAL menores, mas exige dimensionamento específico |

Os números são hipóteses de dimensionamento, sem garantia de capacidade ou
resultados simulados de execução. Medir espaço usado/livre e estimar pior caso
será trabalho futuro autorizado; se não couber, bloquear e pedir decisão sobre
redução de escopo ou nova quota. Não apagar evidências nem elevar limites.
RTO de inspeção é objetivo a medir, nunca inferido desses tamanhos.

## Recomendação para decisão

**Recomendo B para a primeira rodada de inspeção**, condicionada a viabilidade,
mesma versão/binários aprovados e cluster inteiramente novo. O corte limpo
reduz variáveis de exportação seletiva e replay temporal, cobre catálogos/roles
e torna explícito por que S1 posterior não aparece em R. A indisponibilidade
da origem é aceitável somente nesse laboratório, não uma política de produção.

Sequência conceitual futura: criar origem/fixture; fixar S0 com parada fast e
quiescência externa; fazer uma cópia; reiniciar origem, produzir S1 e selar
oráculo; parar origem; recuperar um alvo novo e inspecionar sem escritores.
São três starts totais e um reinício limpo, dentro dos máximos candidatos
anteriores, se aprovados. O alvo vem da cópia física, sem initdb próprio; sua
instanciação deve contar como segundo cluster no orçamento. Nenhuma etapa foi
executada. Manter a origem parada reduz exposição, mas não prova revogação D33
nem o caso antigo-processo-vivo.

A é alternativa se Lucas priorizar reconstrução lógica e revisão de objetos;
exige tratar globais/dependências e não usá-la como teste de recuperação WAL.
C é candidata a rodada posterior para recuperar confirmações até T, depois de
perfil, WAL e orçamento aprovados. Não há fallback automático entre métodos.
Escolher B não retira B05/B06/B08 ou a preservação de S1 dos objetivos globais:
essas provas continuam pendentes nas fases correspondentes.
