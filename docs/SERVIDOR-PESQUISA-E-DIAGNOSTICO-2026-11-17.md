# Servidor Spirit Gate — pesquisa técnica e diagnóstico

Data: **17/11/2026**, America/Sao_Paulo. Checkout `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. Pedido: pesquisar skills, código aberto, treinamentos, fóruns e referências e confrontar com o servidor existente.

## Resultado

**Recomendo evoluir o servidor autoritativo Java existente**, com prioridade para limites do transporte, lifecycle, segurança de admissão e previsibilidade do tick. O núcleo atual já oferece simulação, comandos, snapshots, combate, conteúdo e testes suficientes para justificar uma evolução incremental. A escolha de outro framework precisaria demonstrar benefício em um problema medido.

Esta pesquisa acrescenta **uma lacuna reproduzida de prazo de handshake** e identifica **um limite de conexão de 30 minutos** que precisa de política explícita. Mantém o gate A-04 da auditoria anterior. Não é uma declaração de segurança completa nem homologação de servidor público/MMORPG persistente.

As referências e exercícios estão em [Skills, códigos e treinos](SERVIDOR-REFERENCIAS-E-TREINOS-2026-11-17.md). A [auditoria ALPHA-001](ALPHA-001-RELATORIO-AUDITORIA.md) continua como registro anterior; seus resultados não foram sobrescritos.

## Método, autorização e baseline

Pesquisa pública read-only em sites, GitHub e fórum oficial; inspeção de fontes e testes locais. Skills locais aplicadas: find-skills e spiritgate-server-engineer. Os SKILL.md externos foram consultados como candidatos, sem instalação ou adoção automática das suas instruções.

A autorização desta rodada abrange pesquisa e investigação. Foram executadas duas sondas originais e limitadas em **loopback de processo descartável**, sem conexão ao servidor do usuário, Radmin, contas, banco, saves, releases ou serviços de jogo externos. Nenhuma dependência foi instalada e nenhum código remoto foi executado.

Baseline desta rodada: **56 entradas Git não rastreadas**, sem diff rastreado; **3.829 arquivos existentes inventariados por SHA-256**, incluindo os seis documentos anteriores da ALPHA-001 e trabalho ORG-03. Ignorados e arquivos inacessíveis não entram nessa garantia. As listas completas anteriores permanecem nos relatórios de baseline.

Java 17.0.20.1 e Maven 3.10.0 disponíveis. [server/pom.xml](../server/pom.xml) declara release 17, JUnit em teste e plugins Maven. O transporte atual usa sockets do JDK; a descrição histórica de Kryonet não corresponde a esse caminho operacional.

Foram inspecionados scripts antes de executar: compile-local escreve em target/launcher; qualidade.py grava relatórios com prefixo TASK em docs e chama outras rotinas. Não foram usados para esta rodada. A compilação foi direcionada a pasta temporária exclusiva.

## O que já temos e devemos preservar

| Área existente | Código/testes identificados | Valor para evolução | Limite atual |
|---|---|---|---|
| Transporte TCP com capacidade explícita | [LocalSimulationServer.start/Peer](../server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java#L27) | Owner da simulação separado das threads de socket; conexão fechável e filas limitadas | Duas conexões; não é infraestrutura pública autenticada |
| Simulação autoritativa | [RpgSessionWorld.step/accept/skill/gameplay](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java), [RpgSkillEngine](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java) | Dano, recursos, colisão, inventário e progressão não dependem de pixels do cliente | A-04 e demais limites da auditoria; não aprovado em dois PCs |
| Contratos binários | [Protocol.read/write](../server/src/main/java/com/projetok/multiplayer/Protocol.java#L99), [SkillProtocol](../server/src/main/java/com/projetok/multiplayer/SkillProtocol.java) | Tamanho máximo, tipos, finito, contagens e bytes excedentes validados; aplicações 6/7 preservadas | Frame máximo 4096; MAX_ACTORS=8 e capacidade dois condicionam crescimento |
| Estado privado e anti-replay | [RpgSessionWorld.gameplaySnapshotFor/skillsSnapshotFor](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java), [SharedRpgEncounter.hit](../server/src/main/java/com/projetok/rpg/SharedRpgEncounter.java) | Ficha por destinatário, sequências e geração/participação em recompensas | Não substituem conta/posse autenticada nem durabilidade |
| Robustez existente | [NetworkRobustnessTest](../server/src/test/java/com/projetok/multiplayer/NetworkRobustnessTest.java), [FinalNetworkIsolationTest](../server/src/test/java/com/projetok/multiplayer/FinalNetworkIsolationTest.java), [ProtocolBoundaryTest](../server/src/test/java/com/projetok/multiplayer/ProtocolBoundaryTest.java) | Casos determinísticos, input que expira, mensagens malformadas e snapshot forjado | Testes encontrados/lidos, não reexecutados nesta rodada |
| Apresentação separada | [RemoteTimeline](../server/src/main/java/com/projetok/multiplayer/RemoteTimeline.java), [NetworkSkillPresentation](../server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java) | Interpolação/efeitos sem transferir autoridade ao cliente | A-03 visual e quatro poses corporais continuam limitações conhecidas |
| Diagnóstico | [NetworkDiagnostics](../server/src/main/java/com/projetok/multiplayer/NetworkDiagnostics.java), [ErrorReports](../server/src/main/java/com/projetok/diagnostics/ErrorReports.java) | Métricas de tick, RTT, filas, pintura e logs locais | Precisa política de motivos de saída, coleta e retenção; sem benchmark novo |
| Distribuição | [test_zip_security.py](../tools/test_zip_security.py), [auditar_rc.py](../tools/auditar_rc.py), [Atualizar-SpiritGate.ps1](../tools/updater/Atualizar-SpiritGate.ps1) | Há controles de nomes ZIP, colisões de caminho/symlink e ferramentas de atualização | Leitura de testes não certifica pacote ou cadeia de confiança completa |

Inventário normal: **286 fontes Java de produção** e **280 arquivos com nome *Test.java**. Arquivos não equivalem a casos de teste; não foi alegada execução da suíte completa.

## Achados e hipóteses novos

Classificação de evidência: **REPRODUZIDO** = observado nesta rodada; **CONFIRMADO POR LEITURA** = caminho inequívoco no código, sem ensaio completo; **HIPÓTESE** = precisa de reprodução dirigida. Esses termos descrevem a evidência, sem substituir os cinco estados de prontidão da ALPHA-001.

| ID | Evidência / prioridade | Local | Conclusão e limite | Menor próximo passo proposto |
|---|---|---|---|---|
| S01 | REPRODUZIDO / P1 segurança de admissão | LocalSimulationServer.Peer.read, linhas 56–61; Protocol.read, linha 103 | Timeout de leitura de 2 s não impõe prazo absoluto de handshake. Frame incompleto permaneceu ocupando conexão após 3.666 ms no teste; outro peer válido entrou | Teste focalizado e proposta de prazo total de admissão/frame, sem alterar bytes 6/7 |
| S02 | CONFIRMADO POR LEITURA / P1 política de sessão | LocalSimulationServer.Peer.read, linhas 72–74 | Deadline fixo de 30 minutos a partir da admissão; não é renovado por atividade. Após o prazo, a próxima mensagem completa encerra o peer | Definir se é limite intencional do teste privado e qual aviso oferecer; depois validar com relógio controlado |
| S03 | HIPÓTESE / P1 robustez | LocalSimulationServer.Peer writer, linha 71 | Escrita usa OutputStream bloqueante; SO_TIMEOUT regula leitura. Fila de snapshots 1 limita memória, mas não garante prazo de saída de uma escrita pendurada enquanto cliente continua enviando | Ensaio isolado de cliente que deixa de ler e segue enviando intenções legítimas; medir writer, fila e liberação |
| S04 | CONFIRMADO POR LEITURA / P1 observabilidade | LocalSimulationServer.Peer.read catch/finally, linha 81; closePeer, linha 82 | Exceções de transporte têm descarte agregado/condicional; saída por deadline, rate limit ou fila cheia não produz motivo específico nesse caminho | Definir enum de motivo/contadores locais sanitizados; sem enviar informação privada ao cliente |
| S05 | HIPÓTESE / P1 concorrência | LocalSimulationServer.Peer.read linhas 68–71 versus closePeer/close | Admissão, Welcome, publicação em peers e fechamento ocorrem em threads diferentes; é necessário demonstrar que fechamento concorrente não publica peer já fechado | Teste com barreiras/latches nas bordas de join/Welcome/close; não inserir sleeps na produção |
| S06 | CONFIRMADO POR LEITURA / lacuna para servidor público | LocalSimulationServer/Protocol/SessionCharacterSelection | Nome/raça/role e restrição de rede não autenticam conta ou posse de personagem; não há TLS no caminho inspecionado | Manter escopo fechado atual; modelo de identidade/transporte protegido em decisão separada antes de exposição pública |
| S07 | CONFIRMADO POR LEITURA / P1 automação | .github e tools/qualidade.py | Não foi encontrado workflow YAML de CI na árvore .github inspecionada. Há runners locais com efeitos de escrita | Desenhar regressão isolada e separar testes puros, TCP, GUI e geradores de evidência antes de integrar CI |
| S08 | CONFIRMADO POR LEITURA / P1 capacidade e desempenho | LocalSimulationServer linhas 10–15; Protocol.MAX_ACTORS/MAX_FRAME; RpgWorld.skillReappear | Limites pequenos são coerentes com teste de dois peers, mas mudar só o número de vagas não demonstra escalabilidade. A-04 ainda faz busca no owner | Medir tick/filas/mapa; fixar população de teste e orçamento antes de qualquer ampliação |

### S01: o que a reprodução demonstra

O servidor reserva uma das duas vagas antes de terminar o handshake. A sonda abriu um socket parcial e um peer normal no **servidor recém-criado**, com simulação vazia própria. O frame parcial recebeu pequenas parcelas em intervalos inferiores ao timeout de leitura. Após mais de 3,6 s, ainda havia duas conexões abertas e o peer normal recebeu Welcome.

Isso prova a distinção entre timeout de uma leitura e prazo total de admissão. Não prova ocupação infinita, indisponibilidade completa, exploração em Radmin ou falha de autenticação; nada foi testado contra uma implantação externa. Por leitura do semaphore, uma conexão incompleta retida consome uma vaga compartilhada com jogadores.

A semântica de SO_TIMEOUT é documentada pelo [JDK 17: Socket.setSoTimeout](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/Socket.html#setSoTimeout(int)). A recomendação de limites de recurso/tempo deve ser contextualizada pelo [OWASP: prevenção de DoS](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html).

O teste antigo `malformedConnectionCannotBreakServerAndPendingHandshakeCloses` cobre frame inválido e fecha o servidor explicitamente com um socket silencioso; não demonstra por si um prazo absoluto para frame enviado aos poucos. É por isso que a lacuna merece caso próprio.

### S02: impacto no Alpha

A sessão planejada de 20–30 min está próxima do limite do transporte. A perda do peer pode iniciar readmissão efêmera e reiniciar progresso/cooldowns. Trata-se de uma regra encontrada no código, não de cronômetro medido durante 30 min nesta rodada. O prazo só é verificado depois de uma mensagem completa; o instante exato de saída depende desse recebimento e dos demais timeouts.

Não foi removido nem ampliado o limite. A decisão deve diferenciar duração máxima intencional, timeout de inatividade, deadline de handshake e timeout de escrita.

### S03 e S05: não anunciar como falhas reproduzidas

Ainda faltam ensaios de consumidor lento e interleavings de admissão/fechamento. Não recomendo patch de concorrência com base somente em intuição. [Netty WriteTimeoutHandler](https://netty.io/4.1/api/io/netty/handler/timeout/WriteTimeoutHandler.html) é uma referência de política de tempo de escrita, não uma correção que precise ser instalada. A alternativa mínima pode continuar usando o transporte atual com um mecanismo de encerramento cuidadosamente definido.

## Bloqueios e lacunas já conhecidos, mantidos separados

- **A-04 / P0:** `RpgSkillEngine.LightningField.advance` continua aguardando `skillReappear`; nenhuma alteração feita em fase, prazo ou recuperação.
- **A-03 / P1 conforme UX:** limpeza do lançador presente, curva e terminais do alvo não resolvidos integralmente. Medição 28,676 continua histórica.
- **Jornada / P1:** 10.000 HP no conteúdo versus fixture de 40 HP e domínio no nível 15. Sem balanceamento alterado.
- **Reconexão / P1:** novo personagem de sessão; dados/contas duráveis e ORG-03/D33 permanecem fora desta investigação.
- **Instalação física/EXE e foco / PENDENTE:** não homologados por compilar ou testar parser.
- **Licenças/proveniência / NÃO VERIFICADO para pacote completo:** referências abertas não autorizam copiar código/arte indiscriminadamente.

## Testes novos realmente realizados

### Compilação isolada

Todos os **286 fontes de produção**, mais uma sonda original, compilados com:

```text
javac --release 17 -encoding UTF-8 -d <temporario>/classes @<temporario>/sources.args
```

Resultado do compilador: **exit 0**. Observação: javac emitiu notas sobre operações unchecked/unsafe; não foram diagnosticadas com -Xlint e não foram tratadas como erro de compilação ou vulnerabilidade comprovada.

Uma primeira chamada PowerShell com ErrorActionPreference=Stop interrompeu o wrapper ao receber a nota em stderr. A chamada foi corrigida para capturar stderr/exit code; a compilação confirmada acima passou. Essa ocorrência foi operacional e não motivou patch em Java.

### Duas sondas únicas

```text
PARSER inputs=5000 accepted=0 rejected=5000 unexpectedExceptions=0
HANDSHAKE elapsedMillis=3666 openConnections=2 normalPeerAdmitted=true partialFrameIncomplete=true serverFailure=false
PROBES completed=2 unexpectedFailures=0
JAVA_EXIT=0
```

- Parser: 5.000 entradas pseudoaleatórias determinísticas, com frames limitados e envelope V1, recusadas com IOException; nenhuma exceção inesperada observada. Como **todas** foram rejeitadas, a sonda tem alcance restrito: não cobre roundtrip válido, todas as mensagens, caminhos profundos ou fuzzing guiado por cobertura.
- Handshake: transporte real e fixture sem RPG, conta, assets ou persistência; somente loopback, uma conexão parcial e uma válida; reprodução de S01.
- Uma primeira execução das mesmas sondas produziu 3.644 ms e completou; o wrapper Start-Process não informou ExitCode adequadamente. A execução direta final acima confirmou código zero. **Não somar repetições:** são duas sondas, não quatro e não 10.000 casos únicos.
- **0 casos JUnit executados**, 0 testes físicos, 0 testes Radmin, 0 testes de carga/instalação/publicação. As sondas não exercitaram as aplicações 6/7 nem o runtime RPG; apenas compilar seus fontes não homologa esses fluxos.

A execução usou `java.awt.headless=true`, user.home e spiritgate.logs.dir em diretório temporário novo. Outputs de compilação e sondas ficaram fora do checkout. Nenhum gerador de capturas históricas foi executado.

Diretório desta coleta: `C:/Users/lucas/AppData/Local/Temp/spiritgate-servidor-pesquisa-0a190beaac1b469f964f42d58756018b`. Contém sources.args, SpiritGateTransportProbe.java, classes, compile.txt, probe-output.txt/probe-error.txt e probe-final.txt. Não é artefato distribuível; pode deixar de existir com limpeza normal do sistema. A sonda completa está preservada no apêndice.

## O que falta para cada objetivo

| Objetivo | Próximos requisitos | O que não precisamos presumir agora |
|---|---|---|
| Coop fechado de dois jogadores | Política A-04; prazo real de admissão; duração/saída explícitas; consumidor lento; jornada e dois PCs | Microserviços, cluster, troca de engine ou banco novo |
| Servidor dedicado privado | Processo headless operacional, shutdown/restart, configuração segura, motivos de saída e métricas; plano de rede | Contas persistentes já prontas ou capacidade acima de dois comprovada |
| Servidor online com progresso durável | Identidade/posse e transporte protegido, política de sessão, importação/recuperação completas, operação e dados testados | Integração automática de ORG-03/D33 por existir laboratório |
| MMORPG de maior capacidade | Orçamento de tick/entidades/banda, interesse por região, partição medida, testes de capacidade e segurança | Aumentar semaphore/MAX_ACTORS como prova de escala |

O [modelo de servidor autoritativo de Gambetta](https://www.gabrielgambetta.com/client-server-game-architecture.html) apoia a separação já existente entre intenções do cliente e estado do servidor. Essa é uma correspondência arquitetural inferida a partir do código, não certificação externa do Spirit Gate.

## Sequência recomendada de trabalho, sem iniciar patches

1. Manter o núcleo autoritativo e escrever o contrato de admissão/lifecycle: prazo de handshake, frame completo, inatividade, escrita, duração máxima e motivo de saída.
2. Transformar S01 em regressão focalizada em árvore isolada e preparar proposta mínima de correção, preservando bytes e capacidade 6/7.
3. Ensaiar S03/S05 com fixture controlada; corrigir apenas o que for reproduzido/confirmado e autorizado.
4. Resolver a decisão A-04 em tarefa própria; validar jornada e política de progresso antes de convidar tester externo.
5. Consolidar regressão segura, métricas e teste físico. Só depois comparar custo de manter sockets versus uma biblioteca de transporte.

Esses passos são proposta técnica. Não foram atribuídos novos IDs TASK/ALPHA nem feitas alterações em gameplay, protocolo, recursos ou persistência.

## Preservação e entrega

Somente dois documentos novos desta pesquisa foram criados no checkout; os fontes da sonda/compilação ficaram no temporário. Sem alteração de produção/testes existentes/assets/configuração; sem Git commit/push/pull, instalação ou publicação.

Conferência final: **3.829 arquivos anteriores preservados por SHA-256**, sem divergências ou ausências no inventário. HEAD inalterado; Git passou de 56 para 58 entradas não rastreadas, exclusivamente pelos dois documentos desta pesquisa. Diff rastreado vazio e `git diff --check` sem apontamentos. Os documentos novos tiveram conferência própria: zero links locais ausentes, tabelas inconsistentes ou espaços finais. Ignorados e arquivos inacessíveis continuam fora da garantia de hashes; os outputs temporários são exclusivos desta coleta.

## Apêndice — sonda original reproduzível

Executar somente em processo descartável e loopback, após compilar os fontes atuais para saída nova. O código abaixo não integra o projeto e não é recomendação de cliente de produção. Nenhum código de terceiros foi copiado.

```java
import com.projetok.multiplayer.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** Sonda temporaria: transporte real, mundo sintetico e somente loopback. */
public final class SpiritGateTransportProbe {
    /** Fixture sem arquivos, contas, assets, persistencia ou servicos externos. */
    static final class World implements SessionSimulation {
        final Map<Integer,Protocol.Actor> actors = new TreeMap<>();
        long tick;
        public boolean join(int id,String name) {
            if(actors.size()>=2)return false;
            actors.put(id,new Protocol.Actor(id,name,140,300,100,100,100,0,false));return true;
        }
        public void leave(int id){actors.remove(id);}
        public boolean accept(int id,Protocol.Intent intent){return actors.containsKey(id);}
        public void step(){tick++;}
        public Protocol.State snapshot(){return new Protocol.State(tick,new ArrayList<>(actors.values()));}
    }

