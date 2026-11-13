# Corujão VIII - progresso final

Atualização de 01/11/2026 — build 0.226.0-private: Energy (ENE) integrado à distribuição de pontos, capacidade e regeneração no servidor, ficha e HUD. Cada ponto fornece +2 de capacidade e +1% de regeneração, com bônus de regeneração limitado a +100%. [Relatório completo](ENERGY-RELATORIO.md) · [PDF](Spirit_Gate_Energy_Relatorio.pdf). Os registros abaixo mantêm o histórico das versões anteriores.

31/10/2026. Continuação das TASKs 214-222 expressamente autorizada.

| Tarefa | Estado |
|---|---|
| 213 e adendos | Implementados e validados localmente |
| 214 - Inventário em grid | Implementada e validada localmente |
| 215 - Diário de missões | Implementada e validada localmente |
| 216 - Configurações | Entregue no escopo suportado; sem canais separados de áudio |
| 217 - Identidade visual compartilhada | Implementada e validada localmente |
| 218 - Feedback de combate | Implementada e validada localmente |
| 219 - Minimapa e utilitários | Implementada e validada localmente |
| 220 - Polimento e teclado | Implementada e validada localmente |
| 221 - Regressões | Implementada e validada localmente |
| 222 - Pacote privado | Implementada e validada localmente |

Suíte Java: 698 executados, 673 aprovados, 25 abortos gráficos, zero falhas e zero ignorados. Rodada GUI: 13 classes, 36 execuções aprovadas, zero abortos e falhas. Os totais se sobrepõem.

Dois pacotes privados 0.222.0-private gerados e verificados; nenhuma publicação. Aceite visual, teclado físico e Radmin entre computadores pendentes.

[Relatório](CORUJAO-VIII-RELATORIO-FINAL.md) · [PDF](Spirit_Gate_Corujao_VIII_Relatorio_Completo.pdf) · [Roteiro](CORUJAO-VIII-COMO-TESTAR.md).


## Evolução posterior: alvo de combate — 01/11/2026

Seleção PvE por TAB e ataque dirigido implementados na build privada 0.223.0-private. O ataque básico vira sem caminhar; o Shunpo ofensivo aproxima do alvo dentro de 240 unidades ou avança 70 na direção atual quando não há alvo. Nenhuma habilidade atual exige alvo obrigatório, conforme confirmação do responsável. TAB conserva navegação dentro dos painéis. [Regras, testes e pacotes](TARGET-COMBATE.md) · [PDF](Spirit_Gate_Target_Combate_Relatorio.pdf). Esta evolução é posterior à consolidação 0.222.0-private registrada acima.


## HUD compacta e ficha — 01/11/2026

Evolução 0.224.0-private: ícones menores sem caixas permanentes, detalhes de item ao clicar, Personagem separado de Habilidades, equipamentos e distribuição de Força/Inteligência/Agilidade no servidor. Força usa a fórmula existente; INT/AGI ficam reservadas às próximas técnicas, conforme indicado na interface. Pontos e progresso são de sessão. Validação: 682 testes gerais aprovados e 49 execuções GUI aprovadas, sem falhas. [Relatório e pacotes](HUD-COMPACTA-RELATORIO.md) · [PDF](Spirit_Gate_HUD_Compacta_Relatorio.pdf). As versões e contagens anteriores acima permanecem históricas.


## Regras de atributos — 01/11/2026

Evolução 0.225.0-private: STR aumenta dano básico/desarmado, INT rege o bônus de domínio e AGI aumenta a cadência básica e a chance de esquiva de hit básico. Coeficientes e tetos foram aprovados pelo responsável. Shunpo continua sem dano; executores das próximas técnicas de domínio usarão a fórmula de INT. Validação: 689 testes gerais e 56 execuções GUI aprovados, sem falhas. [Regras e pacotes](ATRIBUTOS-COMBATE-RELATORIO.md) · [PDF](Spirit_Gate_Atributos_Combate_Relatorio.pdf). As indicações anteriores de INT/AGI reservadas são históricas da versão 0.224.0-private.



