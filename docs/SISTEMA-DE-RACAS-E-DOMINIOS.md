# Spirit Gate
## Raças, estilos de combate e domínios

Documento de decisões de design • Versão 1.1 • 21 de outubro de 2026

### 1. Visão do sistema

Spirit Gate é um jogo inspirado em Bleach, centrado em espadas, com combate desarmado, arcos e técnicas espirituais. A identidade do personagem reúne raça, fundamentos, domínio e técnicas livres.

A raça será escolhida na criação do personagem. O começo apresenta uma pequena alma em um caminho de origem para as raças. Cenário, duração e apresentação final desse percurso ainda serão produzidos. O personagem nasce sem classe, domínio ou elemento de especialização; as ações raciais básicas não contam como domínio.

### Conceitos separados

| Conceito | Função |
| --- | --- |
| Raça | Origem escolhida na criação; determina os domínios disponíveis. |
| Modalidade | Espada, arco ou combate desarmado. |
| Domínio | Caminho de especialização escolhido a partir do nível 15. |
| Nível do personagem | Progressão geral obtida com quests e mobs. |
| Nível de domínio | Evolução independente do caminho confirmado. |
| Técnicas livres | Árvore compartilhada, independente do domínio; Shunpo exige nível 20 de personagem. |

“Classe” é o termo usado na conversa; a estrutura formal separa raça, domínio e progressão. Uma raça pode ter vários caminhos disponíveis, mas o personagem só desenvolve um deles até efetuar reset.

### Leitura deste documento

Regras confirmadas registram as decisões do projeto. Exemplos de técnicas e identidades de combate orientam o protótipo; valores não aprovados continuam pendentes. O documento distingue a regra desejada da disponibilidade no cliente local. Registrar uma regra não significa que todos os seus efeitos já foram implementados.

---

## 2. Raças e início do personagem

| Raça | Combate inicial | Domínios disponíveis no nível 15 |
| --- | --- | --- |
| Reaper | Aprendiz de ceifador com uma espada pequena. | Água, trovão, fogo ou vento. |
| Human | Socos desarmados normais. | Luz ou especialização marcial. |
| Aetheri | Socos desarmados normais. | Arco de longa ou curta distância. |
| Spirit | Feixes de luz básicos. | Trevas de efeitos negativos ou de controle. |

### Reaper

Pode escolher água, trovão, fogo ou vento. Luz e trevas não pertencem aos seus domínios. O início como aprendiz ensina os fundamentos da espada antes da especialização.

### Human

Pode seguir luz ou evolução marcial avançada. A luz foi desenvolvida como meio de humanos lutarem ao lado dos Reapers contra espíritos. Pode ferir qualquer raça, inclusive Reapers, e inclui uma vertente de cura. Não possui vantagem automática contra trevas.

### Aetheri

Nome aprovado para a raça do arco. Começa desarmado; o arco define sua evolução especializada. Longa e curta distância são maneiras de usar o arco, sem converter a opção de curta distância em um espadachim.

### Spirit

Começa com feixes luminosos como ataque racial básico. Esse efeito visual não concede acesso ao domínio humano de luz, cura ou habilidades de especialização. Seus domínios continuam sendo trevas: efeitos negativos ou controle.

### Fundamentos compartilhados

Todas as raças podem executar ataques desarmados básicos. A evolução avançada do combate desarmado é uma especialidade humana. As árvores terão técnicas compartilhadas; essa característica não autoriza usar domínios incompatíveis ou acumular mais de um domínio.

---

## 3. Iniciação e progressão

### Jornada inicial

1. O jogador escolhe a raça na criação e inicia como uma pequena alma no percurso de origem.
2. Aprende o combate básico da raça e avança por quests e mobs.
3. No nível 15 de personagem, recebe acesso às prévias dos domínios compatíveis.
4. Visualiza técnicas e habilidades de cada caminho antes de confirmar sua escolha.
5. O domínio confirmado começa do zero e passa a ter progressão própria.

