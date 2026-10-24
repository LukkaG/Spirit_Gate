# Homologação técnica

Referência: 465 testes Java, 457 aprovados e oito abortados ambientais no headless; zero falhas. Resultado final recompilado: 477 executados, 469 aprovados e oito abortados ambientais no headless; execução gráfica 477/477, zero abortados/falhas/ignorados. Python unittest: 30/30. Logs finais em `.task-checkpoints/TASK-174/corujao-v/headless-final.log` e `gui-final.log`; classes em `server/target/local-tests/run-muiksx7e`.

Cobertura real inclui offline/quatro raças/domínios/Shunpo, saves V1/V2, Studio, Quest Builder, Coop histórico, protocolo/limites, dois clientes, encontro compartilhado, concorrência serializada, repetição de comandos, HP, morte/respawn, política sintética, quest autorada, UUIDs efêmeros, portais/visibilidade e aparência.

A demonstração usa servidor e dois sockets reais com janelas Swing; `tools/CorujaoFiveDemo.java`. Capturas printAll são evidência de componentes desenhados, não aprovação humana de monitor/DPI. O teste integrado da missão/portais é automatizado no domínio/sessão; não é declarado como jornada nativa completa capturada.

O remoto Radmin e bind real no adaptador virtual ainda não foram homologados. A tentativa de inventário via Get-NetAdapter encontrou acesso negado; a política de autorização foi testada com dados sintéticos. Nenhuma elevação ou mudança de rede foi feita.

Pacotes devem passar conferência de manifesto SHA-256, exclusão de saves/credenciais e smoke do runtime/jar. O cenário humano obrigatório de duas máquinas está em CORUJAO-V-RADMIN-MANUAL.md.
