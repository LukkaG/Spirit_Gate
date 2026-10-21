package com.projetok.studio;
import javax.swing.*;

/** Ajuda disponivel no proprio editor. */
final class StudioHelp {
    static String layer(WorldDocument.Role role){return role+": "+switch(role){
        case GROUND->"Chao base: grama, terra, agua e pisos.";
        case DETAIL->"Detalhes do chao: caminhos, manchas, pedras pequenas e flores.";
        case BELOW->"Elementos inferiores: partes do cenario desenhadas antes dos objetos.";
        case OBJECTS->"Objetos principais: arvores, moveis, construcoes e marcadores de entidades.";
        case ABOVE->"Elementos superiores: copas, arcos e decoracoes sobre os objetos.";
        case ROOF->"Telhados e coberturas, desenhados sobre as camadas visuais anteriores.";
        case COLLISION->"Organiza barreiras fisicas. Desenhar uma imagem aqui nao cria colisao: use Colisao para criar hitboxes.";
        case INTERACTION->"Organiza marcadores e objetos interativos. Configure a interacao nos menus; a camada sozinha nao cria comportamento.";
    };}
    StudioHelp(WorldStudio ui){
        addButton(ui);
        ui.action("Ajuda","Ferramentas e camadas",()->show(ui));
    }
    void addButton(WorldStudio ui){var help=new javax.swing.JButton("Ajuda do editor");help.addActionListener(e->show(ui));ui.toolbar.add(help);}
    void show(WorldStudio ui){
        String text="""
            FERRAMENTAS E CONTROLES
            Atualizar biblioteca: procura imagens PNG nas pastas de assets do projeto.
            Busca e categoria: filtram a biblioteca. Definir categoria (F2): classifica a imagem e salva a categoria localmente.
            Zoom - / + e roda do mouse: aproximam ou afastam a visualizacao, sem alterar o mapa.
            Botao do meio + arrastar: move a visualizacao do mapa.
            Grade: mostra ou oculta as linhas de referencia.
            Selecionar toda a grade (Ctrl+A no mapa): seleciona os elementos e colisoes da camada ativa em todo o mapa; celulas vazias nao sao objetos.
            Aumentar mapa: amplia largura/altura em pixels, preservando o conteudo. Zoom muda apenas a visualizacao.
            Selecionar: clique ou arraste com o botao esquerdo para selecionar na camada ativa. Shift adiciona a selecao.
            Mover: arraste um objeto ou grupo selecionado para reposicionar. Em Selecionar, arrastar dentro de um objeto ja selecionado tambem move o grupo. O movimento encaixa na grade e pode ser desfeito.
            Selecao multipla: arraste uma caixa no mapa; Shift adiciona elementos. Na aba Tiles, arraste sobre a imagem para escolher um conjunto preservando sua composicao.
            Copiar/Colar: copia a selecao do mapa ou, com foco na aba Tiles, o conjunto da imagem. Os novos objetos ficam selecionados para mover.
            Lapis: pinta com o botao esquerdo. Botao direito + arrastar: pinta com qualquer ferramenta ativa; o gesto inteiro pode ser desfeito.
            Borracha: remove elementos desbloqueados da camada ativa.
            Retangulo: preenche um retangulo de tiles; requer Tile na grade.
            Balde: preenche celulas conectadas com o mesmo tile; limite de 4096 celulas.
            Conta-gotas: captura a imagem e o recorte de um elemento do mapa.
            Colisao: arraste para definir um retangulo fisico; configure-o nas ferramentas de colisao.
            Objeto real: insere o recorte nas dimensoes originais. Tile na grade: ajusta cada recorte ao tamanho de uma celula.
            Aba Tiles: ao escolher uma imagem na biblioteca, ela aparece inteira, com barras de rolagem. Largura/altura do tile ajustam a grade de selecao (padrao 32 x 32).
            Clique esquerdo no asfalto para escolher o pincel esquerdo; clique direito na agua para escolher o direito. Cada pincel preserva a imagem e o tile escolhido.
            Imagem inteira: esquerdo / direito atribui a sprite completa ao respectivo pincel, nas dimensoes originais.
            Delete (com foco no mapa): exclui os tiles ou objetos selecionados. Respeita bloqueios e permite Ctrl+Z para desfazer.
            Desfazer / Refazer (Ctrl+Z / Ctrl+Y): volta ou reaplica uma alteracao.
            Copiar / Colar (Ctrl+C / Ctrl+V): duplica os elementos selecionados na posicao do cursor.

            CAMADAS
            A lista mostra a ordem de desenho: as primeiras sao desenhadas antes, e as seguintes podem cobri-las. Os nomes indicam a organizacao sugerida; nao criam regras de jogo automaticamente.
            [V]: camada visivel. [-]: oculta. [L]: bloqueada para edicao.
            Nova: cria uma camada com nome e papel escolhidos.
            Renomear: muda apenas o nome da camada, sem alterar arquivos de imagem.
            Visibilidade: mostra/oculta a camada; nao desativa barreiras fisicas.
            Bloquear: impede pintura e edicao na camada. Clique novamente para desbloquear.
            Subir / Descer: muda a ordem de desenho da camada na lista.
            Editar elemento selecionado: ajusta posicao, tamanho, camada, referencia, direcao e propriedades do objeto selecionado.

            """;
        for(var role:WorldDocument.Role.values())text+=layer(role)+"\n";
        JTextArea area=new JTextArea(text,30,76);area.setEditable(false);area.setLineWrap(true);area.setWrapStyleWord(true);area.setCaretPosition(0);
        JOptionPane.showMessageDialog(ui,new JScrollPane(area),"Guia de ferramentas e camadas",JOptionPane.INFORMATION_MESSAGE);
    }
}
