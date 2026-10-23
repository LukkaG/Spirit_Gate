# Homologação multiplayer — TASK-159

Entrega experimental privada. Validação automatizada localhost é distinta de aprovação artística, inspeção física de interface e segurança de produção.

| Área | Estado entregue |
| --- | --- |
| Dois jogadores TCP localhost | Validado, movimento autoritativo, colisão e direção |
| Aparências em rede | IDs validados no servidor; cabelo e roupa em camadas |
| Coop histórico e combate offline | Preservados e cobertos pela regressão |
| Combate RPG compartilhado | Parcial: indisponível no novo backend; ações recusadas |
| LAN física | Pendente; não homologada em duas máquinas |
| Sprites | 180 PNGs provisórios Java2D, sem aprovação artística |
| Contas e persistência online | Não implementadas |
| Segurança de produção | Não homologada; não expor à internet |

## Roteiro humano

1. Extraia o pacote privado novo em pasta própria. Preserve os pacotes anteriores e use somente personagens sintéticos.
2. Abra `Iniciar-multiplayer-local.cmd`: servidor em `127.0.0.1:54557` e dois clientes com aparências diferentes. Também pode abrir servidor, cliente A e cliente B individualmente, nessa ordem. Aguarde a janela de conexão terminar o carregamento.
3. Confira dois personagens e IDs distintos em ambas as janelas. Mova com WASD, confira atualização remota, direção, animação e bloqueio por obstáculo/outro corpo. O painel informa que combate de rede está indisponível.
4. Troque cabelo e roupa em um cliente; confira no outro. Selecione `none` para remover. Isso não deve alterar raça, atributos, vigor ou moedas.
5. Feche um cliente e confira remoção no sobrevivente. Abra-o novamente e confira uma nova sessão. Reconectar não recupera progresso online.
6. Feche as duas janelas e o console do servidor. Confirme liberação da porta. Não encerre processos de outras aplicações para liberar portas.
7. Inspecione foco, DPI, legibilidade e camadas no monitor real. Capturas Swing `printAll` comprovam renderização, sem homologar esses aspectos físicos.
8. Abra o Studio e o RPG offline separadamente e siga `CORUJAO-IV-HOMOLOGACAO.md`. Não use saves pessoais em testes de migração.

LAN não recebe tutorial de implantação: faltam testes físicos. Não abrir firewall, roteador ou acesso público para homologar localhost.

## Evidências e protocolo

Logs, JSON e capturas: `.task-checkpoints/TASK-159/corujao-iv/`; combinações: TASK-157; sincronização/reconexão: TASK-158. O pacote inclui evidências selecionadas em `examples/multiplayer/`.

Envelope V1 e bytes históricos de Hello preservados. A versão de aplicação cosmética 2 negocia mensagens adicionais somente no backend com catálogo. Peers legados recebem State V1. Solicitações não contêm credenciais, caminhos ou atributos de combate. Consulte `CORUJAO-IV-PROTOCOLO.md`.
