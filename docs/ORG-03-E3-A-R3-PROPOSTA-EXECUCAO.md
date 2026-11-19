# ORG-03-E3-A-R3 — Proposta de execução

**PROPOSTA PARA REVISÃO HUMANA — NÃO AUTORIZADA — NÃO EXECUTADA.**
Responsável proposto: Lucas. Data da proposta: 15/11/2026.
A P1 autoriza somente correção/validação do harness e não autoriza esta rodada.
Os aceites R2 foram consumidos; nenhum deles é herdado.

## Estado de entrada

R2 terminou C01 PASSOU, C02 FALHOU no transporte, C03–C09 NÃO EXECUTADOS;
cluster próprio encerrado/descartado e evidências imutáveis. P1 validou reset
esperado versus inesperado, captura binária e guardas, com 71 testes puros e
fixture loopback positiva/negativa. Isso não é evidência SQL de C02 ou recovery.

Novo run-id proposto: `20261115-lucas-e3a-r3-9b62f13a70`.
Caminho proposto:
`C:/Users/lucas/Documents/GitHub/Projeto-K/temp/org03-e3a/20261115-lucas-e3a-r3-9b62f13a70`.
Não foi criado diretório/claim/data/secrets para esse run-id. Se a execução
ocorrer em outra data, escolher novo ID, registrar caminho exato/data/responsável
antes do início e nunca reutilizar um run-id consumido.

## Aceites propostos, separados

| Item | Ação a autorizar |
| --- | --- |
| R3-01 | Preparar pacote novo, copiar fontes anteriores sem modificá-las, integrar pump/captura/permit/adaptador P1 e revisar diff integral antes do cluster. |
| R3-02 | Criar exatamente um cluster descartável com binários existentes, data novo e portas fixas 127.0.0.1:55433/55434, depois do preflight. |
| R3-03 | Executar C01–C09 e finalizar exclusivamente workers/relay próprios por handles/manifestos/identidades. |
| R3-04 | Até dois `pg_ctl stop -m immediate` próprios em C03/C04 e até quatro reinícios no mesmo data, sem recriação/fallback. |
| R3-05 | Registrar evidências sanitizadas, consultar verificação, parar fast e descartar só data/secrets próprios após comprovação de ausência. |

Nenhuma linha acima constitui aceite recebido. Identificar nome, data,
run-id/caminho e escopo no novo manifesto antes de qualquer initdb.

## Tetos propostos

Um cluster/initdb; dois crashes; quatro reinícios além do start inicial;
oito personagens sintéticos/dois proprietários; 64 intenções; três tentativas
por intenção; payload 16 KiB; três workers simultâneos/32 totais;
quatro conexões SQL simultâneas; bootstrap privilegiado separado;
um relay simultâneo/duas instâncias totais para C02/C05; nenhum terceiro em C09.
120 s/cenário, 20 min/rodada; stop 20 s, start/recovery 45 s, CIM 8 s,
wait de término 5 s. 256 MiB totais, incluindo até 32 MiB de evidências;
mínimo inicial 2 GiB livres. Fiscalização periódica não é quota física.
Prefixo stderr 64 KiB, blocos 4 KiB, texto sanitizado 64 KiB e retidos do
pump 16 KiB; erro/truncamento invalidam cenário, sem aumentar limites.

## Preparação obrigatória sob R3-01

1. Integrar os adaptadores injetados ao runtime novo, sem executar scripts
   diretamente dos run-ids anteriores. Manter lógica econômica/schema antigos.
2. Registrar identidade do relay e do worker, conexão aceita/connection_id e
   OwningProcess do socket; correlacionar todos os ACKs/eventos no supervisor.
3. Usar stdin/stdout/stderr binários, drenos contínuos e protocolo JSON limitado;
   nunca repetir o canal texto UTF-8 fixo do stderr R2.
4. Persistir UNCERTAIN antes de corte, snapshot pós-commit e recibo do observador.
   Encerrar cliente com o armamento pré-terminate, manter guardas R1 intactas.
5. Fazer o close aguardar o handle/pipes e validar captura/evento final/permit;
   um relay não zero ou zero inesperado interrompe, sem aprovar cenário.
