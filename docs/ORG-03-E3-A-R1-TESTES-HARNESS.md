# ORG-03-E3-A-R1 — Testes do harness

Em 15/11/2026, biblioteca padrão Python local, sem instalação.

```powershell
python -B -m unittest discover -s tools/org03_e3a_r1 -p test_harness.py -v
```

Resultado: **23 testes passaram**, código 0. A versão inicial passou 20 testes;
depois foram adicionados três testes do adaptador real com runtime inerte.
A validação final também grava resultado estruturado e saída da suíte em
`tools/org03_e3a_r1/validation.json` e `test-output.txt`.

Valida??o reproduz?vel sem subprocessos:

```powershell
python -B tools/org03_e3a_r1/validate.py
```

| Pedido | Teste/garantia exercitada |
| --- | --- |
| 1 | test_01: identidade ativa integral |
| 2 | test_02: exit anterior evita CIM/terminate |
| 3 | test_03: saída entre polls, CIM ausente |
| 4 | test_04: criação diferente com PID igual bloqueia |
| 5 | test_05: comando divergente bloqueia |
| 6 | test_06: CIM vazio não comprova saída |
| 7 | test_07: handle original confirma exit zero |
| 8 | test_08: erro no handle permanece UNKNOWN |
| 9 | test_09: metadata ilegível preservada, sem terminate |
| 10 | test_10: reset de socket registrado e relançado |
| 11 | test_11: EOF de cliente identificado |
| 12 | test_12: cliente termina com resposta retida, nenhum byte liberado |
| 13 | test_13: dois pedidos, um terminate; segundo observa exit |
| 14 | test_14: stderr preservado e valores sensíveis removidos |
| 15 | test_15: mismatch bloqueia tentativas subsequentes |

Os oito testes adicionais verificam saída imediatamente antes da ação,
divergência mesmo com saída posterior, vínculo original inválido, timeout sem
kill alternativo, erro desconhecido não suprimido e três caminhos de Relay.close:
EOF esperado com repetição, exit não zero e exit zero inesperado.

Sockets, processo e CIM são mocks; o adaptador importa `lab_runtime.py` novo
com módulo runtime inerte. Nenhuma fixture cria subprocesso real, cluster ou
socket. Não há teste econômico nem execução C01–C09 nesta R1. Os novos loops
e adaptadores ainda precisam de integração operacional e homologação na rodada
autorizada. `ast.parse` verifica sintaxe das cinco fontes sem importar runtime.

A preservação é verificada por SHA-256 antes/depois, incluindo arquivos
rastreados, árvores E2/E3 anteriores e documentos E2/E3. Os seis hashes dos
scripts E3 são comparados ao source-sha256 anterior e o manifesto final E2
é conferido sem executar sua suíte. O escopo não cobre todos os arquivos
pessoais ou ignorados do checkout.
