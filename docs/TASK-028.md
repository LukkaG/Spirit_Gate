# TASK-028 - Autoria e revisão de sprites

Objetivo: facilitar revisão direcional e comparação de versões no pipeline Aseprite.
Base: TASK-027 aprovada, 230 JUnit e 24 Python.
Diagnóstico: ferramentas existentes já verificavam 59 frames, ordem/direção,
duplicação, bordas, pivots, duração, transparência, dimensões e SHA; preview anterior
era somente folha estática com grade e pivots. Preservados esses verificadores.
Implementação: revisar_sprites.py agora inclui animações CSS por estado e direção
em preview.html, usando os recortes sem redimensionar os originais. Adicionado
comando compare, saída JSON com dimensões, alfa e hashes das duas versões.
Saída exclusiva, sem sobrescrita. Relatório sempre artApproved=false.
Arquivos: revisar_sprites.py, test_sprite_review.py e evidências sintéticas.
Testes novos: presença de estados/direções, keyframes, versões iguais/diferentes e
recusa de sobrescrita; regressões prévias continuam verificando negativos Aseprite.
Resultado real: TASK-028-qualidade.json e logs completos. Reprodução:
python tools/qualidade.py --offline --task 028
python tools/revisar_sprites.py template docs/autoria-028-sintetica
python tools/revisar_sprites.py compare antes.png depois.png comparacao.json

Limites: preview é apoio visual, não aprovação da arte nem captura do jogo.
Template vazio é sintético e permanece fora do pacote de produção. Comparação
reporta metadados/hashes, sem avaliar qualidade estética. Homologação Aseprite
humana pendente. Checkpoints/diff padronizados; próxima TASK-029.

## Resultado final observado
Validação local aprovada: 230 JUnit e 25 Python; zero falhas.
