# Relatório final - TASK-022 a TASK-036

Spirit Gate, 19/10/2026 (America/Sao_Paulo). Base: 0.21.0-rc1.
As quinze tarefas foram concluídas na validação automatizada local, em sequência
com checkpoint inicial, suíte completa e documentação por etapa. Nenhuma tarefa
fica bloqueada na implementação. Aprovação artística e homologação humana em outro
computador Windows permanecem pendentes; não são declaradas executadas.

## Entrega

Candidata local: dist/SpiritGate-0.36.0-rc1-20261019T042237Z-a006aab1-portable.zip
Tamanho: 29401846 bytes.
SHA-256: 511d59412598be73395391e21a6cfb2315f4855c714059086abc4d02e46cff31
Auditoria: docs/TASK-036-auditoria.json. Smokes: TASK-036-smoke.json e
TASK-036-map-smoke.json. Java 17 integrado, EXE PE x64, novo JAR de build limpo.
Último release anterior preservado: SpiritGate-0.21.0-rc1-20261019T001737Z-546ce1c5-portable.zip.
Não houve publicação, upload ou operação Git de commit/push.

## Evolução real dos testes

| TASK | Entrega | Situação | JUnit | Python |
|---|---|---|---:|---:|
| 022 | Auditoria e baseline | Concluída na automação local | 223 | 18 |
| 023 | Orquestrador de qualidade | Concluída na automação local | 223 | 22 |
| 024 | Mapas V2 ampliados | Concluída na automação local | 226 | 22 |
| 025 | Culling conservador | Concluída na automação local | 228 | 22 |
| 026 | Camadas de cenário | Concluída na automação local | 230 | 22 |
| 027 | Autoria offline de mapas | Concluída na automação local | 230 | 24 |
| 028 | Prévia e comparação de sprites | Concluída na automação local | 230 | 25 |
| 029 | Robustez de pacotes visuais | Concluída na automação local | 231 | 25 |
| 030 | Regressões de animação | Concluída na automação local | 233 | 25 |
| 031 | Foco e mira | Concluída na automação local | 235 | 25 |
| 032 | Ajuda F1 e HUD | Concluída na automação local | 236 | 25 |
| 033 | Feedback de combate | Concluída na automação local | 237 | 25 |
| 034 | Diagnóstico local F2 | Concluída na automação local | 239 | 25 |
| 035 | Estresse determinístico | Concluída na automação local | 241 | 25 |
| 036 | Release portátil | Concluída na automação local | 241 | 25 |

Evolução: 223 para 241 JUnit (+18), 18 para 25 Python (+7).
Todos os resultados finais: zero falhas/erros/ignorados em JUnit; Python OK.
TASK-035 teve um erro inicial de sintaxe no teste novo, corrigido sem alterar
asserções. O log da tentativa recusada foi preservado. A primeira invocação Maven
da baseline falhou ao selecionar o cache; a repetição com argumento completo
entre aspas usou o cache do projeto e passou. Não se ocultaram falhas de gameplay.

## Alterações e decisões

- Mapas: Properties V1 960x640 compatível; V2 reservado a mapas ampliados,
  960x640 até 8192x8192, máximo 4096 elementos por lista, validações físicas
  preservadas. Mundo, Criostase, câmera e clipping usam as dimensões reais.
- Render: culling conservador por clipping Java2D e comparação de pixels;
  camadas 0-4 traseiras e 5-10 frontais, ordenação estável. Sem índice paralelo.
- Autoria: CLI Python gera exportação determinística e reutiliza o validador
  Java autoritativo. Revisão de sprites inclui prévias direcionais e comparação
  de dimensões, alfa e SHA, preservando contrato de 59 frames e revisão humana.
- Assets: carregador único com fallback, limites de arquivo/dimensões, rejeição
  de chave duplicada, caminho externo e offset extremo. Sem IO por frame.
- Entrada/animação: prioridades históricas mantidas, deltas inválidos recusados,
  testes de transição; perda de foco limpa held/mira sem apagar casts FIFO.
- Apresentação: F1 mostra ajuda e valores publicados; impactos/derrotas usam
  efeitos reais com expiração da simulação; listas do snapshot são copiadas.
