# TASK-039 - Personagem local

Identidade imutavel com UUID, nome normalizado NFC (3-24 caracteres), arquetipo, aparencia referenciada, regiao inicial e atributos base. Construtor permite restauracao deterministica sem UI; fabrica cria UUID local. Nao existe login.

RpgLauncher apresenta criacao com validacao; cancelar encerra sem criar personagem. Snapshot exibe nome escolhido. Referencias de aparencia sao placeholders aprovados por codigo, nao sprites externos. Guardian com 140 HP/18 ataque e mystic com 100 HP/24 ataque; habilidades diferenciadas previstas na TASK-048.

Testes de nomes invalidos, normalizacao, identidade restaurada e suite completa. Persistencia em arquivo sera implementada na TASK-041.
