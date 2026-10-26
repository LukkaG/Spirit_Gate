# SPIRIT GATE — CORUJÃO VI
## Plano diretor: evolução do Game Server, estabilidade e persistência experimental

**Estado:** PROPOSTA DE EXECUÇÃO — não executada.  
**Sequência reservada:** TASK-181 a TASK-200.  
**Base técnica declarada:** TASK-180, World Studio Live Publishing, protocolo de aplicação 4 (V4), servidor `RpgSessionWorld` / `LocalSimulationServer`, dois clientes TCP reais em testes locais, PreviewServer separado.  
**Repositório local esperado:** `C:\Users\lucas\Documents\GitHub\Projeto-K` (confirmar existência e estrutura no início).  
**Branch de trabalho conhecida:** `beco`; **não presumir HEAD nem mudar a branch**: ler `git status -sb` e registrar HEAD. Não promover à branch `updates`.

> Esta é uma proposta nova. Nenhum dos documentos anteriores autorizou ou implementou automaticamente a TASK-181 ou posteriores. Só a inspeção do projeto local poderá confirmar nomes de classes, dependências, comandos e comportamento atual.

## 1. Objetivos mensuráveis

1. Tornar as alterações do servidor mais verificáveis, reprodutíveis e seguras para o projeto.
2. Exercitar publicação V4, rollback e reconexão sob falhas de rede **sem criar um servidor administrativo exposto**.
3. Melhorar telemetria e diagnóstico sem afirmar ganhos de desempenho sem comparação controlada.
4. Preparar persistência **online sintética e isolada**, sem converter saves pessoais V1/V2 ou declarar autenticação de produção.
5. Manter o build do anfitrião/convidado com integridade verificável, e separar testes locais da homologação física Radmin.

### Contratos históricos a preservar

- Envelope binário V1 e `MAX_FRAME=4096`; bytes/opcodes 1–12 históricos. Aplicação V4 negociada por `Hello(4)`, opcodes 13–15 e protocolo de publicação com `BEGIN/CHUNK/END/READY/ACTIVATE/ACTIVE/ABORT/RESUME/REJECT`.
- Descritor de conteúdo V1 máximo 4 MiB; `CHUNK` máximo 2800 bytes; hash SHA-256 lógico e de transação; wrappers limitados; sem downgrade silencioso.
- Studio mantém DRAFT/PREVIEW/PUBLISHED; **somente** `Salvar - aplicar ao jogo` publica, após validação, READY/ACTIVE e confirmação. PreviewServer em porta separada, não conectado à partida principal até o comando explícito.
- Autorização editorial local, ligada à referência Java na JVM do host; **sem** API administrativa exposta na VPN, sem credenciais em logs, sem publicação recebida do jogador.
- Compatibilidade de aplicações 1/2/3; peers antigos bloqueiam publicação incompatível, sem expulsão forçada. Nenhuma mudança nos saves V1/V2, sprites originais, projetos pessoais ou progressão.
- Limites existentes da TASK-180, enquanto não houver projeto separado aprovado: forest/village, root forest, mapas 960×640, até 2 jogadores e 6 outros atores, assets preinstalados (sem download de PNG por conteúdo), até 128 assets e 64 MiB de pixels ativos por revisão.
- Recuperação/journal, ponteiros e histórico versionado; não mover `active.txt` manualmente durante partida, nem tratar hash SHA-256 como autenticação.
- Não presumir TLS, login definitivo, replicação distribuída, transição imperceptível, tolerância testada a energia ou conformidade de segurança de produção.

## 2. Segurança operacional e autonomia do agente

1. **Auditar antes de codificar**: `git status -sb`, HEAD, arquivos não rastreados, `pom.xml`, `AGENTS.md`, skills, MCPs disponíveis, scripts, testes, docs TASK-177..180 e CORUJAO-V-PROTOCOLO.md. Nunca supor que ferramenta opcional foi instalada.
2. **Não executar** `git reset`, `git clean`, `git checkout`, `git switch`, rebase, cherry-pick, commit, push, deploy, atualização remota, migração de dados pessoais, alteração de firewall/roteador ou remoção de arquivo pré-existente sem autorização explícita.
3. Não baixar binários arbitrários nem instalar dependências globais por iniciativa própria. Dependências novas, Docker, GitHub MCP, token, Toxiproxy e banco externo devem passar por aprovação e ser opcionalmente testados em ambiente isolado.
4. Preservar o diretório `dist` e suas versões anteriores, saves, assets originais, `studio-projects`, manifests, checkpoints e qualquer estado não rastreado. Para empacotar, criar nomes **novos** e inventários por SHA-256.
5. Cada mudança: análise → plano curto → patch mínimo → testes positivos e negativos → evidências → relatório. Nenhuma refatoração transversal sem plano/consentimento.
6. Se uma task exigir hardware, rede VPN externa, credencial, ferramenta ausente, exclusão de compatibilidade ou decisão arquitetural, reportar `BLOQUEADA / MANUAL PENDENTE`; não inventar prova. Continuar somente com trabalho independente e previamente aprovado.
7. Não começar TASK-201. A TASK-200 consolida o pacote. Após cada módulo, emitir checkpoint e apresentar diferenças/riscos antes de abrir o próximo.

