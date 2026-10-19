# TASK-065 - Hotbar e controles

Barra clicavel usa IDs rpg.attack, rpg.ability e item.potion, mesmas operacoes do dominio. Atalhos editaveis por acao, persistidos separadamente em controls.properties com escrita temporaria/atomica. Rebinding aplicado na sessao; WASD, F4 e combinacoes de sistema reservados. Duplicatas recusadas.

Entradas press/release impedem repeticao do SO; perder foco limpa teclas mantidas. Arena conserva Q/Shift e demais controles originais. Testes de conflito, persistencia, equivalencia hotbar/dominio e repeticao/foco. Textos gerais de ajuda mostram padrao; dialogo Atalhos informa configuracao efetiva.
