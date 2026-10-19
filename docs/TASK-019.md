# TASK-019 - Integração controlada de pacote visual

18/10/2026. 219 JUnit e 18 Python verdes. Logs TASK-019-java.log e TASK-019-python.log.

ValidatedSpriteAtlas estendido sem duplicar loader. Legado mantém fallback por frame; pacote v1 exige 59 frames, ID estável, tileSize 32, frameSize 64, origem da revisão, SHA aprovado igual ao arquivo, pivots e offsets válidos. Caminhos externos e links diretos recusados. Metadados malformados viram IOException controlada. loadOptional retorna fallback para ausência/corrupção. Imagem e recortes carregados uma vez, render sem IO.

Launcher procura visual/character.properties junto ao EXE por jpackage.app-path; em desenvolvimento, relativo ao diretório corrente. Pacote ausente mantém geometria.

tools/pacote_visual.py reutiliza inspect TASK-013. JSON de entrada: packageVersion=1, assetId estável e um único assets[] de kind character, com review.origin e approvedSha256. Saída: character.png, character.properties e review.json. Build admite VisualManifest e VisualRoot explícitos. Auditor revalida pacote e igualdade da exportação. Sem esses argumentos, release usa fallback e não inclui arte.

Procedimento futuro: autoria e revisão TASK-018; aprovação humana expressa do SHA; registrar review.origin e approvedSha256; executar pacote_visual.py RAIZ MANIFEST NOVA_PASTA; build com -VisualManifest MANIFEST -VisualRoot RAIZ. Edição exige nova aprovação. Fixtures não são arte de produção.

Testes Java: pacote completo, carga única após remover fonte, fallback de hash/revisão/frame/dimensão/arquivo/pivot/versão/origem inválidos e caminho do launcher. Python: staging com whitelist, ZIP sintético, exclusão de PNG pendente e recusa de pacotes inválidos. Fixtures exclusivamente temporárias. Gameplay e fontes artísticas preservados.