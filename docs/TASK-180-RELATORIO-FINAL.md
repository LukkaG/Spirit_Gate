# TASK-180 — relatório

Implementado o contrato negociado aplicação 4 para publicar mapas/perfis no Game Server RPG do launcher. O Studio continua isolando DRAFT/PREVIEW/PUBLISHED; somente Salvar inicia a transação. O destino sintético da TASK-179 foi preservado por um adaptador comum.

Clientes preparam documentos, assets locais e renderer antes de confirmar. O servidor valida candidatos reais e posições no owner, pausa comandos incompatíveis, instala a candidata, verifica ACTIVE de todos e só então confirma publicação e retoma. Snapshots e comandos carregam hash. Falhas recuperam a revisão anterior ou isolam o participante que não restaurar sua cena.

A autorização é uma capacidade local na JVM do host, aberta explicitamente pelo launcher; não há administração exposta na VPN. Clientes V1/V2/V3 continuam nos modos históricos e impedem publicações incompatíveis. Não houve alteração de save, push, release, firewall, assets originais ou TASK-181.

O teste integrado usa Game Server RpgSessionWorld, dois clientes TCP e o renderer de produção, junto ao PreviewServer separado. Evidências finais de ativação, PvE, portal e rollback, contagens de testes, métricas e SHA dos pacotes encontram-se no checkpoint da tarefa. O teste físico entre computadores Radmin e a aprovação visual humana continuam pendentes.

Restrições explícitas: 960×640, forest/village e root forest, recursos preinstalados, sem transferência de PNGs, animações de ambiente/gatilhos editoriais recusados no publicado, até dois jogadores e seis atores adicionais. Arquivos necessários ausentes ou incompatíveis bloqueiam a transação. Não se promete troca imperceptível, simultaneidade entre monitores ou recuperação comprovada de falta de energia.

Os novos ZIPs privados Anfitrião/Convidado e seus manifestos são gerados separadamente dos anteriores. Nenhum feed remoto foi configurado ou atualizado automaticamente. Os documentos de protocolo, sincronização, publicação, rollback e homologação descrevem como operar e verificar o fluxo.

## Verifica??o final

558 testes GUI aprovados, zero falhas/abortos; em headless, 549 aprovados e nove Swing abortados por aus?ncia de display. Python: 33 aprovados. Atualizador PowerShell: 32 checks aprovados.

O ensaio integrado confirmou duas prepara??es/ativa??es reais, revis?o de 2108 bytes e payload de 4216 bytes para os dois clientes, incluindo um asset preinstalado preparado e pintado nos dois, sem transferir imagens. Salvar levou 236,706 ms; a barreira coordenada de ativa??o levou 29,920 ms e a janela coordenada apos preparo 117,975 ms (preparo do servidor medido separadamente). A restaura??o do rascunho e publica??o de rollback levou 619,176 ms. Tick p95: 6,140 ms; paint p95 dos clientes: 11,960/11,570 ms. Preparo observado na ?ltima revis?o do ensaio: servidor 5,151 ms; clientes 5,536/5,620 ms. S?o medidas locais, sem promessa sobre Radmin ou aus?ncia de pausa percept?vel.

Evid?ncias: [resumo](../.task-checkpoints/TASK-180/live-publishing/validation-summary.json), [dois clientes publicados](../.task-checkpoints/TASK-180/live-publishing/integrated-probe-final/published-two-clients.png), [rollback](../.task-checkpoints/TASK-180/live-publishing/integrated-probe-final/rollback-confirmed.png) e [m?tricas brutas](../.task-checkpoints/TASK-180/live-publishing/integrated-probe-final/metrics.json).
