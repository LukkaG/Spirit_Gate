# TASK-178 — Geometria e F2

Diagnóstico antes das mudanças: `.task-checkpoints/TASK-178/collision-fix/diagnosis.md`. O bloco cinza é uma primitiva Java2D, não um asset editado. Colisor original e novo sólido bruto: x=380..500, y=180..300. Pivôs/escala e os 20 PNGs base permanecem originais.

Na reprodução antiga, centro (368,240), raio 12 e alpha x=349..387.5 da pose LEFT Walk 2. Não há penetração física: a menor distância ao retângulo é 12. Há avanço visual de 7.5 sobre a face. A autoridade e a apresentação são iguais nesse caso. Logo não bastaria mudar a interpolação ou apenas desenhar o bloco por cima.

Novo contato lateral esquerdo: centro x=358, alpha até377.5, antes da face380; direito: x=522, alpha a partir503. O perfil não é um círculo de raio22 universal: é uma cápsula de raio12 e segmento horizontal20, aplicada somente ao cenário. Atores e combate permanecem separados. A transformação de Minkowski desloca o retângulo de cálculo para x=370..510, mantendo y=180..300. O círculo12 contra esse retângulo reproduz a cápsula contra o sólido bruto. Faixas de limite do mapa não são dilatadas repetidamente.

Volume visual do bloco: x=380..500, y=132..300. Base física permanece y=180..300; ponto de apoio/profundidade y=300. Um ator atrás (y=168) é desenhado antes do volume e parcialmente oculto. Um ator à frente (y=312) é desenhado depois; é correto que seu corpo possa cobrir a face frontal, desde que o apoio esteja fisicamente livre. Sobreposição na frente não equivale a penetrar o sólido.

F2, desativado por padrão, mostra simultaneamente:

- Magenta: bounds alpha da pose base apresentada.
- Ciano: pivô/apoio apresentado.
- Verde/vermelho: círculo de atores apresentado/última autoridade recebida, ligados por uma linha.
- Laranja: cápsula de contato do personagem com cenário e retângulo sólido bruto.
- Amarelo: retângulo dilatado usado junto ao círculo na consulta física.
- Azul: linha de apoio/profundidade do objeto e seu valor numérico.

O retângulo amarelo não é outro sólido adicionado ao mapa; é a representação do cálculo da cápsula. Os círculos continuam raio12. O debug lê contratos e estados residentes, sem alterar a simulação. Não há corpo de combate inventado: o protocolo não expõe uma hurtbox independente.

Uma segunda causa possível era interpolar em linha reta entre dois estados livres ao redor de uma quina. O cliente agora valida o ponto apresentado contra o mesmo cenário da sessão; se inválido, apresenta uma posição recebida válida e mantém o tick mínimo dessa correção até alcançá-lo. Não aceita posições de cliente como autoridade nem modifica o estado do servidor. Esse fallback pode produzir um pequeno salto visual na quina, preferível a apresentar o corpo dentro do sólido; não há predição nem teleporte de física.

Limitações: imagem original não anexada; comparação usa o bloco cinza central identificado no código. As capturas são automáticas e a homologação visual humana/Radmin permanece pendente. Metadados de profundidade são específicos do cenário sintético atual; o pipeline de camadas de World Studio/offline não foi substituído.