## 3. Tarefas detalhadas

### Módulo I — Agente, baseline e laboratório (181–184)

**TASK-181 — Auditoria e enriquecimento do agente**  
Escopo: inventariar ferramentas MCP e skills já registradas; verificar o Context7 funcionalmente (consta habilitado no registro exibido pelo usuário, não há confirmação de chamada real); detectar GitHub MCP, Developer Docs e Playwright sem instalar nem conceder permissão; revisar `AGENTS.md` existente sem sobrescrevê-lo. Propor/validar `.agents/skills/spiritgate-server-engineer/SKILL.md` com referências a documentos *reais*.  
Pronto quando: inventário com disponível/ausente/não testado; skill descoberta ou impedimento documentado; zero alterações do Game Server; revisão humana de permissões e acesso do agente.

**TASK-182 — Baseline de qualidade e regressão**  
Escopo: reproduzir suites atuais, mapear build Maven/Java 17, testes Java GUI/headless, Python e atualizador PowerShell; registrar métricas e falhas ambientais. Analisar integração opt-in de SpotBugs e OWASP Dependency-Check, em modo inicialmente informativo.  
Pronto quando: contagens, logs, hashes e comandos reproduzíveis; separação entre falha de teste e abortamento ambiental; sem desabilitar testes nem silenciar regras para obter verde.

**TASK-183 — Laboratório de rede reproduzível**  
Escopo: construir roteiro/fixtures para Toxiproxy **somente se instalado e autorizado** (ou ferramenta equivalente já existente); cenários de latência, variação, desconexão, perda de leitura/escrita e reconexão; encaminhar tráfego TCP de testes sem mexer na porta administrativa e sem reconfigurar a rede do usuário.  
Pronto quando: execução repetível localhost com duas conexões e logs por cenário, e rollback da própria configuração de teste. Falhas de instalação externas ficam pendentes.

**TASK-184 — Radmin: roteiro e evidência física**  
Escopo: produzir checklist de execução em dois computadores, seleção explícita de adaptador/IP, compatibilidade de builds, publicação, rollback, movimento, PvE, portal e queda de conexão; habilitar coleta de logs em ambos sem coletar segredos.  
Pronto quando: roteiro testável, campos de evidência e matriz de aceitação. **Homologação física permanece PENDENTE até ser realizada por pessoas em duas máquinas**; não apresentar laboratório local como substituto.

### Módulo II — Consistência do protocolo e recuperação (185–188)

**TASK-185 — Reconnect e sincronização por revisão**  
Escopo: auditar fluxo de ingresso V4 antes de gameplay, rejeição de inputs/snapshots com hash antigo e reconexão em estado ativo/candidato. Corrigir apenas bugs reproduzidos; evitar duplicar entidades ou permissões.  
Pronto quando: testes para login de sessão de teste, desconexão em BEGIN/READY/ACTIVATE/RESUME e reconexão com revisão ativa consistente; sem cliente aceitando conteúdo parcial.

**TASK-186 — Timeouts, falhas de publicação e rollback**  
Escopo: fault injection em staging, ACKs atrasados/duplicados/incorretos, cancelamento pelo host, falha pós-ativação e reinício com journal interrompido. Verificar monotonicidade de estado e confirmação em disco; proibir commit falso.  
Pronto quando: nenhuma fase relata sucesso antes do commit; rollback ou isolamento compatível com a documentação; testes não contornam validação geométrica.

**TASK-187 — Backpressure e limites do transporte**  
Escopo: medir filas, writer único, frames completos, limites de tamanho, peers lentos e pacing; revisar custo de snapshots sem reescrever protocolo.  
Pronto quando: stress limitado comprova proteção contra crescimento sem limite e nenhuma regressão de opcodes/bytes históricos, incluindo V1/V2/V3/V4 e wrappers malformados.

