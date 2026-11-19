# ORG-03-E2 — Laboratório PostgreSQL descartável

## Resultado e autorização

Execução de referência: `20261114-lucas-600aa9e066`, em 14/11/2026. Responsável: Lucas.
Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
Autorização explícita L01–L05 recebida nesta conversa, com seis ressalvas;
registro local em [authorization.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/authorization.json).
As políticas definitivas D14–D22, D27 e D34 não foram aprovadas por este aceite.

C01–C16 passaram no recorte SQL sintético. O pacote está disponível para revisão
humana do gate E2; essa revisão não foi presumida. E3/E4/E5 permanecem fechados.

## Estado atual verificado

Checkout inicialmente limpo, diferente do trabalho local extenso mencionado
historicamente na R7. Foram revisados AGENTS.md, a skill local de engenharia,
relatórios R1–R7, decisões D01–D34, falhas F01–F30, escopo L01–L05 e critérios
C01–C16. R1 valida candidatos; R2–R4 capturam/observam estado, sem recuperação
integral; R5 prova economia em RAM; R6/R7 são planejamento. Suas contagens de
testes não foram reapresentadas como execução atual. O monitor de erros contém
status histórico sem pendências declaradas; não demonstra ausência atual de
erros do jogo. Nenhum atendimento de erro de gameplay foi feito nesta entrega.

## Escopo implementado

Python 3.14.5 e biblioteca padrão orquestram processos psql PostgreSQL 18.6.
O cluster é novo, em `temp/org03-e2/20261114-lucas-600aa9e066/data`, exclusivamente
`127.0.0.1:55432`, database e roles com prefixo `e2_600aa9e066`.
Não houve instalação nem alteração de POM, Java, banco existente ou protocolo.

O bootstrap e as fixtures negativas privilegiadas executam separados das três
sessões normais. A/B são escritoras independentes, O é a observadora. O papel
normal tem somente USAGE no schema e EXECUTE nas funções autorizadas; SELECT
direto das tabelas, CREATE ROLE e CREATE TABLE foram negados. O controlador
define o contexto sintético; logins de proprietário distintos negam acesso
cruzado antes de revelar payload. Isso não é autenticação de jogador.

## Arquivos e modelo experimental

- [schema.sql](../temp/org03-e2/20261114-lucas-600aa9e066/scripts/schema.sql): DDL, fixtures iniciais e API transacional.
- [runtime.py](../temp/org03-e2/20261114-lucas-600aa9e066/scripts/runtime.py): bootstrap, identidade, processos, quotas e descarte.
- [tests.py](../temp/org03-e2/20261114-lucas-600aa9e066/scripts/tests.py): C01–C16, barreiras e reconciliação.
- [Hashes do código executado](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/source-sha256.json).

`characters/principals` representam oito personagens e dois proprietários.
`versions.payload` é a única representação de XP/moedas/bolsa/equipamento/HP
por versão; `heads` escolhe a versão canônica. Histórico não recebe UPDATE nem
DELETE pela API. Um checkpoint copia o mesmo payload e avança só StateRevision;
economia avança ambas as revisões. AdmissionGeneration/fence ficam na cabeça.
O cluster/run-id é a época sintética que delimita o namespace de origens deste
ensaio; não há emissor de épocas durável de produção.

`operations/receipts/grants` guardam intenção integral, resultado e dedup por
operação, GrantId e origem/beneficiário. `pending` conserva procedência individual;
`obligations/watermarks` conservam dívida e prefixo comprovado; `distributions`
guarda elegíveis/resultados fixos; `temporal` conserva envelopes exatos e
`legacy` somente observações sintéticas. Recibos referenciam versões imutáveis.

## Onde cada invariante é protegida

| Camada | Garantias e limites |
| --- | --- |
| Constraints/triggers | PK, unicidade de operação/grant/origem, FKs de versão e beneficiário, quantidade positiva, campos econômicos presentes/não nulos, revisões não negativas, versões imutáveis e cabeça monotônica |
| Transação/API SQL | Lock da cabeça, CAS, fence, validação da intenção existente antes da revisão atual; versão/recibo/índices/cabeça/direitos/obrigações juntos; grants não reaplicados |
| Harness/controlador | Contexto sintético confiável, quotas, integridade SHA-256 do arquivo, UNCERTAIN, política de retry, envelope/recusa decimal e barreiras observáveis |

As constraints não demonstram toda a semântica de um JSON arbitrário.
A autorização de teste permite executar só o contrato experimental. O runner
não recebe acesso administrativo para alterar essas regras.

## Contrato e perfis comparados

Para intenção nova, a função reautoriza beneficiário, bloqueia cabeça, verifica
operação já conhecida, grant/origem, fence/geração e revisões. Intenção inteira
divergente conflita. Operação confirmada retorna o resultado histórico antes
de exigir revisão/fence atuais; não reinstala o snapshot antigo.
O contrato SQL retorna um localizador para recibo arquivado; a consulta do
harness valida identidade/hash do arquivo e retorna o recibo. Arquivo ausente
permanece UNCERTAIN e não cria novo efeito. Não há RNG no retry.

Foram comparados XP truncado versus recusa integral; quest recusando moeda
excedente versus mob saturando; coleta por item versus pacote inteiro;
settlement ordenado versus prefixo/lacunas; commits por beneficiário versus
transação global. Prefixo e lacunas usam a mesma representação candidata de
obrigações explícitas; não são três backends independentes nem política final.
Os tetos 100 XP e 1.000 moedas são exclusivamente fixtures experimentais.

## Limites e critérios de pronto

200 intenções distintas (incluindo recusas e conteúdo divergente),
137 recibos, oito personagens, dois proprietários e no máximo
três sessões simultâneas. Casos técnicos usaram duas tentativas por intenção,
abaixo de três; conflitos CAS não foram rebaseados automaticamente.
Pico observado: 44,199,486 bytes; espaço inicial: 108,612,644,864 bytes.
Testes/encerramento após bootstrap: 24.24 s, abaixo de dez minutos.
Payload foi limitado a 16 KiB; cap de retenção C10 foi 32, sem purge.

As medições de disco são periódicas, não quotas físicas. Arquivos/WAL/logs e
evidências integram o limite de 512 MiB; evidências têm limite de 128 MiB.
Os parâmetros de timeout foram gravados no arquivo de configuração próprio;
o timeout de lock e os erros técnicos foram observados em SQL. Não foi executado
ensaio específico para provocar cada um dos demais timeouts.

## Entrega, reversão e próxima etapa

[Testes](ORG-03-E2-TESTES.md), [matriz](ORG-03-E2-MATRIZ-RESULTADOS.md),
[isolamento](ORG-03-E2-ISOLAMENTO.md), [relatório](ORG-03-E2-RELATORIO-FINAL.md)
e [pendências](ORG-03-E2-PENDENCIAS-E3.md).
Dados/segredos do cluster já foram descartados; evidências ficaram locais.
Para reverter a entrega documental, remover apenas estes seis Markdown novos
após revisão, preservando alterações posteriores e os artefatos desejados.
Não há migration, formato de save ou código do jogo a reverter.