As prévias não concedem técnicas, XP ou desbloqueios permanentes. Sua apresentação e o conteúdo de cada demonstração ainda serão produzidos. A escolha no nível 15 vale para todas as raças.

### Fontes de experiência

| Fonte ou regra | Decisão |
| --- | --- |
| Quests e mobs | Fontes de XP de personagem e de domínio. |
| Um mob de nível 1 | 3 de XP de personagem e 1,5 de XP de domínio. |
| Domínio não escolhido | Não acumula XP de domínio antes da iniciação. |
| Precisão de XP | O progresso de domínio deve preservar valores fracionários. |
| Outros mobs e quests | Valores e fórmulas ainda serão definidos. |
| Limites e desbloqueios | Curvas finais, teto de nível e requisitos de técnicas permanecem abertos. |

A ausência de acúmulo antes da escolha é a interpretação adotada para a base: o nível 15 inicia o desenvolvimento específico, sem reserva retroativa. Essa regra deve permanecer explícita na interface e na validação do sistema.

### Escolha permanente

Não existe alternância normal entre domínios já aprendidos. O personagem desenvolve apenas seu caminho confirmado. Para jogar com outro domínio, o usuário cria outro personagem ou realiza o reset do atual.

### Técnicas livres

A árvore compartilhada acompanha a evolução geral e usa requisitos próprios. Todas as raças têm acesso ao Shunpo somente a partir do nível 20 de personagem; nível de domínio não substitui esse requisito. Os demais requisitos e o catálogo completo ainda serão definidos.

---

## 4. Reset e preservação

### Condições confirmadas

| Regra | Decisão |
| --- | --- |
| Preço | 35 Spirit Coins, equivalentes a R$ 35. |
| Moeda | Cash virtual comprado com dinheiro real. |
| Acesso | Uma quest de reset deve ser concluída. |
| Frequência | Um reset a cada mês. |
| Resultado | Remove a escolha antiga e zera o domínio e suas melhorias. |
| Nova escolha | Outro domínio compatível com a mesma raça, começando do zero. |
| Equipamentos | Não existem equipamentos vinculados ao domínio. |

### O que permanece

O reset preserva a base do personagem: identidade, raça, XP e nível geral, inventário, equipamentos e progresso independente do domínio. Não muda a raça nem concede migração de especialização gratuitamente. Técnicas e melhorias exclusivas do caminho abandonado deixam de estar disponíveis.

A regra “um mês” será tratada na base como um mês de calendário contado do reset anterior, com ajuste para o último dia válido do mês quando necessário. O fuso horário oficial do servidor ainda será definido. A política é por personagem. Isso deve ser confirmado antes da operação comercial, inclusive se o limite será compartilhado por conta.

### Fluxo esperado

1. Cumprir a quest de reset.
2. Verificar a janela mensal, saldo de 35 Spirit Coins e domínio atual.
3. Apresentar o custo e o progresso de domínio que será perdido.
4. Confirmar a operação e processar cobrança e reset de forma indivisível.
5. Registrar a data e retornar à escolha de domínio, preservando a base.

### Limite do protótipo local

A moeda paga exige uma carteira e um serviço de pagamentos confiável no servidor. O cliente local não representa uma compra real nem concede saldo pago. Integração de compra, reconciliação de pagamento, saldo autoritativo e execução comercial da quest ainda precisam de implementação antes do lançamento público. A política de reset pode ser validada localmente sem apresentar um checkout fictício.

---

## 5. Combate e habilidades

| Caminho | Identidade proposta |
| --- | --- |
| Água | Fluidez, aparos e contra-ataques. |
| Trovão | Investidas e golpes rápidos. |
| Fogo | Pressão, queimadura e controle de espaço. |
| Vento | Alcance dos cortes e reposicionamento. |
| Luz | Ataque, proteção e cura espiritual humana. |
| Marcial | Combos próximos, interrupções e pressão desarmada. |
| Arco longo | Precisão, tiros carregados e manutenção de distância. |
| Arco curto | Disparos rápidos, leques de flechas e reposicionamento. |
| Trevas: efeitos negativos | Marcas, enfraquecimento e dano ao longo do tempo. |
| Trevas: controle | Lentidão, imobilização breve, zonas e deslocamentos forçados. |

