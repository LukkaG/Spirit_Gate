# Varredura antes do sistema de classes

Atualização posterior: os defeitos de evento de morte repetido, redução de XP e
seleção de alvo oculto foram corrigidos durante a preparação do launcher e das
novas regras. Este arquivo preserva o diagnóstico original; veja
[entrega de raças e launcher](ENTREGA-RACAS-LAUNCHER.md) para o estado atual.

Data: 21/10/2026. Revisão dos inicializadores, domínio, RPG offline, persistência,
criação/seleção de personagem e integração com o World Studio. Não é uma certificação
de todo o projeto nem uma homologação visual. As regras do jogo não foram alteradas.

## Resultado

Há uma base funcional, mas criar novas classes agora exigiria editar vários pontos
independentes. Recomenda-se corrigir os defeitos abaixo e estabelecer o contrato das
classes antes de acrescentar conteúdo ou alterar o formato dos saves.

## Defeitos encontrados

### 1. Evento de morte pode ser publicado repetidamente — prioridade alta

Em `server/src/main/java/com/projetok/domain/combat/CombatService.java`, `attack`
aplica dano e publica `EntityDeathEvent` sempre que o alvo está morto ao final.
Não verifica se ele já estava morto antes do ataque.

Reprodução executada: alvo com 1 HP, dois ataques de 10, dois eventos para uma morte.
Os chamadores que filtram mortos evitam esse caso, mas o serviço não garante a
invariante. Futuros efeitos de área, habilidades e consumidores de loot/XP podem
processar a mesma morte novamente.

Correção proposta: recusar alvo morto e publicar morte somente na transição de vivo
para morto; testar ataques sucessivos e dano zero. Definir também se o retorno
representa dano calculado ou HP efetivamente perdido, pois hoje retorna o primeiro.

### 2. Recompensa pode diminuir XP aceito pelo sistema — prioridade média

Em `server/src/main/java/com/projetok/rpg/RpgProgression.java`, `level` aceita XP até
1.000.000 e `RpgSave` usa o mesmo teto. Entretanto, `award` limita o resultado ao
limiar do nível máximo, atualmente 11.400.

Reprodução executada: `builtin().award(1_000_000, 0)` retorna `11_400`.
É um caso de estado importado/construído ou save editado; a progressão normal já
limita o XP. O problema é a divergência entre os estados aceitos e a operação.

Correção proposta: escolher uma política única para o teto e a migração de saves.
Uma recompensa não deve diminuir o XP de um estado considerado válido.

### 3. Escolha de alvo ocorre antes do teste de visibilidade — prioridade média

Em `server/src/main/java/com/projetok/rpg/RpgWorld.java`, `strike` escolhe o inimigo
mais próximo no alcance e só depois testa `lineClear`. Se esse inimigo estiver
atrás de uma parede, o golpe falha mesmo havendo outro inimigo visível no alcance.

Achado por leitura, sem reprodução de cenário nesta rodada. Correção proposta:
incluir visibilidade na elegibilidade antes de escolher o alvo mais próximo e
validar com dois inimigos, um oculto e outro visível.

## Incongruências relevantes para as classes

### Catálogo parcial e identificadores duplicados — prioridade alta

`rpg/archetypes.properties` define HP, ataque e habilidade primária, mas:

- `Archetypes` fixa os dois IDs em código e só aceita `dash` ou `mend`.
- `CharacterIdentity` repete os dois IDs permitidos.
- `RpgWorld.attack` fixa alcance em 145 para Mystic e 82 para os demais.
- `RpgSecondaryAbility` fixa habilidades secundárias em um `switch`.
- `CharacterCreation` cria dois botões e usa condicionais entre eles.
- `CharacterSelection` apresenta qualquer ID diferente de Guardian como Místico.
- `RpgPlayerSprites` escolhe efeitos visuais por uma comparação com Mystic.

Adicionar uma linha ao catálogo não cria uma classe utilizável. A ausência de uma
definição central completa também facilita divergências entre tooltip, execução
e aparência. O leitor de `Archetypes` não valida contagem exata de campos nem
transforma campos ausentes em diagnósticos claros de configuração.

### Arena/domínio e RPG possuem contratos distintos — prioridade alta

`domain.factory.PlayerArchetype` agrega raça, atributos, estilo e arma para
`PlayerFactory`. A arena usa componentes, `CombatService`, afinidades de raça e
serviços de habilidades como `CryostasisService`.

O RPG usa `CharacterIdentity`, HP e vigor próprios em `RpgWorld`, dano direto nos
inimigos, defesa por equipamento e habilidades por identificador. Seu jogador é
um `GameEntity`, sem a composição de `PlayerFactory`.

Portanto, criar uma habilidade no domínio não a torna disponível automaticamente
no RPG. O comentário de `CombatService` que o descreve como caminho único de dano
do jogo não corresponde ao RPG atual. É necessário escolher o primeiro runtime
que receberá as classes e definir adaptadores ou uma extração compartilhada.

