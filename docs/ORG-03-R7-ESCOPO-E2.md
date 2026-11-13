# ORG-03-R7 — Proposta de escopo mínimo E2

## Estado, alvo e fronteira

**RECOMENDADA, NÃO APROVADA. E2 NÃO INICIADO.** O alvo é um laboratório
PostgreSQL descartável com dados sintéticos, separado do runtime e do serviço
local 5432. A R7 autoriza apenas preparar este pacote. E1 não está encerrado:
nenhuma recomendação D01–D34 foi promovida a aprovação.

Proposta concreta: binários PostgreSQL 18.6 já encontrados, cluster novo em
`temp/org03-e2/<run-id>/data`, bind `127.0.0.1`, porta candidata 55432, database/
schema/roles próprios. Python 3.14 e biblioteca padrão orquestram psql por lista
de argumentos, sem shell e sem arquivo pessoal de inicialização. Não há driver
novo, JDBC, alteração de POM, serviço Windows novo ou integração multiplayer.
Viabilidade/permissões e porta precisam ser revalidadas antes da execução.

## Autorizações específicas a registrar

Cada item é independente e admite aceite parcial, recusa ou ajuste. Nenhum está
preenchido em nome do responsável. Uma recusa limita o trabalho ao subconjunto
autorizado; não habilita uma alternativa automaticamente.

| ID | Escopo exato proposto para aceite futuro | Não autoriza | Registro atual |
| --- | --- | --- | --- |
| L01 | Criar harness independente e DDL/fixtures exclusivamente experimentais em `temp/org03-e2/<run-id>/`, exercitando D04/D06/D08–D11/D18/D26/D31 e alternativas explicitadas D14–D17/D19/D21/D34 | Editar server/src, testes R1–R6, pom, migrations definitivas ou aprovar gameplay | NÃO SOLICITADO EM EXECUÇÃO / NÃO APROVADO |
| L02 | Criar/iniciar cluster novo com os binários existentes, diretório/porta/roles/credenciais descritos, sem serviço e sem acesso a bancos existentes | Instalar software, usar 5432, ler data directory/contas existentes ou ajustar firewall | NÃO APROVADO |
| L03 | Executar os casos/volumes/limites deste escopo e dos critérios, com duas conexões SQL concorrentes e rollback; simular perda de resposta no harness sem crash | Matar processo de banco, reiniciar cluster, restore, failover, partição de rede, owner real | NÃO APROVADO |
| L04 | Guardar evidências sintéticas, hashes, comandos sanitizados, parâmetros e resultados locais; nenhum segredo em relatório/Git | Publicar, enviar dados ou reter senhas junto de evidências | NÃO APROVADO |
| L05 | Encerrar e descartar somente cluster/dados/segredos criados pela execução identificada, com prova de caminho/propriedade e parada; preservar evidências | Apagar recurso preexistente, limpar árvore temp inteira ou parar serviço instalado | NÃO APROVADO |

Para cada L: registrar responsável, data, aceite/recusa/ajuste, caminhos,
versão/porta/limites aceitos e ressalvas. Autorizar L01 não autoriza L02/L03.
O [quadro E1](ORG-03-R7-QUADRO-APROVACAO-E1.md) detalha cada decisão; nenhuma
aprovação genérica de toda a arquitetura é necessária ou proposta.

## Agregados sintéticos mínimos

| Agregado/fixture candidato | Invariante a ensaiar |
| --- | --- |
| Registro sintético de proprietários/personagens | Dois proprietários e personagens segregados; contexto confiável da fixture não vem do payload. Não são contas do jogo |
| Cabeça canônica + versão de estado | CharacterId único, StateRevision/EconomicRevision separados; economia gravável uma vez só, sem XP/saldo concorrentes em payload e tabela |
| Claim/fence simulado | Uma autoridade de escrita por personagem, geração/fence esperados; troca controlada por fixture. Não testa lease ou suspensão de corpo antigo |
| Operação e recibo | OperationId estável, intenção original imutável, beneficiário/kind/conteúdo verificados; resultado confirmado ligado à versão, nominal/efetivo disponíveis |
| Origem/concessão e índices | Dedup por operação e por origem/beneficiário, sem versão de missão gerar nova entrega; RNG já resolvido e sem rerrolagem |
| Direitos pendentes | Quantidades e origens individualmente rastreáveis na autoridade canônica; índice de coleta não duplica saldo |
| Obrigações e watermark | Intenção autorizada pendente distinta de pagamento; registrar lacunas conhecidas e prefixo liquidado, não só máximo visto |
| Catálogo de evidências/histórico | Referência íntegra a resultado antigo; indisponibilidade não vira ausência e não libera replay |

