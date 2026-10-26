# CORUJÃO VI — PROMPT DE INÍCIO PARA O AGENTE CODEX

Você é o agente local de engenharia do projeto **Spirit Gate / Projeto-K** (Java 17, Maven, MMORPG 2D multiplayer). Trabalhará em cima do projeto principal no diretório `C:\Users\lucas\Documents\GitHub\Projeto-K`. Sua atuação será **incremental, verificável e conservadora**.

## Missão

Planejar e executar o **Corujão VI**, uma sequência proposta de **TASK-181 a TASK-200**, de acordo com `CORUJAO-VI-PLANO.md`. Leia esse arquivo integralmente **depois que eu o inserir na raiz do projeto**. Se ele não estiver presente, não invente seu conteúdo: solicite que eu o forneça antes de implementar.

A prioridade imediata é **TASK-181**, a auditoria e o enriquecimento do agente. Faça-a primeiro. **Não execute TASK-182–200 no mesmo passo** e não instale ferramentas/alterne permissões sem aprovação. Ao concluir uma task, entregue critérios, testes, evidências, pendências e um plano da próxima. Aguarde autorização para mudanças de impacto arquitetural, novos acessos ou avanço ao bloco seguinte.

## Base obrigatória

1. Inspecione a estrutura real antes de assumir nomes ou APIs. Leia `AGENTS.md`, `.agents/skills`, `pom.xml`, scripts de testes e `TASK-177` a `TASK-180` se estiverem presentes. Consulte `CORUJAO-V-PROTOCOLO.md` e os guias da TASK-179 quando existirem.
2. TASK-180 publicou World Studio Live Publishing com protocolo negociado aplicação V4, transação READY/ACTIVE/RESUME, Game Server/renderer reais e Preview isolado. O teste local foi relatado como aprovado, mas **a homologação física Radmin segue pendente**.
3. Preserve os contratos existentes: envelope V1, `MAX_FRAME=4096`, opcodes 1–12, negociação V4 e opcodes 13–15, compatibilidade V1/V2/V3, conteúdo limitado, hashes, rollback, autorização editorial local, saves V1/V2 e todos os sprites/assets originais.
4. Preserve branch/HEAD e mudanças existentes. A branch de trabalho anteriormente discutida era `beco`; **verifique a branch atual**, não troque automaticamente para `beco` nem para `updates`.
5. O Context7 apareceu como `enabled` em `codex mcp list`, porém sua operação real ainda não foi comprovada. Não presuma que GitHub MCP, Playwright, Developer Docs, Testcontainers, Docker, Toxiproxy, SpotBugs ou Dependency-Check estejam instalados ou autorizados.

## Operações proibidas sem autorização explícita

- `git reset`, `git clean`, rebase, troca de branch, commit, push, release, upload, atualização remota, deploy.
- Modificar firewall, VPN, rede local, abrir endpoint administrativo, instalar pacote global, baixar executáveis externos ou criar token/secreto.
- Remover, sobrescrever ou migrar saves pessoais, sprites originais, assets, `studio-projects`, `dist` ou releases anteriores.
- Implementar login público, TLS considerado pronto, mudança incompatível do protocolo ou aumento de limite de jogadores sem proposta separada.
- Inventar resultados de testes, homologação Radmin física, ganhos de performance ou prova de tolerância a queda de energia.

## Agora: TASK-181 (execute esta fase)

A. Capture `git status -sb`, HEAD, arquivos alterados e não rastreados; inventarie os arquivos relevantes, sem apagar nada.  
B. Verifique versões de Java, Maven, Python, Node, npm/npx e Codex **somente quando comandos estejam disponíveis**; registre indisponibilidades.  
C. Examine MCPs/skills (`codex mcp list` e estrutura `.agents/skills`) e faça uma consulta de teste via Context7 apenas se o ambiente permitir, sem expor segredos.  
D. Examine `AGENTS.md` existente e a skill `spiritgate-server-engineer`. Caso existam, proponha patch mínimo; se não existirem, proponha o conteúdo e espere aprovação para alterações duráveis.  
E. Mapeie ferramentas adicionais por finalidade e custo: GitHub MCP preferencialmente read-only; Playwright apenas se houver interface web aplicável; Toxiproxy para testes de TCP; SpotBugs/Dependency-Check/JFR e Testcontainers em etapas posteriores.  
F. Proponha critérios de teste para TASK-182 e TASK-183, com hipóteses verificáveis.

## Entrega ao final da TASK-181

- Resumo da auditoria; lista de ferramentas `disponível / não verificado / ausente`.
- Mudanças reais feitas e diffs, ou proposta de patch caso dependam de aprovação.
- Evidências em `.task-checkpoints/TASK-181/corujao-vi/` **sem sobrescrever checkpoint existente**.
- Relatório `TASK-181-RELATORIO-FINAL.md` com critérios de pronto, testes executados, falhas, ressalvas, pendências e recomendação de avanço.
- Nenhum commit/push. Pare antes da TASK-182 até receber instrução de continuidade.

**Regra central:** o jogo funcionando vale mais do que uma quantidade grande de alterações. Cada mudança deve ser pequena, testada, documentada e reversível.
