# Corujão VIII - pendências após TASK-222

Atualização de 01/11/2026 — Energy entregue na build privada 0.226.0-private, com os coeficientes aprovados e validação local. [Relatório e limites](ENERGY-RELATORIO.md) · [PDF](Spirit_Gate_Energy_Relatorio.pdf). A distribuição de atributos continua de sessão; persistência durável e homologação física por Radmin permanecem fora desta entrega. Os registros seguintes são históricos.

- Homologação artística e legibilidade humana nas capturas e no jogo pendentes.
- Teclado físico no Windows não foi comprovado. A prova anterior mostrou que Robot não entrega eventos nativos neste ambiente. A cobertura usa EventQueue, KeyEventDispatcher, foco e bindings Swing reais; não chama somente ActionMap para simular o resultado. Não equivale a digitação física.
- Radmin entre dois computadores e execução do iniciador nativo EXE ainda exigem conferência humana. Loopback e interface empacotada foram comprovados localmente.
- Música e efeitos não possuem canais separados na base atual; não foram criados sliders fictícios ou novo backend de áudio. Volume/mute mantêm o contrato de preferências existente. Confirmar áudio ouvido e requisitos futuros antes de expandir esse sistema.
- Atalhos personalizados e resolução são de sessão. Preferências existentes mantêm schema e localização. Progresso multiplayer segue efêmero; reconectar abre nova sessão, sem promessa de recuperação de XP/itens.
- A atualização visual não foi estendida a offline/Coop ou interfaces do Studio; seus fluxos foram preservados e cobertos por regressões.
- Nenhuma medição nova de FPS ou homologação de longa duração foi realizada. A atualização usa o timer existente, sem um timer por slot/painel.

Recomenda-se revisar a build privada com teclado físico e dois computadores, verificar brilho/foco nas três resoluções e registrar o aceite visual antes de qualquer publicação. Não há publicação ou feed remoto configurados nesta entrega.


[Relatório final](CORUJAO-VIII-RELATORIO-FINAL.md). Não há tarefa de implementação restante no escopo visual suportado; canais separados de áudio constituem a ressalva da TASK-216. Homologação humana permanece pendente.


## Evolução posterior: alvo de combate — 01/11/2026

Seleção PvE por TAB e ataque dirigido implementados na build privada 0.223.0-private. O ataque básico vira sem caminhar; o Shunpo ofensivo aproxima do alvo dentro de 240 unidades ou avança 70 na direção atual quando não há alvo. Nenhuma habilidade atual exige alvo obrigatório, conforme confirmação do responsável. TAB conserva navegação dentro dos painéis. [Regras, testes e pacotes](TARGET-COMBATE.md) · [PDF](Spirit_Gate_Target_Combate_Relatorio.pdf). Esta evolução é posterior à consolidação 0.222.0-private registrada acima.


## HUD compacta e ficha — 01/11/2026

Evolução 0.224.0-private: ícones menores sem caixas permanentes, detalhes de item ao clicar, Personagem separado de Habilidades, equipamentos e distribuição de Força/Inteligência/Agilidade no servidor. Força usa a fórmula existente; INT/AGI ficam reservadas às próximas técnicas, conforme indicado na interface. Pontos e progresso são de sessão. Validação: 682 testes gerais aprovados e 49 execuções GUI aprovadas, sem falhas. [Relatório e pacotes](HUD-COMPACTA-RELATORIO.md) · [PDF](Spirit_Gate_HUD_Compacta_Relatorio.pdf). As versões e contagens anteriores acima permanecem históricas.


## Regras de atributos — 01/11/2026

Evolução 0.225.0-private: STR aumenta dano básico/desarmado, INT rege o bônus de domínio e AGI aumenta a cadência básica e a chance de esquiva de hit básico. Coeficientes e tetos foram aprovados pelo responsável. Shunpo continua sem dano; executores das próximas técnicas de domínio usarão a fórmula de INT. Validação: 689 testes gerais e 56 execuções GUI aprovados, sem falhas. [Regras e pacotes](ATRIBUTOS-COMBATE-RELATORIO.md) · [PDF](Spirit_Gate_Atributos_Combate_Relatorio.pdf). As indicações anteriores de INT/AGI reservadas são históricas da versão 0.224.0-private.