Não impor tabela por record Java. O DDL experimental futuro deverá indicar
quais invariantes são constraints e quais são validação/transação. Cabeça,
versão, recibo, índices e direitos afetados confirmam juntos. ID de personagem,
época, revisão e fence são valores sintéticos emitidos pelo controlador do
laboratório, nunca copiados de Peer.id/save. Não reutilizar DTO R1 como importador.

## Semânticas ensaiadas, sem aprovação para o jogo

- Perfil de compatibilidade candidato: XP truncado conforme origem, nominal e
  efetivo distintos (incluindo domínio/HP afetados); quest recusa moedas acima
  do teto e mob satura; missão de entrega única. Comparar recusa integral R5 em
  cenários separados, sem tocar no laboratório R5.
- Coleta candidata por item: registrar cada direito de origem consumido na
  transferência. Quantidade agregada de um item cabe inteira ou permanece; outros
  itens podem ficar pendentes. Comparar pacote inteiro em fixture separada.
- Agregados de comando completos: poção (item/HP/deadline), equipamento
  (bolsa/slot/atributos), recompensa (XP/domínio/HP, moedas, itens/direitos e
  missão quando aplicáveis). Dados sintéticos, sem executar gameplay real.
- Distribuição entre beneficiários: manifesto de origem e resultados resolvidos,
  commits individuais. E2 verifica reconciliação entre commits sem crash; E3
  precisa provar sobrevivência da distribuição à perda do processo.
- D34: comparar settlement ordenado, prefixo liquidado e registro de lacunas.
  A candidata combina registro explícito de obrigações e prefixo apenas
  comprovado; geração de mob não vira automaticamente sequência contígua de
  dívida. O stream da fixture enumera intenções autorizadas/sem prêmio resolvido
  para distinguir ausência comprovada de lacuna desconhecida.
- F25: persistir/ler casos de representação temporal e recusar conversões
  impossíveis. Não escolher fórmula ticks→timestamp ou alterar relógio do SO.
  D20/D21 continuam sem aprovação/homologação para recuperação real.

## Execução futura por checkpoints

1. Conferir aceites L01–L05 aplicáveis e manifesto de preservação. Implementar
   harness/DDL experimental no local autorizado; revisar antes de executar.
2. Aplicar isolamento e comprovação de alvo do
   [procedimento de ambiente](ORG-03-R7-AMBIENTE-POSTGRESQL.md). Nenhum teste pode
   usar defaults de conexão ou conectar ao serviço 5432.
3. Bootstrap cria objetos/fixtures. Runner tem privilégio limitado, sem poder
   criar roles ou apagar outro banco. Conexões psql persistentes A/B são processos
   distintos; canal de controle do harness impõe barreiras e lê resultado/SQLSTATE.
   A conexão observadora consulta depois do commit/rollback. Não simular SQL
   concorrente com duas funções que só disputam CAS em memória.
4. Rodar primeiro constraints/serial; depois concorrência com barreiras
   determinísticas, não sleeps como prova; finalmente retenção/lacunas/casos
   negativos. Relatório por caso associa intenção, conexão, base, SQLSTATE,
   revisões, recibo, recursos e consultas posteriores.
5. Lookup autoriza acesso sintético atual e procura recibo **antes** de exigir
   revisão/fence históricos atuais. Só uma transição nova exige claim/fence e
   revisões esperados. Repetição idêntica confirmada devolve o resultado antigo,
   sem instalar seu snapshot como cabeça nova.
