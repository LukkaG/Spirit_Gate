# Roteiro de teste privado - Spirit Gate RPG offline

Extraia o ZIP RPG inteiro em pasta gravavel. Abra SpiritGateRPG.exe. O Java esta integrado. Nenhum servidor, banco ou login e necessario. A arena antiga continua disponivel pelo menu e pelos pacotes anteriores.

1. Crie um personagem com nome de 3 a 24 caracteres. Escolha Guardian (mais vida e investida) ou Mystic (ataque a distancia e cura), e aparencia azure/amber. O save inicial e criado sem apagar outro personagem.
2. Na vila, aproxime-se da Ancia e pressione F. Escolha aceitar Ecos no bosque. Escape fecha dialogo. Consulte J para ver objetivos.
3. WASD move. O bau ao sudeste da vila entrega uma espada. F abre o bau; I abre inventario. Selecione espada e clique Equipar. Atributo de ataque deve aumentar. M mostra mapa e posicao.
4. Caminhe para o portal leste da vila e pressione F. No bosque, SPACE ataca o inimigo mais proximo ao alcance e sem parede entre voces. Cada pressao e um golpe; solte para atacar novamente. O botao Golpe tambem funciona.
5. Q usa habilidade do arquetipo. Guardian faz investida; Mystic cura quando estiver ferido. Vigor e recarga limitam uso. 1 consome pocao apenas se ferido e fora da recarga.
6. Derrote dois ecos. E possivel explorar os dois spawns ou aguardar respawn. HP, XP, nivel, moedas e cristais devem refletir combate. F coleta ervas curativas. Se cair, R recupera na vila preservando progresso.
7. Volte pelo portal oeste. Converse com a Ancia e entregue Ecos. Receba XP, moedas e pocoes. Aceite O Santuario Partido.
8. Nilo fica ao sul da Ancia. Aproxime-se e pressione F. Compre manto/pocoes ou venda cristais. I permite equipar manto, aumentando defesa. Inventario cheio/saldo insuficiente devem recusar a compra sem perder moedas.
9. Atravesse bosque ate portal leste e entre no Santuario. Derrote os dois sentinelas; o chefe so desperta depois deles. Explore a reserva de pocoes ao sul da entrada.
10. Enfrente o Guardiao. Circulo indica preparacao de ataque em area: afaste-se antes da execucao. A segunda fase acelera o aviso e aumenta alcance/dano. Derrota concede reliquia unica.
11. F5 salva. Use Menu para salvar e voltar a selecao. Continue o mesmo personagem e confirme inventario, equipamento, posicao, inimigos mortos e progresso. Volte a Ancia e entregue a reliquia para concluir a campanha inicial.
12. Experimente I: separar/mover pilhas, usar pocao e desequipar. J: objetivos e estado. M: regiao atual/visitadas. Atalhos: remapeie sem conflitos; WASD e teclas de sistema sao reservados. Audio/ambiente: altere volume, mudo e ciclo; reabra para confirmar preferencias.
13. Crie outro personagem e confirme isolamento de saves. Nao e necessario apagar o primeiro. Saves invalidos sao preservados; backups recuperaveis aparecem na selecao e abrem em novo arquivo.
14. Abra Arena historica e confira os controles legados Q/Shift/E, colisao e combate. Configuracoes RPG nao mudam a arena.

Os testes automatizados executam a campanha para os dois arquetipos. Capturas de componentes Swing documentam a janela real em execucao; o desktop deste ambiente pode produzir captura preta. Isso nao e homologacao humana nem teste em outro Windows.

Limitacoes intencionais: arte geometrica provisoria, tres regioes compactas, duas missoes, um chefe e dois arquetipos. Inventario de 20 slots. A arena e o RPG offline nao usam multiplayer. Tons sao sintetizados localmente; sem dispositivo, o jogo fica silencioso.
