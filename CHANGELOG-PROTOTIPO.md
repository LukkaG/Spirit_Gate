# Spirit Gate — Registro técnico de modificações

Data: 17/10/2026 · Versão da entrega: protótipo visual local 0.1

## Arquivos existentes alterados

- `server/pom.xml`: Java release 17, codificação UTF-8, dependência JUnit Jupiter 5.11.4 (somente test), plugins Surefire 3.5.2 e Exec 3.5.0.
- `server/src/main/java/com/projetok/DatabaseManager.java`: remove valores de conexão e senha embutidos e exige `SPIRIT_DB_URL`, `SPIRIT_DB_USER` e `SPIRIT_DB_PASSWORD` no ambiente. **Necessário rotacionar a senha anteriormente commitada.**

## Arquivos novos

- `.gitignore`: impede novos commits de target, artefatos locais e segredos usuais; não remove arquivos já rastreados pelo Git.
- `README-PROTOTIPO.md`: instruções de instalação, gameplay, limites e execução.
- `CHANGELOG-PROTOTIPO.md`: este registro.
- `rodar-prototipo.cmd`: executor Windows (`mvn test` e `mvn exec:java`).
- `server/src/main/java/com/projetok/prototype/DebugInput.java`: intenção de movimento/ataque em uma mensagem imutável.
- `server/src/main/java/com/projetok/prototype/DebugSnapshot.java`: estado do mundo para rendering sem compartilhar mutabilidade.
- `server/src/main/java/com/projetok/prototype/DebugWorld.java`: entidades reais, arena, hitboxes, colisão, movimentação, AI básica, eligibility e eventos de dano.
- `server/src/main/java/com/projetok/prototype/DebugRuntime.java`: game loop isolado a 30Hz, thread única de mutação e snapshots para interface.
- `server/src/main/java/com/projetok/prototype/DebugPanel.java`: renderização Java2D, UI, hitboxes e telemetria.
- `server/src/main/java/com/projetok/prototype/DebugLauncher.java`: janela Swing e entradas de teclado via Key Bindings.
- `server/src/test/java/com/projetok/prototype/DebugWorldTest.java`: cinco testes JUnit 5.
- `server/src/test/java/com/projetok/prototype/DebugWorldSelfTest.java`: seis cenários de validação standalone.

## Componentes preservados

- Todas as classes em `com.projetok.domain.*` permaneceram com código-fonte original.
- `KServer`, `Kclient`, `Main` e `Network` permanecem sem alterações.
- `KServer` continua legado, sem fluxo funcional de autenticação, e não é usado pela arena.
- O domínio não passou a depender de Swing, JDBC ou Kryonet.

## Verificações concluídas na máquina de geração

- Compilação do domínio, demo e protótipo com OpenJDK 21 utilizando `javac --release 17`: sucesso.
- 6 verificações standalone do mundo, movimento, hitbox e combate: sucesso.
- Abertura da interface em ambiente Xvfb, interação simulada com teclas WASD e H e captura real da janela: sucesso.
- Não foi possível executar Maven/JUnit integralmente nem testar cliente/servidor Kryonet por ausência do Maven e das dependências externas no ambiente de geração.

## Limitações assumidas

- A janela é de debug local; ainda não comunica com o processo KServer.
- IA com perseguição reativa simples, sem A* nem navegação por obstáculos complexos.
- Não existe física contínua, rollback, XP, skills especiais, movimentação em rede, inventário visual, persistência ou PvP nesta entrega.
- Escala de depuração em pixels, ainda sem conversão física para metros.
- Autenticação antiga deve ser substituída antes de publicar qualquer servidor na internet.
# 0.21.0-rc1 — TASK-017 a TASK-021

Auditoria reproduzível da base; autoria/revisão de 59 frames; pacote visual
versionado opcional e fallback; câmera pura e janela redimensionável com mira
em coordenadas de mundo. Gameplay preservado. Arte e homologação humana pendentes.

# 0.36.0-rc1 - TASK-022 a TASK-036

Mapas V2, culling conservador, camadas, autoria offline, revisão direcional de
sprites, robustez visual, testes de animação/foco, ajuda F1, diagnóstico local F2,
feedback de impactos e estresse determinístico. Fallback mantido; sem aprovação
artística. Homologação humana em outro Windows permanece pendente.

