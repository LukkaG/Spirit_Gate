# Sprites cosméticas temporárias

Recursos gerados por código Java2D, conforme autorização do anexo. Não são arte definitiva nem substituem o corpo original. Gerador: `tools/GenerateTemporaryCosmetics.java`.

Foram inventariadas 20 poses reais em `experimental-player.properties`: Idle e quatro frames Walk por direção. As dimensões variam entre os PNGs; não foi presumido um canvas 64×64. Cada camada conserva dimensões e pivô exatos da pose correspondente.

Catálogo: cabelo curto, médio e espetado; túnica básica, jaqueta e armadura leve. São 180 PNGs em `assets/dev/generated/corujao-iv-v1/`: cabelos possuem planos traseiro/frontal separados, roupas possuem uma camada. Transparência binária real, sem blur ou antialiasing. Nenhuma roupa concede atributos.

Manifesto `manifest.properties`: versão, marcador TEMPORARIO, ID, tipo/nome, pose, camada, caminho relativo, SHA-256, dimensões e pivô. O gerador só cria diretório novo, valida os hashes dos corpos e nunca sobrescreve os originais. Alterações futuras de arte devem criar revisão nova ou ser autorizadas expressamente.

Os placeholders são deliberadamente simples. Aprovação artística, alinhamento físico em monitor e substituição por trabalho no Aseprite permanecem com o responsável. A integração deve desenhar cabelo traseiro, corpo, roupa e cabelo frontal, sempre pelo mesmo pivô, sem deformar o corpo ou alterar hitbox.