6. Implementar dependências de `c02_integration.py` com os oráculos existentes.
   C05 usa reconciliação sem consulta econômica pré-corte; exige barreira TCP
   retida antes de classificar reset previsto. C09 mantém seu subcaso próprio
   sem terceiro relay e não substitui C02/C05.
7. Conferir sintaxe, hashes, 23 testes históricos e 48 novos, testes focalizados
   da integração e diff completo. Verificar contagem máxima de processos,
   SQL, intenções e armazenamento antes de consumir o claim.

Nenhuma guarda pode ser removida para tornar o runner executável. Qualquer
mudança econômica/SQL ou ampliação desses limites exige revisão/autorização
fora desta proposta. O adaptador P1 não fornece runtime nem conexão SQL.

## Preflight e critérios da rodada

Revalidar versões/SHA-256 dos binários existentes, branch/HEAD/status,
manifestos de preservação, caminho canônico sem reparse point, SID/ACL exclusiva
de secrets, variáveis PG neutralizadas, 2 GiB livres e portas livres com bind
exclusivo imediatamente antes do uso. Conflito exige parar, sem outra porta.
Comparar SQL + SO + PID/criação/comando/opções/listener antes de DDL e controle.
Sem conexão ou comando dirigido ao PostgreSQL preexistente na 5432.

| Caso | Prova mínima exigida |
| --- | --- |
| C01 | Transação sem COMMIT; fim real do backend; novo worker NOT_FOUND; imagem integral igual. |
| C02 | PAUSE antes de COMMIT; observador confirma; contadores retidos; cliente identificado/terminate; EOF/reset previsto validado; relay encerrado e captura completa; worker novo, mesmo OperationId, recibo histórico, retry e comparação integral sem novo efeito. |
| C03 | Barreira sem COMMIT; immediate próprio; logs reais de redo e identidade revalidada; ausência integral de efeitos/índices após recovery. |
| C04 | ACK e observador confirmados antes de immediate; recovery real; recibos/cabeças/recursos preservados; retry sem overwrite. |
| C05 | Intenção durável e UNCERTAIN sem ACK/consulta pré-corte; término do backend; novo lookup e resultado integral confirmado ou não confirmado, mesmos IDs. |
| C06 | Manifesto A/B persistido; A confirmado antes de B; coordenador novo reconstrói e liquida só o pendente. |
| C07 | Obrigação antiga/IDs/prefixo persistidos; novo coordenador identifica e liquida dívida sem saltar lacuna. |
| C08 | Arquivo próprio fsync/hash; novo consulente; indisponibilidade reversível permanece UNCERTAIN; restauração e lookup sem novo prêmio. |
| C09 | PREPARED, ACK e DISPATCHED sem ACK com provas distintas, workers novos e mesmas intenções; nenhum terceiro relay. |

Executar controles finais: autoridade obsoleta em intenção inédita, autoridade
atual positiva, conflito CAS antigo, retry histórico sem reinstalar snapshot,
menor privilégio e integridade global SQL sem órfãos/transações pendentes.
Guardar snapshots/counters/barreiras/falhas intermediárias, sem payload TCP
ou segredos. Opções on não provam durabilidade; fast não é crash.

## Parada e descarte

Divergência de identidade, socket desconhecido, barreira ausente, liberação de
bytes retidos, captura incompleta, timeout invalidante ou quota excedida
interrompem os cenários. Sem kill por nome/PID avulso, fallback de porta,
retry destrutivo ou novo initdb como recuperação.

Encerrar próprios por handles, reidentificar cluster e enviar fast. Exigir
status 3, ausência de postmaster.pid, postmaster/filhos próprios, handles e
listeners antes de remover apenas data/secrets canônicos próprios. Identidade
duvidosa conserva recursos/evidências para decisão humana. Reuso posterior
de PID deve ser distinguido por identidade em leitura, sem controlar o novo
ocupante. Preservar scripts/logs/aceite/claim/hashes e resultados originais.

E3-A só pode ser aprovado com todos os cenários obrigatórios e controles
completos. Qualquer falho/inconclusivo impede aprovação; prova parcial não
é reaproveitada como sucesso de outro caso.

Fora do escopo: E3-B/E4/E5, autenticação, migração/restore/failover/recuperação
de personagens, mudanças no jogo/produção, A-04, ORG-04, TASK-226, instalação,
commit, push ou publicação. Aguardando revisão e aceites R3 próprios.