6. Perda de resposta simulada: suprimir a entrega do resultado de um commit
   conhecido pelo controlador, marcar UNCERTAIN no chamador e consultar por
   outro canal com o mesmo ID. Não criar novo ID. Isso não prova crash ou falha
   de rede real; é apenas uma parte de F04.
7. Falha/timeout: rollback quando comprovado; resultado desconhecido permanece
   UNCERTAIN até reconciliação. Retry técnico limitado mantém intenção e ID;
   conflito de regra/revisão não autoriza rebase ou novo prêmio automático.
8. Interromper em violação de isolamento, invariantes ou quotas; registrar falha,
   encerrar apenas recursos próprios e emitir relatório. Não corrigir o runtime
   para fazer o ensaio passar. Descarte segue L05 e conserva evidências sanitizadas.

## Orçamentos candidatos do experimento

Todos os valores abaixo são **PENDENTE DE APROVAÇÃO**, apenas para E2, sem
equivalência com D25 de produção. Repetições usam namespace próprio por cenário.

| Parâmetro | Valor experimental proposto | Verificação/interrupção |
| --- | --- | --- |
| Personagens/proprietários | 8 personagens, 2 proprietários sintéticos | Fixtures fixas; nenhum identificador de jogador |
| Operações | Até 2.000 intenções únicas por execução | Contador inclui recusas/intentos; retries separados e também limitados |
| Conexões | 2 escritoras + 1 observadora; bootstrap não simultâneo | Máximo 3 sessões do harness, sem pool/JDBC; conexões internas do PG não são esse contador |
| Payload | Até 16 KiB por estado/comando; binário fracionário em representação exata candidata | Medir bytes UTF-8; acima do limite recusar, sem truncar |
| Recibos | Até 1.000 por cenário, até 2.000 na execução | Caso de pressão usa cap lógico 32 e arquivo sintético; nunca purge para caber |
| Histórico | Até 128 MiB de evidências/histórico exportado; até 512 MiB na árvore do run-id | Medir árvore inteira, incluindo dados/WAL/logs, entre lotes; não são quotas físicas garantidas |
| Espaço inicial | Pelo menos 2 GiB livres no volume do run-id | Gate antes de criar; margem para cluster e logs, não estimativa de produção |
| Transações | Um comando semântico por transação; limite externo 10 s | Sem owner/I/O externo; caso de lock é controlado pela barreira |
| Timeouts | Conexão 3 s; statement 5 s; idle em transação 5 s; transação 10 s | Caso normal lock 1 s; deadlock controlado admite até 4 s e timeout maior que detector observado |
| Retry técnico | Até 3 tentativas totais por intenção; espera 50/100 ms nas duas retomadas | Apenas SQLSTATE de deadlock/serialização elegíveis; não retry automático de UNCERTAIN ou regra inválida |
| Duração | Até 10 min de testes após bootstrap; encerramento ordenado com prazo de 15 s | Ao exceder, caso inconclusivo/falha, não sucesso; retenção segura se processo não parar |

Os limites por árvore são observações periódicas, sujeitos a crescimento entre
medições; se o ambiente não permitir respeitar a margem, não iniciar. Limite de
transação requer configuração efetiva suportada ou watchdog e evidência; não
confundir timeout do cliente com rollback certo. Nenhum limite foi configurado.

RPO/RTO, failover, restore, lease, orçamento do owner e configuração definitiva
de retenção estão fora deste aceite. Ver [critérios E2](ORG-03-R7-CRITERIOS-E2.md).

## Saída e exclusões

Saída futura: DDL/harness experimental revisado, fixtures e comandos sanitizados,
perfil efetivo do cluster, resultados por caso, artefatos de antes/depois e
manifesto de encerramento/descarte. Falhas não são excluídas da contagem.

Não inclui conta/Player real, saves pessoais, autenticação, lease real, transporte,
owner, callbacks, spawn, A-04, 40 skills, protocolo 6/7, runtime JDBC, E3/E4,
ORG-04/TASK-226, commit, push, release ou publicação. SQL bem-sucedido em E2
não autoriza anunciar durabilidade entre processos ou salvamento do jogador.
