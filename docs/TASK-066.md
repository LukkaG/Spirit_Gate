# TASK-066 - Audio, preferencias e marco offline B

Audio JDK javax.sound.sampled com tons originais sintetizados para UI, golpe, dano, cura, nivel e ambiente. Volume/mudo e ciclo visual configuraveis, preferencias separadas dos personagens. Fila de oito eventos, executor proprio, fallback silencioso sem dispositivo; nenhum arquivo de origem/licenca desconhecida.

Integracao final: salvamento captura comandos pendentes na thread da simulacao antes do IO, pausa ao sair e mantem sessao diante de erro. Direcao, recargas e RNG deterministico por spawn/geracao preservam continuidade. Catalogos/atributos usados por tick ficam em memoria. Estado semantico de flags validado antes de sobrescrever save; limite de loot pendente impede derrota sem recompensa.

UI: atalhos efetivos no HUD/hotbar, labels legiveis, botoes de dialogo em grade, dados de missao/consumiveis/equipamento reais. Audio e iluminacao nao alteram dominio. Testes incluem dispositivo indisponivel, mudo, preferencias, fila/save, corrupcao sem sobrescrita, direcao/RNG, 20000 ticks e campanha integral para guardian e mystic. Build, auditoria e smoke do pacote registrados no marco B antes de qualquer trabalho de rede.
