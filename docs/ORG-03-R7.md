# ORG-03-R7 — Preparação do gate E1 e proposta E2

## Missão e estado

Preparação documental em 11/11/2026 para revisão humana. O objetivo é tornar
as decisões D01–D34 revisáveis por recorte e apresentar um laboratório SQL
sintético que possa ser autorizado separadamente. **E1 NÃO ENCERRADO; E2 NÃO
AUTORIZADO/NÃO EXECUTADO.** A preparação concluída não é aprovação de política.

Branch `beco`, HEAD `13b91dca20e16d6729d2dced79c1a4431b74ca09`. Há trabalho
local anterior extenso, inclusive fontes modificados e exclusões já existentes.
Status completo/diff anterior e manifesto de preservação foram guardados em
`temp/org03-r7/`; nenhuma alteração foi descartada ou incorporada a commit.

## Auditoria e diagnóstico

AGENTS.md, skill de engenharia do servidor e orientações PostgreSQL foram
consultados. Foram revisados histórico/relatórios R1–R6 e os contratos R6 de
durabilidade, políticas, PostgreSQL, falhas, decisões e gates. Os oito documentos
R6 coincidem com os hashes de sua entrega. Não houve reescrita dos estados de
aprovação ou dos laboratórios anteriores.

| Etapa | Evidência anterior e limite mantido |
| --- | --- |
| [R1](ORG-03-R1-RELATORIO-FINAL.md) | DTO/validador imutável; sem importador/ativação autorizada |
| [R2](ORG-03-R2-RELATORIO-FINAL.md) | Captura experimental; não fornece ledger econômico durável |
| [R3](ORG-03-R3-RELATORIO-FINAL.md) | Read models nativos; ausência/precisão não viram valores fictícios |
| [R4](ORG-03-R4-RELATORIO-FINAL.md) | Captura no owner real e Session atual; sem identidade/época durável |
| [R5](ORG-03-R5-RELATORIO-FINAL.md) | Idempotência econômica em RAM; não comprova disco/restart nem todas as regras do jogador |
| [R6](ORG-03-R6-RELATORIO-FINAL.md) | Contratos e 34 decisões, 12 comparações, 30 falhas futuras; nenhuma garantia SQL implementada |

A matriz continua com 6 decisões aprovadas anteriormente, 13 recomendadas,
11 pendentes e 4 bloqueadas por evidência. É possível testar storage sintético
sem decidir login, lease, transporte ou gameplay definitivo. A instalação local
PostgreSQL 18.6 permite propor binários existentes, mas o serviço ativo 5432 é
preexistente e não pode ser alvo do ensaio. Docker/Podman não foram encontrados
nas consultas; não são necessários para a rota recomendada.

Consultados `logs/README.md`, status do monitor e TXT locais. O status é histórico
e sem pendências declaradas; seis TXT de IDE/World Studio/cliente já existentes
não constituem autorização para corrigir esses sistemas nesta R7. Não houve
novo erro tratado ou alegação de homologação desses relatórios.

## Entregáveis e escolhas propostas

| Documento | Função |
| --- | --- |
| [Quadro E1](ORG-03-R7-QUADRO-APROVACAO-E1.md) | Nove campos por decisão D01–D34 em tabelas complementares; efeitos/settlement detalhados; aceites separados |
| [Escopo E2](ORG-03-R7-ESCOPO-E2.md) | Cinco autorizações L01–L05, agregados sintéticos, sequência de execução e parâmetros experimentais |
| [Ambiente PostgreSQL](ORG-03-R7-AMBIENTE-POSTGRESQL.md) | Inventário real e procedimento futuro de cluster/credenciais/alvo/encerramento/descarte isolados |
| [Critérios E2](ORG-03-R7-CRITERIOS-E2.md) | C01–C16, métricas/evidências, cobertura das 12 falhas prioritárias e separação E3/E4 |
| [Relatório final](ORG-03-R7-RELATORIO-FINAL.md) | Resultado da preparação, conferências e requisitos ainda sem autorização |

Recomendações permanecem não aprovadas: preservar regras por origem em XP e
moedas; entrega única de missão; coleta por item com origem individual;
comandos completos; manifesto + commits por beneficiário; tempo offline somente
após escolha/prova; seis políticas de penalidade separadas; A provisória para
legado; comparar ordenação/prefixo/lacunas sem esquecer obrigação comprovada.

Foi atualizada somente a organização principal, por adendo que registra a
preparação E1. Os seis documentos R7 são novos; não há novo Java, teste de jogo,
SQL, DDL, migration, driver ou harness executável E2. O verificador documental
local é uma evidência R7, não o laboratório futuro.

## Validação, limites e critérios de pronto

Conferir UTF-8/links/tabelas, correspondência de estados com R6, cobertura de
D01–D34/C01–C16/L01–L05 e falhas prioritárias, além de hashes de fontes/dados e
documentos anteriores. O resultado fica em `temp/org03-r7/validation.json`.

Não executar Maven/Java/SQL para esta mudança documental. Consultas de versão
não iniciaram banco. Os 1.196 testes aprovados e 57 excluídos são baseline
histórico R5, sem nova execução R7. Nada nesta entrega prova durabilidade entre
processos, posse real, continuidade temporal ou ativação do personagem.

O pacote está pronto quando puder ser revisado sem inferir aprovações e quando
ambiente/escopo/critérios deixarem explícito o que pode ser autorizado. Uma
decisão humana posterior pode aceitar somente ensaios, mantendo escolhas do
jogo e garantias bloqueadas sem alteração. Nenhuma aprovação foi presumida.

## Reversão e próximo passo

Reverter apenas os seis documentos novos e o adendo R7, comparando com
`temp/org03-r7/ORGANIZACAO-before.md`. Não restaurar a organização inteira se
houver edição posterior, nem executar reset/clean. Não há reversão de banco,
save ou runtime a realizar. Próximo passo sugerido: revisão humana dos itens
do quadro E1 e L01–L05; nenhuma execução E2 automática.
