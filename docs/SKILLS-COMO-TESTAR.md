# Revisão local das 40 habilidades

**Refinamento atual V9:** Água 5, Vento 3 e Raio 4 são investidas frontais sem alvo. Raio 5 oculta o personagem, remove colisão e impede dano durante a fase de aproximadamente 1,07 s; termina com quatro pulsos em toda a área. [Roteiro e capturas](skills-preview-v9/README.md), [detalhes técnicos](SKILLS-REFINAMENTO-V9.md). Os números indicam slots; os atalhos padrão continuam F5–F9 para os cinco slots.

Execute `tools\testar-skills.cmd` pelo Explorador ou, na raiz do projeto, pelo PowerShell:

```powershell
.\tools\testar-skills.cmd
```

O script usa JDK 17 ou superior, sem Maven, rede ou cache de dependências. Compila o jogo por `tools/compile-local.ps1` e a ferramenta separadamente; não instala dependências. As classes da ferramenta ficam em `.task-checkpoints/SKILLS40/preview-classes-v2`. Aguarde uma compilação terminar antes de iniciar outra no mesmo checkout.

Para outro Windows x64 **sem Java/JDK/Maven**, use o ZIP em `testes-portateis/skills-windows-x64`, extraia tudo em uma pasta gravável e execute `Abrir-Skills.cmd`. O Java acompanha o pacote. No repositório, veja as instruções e o SHA-256 em [teste portátil](../testes-portateis/skills-windows-x64/README.md). O roteiro distribuído dentro do ZIP pode ser consultado sem os demais documentos do checkout.

Escolha um dos oito kits na caixa inicial. A ferramenta inicia um servidor sintético de nível 20 em loopback, com porta temporária, e escolhe o domínio pelo pedido normal do jogo. A opção de personagem auxiliar abre uma segunda janela para experimentar curas, limpeza e revive. Fechar a janela principal encerra clientes e servidor.

Com o aliado habilitado, a prévia aguarda a sincronização dos dois personagens antes de escolher os domínios. A etapa 5/6 permanece visível durante essa espera; o kit principal e Luz do auxiliar são confirmados pelo servidor antes de anunciar a etapa 6/6.

O console mostra as três etapas de compilação e abertura. Ao terminar a compilação, aparece a janela **Spirit Gate | Revisar habilidades**; selecione o kit e confirme para iniciar a partida. Essa janela recebe foco na abertura. Se alternar para outro aplicativo, use ALT+TAB para voltar à seleção. Cancelar encerra sem iniciar servidor. Erros permanecem visíveis no console.

Essa ferramenta está em `tools/SkillsLocalPreview.java`, fora dos fontes de produção. Os personagens são efêmeros e não carregam saves pessoais. Preferências da prévia ficam em `.task-checkpoints/SKILLS40/preview-data/session-*/`; os caminhos criados para preferências são limpos ao encerrar normalmente. Arquivos adicionais são preservados.

Com a partida aberta, use **Domínio de treino**, no topo da janela, e clique em **Trocar kit / reiniciar treino**. O seletor oferece os dez domínios existentes: os oito kits, incluindo as variantes Arco curto e Trevas: efeitos negativos. A janela, seu tamanho e sua posição permanecem; a sessão temporária é recriada com a raça/domínio escolhidos, nível 20 e recursos novos. Isso também permite reiniciar o mesmo kit para testar novamente sem aguardar recargas.

A troca reinicia mobs, atributos e demais estados do treino; não transporta progresso entre kits. Se o auxiliar foi habilitado, ele volta como Human/Luz. Aguarde o aviso **Pronto** antes de usar as técnicas. Essa facilidade existe somente na ferramenta de revisão; o reset de domínio no jogo normal mantém suas regras. Para carregar esta atualização pela primeira vez, reabra `tools/testar-skills.cmd`; depois as trocas são feitas dentro da mesma janela.

## Controles e acesso normal

No jogo normal, técnicas exigem nível 15 e domínio escolhido. Shunpo continua exigindo nível 20. A prévia começa no nível 20 para permitir a revisão sem ganhar experiência primeiro.

| Controle padrão | Uso |
| --- | --- |
| Domínio de treino → Trocar kit / reiniciar treino | Trocar domínio/raça ou reiniciar o kit sem fechar a janela |
| WASD ou setas | Mover; durante a seleção de área, mover somente a mira |
| TAB | Circular inimigos, depois outros aliados vivos/derrotados e, por último, o próprio personagem |
| Clique em um personagem | Selecionar aliado ou o próprio personagem |
| F5, F6, F7, F8, F9 | Usar técnicas 1, 2, 3, 4 e 5 do domínio |
| Clique nos cinco novos slots | Usar a técnica correspondente |
| ENTER, mesma tecla da técnica ou clique esquerdo | Confirmar uma área preparada |
| ESC ou botão direito | Cancelar uma mira preparada |
| SPACE | Ataque básico; com arco, flecha frontal; durante Sentinela, flecha no alvo selecionado |
| SHIFT / Q | Shunpo ofensivo / defensivo |
| F2 | Diagnóstico visual existente |

