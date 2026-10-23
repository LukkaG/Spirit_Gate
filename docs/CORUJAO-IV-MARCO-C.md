# Marco C — Exploração, confiabilidade e diagnóstico

TASK-142: limites explícitos e validação física do spawn no adaptador. Quatro raças executam o mesmo mapa sem restrição racial. Mapas maiores que 960×640 permanecem editáveis, mas o playtest atual os recusa, pois o snapshot/save V2 ainda não representa suas posições. Não foi promovida uma expansão incompatível.

TASK-143: minimapa de descritor autorado e culling do renderer por CanvasIndex. Câmera existente usa posição lógica e preserva os contratos da arena/Coop.

TASK-144: validação do corpo no trajeto do Shunpo ofensivo, feedback de recarga e vigor. Testes de diagonal, drift, parede fina e cooldown persistido. Nenhum valor de velocidade, alcance, raio ou desbloqueio foi alterado.

TASK-145: primeira gravação local mediana de aproximadamente 8,23 s para 243 ms em três processos, removendo inicialização de temporário criptográfico do caminho de DocumentStore. Backup, validação e movimento atômico preservados. Metodologia, variações e limitações em CORUJAO-IV-OTIMIZACOES.md.

TASK-146: diagnóstico opt-in e checkpoint/retomada em memória de sessão sintética de quests. Personagens normais não podem usar esse checkpoint; runtime recusa persistência sintética em LocalSaveStore.

Regressão final: 446 executados, 438 aprovados, oito abortados headless e zero falhas/ignorados. Studio gráfico: 56 aprovados. Evidências individuais nos checkpoints. Ampliação de mapas além dos limites do save não está concluída; o trabalho compatível foi mantido separado dessa pendência.
