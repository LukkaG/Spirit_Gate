# ORG-03-E3-B-A1 — Alternativas para D33

Revisão iniciada em 16/11 e concluída em 17/11/2026. **D33 continua BLOQUEADA POR EVIDÊNCIA e sem mecanismo aprovado.**
Este documento compara possibilidades; não cria eleição, quorum, lease, era,
promoção ou serviço externo. Referências: [ADR R6](ORG-03-R6-ADR-DURABILIDADE.md),
[D33 na matriz](ORG-03-R6-MATRIZ-DECISOES.md) e
[D07/D33 no quadro R7](ORG-03-R7-QUADRO-APROVACAO-E1.md).

## O problema que não se resolve com restore

Um backup pode restaurar geração/fence/época/índices antigos. O processo que
conhecia aqueles valores pode continuar ativo; o novo processo também pode
considerá-los válidos. Dois bancos internamente íntegros não definem entre si
qual é a fonte canônica nem revogam credenciais, callbacks, corpos ou comandos.
Operação conhecida e dados íntegros não autorizam escrita.

E3-A-R3 recusou fence antigo no banco corrente e preservou invariantes após
restart. Não restaurou uma versão antiga da própria autoridade. O controle
`D33: NOT_TESTED` e o [encerramento auditado](ORG-03-E3-A-ENCERRAMENTO.md)
permanecem históricos e intactos.

## Compatibilidade com a arquitetura observada

Inspeção estática limitada, sem inicialização:

| Ponto real consultado | O que se observa | Implicação para a decisão |
| --- | --- | --- |
| [LocalSimulationServer](../server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java) | Owner em executor de uma thread, peers/conexões locais e fila limitada | Exclusividade local da thread não é exclusividade entre duas JVMs ou clusters |
| [RpgSessionWorld](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java) | Mundo/sessões e metadados de captura ligados à instância; referência a coordenação de revisões de conteúdo | Não assumir que RevisionCoordinator seja eleição de autoridade econômica |
| [SyntheticOnlineRepository](../server/src/main/java/com/projetok/rpg/SyntheticOnlineRepository.java) | Grant com Object authority da instância, documento/revisão e lock de arquivo | Protótipo local, não identidade de conta nem fonte global resistente a restore |
| [EconomicLaboratory em src/test](../server/src/test/java/com/projetok/rpg/economy/EconomicLaboratory.java) | CAS de imagem imutável em RAM, IDs e geração sintéticos | Base conceitual de invariantes, não serviço durável de autoridade |

Não foi identificado mecanismo de eleição/revogação global homologado nesses
pontos e na governança consultada. Isso não é prova de ausência em todo sistema
externo possível. Propor um coordenador distribuído agora ampliaria arquitetura,
dependências e fronteiras de falha; exige decisão própria.

## Possibilidades comparadas, sem seleção automática

| Opção | Estado durável e sobrevivência ao rollback | Processo antigo / revogação verificável | Múltiplas instâncias e comunicação | Admissão de escrita / risco principal |
| --- | --- | --- | --- | --- |
| Q — quarentena de recuperação | Inventário/oráculo independente guarda evidências; não pretende guardar autoridade produtiva | Destino não admite escritores; origem parada ou isolada no primeiro recorte. Não demonstra revogação global de processo vivo | Pode inspecionar várias imagens sem elegê-las; falha de evidência mantém bloqueio | Nenhuma escrita econômica admitida. É contenção temporária, não solução positiva de D33 nem disponibilidade |
| M — recuperação manual exclusiva com isolamento comprovado | Registro operacional externo ao backup de todas as instâncias/credenciais/rotas e decisão de fonte canônica, com persistência/integridade a definir | Procedimento deve revogar ou isolar todas as execuções antigas em todos os pontos de acesso; provar cobertura, inclusive reconexão e instância esquecida | Adequada como candidata a operação local pequena; escala mal. Se não for possível comprovar isolamento completo, não promover | Somente após prova de exclusividade e credenciais/rotas antigas ineficazes; matar um PID conhecido ou parar origem local não basta. Processo vivo só pode continuar sem caminho de efeito comprovado |
| E — registro durável de autoridade fora do domínio restaurável | Autoridade/geração/revogações persistidas em domínio independente, com política própria de backup/rollback e autenticação; nenhum formato ou algoritmo escolhido | Escritor antigo precisa ser recusado no ponto que efetiva a escrita, mesmo com cache ou conexão antiga; revogação auditável | Pode atender múltiplas instâncias; indisponibilidade/partição exige recusa conservadora salvo política aprovada | Provar vínculo entre validação e commit: consultar serviço e escrever depois abre corrida. Dependência externa e sua própria recuperação precisam de prova |
| C — coordenação por consenso/quorum aprovado | Log durável/termos/membros e recuperação do conjunto fora do rollback da economia, conforme produto/algoritmo ainda não escolhido | Liderança deve produzir fencing efetivo no armazenamento/rotas; líder antigo isolado não pode continuar produzindo efeito | Suporta múltiplas instâncias se particionamento/membership estiverem corretos; parte sem quorum não pode ganhar autoridade por conveniência | Eleição não é fencing nem prova de histórico completo. Lease, se houver, exige limites de relógio/suspensão e D07; novas dependências/custos |
| H — topologia de banco com replicação/fencing operacional | Histórico de replicação, posições/timelines e política de promoção/retorno duráveis; configurar confirmação segundo D12 | Infraestrutura deve impedir que primário antigo continue escritor após troca; restaurar backup isolado não prova essa condição | Possível múltiplas instâncias, mas replica lag, partições, rotas e retorno do primário exigem tratamento explícito | Replicação por si só não elege nem revoga ator; confirmação só sobrevive no perfil realmente coberto. Aplicação continua precisando validar posse/fence |

