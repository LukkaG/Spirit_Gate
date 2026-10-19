# TASK-016 - Candidato consolidado e verificacao final

18/10/2026. AUTOMATIZADA APROVADA. HOMOLOGACAO HUMANA PENDENTE.

Baseline exato: mvn.cmd -Dmaven.repo.local=.maven-repository -f server/pom.xml clean test, 216 testes, zero falhas/erros/ignorados. Revisadas TASK-012 a TASK-015: diffs, PDFs, suites, replays e capturas. Nenhum asset reprovado ativado. Risco previsto: empacotamento, ferramentas e kit de teste; sem alteracao de gameplay nesta etapa.

Build agora exige auditoria oficial do ZIP original antes de anunciar sucesso. Parametro MavenRepository permite cache local gravavel sem alterar configuracao global. Kit permite mapa-demo.properties opcional com nome explicito, mantendo kits historicos de tres arquivos. Smoke admite argumentos para testar a variante local. Revisao reforcou nomes Windows reservados (incluindo dispositivos especiais e numeros sobrescritos), symlink de asset e validacao de pivot/offset. Novos testes de exportacao de 59 frames, pivot invalido e CRC adulterado.

Suite final do proprio build: 216 JUnit, zero falhas/erros/ignorados. Repeticao offline: mesmos 216 verdes. Python: 12 testes verdes. Inventario final repetido byte a byte: 228 PNGs reprovados para grade pretendida, zero validados; sem atlas de producao ou personagem aprovado. Arte-fonte preservada e fallback ativo. Manifest Properties de personagens e atlas foram exercitados apenas com fixtures sinteticas temporarias.

Novo ZIP: dist/SpiritGate-0.16.0-rc1-20261018T230252Z-a6150a66-portable.zip
Tamanho: 29.386.343 bytes.
SHA-256: 9bc425107d23ccfd9a4d36279be4da23c7f016a74c92fe03a9bb1f0d36eb79c1

Auditoria original PASS nos seis controles: SHA externo, estrutura segura (150 arquivos), manifest/inventario, runtime/PE, whitelist/JAR e bytecode (138 classes comparadas) e busca de padroes concretos de credenciais. Nenhuma normalizacao intermediaria. Controle negativo: copia descartavel com JAR adulterado e SHA externo recalculado recusada por tamanho do manifest; negativeControl=PASS. Original preservado.

Dois smokes Windows reais fora de Projeto-K: padrao e --map SpiritGate/mapa-demo.properties. PATH sem Java/Maven; jvm.dll carregada do runtime integrado comprovada; launcher e janela encerraram com codigo 0, sem fechamento forcado. Capturas TASK-016-exe.png e TASK-016-mapa-exe.png. Mapa tem aviso de tiles sinteticos. Testes de sockets por CIM podem estar indisponiveis no ambiente; o JSON registra o limite. Nao houve teste independente em outro computador.

Falha operacional intermediaria: invocacao direta dos scripts encontrou ExecutionPolicy restrita. Reexecutado em powershell -NoProfile -ExecutionPolicy Bypass somente no processo, sem mudar politica global, pelo script reproduzivel TASK-016-reproduzir-smoke.ps1. A verificacao de Linux/WSL retornou ajuda em vez de ambiente utilizavel; nao foi executada auditoria Linux nesta sessao. Evidencia bruta em TASK-016-linux-disponibilidade.log.

Reconstruir na raiz Projeto-K: powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-windows-portable.ps1 -Version 0.16.0-rc1 -MavenRepository .maven-repository -TesterKitDirectory docs/kit-homologacao-012-016. O script usa JDK 17, Maven e Python 3 da maquina de build; o jogo portatil nao exige essas instalacoes. Nome unico com timestamp e identificador evita sobrescrita. Auditar: python tools/auditar_rc.py --zip ARQUIVO --report RELATORIO. Smoke: powershell -NoProfile -ExecutionPolicy Bypass -File docs/TASK-016-reproduzir-smoke.ps1 -ZipPath ARQUIVO.

## O QUE FOI REALMENTE EXECUTADO

Revisao local, suites normal/offline, 12 testes de ferramentas, inventario deterministico, rebuild atual, auditoria original, controle negativo e dois smokes EXE com captura/fechamento real. Kit de teste, relatorios/PDFs/diffs/checkpoints e hashes produzidos. Nenhum push, publicacao, servico, servidor remoto, banco, protocolo ou credencial alterado.

## O QUE AINDA EXIGE O USUARIO

Homologacao humana de jogabilidade, teste em outro Windows x64 sem Java/Maven e aprovacao/exportacao da arte. Executavel sem assinatura e sem multiplayer. Pouso com rota totalmente ocupada pode permanecer aguardando saida, comportamento historico preservado. Hashes nao sao assinatura nem prova absoluta de ausencia de segredos. Rodada encerrada na TASK-016.
