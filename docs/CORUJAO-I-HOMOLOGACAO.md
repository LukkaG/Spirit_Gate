# Corujão I — roteiro de homologação humana

Arte e balanceamento continuam experimentais. Nenhuma aprovação humana foi presumida.

## Navegador

Abra https://spirit-gate-sprites-lab.eageremu2049.chatgpt.site e selecione **Recomendada: 50% / 250 ms**. Escolha Guardian ou Mystic e clique em Iniciar teste. Cada início cria um visitante novo; para carregar um save manual, use Salvar e voltar ao menu e selecione o personagem. Os arquivos ficam no navegador, separados do desktop.

1. Use WASD nas quatro direções; observe os quatro passos por direção, pés, proporção e transparência. Pare e tente andar contra uma parede: o personagem deve voltar ao Idle quando não houver deslocamento efetivo.
2. Verifique se a escala e o ritmo estão agradáveis. Um frame dura 250 ms, quantizado pelo relógio de 30 ticks/s; o ciclo completo tem quatro frames. O deslocamento continua em 215 unidades/s.
3. Atravesse o portal para o bosque e ataque com Espaço. Golpes acertados mostram o dano aplicado; uma tentativa sem alvo não deve mostrar dano falso. Dano recebido e cura têm cores diferentes.
4. Use Q para a habilidade original. O botão Golpe de Impacto (Guardian) ou Pulso Arcano (Mystic) ativa a adicional. Elas compartilham recarga com Q e respeitam a recarga do golpe básico. Observe os custos no tooltip e as barras de vigor/vida.
5. Complete a campanha e encontre o chefe. Compare preparação pontilhada, tempo restante, recuperação, vida e fase enfurecida. A regra de visibilidade mantém a amostragem histórica de até seis pixels; trajetos rasantes a quinas têm essa limitação conhecida.
6. Teste perder foco, soltar teclas, pressionar rapidamente, salvar com F5, voltar ao menu e continuar. F5 deve salvar no jogo sem recarregar a página.
7. Registre clareza do HUD, tamanho, ritmo, deslizamento dos pés, legibilidade do chefe e desempenho. O formulário copia o texto ou prepara uma issue; nada é enviado automaticamente.

O navegador foi exercitado em Edge com teclado. Outros navegadores, desempenho em outras máquinas e controles de toque ainda precisam de avaliação. A primeira abertura depende do download do CheerpJ; não há servidor multiplayer nem salvamento em nuvem.

## Desktop experimental

O pacote indicado no relatório final contém Java. Extraia todo o ZIP e abra SpiritGateRPG.exe. `Abrir-Geometrico.cmd` conserva o modo anterior. Não mova saves pessoais para dentro de pacotes de teste sem manter sua própria cópia.

No código local, execute `testar-rpg-sprites.cmd`; `--geometrico` abre o fallback. As variáveis `SPIRIT_GATE_SPRITE_SCALE` e `SPIRIT_GATE_FRAME_MS` continuam opcionais e não exigem recompilação. As classes desta rodada já foram compiladas.

## Decisão do responsável

Registrar separadamente: aprovação visual das sprites e pivôs; aprovação do ritmo; aprovação dos efeitos provisórios; legibilidade do HUD/chefe; jogabilidade de Guardian/Mystic; problemas e configuração da máquina. A rodada seguinte depende dessa avaliação. TASK-091 não foi iniciada.
