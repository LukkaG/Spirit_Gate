# Compatibilidade ? Coruj?o IV

Fonte de verdade: checkout auditado na TASK-134; consulte CORUJAO-IV-ARQUITETURA.md.

| ?rea | Contrato preservado | Consequ?ncia para pr?ximas tarefas |
|---|---|---|
| Ra?as | CharacterRaces, quatro op??es atuais | Editor consulta cat?logo; legado somente na leitura |
| Dom?nios | Domains, dez defini??es | Filtrar por ra?a; nenhuma t?cnica conceitual execut?vel |
| Escolha | N?vel 15 e escolha ?nica | Quest n?o concede dom?nio |
| XP de dom?nio | Meias unidades inteiras | Recompensas usam DomainProgression; sem XP antes da escolha |
| Shunpo | N?vel 20, vigor e recarga compartilhada | Testar geometria e estado sem alterar velocidade |
| Save | V2 com leitura V1 | Quests novas somente sint?ticas enquanto cat?logo fixo limitar valida??o |
| Mapas | sgmap V1, adaptador de cenas | Dimens?o do editor n?o amplia limites do save |
| Rede | Coop presente | N?o alterar protocolo nem restaurar classes antigas |
| Arte | Escala 0.5, walk 250 ms | Nenhuma substitui??o de PNGs ou associa??es inventadas |

A continuidade exige contrato declarativo seguro e inje??o controlada no QuestService, com sess?o sint?tica que n?o escreva em saves pessoais. N?o h? autoriza??o para alterar schema incompat?vel.
