# Pendências e limites conhecidos

- Mapas acima de 960×640 são editáveis, mas não executam no adaptador atual. O save/snapshot V2 valida posições e regiões fixas. Expansão incompatível depende de contrato e autorização próprios.
- Quests autoradas usam ledger em memória. `.sgquest` persiste autoria; não persiste progresso de personagem. Checkpoint de quests em memória é disponível somente no mapa padrão; não representa alteração do schema V2.
- Aprovação artística e homologação física de DPI, foco, cores e monitor permanecem humanas. Sprites originais não foram substituídas.
- As técnicas de DomainPreview continuam conceitos. Reset com Spirit Coins continua indisponível na interface de gameplay; não existe carteira/checkout nesta rodada.
- Medições de desempenho são locais, com cache do sistema e atividade externa não isolados. FPS físico e memória total sob carga prolongada não foram homologados.
- O Coop histórico permanece distinto do novo backend RPG experimental. TASK-150 a TASK-159 implementaram movimento autoritativo e aparência localhost. Combate RPG compartilhado permanece indisponível, com ações recusadas; combate offline e Coop histórico preservados.
- LAN física, contas, autenticação pública, persistência online e segurança de produção não foram homologadas. Reconexão cria sessão sintética nova. Não expor o servidor.
- Os 180 PNGs gerados são provisórios. Validação técnica de pivôs, hashes e composição não equivale a aprovação artística. IDs cosméticos não concedem atributos.
- Encerramento na TASK-159. Não iniciar TASK-160 sem novo escopo autorizado.