**TASK-188 — Compatibilidade e controle de admissão**  
Escopo: matriz para clientes mistos, publicação bloqueada por cliente antigo, ingresso durante barreira e cliente novo após commit. Validar mensagens claras e ausência de downgrade.  
Pronto quando: testes cruzados explícitos e condições de bloqueio/join auditáveis; sem desconectar cliente antigo para forçar Save.

### Módulo III — Operação e desempenho (189–192)

**TASK-189 — Observabilidade e diagnóstico local**  
Escopo: incorporar métricas limitadas de tick, render, filas, RTT, latência de READY/ACTIVE e falhas; usar Java Flight Recorder/JDK quando viável. Logs estruturados sem token, dados pessoais ou vazamento de hashes desnecessários.  
Pronto quando: instruções reproduzíveis de coleta e leitura de dados, sem instrumentação com impacto perceptível não medido.

**TASK-190 — Soak test e orçamento de recursos**  
Escopo: ensaio longo com até 2 clientes/6 atores, cenários de perfis de colisão, assets, 4 MiB de descritor e sucessivas publicações/rollbacks dentro dos limites atuais; comparar heap, threads, tick e paint com o baseline sob mesmas condições.  
Pronto quando: percentis, duração, ambiente, método e limites são explicitados; nenhuma alegação de melhora sem controle comparável.

**TASK-191 — Lifecycle: encerramento e retomada**  
Escopo: encerramento controlado de Studio, Preview e Game Server; restart confirmado; tentativas de shutdown sob transação; isolamento das evidências e dos saves pessoais. Falta de energia só pode ser simulação de falha de processo, com linguagem precisa.  
Pronto quando: ordem de encerramento e recuperação verificável; sem perda silenciosa de revisão confirmada.

**TASK-192 — Modelo de ameaças e proteções experimentais**  
Escopo: revisar superfície TCP, rate limit, adulteração de frame, spoof de ACK, serialização, traversal, limites de memória e privilégios de host; elaborar plano para autenticação/TLS futuros em vez de declarar segurança de produção.  
Pronto quando: matriz de ameaça→mitigação→teste→risco residual, com casos negativos automatizados e sem endpoint administrativo adicional.

### Módulo IV — Persistência online sintética isolada (193–196)

**TASK-193 — Contrato de repositório online experimental**  
Escopo: estudar `SyntheticOnlineRepository` e desenho da persistência antes de conectar a partida real; mapear atomicidade, versões, integridade, idempotência e isolamento físico de pastas. Testcontainers/PostgreSQL **somente como extensão opcional e autorizada**, sem exigir Docker ao jogador.  
Pronto quando: ADR aprovado, testes de contrato e migração **apenas sintética**, sem tocar saves V1/V2.

**TASK-194 — Escrita atômica e conflitos de revisão**  
Escopo: implementar e testar, se aprovado, persistência de personagem sintético versionado com recuperação de falha e rejeição de escrita concorrente/obsoleta. Sem credenciais reais nem importação de personagens pessoais.  
Pronto quando: operações são idempotentes onde necessário; testes de crash/concorrência e checksum; nenhum estado parcial é tomado por confirmado.

**TASK-195 — Identidade de sessão experimental**  
Escopo: isolar identificadores de sessão, posse autoritativa do personagem e anti-duplicação em laboratório; produzir projeto separado para autenticação de contas, gestão de segredos e TLS. Não implementar login público sem revisão de segurança.  
Pronto quando: impedir controle do personagem de outra sessão nos testes e documentar riscos residuais.

**TASK-196 — Retomada de personagem sintético**  
Escopo: demonstrar criação, gravação, desconexão e restauração de personagem **sintético** sem duplicação de XP/loot/quest; verificar progressão e região coerentes com o conteúdo confirmado. Integração com launcher real fica atrás de aprovação explícita.  
Pronto quando: ciclo completo testado com identidade sintética e critérios de exclusão das contas e saves pessoais.

### Módulo V — Distribuição, integração e encerramento (197–200)

**TASK-197 — Catálogo e assets consistentes**  
Escopo: conferir manifests e hashes de assets em host/convidado, ausência de recurso, limites de decodificação e instalação. Sem habilitar PNG dinâmico ou importar assets remotos por URL como efeito colateral.  
Pronto quando: faltas e adulterações são rejeitadas com causa clara; nenhum placeholder é considerado sucesso sincronizado.

**TASK-198 — Build privado verificável por papel**  
Escopo: produzir, após a qualidade passar, novos pacotes de Anfitrião/Convidado com Java integrado, allowlist e SHA-256; conferir ausência de credenciais, saves, ferramentas de desenvolvimento e arquivos antigos. **Não substituir** distribuição anterior.  
Pronto quando: extração em pasta limpa, self-check e sessão local de dois clientes funcionam. Sem feed remoto e sem release publicado.