A documentação PostgreSQL descreve a necessidade de tratar o antigo primário
para evitar dois primários e deixa a detecção/notificação de failover para
mecanismos externos. Isso reforça que instalar PostgreSQL não fornece uma
política completa para o Spirit Gate.
[Fonte primária, failover série 18](https://www.postgresql.org/docs/18/warm-standby-failover.html).

Q é a **recomendação para conter a primeira rodada de inspeção**, não uma
escolha de mecanismo D33 definitivo. M merece avaliação se a arquitetura futura
for estritamente uma instância administrada; E/C/H fazem sentido apenas após
definir necessidade de múltiplas instâncias, disponibilidade e falhas admitidas.
Não há recomendação de produto, eleição ou quorum específico nesta entrega.

## Possibilidades insuficientes como solução isolada

| Proposta insuficiente | Por que não fecha D33 |
| --- | --- |
| Incrementar max(fence) do banco restaurado | O máximo também retrocedeu e pode coincidir com autoridade já emitida fora de R |
| Gerar UUID/WorldEpoch novo | Unicidade não prova que todos os escritores anteriores serão recusados |
| Advisory lock ou mutex apenas local ao cluster | Outro cluster restaurado pode adquirir seu próprio lock; JVMs não compartilham mutex em RAM |
| Rotacionar apenas senha do alvo | Conexões antigas e origem ainda acessível podem continuar válidas; cobertura de todos os caminhos não está demonstrada |
| Lease expirado sem suspensão/fencing | Owner antigo pode executar comandos/efeitos e escrever após pausa; D07 não está resolvida |
| Encerrar um processo conhecido | Reduz risco local, mas não prova revogação resistente a rollback, processos desconhecidos ou retorno da instância |
| Quorum que só seleciona líder | Se armazenamento e mundo ainda aceitam o líder antigo, persistem duas autoridades efetivas |

Esses itens podem ser componentes de um mecanismo maior aprovado, nunca
atalhos para declarar a prova concluída.

## Negativos de detecção versus positivos de recuperação

**Negativos propostos para fase de inspeção:** classificar a autoridade como
não validada, demonstrar que nenhum escritor é iniciado/admitido, registrar duas
pretensões incompatíveis como bloqueadas e conservar evidências. Podem usar
validadores estáticos em fase futura autorizada. Isso não demonstra que o banco
recusaria uma credencial velha em uma operação econômica real.

**Positivos B06/B08, bloqueados agora:** manter processo antigo controlado vivo,
aplicar mecanismo aprovado, comprovar sua recusa sob reconexão/cache/partição
previstos e demonstrar que somente a autoridade nova pode efetivar operações.
Medir efeito, recibo, GrantId/origem, head e revisão. Se ambos forem bloqueados,
o negativo de segurança pode passar, mas recuperação de autoridade não passou.

Qualquer tentativa econômica pós-restore, inclusive replay B05, pagamento B07
ou atualização de head B09, depende de autoridade aprovada e autorização própria.
Uma tentativa “de teste” não dispensa essa fronteira. A1 não autoriza nenhum
processo, SQL ou escrita. Retirada do ator/combate continua exigindo E4.

## Perguntas que o mecanismo aprovado precisará responder

1. Onde vive a verdade de autoridade e qual domínio pode retroceder? Quem a
   restaura, autentica e revoga? O que acontece se ela também perder estado?
2. Quais instâncias, conexões, credenciais, callbacks e caminhos de efeito são
   abrangidos? Como detectar uma instância desconhecida sem encerrá-la?
3. Como impedir intervalo entre validação de autoridade e commit? Que garantia
   o armazenamento fornece para recusar autoridade já revogada?
4. Que falhas de comunicação/relógio são admitidas? Quando recusar novas
   admissões e quando é permitido retomar? Qual disponibilidade se perde?
5. Que prova positiva e negativa é necessária antes de alterar o status D33?

Sem respostas aceitas e implementação/prova revisadas: **AUTH bloqueado,
B06/B08 positivos bloqueados, nenhuma admissão econômica após restore.**
