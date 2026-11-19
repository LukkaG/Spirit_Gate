# Servidor Spirit Gate — skills, códigos e treinos selecionados

Pesquisa em **17/11/2026**. Complementa o [diagnóstico do servidor](SERVIDOR-PESQUISA-E-DIAGNOSTICO-2026-11-17.md). Seleção para Java 17, TCP autoritativo e evolução incremental de duas sessões. Nenhum item foi instalado, clonado ou executado como código remoto.

## Recomendação de adoção

Prioridade imediata: skill local de engenharia do servidor, revisão por invariantes, exemplos de framing/timeout do Netty e testes próprios com JDK/JUnit já existentes. Depois, avaliar Jazzer e análise estática em ambiente isolado. Servidores completos de outros jogos servem como estudo de padrões; uma migração precisa de decisão arquitetural própria.

Pesquisa encontrou uma skill de revisão de segurança proposta em PR no repositório da Anthropic. Uma PR não foi tratada como skill oficial entregue. Skills Azure Java encontradas no catálogo Microsoft são voltadas aos serviços Azure; não resolvem diretamente nosso transporte/gameplay. Foram preteridas por falta de encaixe imediato.

A descoberta usou o [diretório skills.sh](https://www.skills.sh/) e os repositórios dos autores. Não foi usado `npx skills find`, pois os candidatos relevantes já estavam no catálogo e não era necessário baixar/executar o CLI.

## Skills recomendadas, com verificação de origem

Todas as externas abaixo são candidatas, não instruções incorporadas à sessão. Popularidade é indício de uso, não prova de segurança, qualidade de achados ou compatibilidade.

| Skill | Origem e sinais observados | Encaixe no Spirit Gate | Limite e decisão |
|---|---|---|---|
| spiritgate-server-engineer | [SKILL.md local](../.agents/skills/spiritgate-server-engineer/SKILL.md), já disponível | Preservar autoridade, contratos, owner, desconexão e evidência | Aplicada nesta pesquisa; melhorar sua precisão futuramente pode ser útil, sem editá-la agora |
| audit-context-building | [Trail of Bits / catálogo](https://www.skills.sh/trailofbits/skills/audit-context-building): cerca de 7,8 mil instalações | Mapear entrada de dados, donos de estado e invariantes antes de buscar falhas em módulos novos | O próprio escopo distingue construção de contexto de identificação de vulnerabilidades; não serve como selo de auditoria concluída |
| property-based-testing | [Trail of Bits / catálogo](https://www.skills.sh/trailofbits/skills/property-based-testing): cerca de 8,1 mil instalações; [fonte](https://github.com/trailofbits/skills/blob/main/plugins/property-based-testing/skills/property-based-testing/SKILL.md) | Roundtrip de DTOs válidos, limites numéricos, idempotência e sequências de lifecycle | Método é útil com JUnit atual; não obriga adicionar jqwik; invariantes reais valem mais que geração aleatória sem oráculo |
| variant-analysis | [Trail of Bits / catálogo](https://www.skills.sh/trailofbits/skills/variant-analysis): cerca de 7,4 mil instalações | Depois de confirmar S01, localizar repetição da mesma suposição de timeout nos demais caminhos | Usar quando há padrão de bug conhecido; não substituir descoberta inicial por grep de palavras |
| codeql, plugin static-analysis | [Trail of Bits / catálogo](https://www.skills.sh/trailofbits/skills/codeql): cerca de 9,1 mil instalações; [fonte](https://github.com/trailofbits/skills/blob/main/plugins/static-analysis/skills/codeql/SKILL.md) | Fluxos Java/Kotlin e também Python das ferramentas; resultado SARIF revisável | Exige ferramenta, extração e ambiente próprios; não executada. O TCP customizado precisa de modelagem; resultado vazio não comprova segurança |

As páginas de catálogo mostram aproximadamente 8,2 mil estrelas para o repositório Trail of Bits; a [página GitHub](https://github.com/trailofbits/skills) consultada mostrou 8,3 mil. Diferença de atualização/arredondamento entre fontes, sem pretender fixar um número exato. Repositório, README e arquivos dos candidatos foram consultados; não se recomendou apenas pelo ranking.

Os comandos abaixo são os apresentados pelo catálogo para instalação futura; **não foram executados** e dependem do aceite de instalação do responsável:

```text
npx skills add https://github.com/trailofbits/skills --skill audit-context-building
npx skills add https://github.com/trailofbits/skills --skill property-based-testing
npx skills add https://github.com/trailofbits/skills --skill variant-analysis
npx skills add https://github.com/trailofbits/skills --skill codeql
```

Antes de eventual uso, selecionar revisão/commit e revisar scripts, destinos de escrita e ferramentas invocadas. Instruções externas não substituem AGENTS.md nem autorizam publicação, dados reais ou integração ORG-03. A licença do repositório está indicada pelos autores; nenhuma parte dele foi incorporada ao projeto.

## Código aberto e ferramentas: o que aproveitar

| Referência primária | Uso proposto / problema local | Compatibilidade e custo de adoção | Decisão desta pesquisa |
|---|---|---|---|
| [Netty — guia oficial 4.x](https://netty.io/wiki/user-guide-for-4.x.html) e [repositório](https://github.com/netty/netty) | Estudar pipeline, framing de TCP, timeouts e escrita assíncrona; comparar com Protocol/Peer | Biblioteca Java; selecionar versão/JDK e validar contrato 6/7 em qualquer experimento. Substituir transporte aumenta escopo e manutenção de buffers | **Estudo imediato; migração não recomendada sem medição** |
| [LengthFieldBasedFrameDecoder](https://netty.io/4.1/api/io/netty/handler/codec/LengthFieldBasedFrameDecoder.html) | Referência de decodificação de frame com campo de tamanho e limite | O protocolo atual já usa prefixo de tamanho; não basta trocar decoder e presumir equivalência | Usar exemplos como comparação; preservar os bytes atuais |
| [IdleStateHandler](https://netty.io/4.1/api/io/netty/handler/timeout/IdleStateHandler.html) e [WriteTimeoutHandler](https://netty.io/4.1/api/io/netty/handler/timeout/WriteTimeoutHandler.html) | Separar inatividade, tempo de escrita e deadline de admissão | Handler de ociosidade não prova um prazo absoluto de handshake; ordem da pipeline e ponto de medição importam | Elaborar política própria antes de escolher biblioteca |
| [Jazzer](https://github.com/CodeIntelligenceTesting/jazzer) | Fuzzing guiado por cobertura do parser e validação de frames/DTOs | README declara integração JUnit e suporte Windows x86_64; ainda exige dependência/instrumentação, corpus e outputs isolados | **Bom candidato após aceite de instalação**; não executado |
| [SpotBugs](https://github.com/spotbugs/spotbugs) | Verificação Java de bugs que análise de texto não detecta | Precisa analisar bytecode da compilação correta e triar resultados | Candidato a ferramenta de revisão, sem scanner instalado |
| [Find Security Bugs](https://github.com/find-sec-bugs/find-sec-bugs) | Detectores de segurança complementares a SpotBugs | Projeto enfatiza aplicações web/Android; transporte de jogo customizado pode não ser modelado | Complemento, não substitui testes de lifecycle/recursos |
| [Nakama — repositório](https://github.com/heroiclabs/nakama) e [multiplayer autoritativo](https://heroiclabs.com/docs/nakama/concepts/multiplayer/authoritative/) | Estudar callbacks de partida, admissão, processamento por tick e separação de sessão | Servidor/projeto Go e runtime em Go/TypeScript/JavaScript/Lua; não é substituição direta do motor Java | Referência arquitetural; não migrar autenticação/persistência por inferência |
| [Minestom](https://github.com/Minestom/Minestom) | Estudar organização de servidor Java e ferramentas de teste para multiplayer | Protocolo/ecossistema Minecraft; JDK da revisão escolhida precisa conferência, compatibilidade Java 17 não certificada | Estudo de código, sem dependência proposta para Spirit Gate |
| [The Forgotten Server](https://github.com/otland/forgottenserver) | Referência de domínio MMORPG top-down: criaturas, mapas e scripts | C++, protocolos/cliente próprios; não reutilizável diretamente como módulo Java | Estudar conceitos; não copiar engine, assets ou gameplay |
| [jqwik — README atual](https://github.com/jqwik-team/jqwik/blob/main/README.md) | Biblioteca de testes por propriedades considerada | README informa maintenance mode e uma cláusula adicional desde 1.10; termos/versão exigem avaliação antes de adoção | **Adiar escolha da dependência**; começar com geradores determinísticos/JUnit existente |

O guia Netty tem exemplos introdutórios antigos; não usar o requisito de JDK mostrado nessa página como declaração de suporte de toda versão atual. Exemplos de documentação também não definem a política de segurança do Spirit Gate.

Uma alteração de transporte não resolveria sozinha A-04, jornada de XP/missão, identidade durável, autorização econômica ou foco da GUI. Esses problemas têm donos e contratos distintos.

## Fóruns e discussões úteis, com limites

1. **[Heroic Labs: Match Handler instant response](https://forum.heroiclabs.com/t/match-handler-instant-response/5887)** — discussão do processamento de mensagens em ticks. Serve para formular a pergunta de latência versus orçamento de processamento. A explicação também é consistente com a [documentação de multiplayer autoritativo](https://heroiclabs.com/docs/nakama/concepts/multiplayer/authoritative/). Não aplicar os números/hipóteses de partidas daquele tópico como benchmark do Spirit Gate.
2. **[Comunidade OTLand indicada pelo próprio TFS](https://github.com/otland/forgottenserver#support)** — ponto de descoberta para problemas de criaturas/mapas/progressão no ecossistema OpenTibia. O README foi verificado; não houve validação de um tópico específico como solução do nosso código. Não atribuir autoridade técnica a qualquer postagem sem rastrear a implementação.
3. **PRs de skills e resultados Reddit** apareceram na descoberta, mas não sustentam recomendações técnicas sem conferência no autor/repositório/documentação. Nenhuma mensagem foi enviada, issue aberta ou fórum contatado.

Foram usadas fontes primárias para conclusões técnicas. Tópicos de comunidade ajudam a encontrar perguntas e cenários; não são evidência de que nosso runtime já os atende.

## Trilha prática de estudo e exercícios

São exercícios propostos para colaboração futura, não executados nesta entrega e não novas TASKs numeradas. Duração sugerida é orçamento de aprendizagem, não promessa.

| Exercício | Material | Aplicação no código atual | Entregável verificável |
|---|---|---|---|
| 1. Desenhar contrato do servidor — 30–45 min | [Gambetta: Client-Server Game Architecture](https://www.gabrielgambetta.com/client-server-game-architecture.html) | LocalClient → fila → RpgSessionWorld → snapshot | Você marca quais campos o cliente pode propor e quais somente o servidor decide; revisão conjunta do mapa |
| 2. Entender mensagens TCP — 45–60 min | [Netty: guia oficial](https://netty.io/wiki/user-guide-for-4.x.html), [JDK Socket](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/Socket.html#setSoTimeout(int)) | Protocol.read e Peer.read | Tabela: frame parcial, inválido, grande, truncado e lento; distinguir prazo total de timeout de leitura |
| 3. Criar invariantes — 45–60 min | [skill property-based-testing](https://github.com/trailofbits/skills/blob/main/plugins/property-based-testing/skills/property-based-testing/SKILL.md) | Protocol/SkillProtocol/GameplayProtocol | Gerar DTOs válidos para roundtrip e casos inválidos; efeito econômico nunca deriva de dado arbitrário do cliente |
| 4. Estudar orçamento de tick — 30–45 min | [Fiedler: Fix Your Timestep](https://gafferongames.com/post/fix_your_timestep/) | Owner 30 Hz, filas e busca skillReappear | Explicar como um tick lento afeta todos os peers; plano de medição por percentis, sem mudar dt de gameplay |
| 5. Revisar admissão e descarte — 45–60 min | [OWASP DoS](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html) e [Nakama autoritativo](https://heroiclabs.com/docs/nakama/concepts/multiplayer/authoritative/) | Aceite do socket, reserva de vaga, join, Welcome, leave e close | Máquina de estados com motivos de saída e testes de borda; nenhuma conexão real de terceiros |
| 6. Aprender fuzzing — sessão separada | [Jazzer README](https://github.com/CodeIntelligenceTesting/jazzer), [exemplos JUnit](https://github.com/CodeIntelligenceTesting/jazzer/tree/main/examples/junit) | Parser em processo isolado | Corpus, sementes válidas/inválidas e regressões minimizadas; só após aprovar ferramenta e caminhos de escrita |
| 7. Planejar identidade antes de exposição pública — revisão conjunta | [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html) | Fronteira futura entre conexão, sessão e personagem | Requisitos de posse/revogação/transporte; não implementar contas nem conectar persistência nesta etapa |

O [jazzer-workshop do autor](https://github.com/CodeIntelligenceTesting/jazzer-workshop) também foi localizado, mas a página consultada mostrou material pequeno/antigo e pouca atividade aparente. Preferir README/exemplos atuais para uma primeira prática; nenhuma promessa de que o workshop esteja pronto para nosso JDK/JUnit sem ajuste.

O exercício didático mais útil para você agora é **o segundo**: registrar em uma tabela o que deve acontecer em cada falha de conexão. Ele torna concretas as decisões para S01/S02 sem exigir reescrever o servidor.

## Critérios de seleção de código externo

Antes de eventual adoção: fixar revisão/versão, demonstrar compatibilidade Java 17, inspecionar licença/avisos do trecho escolhido, testar equivalência dos contratos atuais e conhecer ferramentas/scripts de instalação. Não importar autenticação, serialização ou criptografia “pronta” sem rastrear confiança e estado.

Para Netty: comparar um adaptador em recorte isolado com os mesmos vetores de bytes e testes de conexão; medir somente os gargalos pertinentes. Para Jazzer/SpotBugs/CodeQL: isolar corpus, builds e resultados, revisar falsos positivos e preservar segredos. Nenhuma dessas ferramentas representa aprovação automática para distribuição.

O plano inicial usa o mínimo já disponível: JDK 17, os testes existentes, fixture sem I/O, hashes e NetworkDiagnostics. Dependências adicionais precisam justificar qual teste ou problema concreto passam a resolver.

## Resultado desta pesquisa

Selecionados quatro candidatos externos de skills com origem e métricas conferidas, exemplos oficiais de transporte, ferramentas de teste/análise e referências de servidor/treinamento. Apenas find-skills e spiritgate-server-engineer locais foram aplicadas. Nenhuma skill externa foi instalada; nenhum framework foi incorporado.

O diagnóstico registra a compilação real e as duas sondas atuais. Os materiais desta página são recursos de estudo e propostas de adoção, não testes executados no projeto. A recomendação imediata é fechar o contrato de admissão/lifecycle e fazer correções localizadas comprovadas, com A-04 e decisões de gameplay em seus recortes próprios.

