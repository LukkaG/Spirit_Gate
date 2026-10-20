# Tutorial de um mapa completo

1. Extraia o ZIP inteiro e abra `SpiritGateWorldStudio.exe`, ou compile com `python tools/qualidade.py --offline --task local` e execute `world-studio.cmd`.
2. Clique Atualizar biblioteca. Aguarde a indexacao dos PNGs. Use busca e categorias manuais; as imagens originais nao sao regravadas.
3. Arquivo > Novo mapa: ID `minha_vila`, nome, 960x640, grade 32 e regiao `village`. O adaptador real exige essas dimensoes; a autoria permite mapas maiores.
4. Selecione um terreno existente na biblioteca. Se for atlas, use Recortar tileset e informe recortes explicitamente; nao suponha que a arte respeita uma grade. Ative Tile na grade e pinte com Lapis, Retangulo ou Balde na camada GROUND. As bordas da arte precisam de avaliacao humana.
5. Selecione uma arvore ou construcao, mude para Objeto real e camada OBJECTS ou ABOVE. Posicione e ajuste dimensoes no inspetor. Nenhum obstaculo e inferido automaticamente pela transparencia.
6. Crie colisao na base do tronco ou construcao: ferramenta Colisao ou Mapa > Nova colisao. Ative Ver > Raio do jogador para consultar o CollisionService real (raio 18). Oculte a camada somente para editar; isso nao desativa sua fisica.
7. Entidades > Inserir NPC: escolha `elder`, posicione perto do spawn, em ponto livre, por exemplo 190,320. Depois selecione pelo canvas ou Selecao > Selecionar por ID e use Editar NPC selecionado. Escolha `elder.start` ou `elder.help`; nomes personalizados sao permitidos. Missoes associadas sao referencias e nao sao concedidas automaticamente.
8. Entidades > Inserir PLAYER: exatamente um, por exemplo 140,320. Para inimigos, use outro mapa com sua regiao de conteudo correspondente; selecione ENEMY e configure encontro. Quantidade e limite simultaneo coincidem; respawn e IA continuam originais.
9. Mapa > Novo portal: destino `minha_vila`, origem 750,320, chegada 140,320, missao vazia. Para conectar outro arquivo, abra documentos no mesmo diretorio e use IDs distintos/regioes compativeis. Consulte Conexoes entre mapas.
10. Ctrl+Shift+S ou Arquivo > Salvar como em `studio-projects/minha_vila.sgmap`. Proximos salvamentos preservam backup integro `.bak`. Use Diagnostico > Analisar mapas conhecidos para revisar erros e avisos.
11. Arquivo > Exportar para RPG gera um subdiretorio novo com manifesto, documentos e PNGs conferidos por SHA-256. Erros bloqueiam a exportacao; nao substitui os mapas padrao do jogo.
12. Mapa > Testar mapa no RPG abre cliente e runtime reais em sessao sintetica sem saves pessoais. WASD move, Espaco ataca, Q usa habilidade, F interage. R recupera apos KO. O botao Colisoes mostra a geometria. Teste parede, NPC e portal; feche para gerar o log de diagnostico no diretorio exportado.

O exemplo `assets/maps/studio_tutorial.sgmap` ja contem PLAYER, NPC, parede e portal local. Ele e um exemplo tecnico sem arte aprovada. Para comparar o resultado com terreno e construcoes, siga os passos de pintura acima. O kit de homologacao lista verificacoes artisticas e de usabilidade ainda humanas.

Recursos avancados: Terrenos cadastra mascaras NESW e fallback; Prefabs cria conjuntos relativos com IDs novos por insercao; Selecao oferece mover, alinhar, distribuir, ordenar e bloquear; Ambiente permite sequencias e clima; Eventos usa somente gatilhos e acoes cadastrados; Ver > Minimapa permite centralizar e inspecionar isolamento potencial. Consulte o catalogo para saber o que e exportado.
