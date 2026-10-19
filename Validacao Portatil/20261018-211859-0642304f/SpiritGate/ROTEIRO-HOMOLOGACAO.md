# Homologação humana - 0.21.0-rc1

PENDENTE. Testes locais automatizados não equivalem a jogar ou testar outro PC.

1. Conferir SHA externo e extrair todo o ZIP em pasta com espaços. Em outro Windows x64 sem Java/Maven, abrir SpiritGate/SpiritGate.exe. Preservar app/ e runtime/. EXE sem assinatura; não desativar proteções.
2. WASD, Espaço, E (20 vigor), Q (55) e Shift (35): conferir custos, cooldowns e feedback, mira por mouse em quatro direções e diagonais. H: hitboxes. R: reinício. Sem nova mecânica de corrida.
3. Colisões: andar bloqueia leves; dash atravessa leves/atores, bloqueia paredes e bordas, pousa em posição livre. Testar congelamento, morte e reinício. Rota de retorno inteiramente ocupada pode aguardar saída, limitação histórica.
4. Redimensionar a janela para maior e menor; conferir proporção, barras laterais, mouse sobre arena, hitboxes, efeitos e HUD. HUD e margens não são alvos. A resolução padrão mantém arena 960x640. Redução muito pequena diminui legibilidade.
5. Variante de mapa: em PowerShell na pasta SpiritGate, executar .\SpiritGate.exe --map mapa-demo.properties. Conferir aviso de tiles sintéticos e R retornando ao mesmo mapa. Reabrir sem argumentos retorna ao padrão. Schema continua 960x640.
6. Fallback geométrico é intencional. Nenhum dos 228 PNGs foi aprovado. Pacotes futuros precisam revisão humana por SHA e origem documentada; nenhuma aprovação por nome do arquivo.

Câmera/zoom 1 a 4 disponíveis como API de integração em DebugPanel.setCameraZoom; padrão 1, sem novo atalho. Mapas maiores exercitados somente por transformação sintética, sem habilitar schema de produção.

Registrar resultado com MODELO-RELATO-BUG.md, versão e hash. Aprovação visual, jogabilidade e outro Windows continuam dependentes de relato humano. Não publicar automaticamente.
