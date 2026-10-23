# TASK-158 — Relatório

Capacidade cosmética de aplicação 2 negociada por peer, mantendo envelope e bytes V1. AppearanceRequest altera apenas a própria sessão e valida catálogo, sequência e limites. Snapshots estendidos carregam IDs e raça atribuída pelo servidor; nenhum PNG. Clientes renderizam camadas locais e fallback. Probe real confirmou A/B com aparências e raças diferentes, movimento, saída e reconexão limpa.

Limitações: Sem atributos cosméticos de combate, alteração de raça/domínio, caminhos arbitrários ou persistência online. Peer V1 e peer cosmético coexistem em teste real. LAN física e segurança de produção não homologadas.

Validação: JUnit: 463 executed, 455 passed, 8 aborted, 0 failures, 0 skipped. Nenhuma aprovação artística automática. Diff e hashes no checkpoint.
