# Continuidade autorizada

Trabalho limitado a TASK-160..175. O Corujão V permanece experimental com pendências enumeradas; não iniciar TASK-176. Para reproduzir: `python tools/run-local-tests.py`; `python -m unittest discover -s tools -p "test_*.py"`; build privado `python tools/build-corujao-v.py`.

Para demonstração, compile `tools/CorujaoFiveDemo.java` com classpath das classes recém-compiladas e execute `com.projetok.multiplayer.CorujaoFiveDemo` a partir da raiz, passando diretório novo para evidências. Janelas reais, dois clientes TCP e servidor na mesma JVM, cada conexão com seus sockets. Testes históricos de dois processos também permanecem na suite.

Resta ampliar as validações específicas das tarefas parciais e executar roteiro Radmin em ambiente autorizado com duas máquinas. O checkpoint inicial contém fonte recuperável e hashes de assets/saves/projetos/releases existentes. Restaurar em pasta nova, nunca sobre saves pessoais. Nenhum commit/push/publicação faz parte deste encerramento.
