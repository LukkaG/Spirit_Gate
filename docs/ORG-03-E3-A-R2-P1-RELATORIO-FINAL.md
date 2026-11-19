# ORG-03-E3-A-R2-P1 — Relatório final

Lucas, 15/11/2026. **Correção e validação isoladas concluídas no escopo P1.**
**E3-A permanece NÃO APROVADO. R3 não autorizada nem executada.**

## Resultado

O reset deliberado de cliente identificado pode concluir o transporte apenas
com permit/ACK correlacionados às barreiras e ao pedido real de terminate.
Reset não armado, servidor, fase inválida, divergência de identidade/endpoint/
contadores, timeout ou erro inesperado continuam falhas. Saída zero do relay
não aprova C02; o gate exige novo worker, mesmo OperationId, recibo histórico,
retry idêntico e imagem integral inalterada.

A captura de stderr lê bytes, drena além do limite para manter contagem/hash,
tolera UTF-8 inválido com substituições registradas e omite texto livre/
segredos/SQL da persistência. Erro, ausência de EOF ou truncamento invalidam
captura e finalização. Guardas de processo R1 permanecem na classe original.

## Validação e evidências

- **71 testes puros PASSOU:** 23 preservados e 48 novos.
- **Fixture final: dois controles PASSOU**, com 10054 real; caso esperado exit 0,
  caso não armado exit 1; ambos sem recursos próprios remanescentes.
- **4.081 arquivos anteriores preservados**, hashes sem diferenças.
- Sintaxe e hashes das cópias legacy/novos artefatos conferidos.
- Uma fixture e uma asserção de teste falharam durante o desenvolvimento;
  evidências preservadas, causas corrigidas e validação final aprovada.

[Testes e limitações](ORG-03-E3-A-R2-P1-TESTES.md),
[diagnóstico completo](ORG-03-E3-A-R2-P1-ANALISE-TRANSPORTE.md),
[resultado estruturado](../tools/org03_e3a_transport_fix/evidence/validation.json).

Fontes e manifestos estão em `tools/org03_e3a_transport_fix/`; os scripts e
evidências dos run-ids anteriores não foram modificados. Foram criados os
cinco documentos solicitados, incluindo a proposta separada de R3.

## Compatibilidade e pendências

Nenhum PostgreSQL iniciado, SQL executado, cluster criado ou crash realizado.
Nenhuma mudança no jogo/Java/POM, protocolos, habilidades, sprites, HUD,
launcher, World Studio, saves, contas, serviço 5432 ou laboratórios anteriores.
Sem instalação, reset/clean, commit, push, publicação ou gates posteriores.

O adaptador C02 está preparado com dependências injetadas e testado sem SQL;
não existe runner PostgreSQL operacional nesse pacote. A integração com o
novo runtime completo, seu run-id e hooks de socket/eventos deverá ser
concretizada/revisada na preparação R3 autorizada, antes de criar o cluster.
Essa separação impede reaproveitar o runner encerrado ou iniciar SQL pela P1.

A fixture usa SO_LINGER para obter reset reproduzível; não homologa todos os
desfechos Windows, PostgreSQL, durabilidade ou economia. Texto stderr não
estruturado é omitido por privacidade, com contagem/hash preservados; não se
promete recuperar o texto bruto perdido na R2. Os resultados R2 continuam
C01 PASSOU/C02 FALHOU/C03–C09 NÃO EXECUTADOS.

Próximo passo submetido à revisão humana:
[proposta R3](ORG-03-E3-A-R3-PROPOSTA-EXECUCAO.md), com aceites novos por recurso.
Nenhuma autorização R2 é herdada e nenhum cluster foi preparado/inicializado.

Reversão segura: retirar somente adições P1 após revisão e sem apagar as
evidências históricas ou trabalho posterior. Não há banco/modificação de
produção a reverter; não usar reset/clean.