### Shunpo: todas as raças, nível 20 de personagem

Ofensivo: teleporta para um ponto livre à frente do inimigo. A execução deve validar alvo, alcance, linha de visão e obstáculos. O deslocamento não precisa causar dano por si só.

Defensivo: permite esquivar de uma habilidade pela execução no momento adequado. Janela, direção, interação com projéteis e áreas ainda exigem detalhamento. A recarga compartilhada entre ofensivo e defensivo continua como diretriz proposta, sem valor final aprovado.

### Cura e controle

A cura integra a luz. A direção aprovada prevê tempo de execução e possibilidade de interrupção; alvos, autocura, alcance e números finais serão definidos no catálogo de técnicas.

Controle pode afetar aliados, inclusive integrantes da mesma party. Não haverá imunidade automática por vínculo de grupo: o jogador precisa considerar área, direção e posicionamento. Essa regra não determina automaticamente que todo dano direto também atinja aliados.

A proteção contra cadeias de controle deve permitir reação, inclusive em fogo amigo. Fórmula de retorno decrescente, imunidade temporária e durações ainda precisam de balanceamento. O cliente offline não valida encontros reais entre jogadores; essa interação precisa de um teste com múltiplos participantes no servidor.

---

## 6. Transição e pontos pendentes

### Direção técnica aprovada

Guardian e Mystic deixam de representar opções da criação atual. O novo fluxo usa raça na criação, ausência de domínio no início, iniciação no nível 15 e Shunpo no nível 20. Componentes antigos incompatíveis podem ser removidos; saves pessoais e ferramentas úteis não são descarte automático.

O launcher desktop é o caminho principal de desenvolvimento e distribuição. Deve compilar as fontes atuais, respeitar Java 17 como alvo e impedir uso silencioso de classes antigas. World Studio e testes de mapas continuam úteis à produção do jogo.

O experimento web deixou de ser o caminho do produto. Não foi encontrado um aplicativo web autônomo nesta cópia do repositório. Os HTML remanescentes são relatórios de revisão de arte e devem ser preservados enquanto usados pelas ferramentas. Em 21/10/2026, o laboratório público anterior foi restrito ao proprietário via Sites, sem permissões externas. O conector não oferece exclusão definitiva; o site não foi republicado.

### Ainda precisa ser definido ou produzido

- Cenários e apresentação final do percurso da pequena alma por raça.
- Curvas finais de XP, limites e requisitos das técnicas; recompensas além do exemplo de mob nível 1.
- Catálogo de técnicas e divisão entre árvores próprias e compartilhadas.
- Demonstrações visuais completas de todos os domínios na iniciação.
- Conteúdo da quest de reset e regra comercial mensal por conta ou personagem.
- Serviço real de carteira, compra de cash e execução autoritativa do reset.
- Alcances, custos, recargas e janela do Shunpo.
- Valores de cura, interrupção, efeitos negativos e controle que atinge aliados.
- Validação multiplayer das regras de alvo e controles, instalador/atualizador e homologação de lançamento.

### Critérios de validação

- Novo personagem escolhe raça e começa sem domínio.
- Human e Aetheri começam com socos; Reaper com espada pequena; Spirit com feixe básico.
- Iniciação e escolha só ocorrem a partir do nível 15 e respeitam a raça.
- Prévias não alteram progresso permanente; domínio não pode ser alternado sem reset.
- Mob nível 1 concede 3 de XP geral e 1,5 de domínio após a escolha.
- Reset exige quest, 35 Spirit Coins e intervalo mensal, sem apagar a base ou equipamentos.
- Shunpo exige nível 20 de personagem em todas as raças.
- Luz não recebe bônus automático contra trevas; controles podem atingir aliados da mesma party.

Referência complementar: docs/AUDITORIA-PRE-CLASSES.md. O status de execução e testes da rodada deve ser conferido no relatório de implementação; este documento consolida as regras e os limites conhecidos.