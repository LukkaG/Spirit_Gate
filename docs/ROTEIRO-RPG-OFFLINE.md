# Roteiro de validação — Spirit Gate RPG desktop

Extraia o ZIP inteiro em uma pasta gravável e abra `SpiritGateRPG.exe`. O Java acompanha a distribuição. No repositório, `rodar-prototipo.cmd` compila e abre o mesmo cliente com JDK 17 ou superior. Esta é uma base local, sem servidor de pagamento ou multiplayer de produção.

1. Crie separadamente Reaper, Human, Aetheri e Spirit, com nomes válidos. Confirme que o novo save não apaga outro personagem e que nenhuma raça começa com domínio.
2. Confira a apresentação inicial e o ataque básico: espada pequena para Reaper, socos para Human/Aetheri, feixe básico para Spirit. O feixe não deve liberar o domínio humano de luz.
3. Abra as prévias de domínio, confira apenas os caminhos compatíveis e feche. Não deve haver mudança permanente de domínio, XP ou técnicas.
4. Antes do nível 15, a escolha permanente deve estar bloqueada. No nível 15, confirme um domínio da raça; uma segunda escolha deve ser recusada enquanto não houver reset.
5. Complete quests e derrote mobs. Um mob de nível 1 concede 3 XP de personagem. Após escolher domínio, também concede 1,5 XP de domínio; antes disso não deve acumular reserva de domínio.
6. Confira o Shunpo em todas as raças: bloqueado antes do nível 20 de personagem. As duas aplicações devem respeitar obstáculos e as regras de alvo, alcance e esquiva do protótipo.
7. Use WASD para mover, Espaço para atacar, F para interagir, I para inventário, J para diário, M para mapa e F5 para salvar. Verifique os atalhos na própria interface.
8. Converse com a Anciã, aceite a quest inicial e avance por vila, bosque e santuário. Valide objetivos, recompensas, portais, coleta, combate e recuperação após derrota.
9. Teste inventário, equipamento e comércio: equipar/desequipar, mover pilhas, usar poção e rejeitar compra sem saldo ou espaço, sem perder itens ou moedas.
10. Salve, volte à seleção e continue o mesmo personagem. Confirme raça, domínio, XP fracionária de domínio, inventário, equipamentos e quests. Saves pessoais permanecem fora do pacote.
11. O reset deve exigir 35 Spirit Coins, quest cumprida e um mês desde o anterior, preservando a base e zerando apenas o domínio. A validação local dessa política não representa uma compra real; o fluxo comercial depende do futuro serviço autoritativo.
12. Mantenha a regra de controle em aliados, inclusive mesma party, no contrato de alvo. A homologação real exige ambiente com múltiplos participantes; uma sessão offline não comprova esse comportamento em rede.
13. Abra `world-studio.cmd` e use `testar-mapa-rpg.cmd "pasta-exportada"` para confirmar que o editor e seus mapas continuam compatíveis.

As curvas finais, requisitos de técnicas, preços de itens e números de combate ainda são provisórios. As regras aprovadas e as pendências estão em `RACAS-E-DOMINIOS.md` no pacote e em `docs/SISTEMA-DE-RACAS-E-DOMINIOS.md` no repositório. Não há troca gratuita entre domínios nem equipamentos vinculados ao domínio.