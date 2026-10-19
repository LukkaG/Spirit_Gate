# TASK-013 - Pipeline de validacao de assets

Baseline limpo: 202 testes, zero falhas/erros/ignorados. Arquivos previstos: ferramentas Python, manifest de assets e documentacao. Risco restrito a ferramentas offline; gameplay preservado.

Implementado leitor PNG RGB/RGBA 8 bits sem dependencias externas, com assinatura, CRC, filtros e limites. Inventario deterministico inclui dimensoes, SHA-256, grade, alfa, origem, camada, pivot, offset e recortes. Formatos PNG nao suportados ficam pendentes. Coordenadas invalidas, recortes sobrepostos, bordas ocupadas, frame vazio, alfa parcial e fundo opaco em personagens sao reportados. Inspecao de pixels nao comprova qualidade artistica nem detecta todas as linhas internas: revisao humana obrigatoria vinculada ao hash.

Contrato formal: 64x64; IDLE 1x4, WALK 4x4, RUN 4x4, PUNCH 3x4, HIT 2x4 e KO 3 FRONT, total 59. Nenhum personagem aprovado presente. Exportacao de manifest Java Properties e atlas ocorre exclusivamente para assets VALIDADO; nenhum atlas de producao foi gerado nesta rodada.

Inventario real: 228 PNGs, {'REPROVADO': 228}. Fontes PNG/Aseprite e recortes anteriores preservados. Nenhuma imagem foi gerada ou importada da conversa.

Cinco testes Python aprovados: PNG valido, determinismo, dimensoes invalidas, 59 frames/frame ausente, fundo irregular/opaco, sobreposicao e aprovacao por hash. Fixtures temporarias sinteticas excluidas da producao. Suite Maven final: consultar TASK-013-final.log e TASK-013-testes.txt. Nenhum Java ou teste historico editado.

Reproducao: python -m unittest discover -s tools -p test_assets.py -v; python tools/validar_assets.py; mvn.cmd -Dmaven.repo.local=.maven-repository -f server/pom.xml clean test. Manifest: assets/manifest-assets.json; integridade: TASK-013-assets.json. Repeticao comparada byte a byte em TASK-013-determinismo.json.

## Checklist Aseprite

- Trabalhar em copia, preservar fontes e conferir grade 32x32/64x64.
- Conferir todos os 59 frames, quatro direcoes, KO frontal e indices contiguos.
- Remover fundo, revisar pixels semitransparentes, linhas de corte e bordas ocupadas.
- Conferir alinhamento, escala e pivot constante em todos os frames.
- Conferir continuidade de tiles, paleta e leitura artistica manualmente.
- Registrar review.approvedSha256 somente apos aprovacao humana; qualquer edicao invalida o hash.
- Reexecutar pipeline e usar somente exportacoes VALIDADO no renderer.

O QUE FOI REALMENTE EXECUTADO: inventario offline, testes sinteticos e suite. O QUE AINDA EXIGE O USUARIO: revisar/exportar arte no Aseprite e aprovar visualmente. Ausencia de personagem aprovado mantem fallback geometrico.
