# TASK-023 - Orquestrador de qualidade

Objetivo: um comando reprodutível para validação local completa.
Situação: APROVADA. Base: 223 JUnit e 18 Python.

Diagnóstico: os scripts anteriores empacotavam e auditavam, mas a execução conjunta
de Java e Python dependia de comandos manuais. Implementado tools/qualidade.py;
reutiliza Maven/Surefire e unittest, sem duplicar testes ou instalar dependências.
Verifica Java 17, Maven e Python; cache Maven do projeto e opção --offline.
Logs individuais completos, resumo TXT e relatório estruturado JSON por execução.
Códigos: 0 aprovado, 2 ambiente, 3 testes, 4 build. Não muda o Windows global.
Adicionado tools/test_qualidade.py: classificação, totais XML, ambiente ausente e
rejeição de identificador com escape de diretório. Testes negativos usam mocks
somente no teste unitário, sem substituir a execução real do orquestrador.

Execução real: python tools/qualidade.py --offline --task 023
Resultado: 223 JUnit, zero falhas/erros/ignorados; 22 Python aprovados.
Evidências: TASK-023-qualidade.json, TASK-023-testes.txt e logs TASK-023-*.log.
Arquivos alterados/adicionados constam no diff; checkpoints before/after em
.task-checkpoints. Retomada: TASK-024 após esta aprovação.

Limitações: logs técnicos locais podem conter caminhos do ambiente de build;
não são o pacote de diagnóstico sanitizado da TASK-034. Não realiza homologação
humana nem certifica arte final. Falhas Maven não classificadas como teste são
tratadas como build. O modo online não foi necessário nesta validação.

## Resultado final observado
Validação local aprovada: 223 JUnit e 22 Python; zero falhas.
