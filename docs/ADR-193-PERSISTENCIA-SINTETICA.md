# ADR-193 — persistência sintética local e isolada

Estado: **APROVADO PELO RESPONSÁVEL**. Data: 25/10/2026. Escopo de decisão: TASK-193 a TASK-196 do Corujão VI.

## Decisão proposta

Evoluir o `SyntheticOnlineRepository` existente como laboratório de arquivos locais, com novos diretórios `online-synthetic-*` exclusivos de testes/checkpoints. Reutilizar Java 17, os codecs e `RpgWorld` existentes. Não instalar banco, Docker, Testcontainers ou biblioteca nova.

Preservar o wrapper SGO1 e o payload V2 existentes. Não converter, importar ou escrever personagens pessoais; não mudar os codecs pessoais V1/V2. Não ligar o repositório a `RpgLauncher`, `LauncherSession`, contas reais ou gameplay do usuário. A prova de retomada será um harness sintético dedicado, usando o motor real `RpgWorld`, sem apresentar esse harness como integração do launcher ou serviço online publicado.

## Situação auditada

O protótipo já usa UUID, revisão positiva, payload validado, SHA-256, CAS, lock, temporário validado/force e rename atômico. Grant opaco vincula UUID e autoridade efêmera da instância; o grant anterior é recusado após restart. Não é credencial de conta.

A caracterização atual confirmou roundtrip, CAS, backup anterior, leitura após restart, cópia exclusivamente sintética e um único commit entre duas escritas concorrentes. Encontrou estas lacunas, ainda sem alterar produção:

1. `recoverBackup(id)` não compara o UUID pedido com o UUID decodificado. Um backup sintético B copiado sob o nome A é devolvido como B. A escrita com grant A rejeita esse estado, mas a API de recuperação precisa recusar a incompatibilidade antes da retomada.
2. Duas escritas na mesma JVM podem produzir `OverlappingFileLockException`, em vez de um conflito de armazenamento tratado de modo uniforme. Nenhuma escrita foi perdida na prova atual.
3. Retry de snapshot idêntico com revisão antiga resulta em conflito; não há confirmação idempotente/reconciliação de commit.
4. Grants não podem ser reutilizados após restart. Qualquer concessão nova a personagem existente precisa de um controlador local explícito de laboratório, sem expor UUID como autorização de rede.

Resultados completos: `.task-checkpoints/TASK-193/corujao-vi/run-20261025T233454Z-39c70656/audit/characterization.json`. `PASS_CHARACTERIZATION` significa que o comportamento foi medido, não que as lacunas foram corrigidas ou que este ADR está aprovado.

## Contrato para implementação autorizável

| Área | Decisão proposta |
| --- | --- |
| Isolamento | Pastas novas controladas, fora de saves pessoais/launcher/instalações; recusar links e caminhos incompatíveis com o sandbox sintético; não aceitar paths vindos de socket |
| Identidade/integridade | UUID do nome solicitado, cabeçalho e payload devem concordar, inclusive no backup; SHA é integridade, não autenticação |
| Escrita | Preservar CAS estrito de `write`, revisão monotônica e falha segura; nenhum arquivo parcial é retornado como confirmado |
| Concorrência | Um commit por revisão-base; conflito de lock/CAS comunicado sem substituir o estado confirmado |
| Atomicidade | Temporário validado, force e movimento atômico; ausência de suporte atômico é erro, sem fallback de cópia sobre o primário |
| Recuperação | Leitura de backup validado; aplicação explícita em laboratório, com causa e revisão registradas; nenhum fallback silencioso toma estado parcial por confirmado |
| Retry | Facade sintética pode reconciliar somente snapshot inteiro e idêntico já confirmado na revisão `base+1`; não incrementar revisão/recompensa num retry. Outro estado/revisão continua conflito |
| Posse de sessão | Registro local de laboratório associa handle efêmero, UUID e grant; um personagem possui no máximo uma sessão autoritativa nesse registro; outro handle/instância não reutiliza a posse |
| Retomada | Controlador local de laboratório pode conceder nova autoridade a UUID sintético validado após restart; nenhuma função de login público ou concessão por mensagem do cliente |
| Conteúdo/progressão | Validar região/posição contra o conteúdo confirmado conhecido do harness; fixtures limitadas ao conteúdo/catálogos suportados pelo codec, sem inventar suporte a quests autoradas ou importar campanha |
| Migração | Somente cópia/validação de documentos SGO1 sintéticos em nova pasta; sem migração de V1/V2 pessoal, credencial ou instalação anterior |

O retry proposto reconcilia armazenamento de snapshots, não oferece exactly-once global para comandos de combate. A prova de XP/loot/quest não duplicados deve especificar as operações e replays sintéticos testados; nenhuma garantia de contas de produção será inferida.

## Arquivos candidatos e sequência

- TASK-194: ajustes mínimos em `SyntheticOnlineRepository.java`, testes de UUID de backup, CAS/lock, checksum, falha de processo e escrita parcial; facade isolada de retry se necessária.
- TASK-195: registro/handles de sessão somente do laboratório e testes negativos de posse/duplicação; projeto separado documentado de autenticação/TLS futuros.
- TASK-196: harness sintético com `RpgWorld`, criação/gravação/desconexão/restauração, verificando snapshot, progressão e conteúdo compatíveis. Nenhuma chamada nova do launcher real ao repositório.

Novos auxiliares/testes devem permanecer explicitamente sintéticos, com API e nomes de laboratório; nenhuma refatoração transversal do servidor para acomodá-los. Qualquer necessidade de novo schema, banco externo, credencial ou ponte para launcher excede esta decisão e exige proposta própria.

## Critérios de aprovação técnica posterior

Testes positivos/negativos reais, CAS/retry e concorrência, interrupção de processo em pasta sintética, backup adulterado/UUID trocado, sessão errada/duplicada, retomada e replays descritos, regressão essencial e hashes protegidos sem divergência. Somente marcar 194–196 validadas quando essas provas existirem; este ADR não as antecipa.

## Alternativas e riscos

Manter apenas a auditoria atual conserva o protótipo e suas lacunas; não permite declarar 194–196 implementadas. PostgreSQL/Testcontainers são alternativas futuras opcionais e não fazem parte desta proposta. Prefixo de diretório e grants em memória não são barreira contra administrador local/host comprometido. TOCTOU, comportamento de filesystem e durabilidade física continuam riscos residuais; interrupção de processo não homologa falta de energia.

## Gate de decisão

O Plano Diretor, TASK-193, exige **“Pronto quando: ADR aprovado”**. O Prompt Mestre também reserva essa decisão e diz para não interpretar passagem automática como aprovação do ADR. A autorização de executar tasks em sequência já está vigente; a decisão solicitada aqui é sobre este contrato concreto de persistência.

Aprovar este ADR autoriza somente a implementação sintética acima e seus testes. **Não autoriza integração ao launcher/contas/saves reais, instalação externa, mudança de protocolo pessoal, commit, push, release ou publicação.** Esses limites persistem até autorização específica separada.


## Decisão formal registrada

Aprovação explícita recebida em resposta à pergunta sobre este ADR. Autoriza implementar e testar 194–196 nos limites acima; não homologa resultados antecipadamente. Decisão e hash da proposta preservados em `.task-checkpoints/TASK-193-approval/corujao-vi/run-20261026T000457Z-2ec3bf50`.
