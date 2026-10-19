# Spirit Gate - Auditoria independente de 0.11.0-rc1

Data: 18/10/2026. Fonte: upload `Validacao Portatil.zip` contendo `Projeto-K` e a distribuição Windows x64.

## Resultado técnico: APROVADO COM RESSALVAS

- **Hash real**: o ZIP distribuível possui 29.359.239 bytes e SHA-256 `7484f90f5de5ca0475c8d73a918a8868b20a089d5a3fb3a12dd2e40f5fc19d01`, idêntico ao relatório e ao sidecar.
- **Arquivo ZIP**: inspeção CRC de todas as entradas passou (`ZipFile.testzip()`); 150 entradas no cabeçalho, incluindo uma entrada de diretório, totalizando 149 arquivos.
- **Relatórios JUnit fornecidos**: 30 XMLs Surefire; soma de 202 testes, 0 falhas, 0 erros, 0 ignorados. Isso valida os **registros anexados**, não uma repetição do Maven neste ambiente.
- **Compilação independente**: os 73 arquivos Java de `domain/` e `prototype/` compilaram com `javac --release 17 -encoding UTF-8` (OpenJDK 21 no Linux), resultando em 122 arquivos `.class`. Zero erros.
- **Auditoria de conteúdo**: uma cópia temporária criada **somente para verificação**, com nomes do ZIP normalizados para `/`, passou todos os seis controles do próprio `tools/auditar_rc.py`: sidecar, estrutura, inventário, JVM/launcher PE x64, igualdade byte a byte das 122 classes **empacotadas** com `server/target/classes`, e inspeção de padrões concretos de segredos. Os resultados não garantem ausência absoluta de credenciais nem substituem assinatura digital.
- **Teste funcional independente**: executado no Linux, em modo headless, a partir das classes Java compiladas. Verificou três sólidos e três leves, HUD de Q/Shift, travessia de arbusto durante dash, destino livre, gasto de vigor, renderização sem mutação do snapshot e `R` retornando o jogador a `(135,340)` e recriando obstáculos. Captura `smoke_arena.png`.

## Ressalva de empacotamento (prioridade TASK-012)

O ZIP original usa nomes internos com barra invertida, por exemplo `SpiritGate\app\spirit-gate-arena.jar`. O leitor de ZIP da plataforma Windows pode aceitar/normalizar esse formato, mas sob Linux `tools/auditar_rc.py` rejeitou diretamente o arquivo oficial com a mensagem **"Caminho nao portatil ou com fluxo alternativo no pacote."** (exit code 1). Não foi adulteração: seu SHA bate com o publicado. A auditoria **aprovada** foi sobre uma **cópia temporária com separadores `/`**, cujo conteúdo descompactado foi preservado. A cópia tem hash diferente e **não deve ser distribuída como a versão oficial**, nem se deve alegar que o ZIP original passou no Linux.

Corrigir no processo de geração: gravar nomes internos com `/` desde a origem; validar os nomes originais (`ZipInfo.orig_filename`) inclusive no Windows; reconstruir, republicar hashes novos e reexecutar auditoria sem normalização temporária. Não sobrescrever o ZIP histórico.

## Limites da revisão

- `mvn clean test` não pôde ser reexecutado aqui: Maven ausente e repositórios externos indisponíveis. Os 202 testes são comprovados pelos XMLs entregues, não reproduzidos independentemente nesta sessão.
- O executável Windows **não foi executado neste Linux**. O smoke funcional cobriu a simulação Java via OpenJDK 21 em modo headless, não a JVM empacotada no Windows.
- A verificação em **outro Windows x64 sem Java/Maven** e testes manuais jogando permanecem pendentes.
- O projeto inclui recursos raster de Karakura; `assets/README.md` alerta que os recortes gerados **não garantem** grade de 32x32, ausência de antialiasing ou transparência perfeita. Não há sprite-sheet de personagem validada incorporada à pasta de assets; integrar somente recursos previamente aprovados e validados.
- O estado `awaitingLanding` pode persistir se a rota inteira estiver ocupada: comportamento conhecido e documentado, que deve receber testes adicionais antes de qualquer mudança de regra.

## Decisão

**GO para desenvolvimento interno incremental**, desde que a TASK-012 corrija a portabilidade do pacote e bloqueie novo release caso haja erro de validação. **NO-GO para declaração de homologação externa/publicação** até teste humano e verificação do novo ZIP. Nenhuma modificação foi aplicada aos arquivos do usuário; scripts e testes desta auditoria foram executados fora do projeto.