Os atalhos das técnicas podem ser alterados nas configurações da partida. As quatro ações anteriores permanecem na hotbar. Os números 1–5 deste guia indicam posições no kit; os atalhos padrão continuam F5–F9.

Técnicas target usam o alvo selecionado; técnicas self cast saem do próprio personagem. Skillshots em linha e cone usam smartcast: apertou, saiu na direção em que o personagem está olhando, sem confirmação pelo mouse. A caminhada da aplicação de skills e o eixo de lançamento usam quatro direções: frente, costas, esquerda e direita. Ao apertar duas direções juntas, o servidor conserva um eixo; em empate, usa o vertical, como a pose do personagem. Leques mantêm sua abertura ao redor desse eixo, e projéteis target seguem o destinatário.

Os disparos aparecem no centro visual do personagem, 28 unidades acima do apoio dos pés, e mantêm essa altura durante o voo. O servidor calcula os contatos no plano de apoio dos corpos atuais; fissuras e crateras continuam no chão.

Ao encontrar o primeiro inimigo ou obstáculo, o disparo desaparece e dá lugar à animação de impacto. Se não houver contato, continua até seu alcance e encerra sem impacto falso. Fissuras abrem o chão por trechos, desde o personagem até o limite permitido pelo cenário; o golpe inicial de espada aparece no corpo. A fissura e a corrente de raio mantêm suas mecânicas de área/saltos.

Vento 2 lança três cortes, um a cada 0,1 segundo, em vez de aplicar os impactos diretamente no alvo. A chuva do Aetheri cai até a área selecionada e cria dissipação no solo; o dano acontece na chegada de cada onda. Martial 1/2 e Impacto sísmico receberam fontes com mais detalhe, e Água recebeu novas animações. O empacotamento V4 não amplia pixels para simular resolução.

Ao preparar uma técnica de área, a mira começa à frente do personagem. Mova-a pelo mouse, WASD ou setas; confirme com ENTER, a mesma tecla da técnica ou clique. Enquanto a mira está preparada, essas teclas não deslocam o personagem. Ao confirmar, solte as teclas de movimento antes de voltar a andar. ESC, botão direito, perda de foco ou abertura de outro painel cancelam a mira. A indicação é local; o servidor revalida alcance, obstáculos, alvo e recursos.

No kit Luz, o próprio alvo selecionado define o destinatário. Luz 1/5 curam o aliado selecionado, causam dano ao mob selecionado ou curam o próprio usuário quando não há alvo. Luz 2 cura em área ao redor do usuário. Luz 3 exige outro personagem derrotado selecionado e posição livre; Luz 4 remove estados negativos. Cura em vida cheia é recusada sem gastar energia ou iniciar recarga. A cura comum não revive.

Para testar suporte, habilite o auxiliar e use sua janela para aproximá-lo de um mob até receber dano. Na janela principal, clique nele ou use TAB para selecioná-lo e lance a cura. Deixe-o ser derrotado para testar Luz 3: aliados derrotados continuam selecionáveis. Com aliado selecionado, ataques ofensivos não trocam silenciosamente para um inimigo.

Sentinela/Aetheri 2 deixa o personagem imóvel por **4 segundos** e pode ser ativada sem alvo. Selecione um inimigo e pressione SPACE para cada flecha target; não há mais cinco munições fixas. Mais agilidade permite disparar mais vezes durante a janela. Sem alvo válido, o tiro é recusado. Pressione novamente a tecla da segunda técnica (**F6 por padrão**) para cancelar, mesmo durante a recarga; cancelar não cobra energia novamente nem remove a recarga. O painel mostra os segundos restantes.

Fora da Sentinela, SPACE dispara uma flecha frontal que pode errar, mesmo com inimigo selecionado, usando a recarga e o dano do ataque básico. A técnica 1 continua sendo a flecha target independente. Aetheri usa energia azul em todo o kit, sem gelo. Vendaval/Vento 5 só fica disponível enquanto existe o checkpoint criado por Vento 4.

## Roteiro por kit

