# TASK-192 — modelo de ameaças experimental

Escopo: código e laboratório local do Corujão VI; transporte TCP sem autenticação de contas/TLS. Este documento não certifica produção nem substitui homologação física Radmin, revisão de dependências ou auditoria independente.

| Ameaça | Mitigação existente/verificada | Evidência automatizada | Risco residual |
| --- | --- | --- | --- |
| Frame excessivo, truncado, UTF-8 inválido, opcode desconhecido | Envelope V1, tamanho 2..4096, DTOs explícitos e leitura limitada | ProtocolTest/ProtocolBoundaryTest, golden 1–15 e Task192 mutações seed 192 | Mutação finita não prova ausência de todo bug de parser; tráfego válido também custa CPU |
| Wrappers recursivos/desserialização arbitrária | Profundidade explícita e tipos aninhados permitidos; sem ObjectInputStream | Task187, WorldRevisionTest, Task192 | Não constitui autenticação do emissor |
| Flood, peer lento e crescimento de filas | Duas conexões, fila global 64, controles 4/peer e snapshot latest-state 1/peer; sessão/handshake/leitura limitados, janela de 120 mensagens/s | Task187, Task190ControlFairnessTest, Task192 flood | Ocupação das duas vagas ainda causa DoS; janela não é bandwidth cap deslizante e overflow pode isolar antes de 120/s |
| Keepalive competindo com conteúdo | Produtor deixa capacidade para Pong, sem aumentar fila | Negativo vermelho e correção da 190; soak de 4.192.250 bytes | Múltiplos Pings ou leitor que nunca drena podem ser isolados; rede lenta pode atingir timeout |
| ACK atrasado, duplicado, errado ou contado por outro peer | Transação/hash/fase e ID derivado do socket; quorum por participante | WorldRevisionTest, Task186, Task192 com A pronto/B retido | Um participante legítimo pode recusar/mentir sobre seu próprio preparo; ACK não atesta pixels nem identidade de conta |
| Cliente iniciando publicação/obtendo privilégio de host | Referência Java local; socket só reconhece ACK de oferta vigente | ordinaryGameplaySocketCannotStartPublication, guestCannotObtainAdministrativeCapability, Task192 BEGIN inválido | JVM/host comprometido permanece confiável demais; flag guest é modo de UI, não fronteira de segurança do JAR |
| Cliente controlando personagem/dano de outro | ID de sessão atribuído no servidor, sequências/limites, intenções sem dano final enviado pelo cliente | RpgAuthoritativeMovementTest, combate compartilhado e isolamento/reconexão | Não há conta persistente autenticada nem defesa contra MITM |
| Admissão recusada cancelando publicação | Apenas saída de participante admitido altera reserva | Task188 e Task191 Preview | Participante real desconectando continua podendo forçar recuperação, por segurança |
| Commit falso ou revisão divergente | Confirmação accepted+hash e validação do owner; snapshots/inputs com revisão; rollback/journal | Task185/186/188/191 | Não é consenso distribuído nem prova de interrupção física de energia |
| Operação órfã após shutdown | Futuros associados à fila limitada e fechamento serializado | Task191 owner bloqueado/fechamento por fase | Callback local arbitrário continua responsabilidade do controlador; nenhum shutdown remoto foi adicionado |
| Traversal/caminho recebido de jogador | IDs de assets, paths locais validados, descritores sem canal de download/escrita | WorldRevisionTest traversal, testes de catálogo/DocumentStore e ZIPs | Atacante com escrita no filesystem pode alterar conteúdo/manifests; SHA sem chave não autentica autoria |
| Asset ausente/adulterado, PNG/decode excessivo | Catálogo por hash, recursos preinstalados, dimensões/bytes/pixels limitados, preparo antes de READY/ACTIVE | Testes de catálogo/renderer e missingClientAssetRejectsCandidate; soak com PNG sintético local | Não foi exercitado todo asset em toda máquina; revisão específica 197 e Radmin permanecem necessários |
| Memória/conteúdo excessivo | Até 4 MiB, CHUNK 2800, até 128 assets e 64 MiB de pixels ativos; amostras diagnósticas 256 | Task187, WorldRevisionTest, soak/overflow 190, Task189 | Esses limites não são teto de heap total; soak durou cinco minutos medidos, não prova uptime ilimitado |
| Vazamento de dados em diagnóstico | Eventos locais opt-in sem nome/hash/token/path de save; JFR com eventos sensíveis desabilitados | Parser JSON estrito e jfr summary 189 | Console/comandos de laboratório contêm paths do projeto; trocar configuração JFR pode expor outros dados |
| Distribuição adulterada/dependência vulnerável | Integridade/allowlist existentes; nenhuma publicação por esta rodada | Regressão Python; baseline do atualizador registrado na 182 | Hash sem assinatura não autentica publisher; atualizador PowerShell bloqueado ambientalmente; sem novo scan CVE/SpotBugs |

O flood é finito, em loopback, com um peer saudável. O teste exige remoção do emissor em menos que o timeout de inatividade, fila <=64 e continuidade do outro cliente. O parser sofreu 600 mutações: mutações que continuam sendo mensagens válidas são aceitas e reencodadas; não se declara que toda alteração de bytes deve ser rejeitada.

As falhas de 186/188/190/191 foram preservadas e corrigidas sem desativar regras de geometria, reduzir conteúdo do soak ou remover casos negativos. As regressões do Gate C incluem execução Java headless/gráfica e Python com classes novas. Bytes/frames diagnósticos do servidor cobrem o writer instrumentado, não necessariamente os frames de handshake/erro escritos antes dele.

## Projeto futuro separado de autenticação/TLS

Antes de qualquer exposição pública: definir identidade autenticada e posse de personagem independente do IP/nome, modelo de sessão/revogação, distribuição e validação de certificados/chaves, armazenamento de segredos, limites por identidade e políticas de proteção contra abuso. Planejar downgrade explícito/rejeição, testes de MITM/replay e compatibilidade com builds existentes; não inserir login/TLS silenciosamente no envelope atual. O prefixo Radmin 26.x.x.x seleciona rede, não autentica pessoas.

Esse projeto exige revisão própria e autorização para credenciais/infraestrutura. Nenhum endpoint administrativo, conta pública, token real, telemetria externa ou dependência nova foi criado nesta TASK-192.
