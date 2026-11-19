# ORG-03-E2 — Testes e comandos

Execução de referência: `20261114-lucas-600aa9e066`, em 14/11/2026. Responsável: Lucas.
Branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`.
Autorização explícita L01–L05 recebida nesta conversa, com seis ressalvas;
registro local em [authorization.json](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/authorization.json).
As políticas definitivas D14–D22, D27 e D34 não foram aprovadas por este aceite.

## Comandos efetivamente executados

```powershell
python -m py_compile temp/org03-e2/20261114-lucas-38e6b25bae/scripts/tests.py
python temp/org03-e2/20261114-lucas-600aa9e066/scripts/tests.py
```

A primeira linha conferiu sintaxe na rodada reforçada anterior. A execução
final importou e executou runtime/tests completos, encerrando com código 0.
Comando de reexecução com o mesmo run-id foi testado depois do descarte e
retornou código 1 com `RUN_ALREADY_USED`, sem alterar qualquer evidência ou
recriar dados. A recusa é sucesso da proteção, não falha de C01–C16.

[Resultados](../temp/org03-e2/20261114-lucas-600aa9e066/evidence/results.json): 16 critérios PASSOU, 200
intenções distintas, 137 recibos, 24.24 s incluindo
o encerramento após bootstrap. Os tempos de cada caso estão nesse JSON;
não são benchmark nem SLA. Não houve Maven, testes Java ou suíte antiga R5.

## Concorrência e rollback

Processos psql A/B permanecem conectados em backends diferentes. O harness
inicia ambas as transações, confirma que o segundo backend está bloqueado
pelo primeiro com pg_blocking_pids, e só então libera o commit.
Pequenos intervalos de polling não substituem a evidência de lock.
20 pares de revisão e 20 de grant alternam A/B. Checkpoints e claims recebem
ensaios próprios. A observadora lê estado confirmado após a resolução.

C15 inverte locks somente na fixture de deadlock, depois que cada escritor
preparou sua própria operação na transação. Um erro 40P01 aborta a tentativa;
o perdedor faz ROLLBACK e repete a mesma intenção na transação inteira.
Na serialização, ambas as sessões leem duas cabeças antes das escritas para
provocar SSI/write skew; 40001 exige o mesmo tratamento. Foram duas tentativas
em cada rodada técnica (máximo autorizado: três), com espera de 50 ms. Não
houve retry automático de conflito CAS, regra inválida ou resultado incerto.
O timeout SQL de lock 55P03 foi separado e teve rollback/consulta comprovados;
timeout do cliente nunca foi anunciado como rollback.

## Cobertura e resultados

A [matriz](ORG-03-E2-MATRIZ-RESULTADOS.md) associa todos os critérios a provas
individuais. C03 injeta falha depois de versão, recibo, índice de operação,
índice de grant, cabeça, direitos e obrigações, para os três agregados.
As injeções P0099 são falhas esperadas de teste, sem correção do jogo.
C07 usa conexão privilegiada isolada para corromper uma fixture, restaurada
após comprovar recusa; o runner normal não tem esse poder.

## Reprodução e descarte

O diretório final é de uso único e já foi descartado quanto a dados/segredos.
Não tente retirar a guarda para reutilizá-lo. Uma rodada adicional precisa
de run-id novo, cópia dos três scripts, manifesto/aceite aplicável e
revalidação completa de porta/caminho/espaço/ACL/identidade. O runner deriva
o caminho do próprio arquivo; não aceita DSN nem porta alternativos.
Não instalar drivers ou apontar para o serviço 5432.
As evidências em temp são locais/ignoradas pelo Git; não constituem pacote
publicado. Falhas anteriores permanecem nos respectivos run-ids.

Referências técnicas consultadas: [isolamento PostgreSQL 18](https://www.postgresql.org/docs/18/transaction-iso.html),
[locks/deadlocks](https://www.postgresql.org/docs/18/explicit-locking.html) e
[initdb](https://www.postgresql.org/docs/18/app-initdb.html).
Essas referências orientam o ensaio; os resultados provêm dos artefatos locais.
