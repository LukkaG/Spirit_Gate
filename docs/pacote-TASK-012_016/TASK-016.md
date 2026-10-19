# TASK-016 - Candidato de testes consolidado + verificacao final

**Prioridade P1 | Nenhuma publicacao automatica**

## Objetivo
Consolidar as melhorias TASK-012 a TASK-015 em um pacote Windows x64 de testes, sem confundir verificacao automatizada com homologacao externa.

## Implementacao autorizada
1. Revise diffs, testes, riscos e assets de cada etapa. Recuse avancar se alguma task previa nao tiver testes verdes ou se um asset invalido tiver sido ativado no renderer.
2. Regere compilacao e pacote usando fontes atuais; nao reutilize JAR historico. Rode `mvn clean test` e repeticao offline se as dependencias locais existirem.
3. Confira inventory SHA/manifest externo/PE x64/runtime Java 17, classe principal, whitelists de bytecode e **nomes ZIP POSIX `/` sem normalizacao**. Faça controle negativo em copia descartavel.
4. Executar smoke Windows sem Maven/Java no PATH (e fora de Projeto-K), com janela, fechamento e captura real. Documentar explicitamente limites: sem assinatura, sem multiplayer, sem teste externo.
5. Incluir instrucoes ao testador, changelog incremental, roteiro por task, modelo de relato de bug, pendencias de assets e hashes. Produzir nova versao RC unica sem sobrescrever nenhuma anterior.
6. Fazer checkpoints/diffs isolados e relatorio consolidado. Nao alterar versoes do protocolo, cliente remoto, banco ou rede; nao fazer push, publicar ou distribuir automaticamente.

## Gate final
- Proxima etapa so fica **AUTOMATIZADA APROVADA** com JUnit verde + ZIP canonico + auditoria no proprio pacote + smoke local real.
- Declarar **HOMOLOGACAO HUMANA PENDENTE** ate teste independente de jogabilidade e computador externo.
