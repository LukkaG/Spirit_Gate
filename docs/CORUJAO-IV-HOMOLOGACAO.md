# Homologação privada — marco TASK-149

Execute somente a cópia privada nova. Não use personagens pessoais para testar migração ou ferramentas de desenvolvimento.

1. Abra SpiritGateWorldStudio.exe e compare legibilidade em 1400×900 e 1000×700. Verifique menus, toolbar, teclado, foco e diálogos. As capturas printAll registram Swing renderizado; não homologam monitor, DPI ou contraste físico.
2. Atualize a biblioteca. Pesquise, filtre categoria, favorite uma imagem, altere o tamanho da miniatura e amplie o preview. Reinicie o editor para conferir preferências; os PNGs originais devem permanecer intactos.
3. Crie ou abra `.sgmap` V1. Pinte, selecione, mova, copie, cole e use Undo/Redo. Inicie um gesto e pressione Escape. Confirme prévia, snap, zoom/pan, colisão e camadas.
4. Abra Missões → Quest Builder. Crie missão com NPC elder e objetivo visit/village, configure raça, nível e recompensa. Valide, salve, reabra e exporte `.sgquest`. Faça uma tentativa com domínio incompatível ou dependência circular e confira o diagnóstico.
5. Use Testar no RPG e escolha perfil sintético. Aceite no NPC, execute o objetivo e conclua. Tente concluir novamente. Confira recompensa única e domínio ausente quando não escolhido.
6. Teste perfis 1, 15 e 20. Domínio deve respeitar raça e nível 15; Shunpo deve exigir nível 20, vigor, recarga compartilhada e destino seguro. Técnicas conceituais não são habilidades.
7. Ative Diagnóstico. Crie checkpoint em memória e retome uma cópia. Essa retomada não deve duplicar a recompensa de missão já concluída.
8. Abra os exemplos `demo.sgmap` e `demo.sgquest` da entrega. Execute o mapa no playtest, confira NPC, portal, colisão e minimapa do documento autorado. O relatório do cenário automatizado distingue testes já executados de verificação humana pendente.
9. Encerre todas as janelas e reabra. Não interpretar a ausência de progresso autorado após fechar como corrupção de personagem: o ledger de playtest é efêmero por contrato.

Aceitação técnica automatizada, aprovação artística e homologação física são estados distintos. Não publicar a distribuição nem promover assets experimentais a definitivos por aprovação automática.