**TASK-199 — Prova integrada de jogo e publicação**  
Escopo: combinar duas janelas reais de cliente, Game Server real, Preview separado, alteração de geometria/perfil, publish/rollback, movimento, PvE e portal, incluindo falhas de rede quando disponíveis.  
Pronto quando: hashes do cliente/servidor, métricas e evidências identificam cenário, execução e rollback; sem usar mockup como Game Server.

**TASK-200 — Consolidação e veredito Corujão VI**  
Escopo: rodar regressão completa, produzir inventário de modificações/risco, matriz de compatibilidade, deltas de performance, SHA, pendências e checklist manual Radmin; produzir relatório final com estados `VALIDADA`, `VALIDADA COM RESSALVAS`, `PARCIAL` ou `BLOQUEADA`, sustentados por evidência.  
Pronto quando: nenhuma pendência física é ocultada e nenhum lançamento é feito. **Não iniciar TASK-201.**

## 4. Roteiro de execução e gates

- **Gate A (após 184)** — agente configurado e teste de rede local preparado, baseline preservado; Radmin físico pode continuar pendente.
- **Gate B (após 188)** — protocolo, compatibilidade e rollback estáveis sob cenários negativos.
- **Gate C (após 192)** — métricas, ciclo de vida e ameaças documentados, sem regressões importantes.
- **Gate D (após 196)** — persistência estritamente sintética; qualquer integração ao launcher/conta real depende de aprovação.
- **Gate E (após 200)** — testes, builds privados, inventário e relatório; homologação Radmin real indicada corretamente.

**Regra de bloqueio:** falha de baseline essencial, divergência de hash, perda confirmada de dados, incompatibilidade com V1–V3, permissão administrativa indevida, rollback falso ou alteração acidental de saves/assets = interromper bloco e apresentar correção e evidência antes de continuar.

## 5. Testes conhecidos (validar caminhos antes de executar)

```powershell
python tools/run-local-tests.py
python -m unittest discover -s tools -p 'test_*.py'
python tools/task180-verify.py <run>              # aceitar somente argumento válido confirmado no projeto
# tools/updater/Test-Atualizador.ps1 -EvidenceDir <checkpoint>
```

Não inferir resultado desses comandos a partir de relatórios anteriores; a TASK-180 **relatou** 558 Java GUI / 33 Python / 32 checks do atualizador no ambiente de conclusão. Reexecutar nas condições atuais. GUI Swing exige display; distinguir skips ambientais de testes verdes.

## 6. Evidências obrigatórias de cada task

Diretório recomendado (criar sem apagar existente): `.task-checkpoints/TASK-<N>/corujao-vi/`.

- `before.json`: branch, HEAD, working tree, ferramentas presentes, baseline de testes e inventário protegido.
- `plan.md`: escopo exato, riscos, decisões e critérios de pronto.
- `tests.md` + logs: comandos, status, contagens, duração, ambiente, falhas e retries.
- `after.json`: hashes SHA-256, lista de arquivos alterados e análise de regressões.
- `TASK-<N>-RELATORIO-FINAL.md`: o que foi implementado, o que não foi, limitações e próximos passos.

No fechamento da TASK-200: `CORUJAO-VI-RELATORIO-FINAL.md`, `CORUJAO-VI-PROTOCOLO.md` (apenas se houver mudança negociada/documentada; caso contrário declarar nenhuma), `CORUJAO-VI-HOMOLOGACAO.md`, `CORUJAO-VI-PENDENCIAS.md` e inventário dos ZIPs privados/manifestos, se produzidos.

## 7. Teste humano obrigatório de Radmin

O usuário/operador precisará conduzir ao menos: máquina A Anfitrião, máquina B Convidado, pacotes privados pareados e conexão pelo adaptador Radmin explicitamente selecionado; confirmar sprites e revisão base, alterar mapa/perfil no Preview (sem afetar Game Server), clicar Salvar, verificar READY/ACTIVE/RESUME e hashes em A/B, testar movimento/PvE/portal, rollback, desconexão, retorno e tentativa com cliente antigo. Guardar logs dos dois lados. **Sem execução física e evidências, o status deve continuar pendente.**

---

**Escopo congelado como proposta de planejamento.** A execução começa pela TASK-181, com auditoria somente-leitura e plano de mudanças, e não por implementação simultânea de vinte tarefas.
