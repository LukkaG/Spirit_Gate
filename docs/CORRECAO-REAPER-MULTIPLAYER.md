# Reaper no multiplayer ? 0.200.3

O cliente online usava o atlas player-base para todas as ra?as. Agora NetworkPlayerVisuals escolhe o atlas Reaper validado por SHA-256 usando a ra?a enviada pelo servidor. A caminhada usa oito quadros a 125 ms e escala 1.0. Human conserva player-base; ra?a ainda n?o recebida ou atlas Reaper inv?lido mant?m fallback legado. Cosm?ticos do corpo antigo n?o s?o sobrepostos ? Reaper. Protocolo, saves, colis?o e regras autoritativas preservados.

Feche servidor, clientes e launcher antigos. Abra o SpiritGate-Launcher.exe do Anfitri?o em dist/SpiritGate-Launcher-Grafico-0.200.3. Envie o ZIP Convidado 0.200.3 ao amigo. O canal online continua sem publica??o; n?o basta reabrir o launcher antigo.

Na sess?o experimental atual, o servidor atribui Reaper ao primeiro participante e Human ao segundo; n?o h? sele??o de ra?a pessoal integrada a essa sess?o. Portanto o segundo personagem ainda usa o corpo Human anterior.

Valida??o: su?te multiplayer com 141 testes executados, 140 passaram, um abortado por depender de UI e nenhuma falha. Testes novos verificam atlas real por ra?a e ciclo completo de oito quadros. Dois processos clientes dos pacotes 0.200.3 conectados a servidor local real; captura inspecionada mostra Reaper nova e Human anterior. Evid?ncias: .task-checkpoints/reaper-multiplayer-0.200.3, incluindo client-a.png e packaged-smoke.json. Radmin f?sico permanece pendente.