- Diagnóstico: F2 salva ZIP local explícito com um JSON e campos por whitelist;
  sem nome pessoal, caminho completo, mensagens livres ou transmissão externa.
- Qualidade: comando único offline, logs individuais, JSON/TXT e códigos de
  ambiente/teste/build, sem instalação de dependências ou mudanças globais.

## Preservação e estresse

82 arquivos históricos de domínio e seus testes comparados com
checkpoint inicial: nenhuma alteração. 228 PNGs conferidos contra
inventário histórico: nenhum alterado. ZIP 0.21 original com hash preservado.
Fórmulas, vigor, cooldowns, Freeze/Criostase, SOLID/SOFT, dash seletivo,
recuperação de posição, FIFO e três replays históricos preservados pela suíte.
Estresse: dois runtimes, 6000 ticks cada e 6000 comparações completas, dez R por
runtime, 780 ticks com Freeze, 3000 com gelo e 80 com dash; 4096 visuais em mapa
8192x8192, câmera extrema e 32 repetições de dash sólido. Métricas observadas,
sem limiar temporal para aprovar gameplay. Ver TASK-035-estresse.json.

## Inventário e recuperação

Cada tarefa tem docs/TASK-NNN.md, Spirit_Gate_TASK-NNN.pdf, TASK-NNN-testes.txt,
TASK-NNN.diff e logs. Checkpoints em .task-checkpoints/TASK-NNN-before.json
(conteúdo textual recuperável) e TASK-NNN-after.json (hashes/arquivos alterados).
O diff reconstrói o estado posterior a partir do snapshot anterior. Não existe Git
neste checkout e não foi criado repositório. Inventário completo com hashes em
    docs/INVENTARIO-TASK-022_036.json (fontes locais); o consolidado JSON lista alterações por tarefa.
Snapshots locais conservam fontes; pacote documental inclui metadados e evidências,
separado do ZIP do jogo. Os logs textuais exportados têm caminhos pessoais redigidos.
Continuar por docs/CONTINUIDADE-TASK-022_036.md, sem repetir tarefas aprovadas.

## Reprodução

Na raiz Projeto-K: python tools/qualidade.py --offline --task local
Requer Java 17, Maven, Python e cache .maven-repository existente.
Somente Python: python -m unittest discover -s tools -p "test_*.py" -v
(os testes de autoria de mapas requerem classes Java compiladas).
Mapa: python tools/mapas.py create nome-novo.properties; depois validate nome-novo.properties.
Build: powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-windows-portable.ps1
-Version 0.36.0-rc1 -MavenRepository .maven-repository
-TesterKitDirectory docs/kit-homologacao-022-036
O build cria outra saída única, preservando releases anteriores.
Auditar ZIP: python tools/auditar_rc.py --zip dist/SpiritGate-0.36.0-rc1-20261019T042237Z-a006aab1-portable.zip --report docs/re-auditoria.json
Executável opcional: SpiritGate.exe --map mapa-demo.properties

## Limitações e roteiro humano por tarefa

Roteiro separado por TASK em docs/kit-homologacao-022-036/ROTEIRO-HOMOLOGACAO.md,
incluído também no jogo. Revisar F1/F2, foco, colisões seletivas, mapa ampliado,
R, camadas e sessão prolongada; em outro Windows sem Java, conferir abertura e SHA.
Capturas do EXE na TASK-036 são reais. Template/autoria e cenários de teste são
sintéticos identificados, sem aprovação artística. Não há sprites definitivos.
EXE não assinado; sem teste Linux ou homologação em outro computador. Inspeção
de sockets por CIM é indisponível neste ambiente; não se declarou medição negativa.
Não há multiplayer, autenticação, migração de banco ou mudança de protocolo.
Zoom inteiro continua API de integração; não foi inventado atalho novo.
Fonte escala com janela e precisa de revisão humana de legibilidade em tamanhos
muito pequenos. Culling conserva efeitos/atlas com offsets e ainda percorre fundo;
não foi alegada eliminação de todo custo fora da viewport. Estresse sintético não
prova ausência de todo vazamento e não substitui sessão humana longa.