### Atributos de classe persistidos sem versão de balanceamento — prioridade média

`LocalSaveStore` grava `baseHp` e `baseAttack`; `CharacterIdentity` valida limites,
mas não sua correspondência ao catálogo. O comportamento permite preservar dados
antigos após mudanças de balanceamento, porém não distingue valores históricos
de valores arbitrários. Não é necessariamente um defeito no modo offline.

Antes das classes, decidir se atributos-base serão derivados da definição atual
ou preservados com uma versão explícita. Não exigir igualdade com o catálogo
atual sem considerar migração de personagens existentes.

### Recursos e recargas têm unidades distintas — prioridade média

O domínio usa vigor em ponto flutuante e recargas em segundos. O RPG usa vigor
interno de 0 a 1000, custos multiplicados por 10 e recargas em ticks. Habilidades
primária e secundária compartilham `abilityReadyTick` intencionalmente.

Uma definição compartilhada precisa declarar unidades e grupos de recarga; copiar
os números entre esses runtimes muda o comportamento. O compartilhamento atual
de recarga não foi classificado como erro, pois está documentado no código.

## Inicialização e ferramentas

- `rodar-prototipo.cmd` abre a arena histórica, não a criação de personagem RPG.
  Ele executa os testes antes de abrir; sua falha pode ser de ambiente/build.
- `testar-mapa-rpg.cmd` não verifica compilação antes de chamar `StudioPlaytest`,
  ao contrário de `world-studio.cmd` e `testar-rpg-sprites.cmd`.
- Os inicializadores com classpath direto podem usar bytecode desatualizado:
  verificar a existência de um `.class` não confirma sua correspondência ao fonte.
- O ambiente desta rodada usa JDK 24; `tools/qualidade.py` exige Java 17 exato,
  enquanto o POM declara alvo Java 17. Compatibilidade de compilação e versão
  exigida pela ferramenta precisam estar documentadas de maneira consistente.
- `qualidade.py` força `.maven-repository`, enquanto o inicializador da arena usa
  o repositório padrão. Um teste offline pode funcionar em um fluxo e falhar no outro.
- `StudioPlaytest` registra a tecla F duas vezes consecutivas. O segundo vínculo
  substitui o primeiro; remover a duplicação simplifica a manutenção.

## Sequência recomendada

1. Corrigir evento de morte, política de XP e seleção de alvo, com regressões.
2. Restabelecer a execução completa dos testes em Java 17 com cache Maven válido.
3. Definir o runtime inicial das classes e separar os conceitos de raça, classe,
   aparência e estilo/arma. Não transformar raça e classe no mesmo identificador.
4. Expandir um catálogo único de classes com ID estável, nome, descrição,
   atributos-base, progressão, ataque básico, habilidades e referências visuais.
   As telas devem listar esse catálogo e resolver seus rótulos por ID.
5. Separar definição imutável de classe do estado do personagem: HP/vigor atuais,
   recargas e efeitos ativos pertencem à sessão, não ao catálogo.
6. Extrair execução de habilidades com validação de alvo, alcance, linha de visão,
   custo e recarga. Validar antes de consumir recursos e publicar resultados para
   a interface sem inserir regras de classe no Swing.
7. Definir compatibilidade e migração do save antes de alterar atributos ou IDs.
8. Adicionar uma terceira classe de teste para verificar que criação, combate,
   apresentação e save funcionam sem novos condicionais por classe.

Critérios de conclusão: classes enumeradas pelo catálogo, ID desconhecido com erro
claro, uma morte com um evento, recompensa sem perda de XP, seleção de alvo visível,
recusa sem consumo, recargas consistentes e save antigo carregando pela política
definida. Balanceamento e nomes das classes permanecem decisões de conteúdo.

## Verificação desta rodada

- `python -m unittest discover -s tools -p 'test_*.py'`: **30 testes aprovados**.
- Compilação direta com `javac --release 17`: **188 fontes de produção** compilados;
  excluídos apenas `KServer`, `Kclient` e `Network`, que dependem do Kryonet legado.
  JDK utilizado: 24; isso valida o alvo de compilação 17, não a execução em JDK 17.
- `DebugWorldSelfTest`: **6 verificações aprovadas**, sem abrir janela.
- Sondas locais confirmaram regressão de XP e repetição de evento de morte.
- Maven/JUnit completo: **não concluído**. A primeira tentativa falhou ao criar
  `C:\.m2\repository`; com cache explícito do usuário, falhou na dependência
  `kryo-2.24.0.jar`, e também informou ausência do POM do Surefire 3.5.2 no cache.
  Esses resultados não permitem afirmar que a suíte Java passou ou falhou por
  defeito do código. Não foram baixadas dependências nem alteradas configurações
  globais do Java/Maven.
- Nenhuma janela foi aberta e nenhum save do usuário foi alterado. Artefatos de
  compilação e sondas ficaram em `server/target/auditoria`, ignorado pelo Git.

Os números históricos dos READMEs e relatórios anteriores são registros de outras
rodadas e não substituem os resultados acima.