    /** Confere entradas binarias limitadas, sem executar comandos de jogo. */
    static void parserProbe() throws Exception {
        Random random=new Random(173117L);
        int accepted=0,rejected=0;
        for(int i=0;i<5000;i++){
            int n=2+random.nextInt(255);
            byte[] payload=new byte[n];random.nextBytes(payload);payload[0]=1;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            DataOutputStream data=new DataOutputStream(bytes);data.writeInt(n);data.write(payload);
            try {Protocol.read(new ByteArrayInputStream(bytes.toByteArray()));accepted++;}
            catch(IOException expected){rejected++;}
        }
        System.out.println("PARSER inputs=5000 accepted="+accepted+" rejected="+rejected+" unexpectedExceptions=0");
    }

    /** Observa prazo de handshake com leitura parcial e mantem um peer valido. */
    static void partialHandshakeProbe() throws Exception {
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new World())){
            server.start();
            try(var partial=new Socket(InetAddress.getLoopbackAddress(),server.port())){
                var out=new DataOutputStream(partial.getOutputStream());
                out.writeInt(64);out.flush();
                long began=System.nanoTime();
                for(int i=0;i<6;i++){
                    out.writeByte(1);out.flush();
                    Thread.sleep(600);
                }
                try(var normal=new Socket(InetAddress.getLoopbackAddress(),server.port())){
                    normal.setSoTimeout(2000);
                    Protocol.write(normal.getOutputStream(),new Protocol.Hello(1,"Probe"));
                    var reply=Protocol.read(normal.getInputStream());
                    if(!(reply instanceof Protocol.Welcome))throw new AssertionError("No normal admission");
                    int connections=server.connections();
                    long elapsed=(System.nanoTime()-began)/1_000_000;
                    if(connections!=2)throw new AssertionError("Partial connection not retained: "+connections);
                    if(elapsed<=2000)throw new AssertionError("Invalid observation duration");
                    if(server.failure()!=null)throw new AssertionError(server.failure());
                    System.out.println("HANDSHAKE elapsedMillis="+elapsed+" openConnections="+connections+" normalPeerAdmitted=true partialFrameIncomplete=true serverFailure=false");
                }
            }
        }
    }

    /** Executa apenas as duas sondas locais com limites externos de tempo. */
    public static void main(String[] args) throws Exception {
        parserProbe();partialHandshakeProbe();
        System.out.println("PROBES completed=2 unexpectedFailures=0");
    }
}

```

