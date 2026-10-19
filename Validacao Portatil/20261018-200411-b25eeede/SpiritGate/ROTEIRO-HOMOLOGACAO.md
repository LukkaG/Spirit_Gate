# Roteiro TASK-012 a TASK-016

HOMOLOGACAO HUMANA PENDENTE. Testes automatizados locais nao equivalem a jogar nem a testar outro PC.

1. Confira o SHA do ZIP e extraia tudo. Em outro Windows x64 sem Java/Maven, abra SpiritGate/SpiritGate.exe. Preserve app/ e runtime/. Feche normalmente e registre qualquer aviso; executavel sem assinatura.
2. TASK-012: extrair em caminho com espacos; abrir, fechar, reabrir e confirmar versao em VERSION.txt.
3. TASK-013: nenhuma arte Karakura ou personagem final esta ativa. Relatorio do projeto lista 228 PNGs e zero validados; revisar no Aseprite antes de integrar.
4. TASK-014: testar quatro direcoes, parar/retornar, atacar, receber dano, Freeze, morrer e R. Sem personagem aprovado, o fallback geometrico e intencional; fixtures magenta nao estao no pacote.
5. TASK-015: variante isolada sintetica. Abra um PowerShell na pasta SpiritGate e execute .\SpiritGate.exe --map mapa-demo.properties. Confira aviso de tiles sinteticos e R voltando ao mesmo mapa. Feche e reabra sem argumentos para voltar ao padrao. Desenho maior nao muda hitbox.
6. TASK-016/gameplay: WASD, Espaco, E (20 vigor), Q (55), Shift (35, mirar com mouse), H e R. Caminhar bloqueia nos leves; dash atravessa atores/leves, bloqueia em parede/borda e termina em destino livre. Testar gasto, cooldown, congelamento, morte e reinicio. Nao ha corrida nova.

Aprovacao visual, jogabilidade e outro computador exigem relato humano. Espera por pouso com rota inteiramente ocupada continua limitacao conhecida. Nao publicar automaticamente.