| Kit e domínio da prévia | Conferências principais |
| --- | --- |
| Água — `water` | Corte target com slow; corte em área preso ao jogador; prisão impede movimento por 2 segundos e permite atacar; vórtice acompanha o jogador; dash percorre o caminho sem atravessar sólidos. |
| Vento — `wind` | Corte target; três cortes e sangramento; dash; repulsão e checkpoint; retorno consome o checkpoint uma vez. Testar também junto a paredes e bordas. |
| Raio — `thunder` | Corte target; lança frontal com stun; corrente limitada a quatro impactos, reduzindo dano ao repetir o mesmo alvo; dash com stun; circuito de quatro segmentos que alcança inimigos próximos nas quatro direções. Testar também com um trecho bloqueado por parede. |
| Fogo — `fire` | Corte target; fissura que avança desde o usuário e deixa fogo no chão; vórtice móvel; fissura com erupção na chegada; chuva de cortes na área escolhida. |
| Spirit — `darkness_control` | Poça com dano/root; teia em linha; feixe; prisão em área sem dano; areia movediça com slow sem dano. `darkness_debuff` usa o mesmo conjunto. |
| Aetheri — `bow_long` | Básico frontal; técnica 1 target; Sentinela imóvel de 4s e tiros target por SPACE; chuva de energia; grande flecha de energia; leque de sete flechas com um acerto por vítima. `bow_short` usa o mesmo conjunto. |
| Luz — `light` | Cura básica/forte e dano apenas contra mobs; cura em área; revive; limpeza de estados negativos. Conferir seleção de aliado vivo, derrotado, próprio personagem e mob. |
| Artista marcial — `martial` | Soco target; sequência de socos; cratera ampliada; body slam com stun; novo efeito de punhos/chutes em cone frontal com alcance 170. |

Depois de usar uma técnica, conferir recarga, energia, alcance e a animação nos dois clientes. Uma recusa não deve debitar energia nem iniciar recarga. Para testar habilidades target novamente, aguarde o respawn do mob ou reinicie a prévia. Em uma janela auxiliar, o domínio escolhido automaticamente é Luz; o kit principal é o escolhido na caixa inicial.

## Evidências e limites desta revisão

A [revisão V8](skills-preview-v8/README.md) acrescenta pegada e pancada ao Martial 4, raio amarelo/laranja nos cinco slots e rastros progressivos em Água 5, Vento 3 e Raio 4/5. Confira o rastro somente atrás do corpo e no observador; no circuito, acompanhe as quatro mudanças de direção. O agarrão continua target de curto alcance, com as mesmas regras de dano/stun.

O impacto sísmico recebeu a [revisão visual V7](skills-preview-v7/README.md), com fissuras mais detalhadas, bordas contrastadas, pequenas lascas e pulso de poeira. Observe formação, salto/assentamento e dissipação. O tamanho continua 240 × 180; o raio de dano permanece 110.

Na revisão V6, Fogo 5 apresenta cortes em X com formação/dissipação, Água 2 usa um arco de espada ao redor do corpo e Martial 3 tem rachaduras mais discretas. **Vento 4 é skillshot frontal sem seleção; Vento 5 continua target**, disponível somente após acertar e usando o inimigo do checkpoint. Errar ou bater numa parede não concede retorno. [Prévias V6](skills-preview-v6/README.md) · [Relatório V6](SKILLS-REFINAMENTO-V6.md).

A erupção de Fogo 4 e os vórtices de Fogo 3/Água 4 agora nascem pequenos, atingem o ápice e se dissipam, sem reiniciar o ciclo. Observe as três fases: os vórtices acompanham o usuário e a erupção só começa no fim da fissura. [Prancha e GIFs V5](skills-preview-v5/README.md) · [Relatório V5](SKILLS-REFINAMENTO-V5.md).

Os efeitos adaptados ficam em `assets/skills/`, com recortes, origem e hashes em `provenance.json` e metadados em `manifest.properties`. A revisão V2 usa uma sobreposição própria em `assets/skills/refined-v2/`, preservando os PNGs aprovados anteriormente. Suas pranchas, GIFs e QA ficam em [skills-preview-v2](skills-preview-v2/README.md): [água](skills-preview-v2/contact-1.png), [raio](skills-preview-v2/contact-2.png) e [flecha/cratera](skills-preview-v2/contact-5.png).

As pranchas `docs/skills-preview/contact-*.png` registram o conjunto anterior. A versão V3 está em [skills-preview-v3/contact.png](skills-preview-v3/contact.png). Os efeitos atuais de impacto, fissura, Martial e Água ficam em [skills-preview-v4/contact.png](skills-preview-v4/contact.png), com capturas em `skills-preview-v4/runtime/`. Relatórios: [V2](SKILLS-REFINAMENTO-V2.md), [V3](SKILLS-REFINAMENTO-V3.md) e [V4](SKILLS-REFINAMENTO-V4.md).

Os valores de dano, custo, alcance e recarga são provisórios para balanceamento. Os sprites, escalas, pivôs e timings ainda precisam de aprovação visual dentro do jogo. As aparências próprias de Spirit e Aetheri continuam dependendo do catálogo de personagens existente, que pode usar o visual Human provisoriamente. O efeito não substitui uma animação corporal inédita do personagem.

Esta prévia local não comprova testes físicos de teclado, Radmin, launcher nativo ou distribuição. Não publica conteúdo, não cria release e não altera os modos offline/Coop. A janela Habilidades informa se o carregamento dos sprites falhar; nesse caso a mecânica continua sendo decidida pelo servidor, mas a apresentação visual precisa de correção antes da aprovação.
