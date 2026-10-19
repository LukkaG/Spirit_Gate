# TASK-018 - Autoria e revisão de sprites

18/10/2026. 216 JUnit e 16 Python verdes; logs TASK-018-java.log e TASK-018-python.log.

Criada tools/revisar_sprites.py reutilizando contrato e inspetor TASK-013. Template transparente vazio 512x512: 59 posições 64x64 e cinco células não usadas. Índice JSON: ação, direção, frame, duração 100ms, pivot 32,48. Arquivos reproduzíveis em docs/autoria-018. Template vazio é intencionalmente reprovado como personagem; não é arte de produção.

CLI review produz HTML com grade, fundo quadriculado, índices, pivots, tamanho, alfa, bordas e SHA por arquivo. Reutiliza inspect e acrescenta ordem/direção/pivot/duração. Saídas fora de assets, sem alterar fontes. Relatório e preview dos 228 PNGs em docs/revisao-fontes-018: todos continuam reprovados. Nenhum atlas de produção.

Aseprite: nomear ACTION.DIRECTION.INDEX; direções FRONT/BACK/LEFT/RIGHT. Exportar json-array e PNG, duração 100ms, sourceSize e spriteSourceSize 64x64; sem trim/rotate/pack. CLI aseprite PNG JSON NOVO_MANIFEST rejeita incompatibilidade. Orientação visual exige revisão humana.

Comandos: python tools/revisar_sprites.py template NOVA_PASTA; python tools/revisar_sprites.py review RAIZ MANIFEST NOVA_PASTA; python tools/revisar_sprites.py aseprite PNG JSON NOVO_MANIFEST. Após aprovação humana expressa, registrar SHA exato em review.approvedSha256 e executar validar_assets.py. Edição exige nova revisão.

Testes novos: template vazio/59/reprodução, direção invertida, ordem, ausência, pivot, sobreposição, alfa parcial, hash alterado, importação Aseprite válida e rejeição de trim/ordem. Fixtures sintéticas temporárias. Nenhuma aprovação de arte existente.