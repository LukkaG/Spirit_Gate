# ORG-03-E3-A-R1 — Proposta de nova execução

**PROPOSTA, NÃO AUTORIZADA, NÃO EXECUTADA.** Responsável proposto: Lucas.
Autorizações E3A-01–05 da rodada anterior foram consumidas; não serão herdadas.

## Recursos e limites propostos

Novo run-id proposto: `20261115-lucas-e3a-r2-8c71d4a902`.
Não foi criado diretório operacional desse run-id nem manifesto de aceite.
Se o início ocorrer em outra data, escolher outro ID e registrar o aceite exato.

| Recurso | Limite proposto |
| --- | --- |
| Cluster/initdb | Exatamente 1 novo, sem reutilizar dados ou execution.claim anteriores |
| Caminho | temp/org03-e3a/20261115-lucas-e3a-r2-8c71d4a902/data; secrets irmão restrito ao SID executor |
| Portas | Candidatas 127.0.0.1:55433 e 55434; revalidar listeners, propriedade e bind exclusivo imediatamente antes do uso |
| Crashes | Até 2, somente pg_ctl stop -m immediate próprio, C03 e C04 |
| Reinícios | Até 4 além do start inicial, no mesmo data; sem novo initdb |
| Processos | 1 controlador, até 1 relay simultâneo/2 instâncias totais para C02 e C05; até 3 workers simultâneos/32 instâncias totais |
| SQL | Até 4 conexões simultâneas; bootstrap privilegiado separado |
| Fixtures | 8 personagens/2 proprietários; 64 intenções; até 3 tentativas por intenção; payload até 16 KiB |
| Prazos | 120 s/cenário, 20 min totais; stop 20 s, start/recovery 45 s; inspeção CIM 8 s; wait worker 5 s |
| Armazenamento | 256 MiB totais, incluindo 32 MiB de logs/evidências; mínimo inicial 2 GiB livres |

As portas não foram declaradas livres nesta R1. Não haverá contato com 5432.
Quotas são limites de parada, não metas; não ampliá-las para obter aprovação.
Medição periódica de disco não constitui quota física.

## Preparação antes do início

Revisar e integrar as cópias R1 em novo pacote, conectando pump ao listener
loopback exclusivo e ao upstream fixo aceito, com run-id próprio. A R1 não traz
runtime/runner SQL e recusa ativação do relay por CLI. Conferir sintaxe, hashes,
23 testes e revisão do diff completo; não desativar guardas para permitir uso.
Não executar scripts diretamente dos run-ids E2/E3 encerrados.

Registrar nome, data, escopo e limites exatos do novo aceite. Revalidar versões
e hashes dos binários existentes, caminho canônico sem junction, SID/ACL,
espaço livre, variáveis PG neutralizadas, ausência de recursos conflitantes e
guarda de uso único. Conflito de porta exige parar, sem buscar porta alternativa
automaticamente. Identidade SQL e SO completas antes de qualquer DDL.

## Ordem e provas

1. C01 como regressão: fim do backend, novo worker, NOT_FOUND e imagem integral igual.
2. C02 completo: identidade e handle; PAUSE confirmado; COMMIT; observador confirma;
   bytes retidos e encaminhamento imóvel; cliente termina; relay inspecionado;
   novo worker consulta mesmo OperationId; retry integral não cria efeitos.
3. C03 e C04 com as duas interrupções, somente se aceitas; provar barreiras e
   recovery no log e identidade revalidada. Parâmetros on não bastam como prova.
4. C05–C09 sem reaproveitar prova parcial de outro caso; controles de fence,
   snapshot, manifestos, obrigações, arquivo, menor privilégio e integridade final.

## Recuperação e encerramento

Crashes autorizados preservam data e usam somente recuperação no mesmo cluster.
Não aplicar novo initdb como reparo. Divergência de identidade, isolamento,
barreira ausente, processo desconhecido, timeout invalidante ou quota excedida
interrompem os cenários. Nenhum kill por nome/PID avulso e nenhum fallback.
Identidade duvidosa conserva recursos/evidências e requer decisão humana.

Na saída normal, encerrar workers/relay pelos handles registrados e revalidar
cluster antes de pg_ctl fast. Comprovar status 3, ausência de postmaster.pid,
processos próprios e listeners. Descartar só data/secrets canônicos próprios
após essas provas; preservar scripts/logs/hash/evidências sanitizadas. Se houver
incerteza, não apagar nem tentar contornar a proteção.

## Aceites necessários, separados da R1

- R2-01: preparar/integrar pacote isolado no novo run-id, sem alterar originais.
- R2-02: criar o único cluster nas portas/caminhos/quotas acima após preflight.
- R2-03: executar C01–C09 e encerrar somente workers/relay próprios identificados.
- R2-04: até dois crashes immediate e até quatro reinícios próprios.
- R2-05: observação/evidências, parada fast e descarte estritamente próprios.

Não há pedido de execução implícito neste documento. Permanecem fora E3-B,
E4/E5, ORG-04, TASK-226, produção/gameplay, protocolo, restore/failover e
publicação. C02 anterior continua FALHOU e C03–C09 anteriores NÃO EXECUTADOS.
