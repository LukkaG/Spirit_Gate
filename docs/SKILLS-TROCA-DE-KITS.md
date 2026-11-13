# Troca de domínio na prévia sem fechar a janela

Na ferramenta `tools/testar-skills.cmd`, o topo da partida contém **Domínio de treino** e **Trocar kit / reiniciar treino**. Há dez domínios, cobrindo os oito kits e as variantes de arco curto/trevas. Escolher o mesmo kit também reinicia seu treino.

## Implementação

- `tools/SkillsLocalPreview.java`: controlador de sessões temporárias, seletor, status e restauração de foco. Cria servidor/clientes novos fora da EDT e aguarda os personagens antes de escolher domínios pelo pedido normal validado. Atualiza raça e domínio juntos. Fecha clientes/servidor anteriores, conserva a mesma janela e seus limites e impede novas trocas durante sincronização.
- `RpgNetworkClientLauncher.java`: montagem reaproveitável por `replacePreview`, usada somente pela ferramenta. Descarte idempotente de timer, painéis, roteador, callbacks e cliente antigos. Remove apenas listeners pertencentes à montagem anterior e impede workers antigos de alterar a apresentação nova. Os fluxos normais de abertura/reconexão conservam seus caminhos.
- `tools/SkillsLiveSwitchProbe.java`: verificação gráfica da entrada real da ferramenta e seus controles.

Cada troca reinicia a sessão sintética: nível 20, atributos, HP, energia, recargas, mobs e estados temporários. O auxiliar, quando habilitado, volta como Human/Luz. Isso não libera trocas gratuitas no jogo normal. Não houve alteração de protocolo, validações de raça/domínio, servidor de gameplay, saves, banco, assets, offline/Coop ou publicação.

## Verificação

Evidências em `.task-checkpoints/SKILLS40/live-switch/`:

- `gui-regression.log`: **nove testes aprovados**, sem falhas/erros/abortos: skills, teclado, painéis, reconexão/fechamento e fluxo de janela. Maven offline com JDK 17 e o cache existente.
- Compilação da ferramenta e probe por `javac --release 17 -encoding UTF-8`.
- `probe-first.log`: oito kits trocados com/sem auxiliar, preservando janelas e encerrando sessões antigas.
- `probe-final.log`: a verificação adicional de foco encontrou um timeout em uma troca sem auxiliar. A nova hierarquia podia ainda não ter terminado sua validação quando o foco era solicitado.
- `probe-focus-fixed.log`: passou após validar a janela e repetir a solicitação no próximo evento AWT, vinculada ao painel atual. Sem temporizador permanente ou atraso fixo.
- `probe-ten-domains.log`: **dez domínios e retorno ao primeiro**, com/sem auxiliar; mesmas janelas, tamanho/posição preservados, foco devolvido ao jogo, listeners estáveis, clientes e portas anteriores encerrados. Verificou os controles em 640×480 e fechamento durante uma troca, sem reabrir a janela. Resultado em `evidence/result.json` e capturas na mesma pasta.

Não houve teste de teclado físico, rede entre máquinas ou launcher nativo. A prova usa Swing e TCP reais em loopback. Não foi necessária instalação de dependências.

## Reversão

Referências anteriores dos dois fontes modificados em `live-switch/baseline/`. Reverter somente esses trechos e os documentos desta entrega, preservando as habilidades V3 e o restante do checkout. Não usar restauração global do Git. Não houve commit, push ou publicação.
