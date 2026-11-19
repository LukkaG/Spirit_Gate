# ORG-03-E3-A-R3-01 — Validação da integração

Lucas, 15/11/2026. **117 testes PASSOU; zero falhos ou ignorados na suíte final.**
23 históricos + 48 P1 + 46 novos, todos nas cópias do pacote R3.

```powershell
python -B tools/org03_e3a_r3/validate.py
```

O validador registra sintaxe de 19 módulos, origem/diff/hash por arquivo,
preservação de 4.147 arquivos e a ausência de recursos operacionais. Saídas
completas das três suítes em `preparation/validation-cc5ccccec857/` do run-id,
também referenciadas pelo `preparation/validation.json`.

`safe_tests.py` instala guardas durante importação e execução que recusam
subprocess.Popen/run, socket.socket/create_connection e shutil.rmtree reais.
Cada suíte usa um processo Python de testes separado para isolar importações;
esses intérpretes não são workers/relay do laboratório. Um acesso acidental a
um recurso operacional é uma asserção falha, não autorização nem fallback.

## Cobertura nova

| Testes | Validação efetiva do código, usando dependências inertes |
| --- | --- |
| 01–13 | Entrada nega antes de claim/Lab; cada aceite faltante; R3-01 insuficiente; portas/quotas/run-id inválidos; forma do manifesto; start/comando/worker/relay/entry bloqueados; caminhos e imports/factories novos corretos. |
| 14–20, 45 | Stdout inválido/limite/EOF; protocolo Session escreve bytes; sessão texto não cria processo; comando privado JSON; ACCEPTED não rouba ACK; conexão não pode ser substituída. |
| 46 | EOF do canal privado é enviado antes de aguardar o relay, liberando a thread de controle do filho. |
| 21 | Suite.c02 e TransportIO concretos chamam todos os hooks P1: UNCERTAIN, identidade/socket, permit/ACK, término do handle, novo worker e reconciliação. Resultados são mocks, não commit real. |
| 22–25 | Dono de socket divergente, ACK inválido, exit inesperado e reutilização do worker impedem continuação/aprovação. |
| 26–28 | C05 não consulta resultado antes do corte; lost antigo é recusado; C09 tem três fronteiras e nenhum relay. |
| 29–31 | Quotas verificadas antes de spawn/listener; bootstrap privilegiado não sobrepõe workers. |
| 32–35 | Registro parcial/identidade bloqueada impedem descarte; nove rótulos não aprovam sem controles; caso falho impede conclusão. |
| 36–37 | Schema e fontes P1 byte a byte; comparação AST preserva oráculos C01/C03/C04/C06–C09 e controles/helpers, normalizando somente o rótulo ECONOMIC_UNCERTAIN. |
| 38–40 | Immediate só no próprio data após identidade; terceiro crash e quinto restart recusados antes de pg_ctl/start. Tudo interceptado por mocks. |
| 41–44 | Monitor precisa terminar; caminho externo não pode ser removido; cleanup não depende de stdin bloqueado e mantém erro de captura após parada segura simulada. |

Os 23 históricos permanecem íntegros em legacy e a classe OwnedProcess é a
mesma usada pelo runtime novo. Os 48 P1 exercitam as cópias integradas de
transport/lifecycle/relay/c02_integration; hashes comprovam identidade com P1.
Não se alteraram testes históricos para permitir um resultado antes recusado.

## O que não foi executado

**NÃO EXECUTADOS:** preflight de disponibilidade/versão/binários/bind/ACL real,
PostgreSQL, initdb, SQL, workers/relay reais, fixtures loopback reais, C01–C09
operacionais, crash/recovery e descarte operacional. Nenhuma porta foi usada,
incluindo 5432/55433/55434 e as novas candidatas 55435/55436.

Chamadas a query/stop/recover nos testes operam exclusivamente sobre objetos
inertes e dependências interceptadas. O resultado de um teste de orquestração
não é resultado de cenário econômico e não gera C02.json, results.json ou
evidence operacional. Não foi criada autorização operacional para os testes.

Não existem data/secrets/claim/evidence operacional. O gate E3-A continua não
aprovado e os resultados antigos são preservados; somente a preparação tem
validação aprovada. Limites/riscos e pendências no
[relatório](ORG-03-E3-A-R3-01-RELATORIO-FINAL.md).
