# TASK-012 - ZIP canonico e auditoria multiplataforma

18/10/2026. APROVADA na verificacao automatizada local; homologacao humana pendente.

Baseline: mvn clean test, 202 testes, zero falhas/erros/ignorados. A primeira tentativa falhou ao acessar C:/.m2; copia do cache existente em .maven-repository resolveu sem alterar dependencias. Comando real: mvn.cmd -Dmaven.repo.local=.maven-repository -f server/pom.xml clean test. Logs inicial, repeticao e final preservados.

Arquivos afetados: build-windows-portable.ps1, auditar_rc.py, test-windows-portable.ps1 e novos controles Python. Risco: formato do ZIP e validacao de extracao; nenhuma mecanica alterada.

Origem corrigida: Compress-Archive gravava separadores do host. ZipArchive agora recebe arcname POSIX explicito para cada arquivo. Auditor verifica orig_filename antes de normalizar; smoke Windows consulta FullName original e recusa nomes inseguros, duplicatas e symlinks. Manifest, inventarios, PE/JVM/JAR e whitelist preservados.

Suite final: 202 testes, zero falhas/erros/ignorados. Python: 5 testes aprovados (nomes invalidos, NUL original, duplicatas, traversal, nomes reservados e symlink). Falhas intermediarias preservadas: escritor Python normalizava dados de teste, corrigido com nome cru; primeiro build exigiu carregar assembly System.IO.Compression explicitamente. Pacote vazio dessa tentativa nao e distribuivel; nao foi sobrescrito nem recebeu sidecars.

Auditoria oficial PASS diretamente no ZIP original, sem copia normalizada. Controle negativo em copia adulterada recusado. Smoke EXE real fora de Projeto-K, PATH sem Java/Maven, JVM integrada conferida e fechamento normal; captura TASK-012-exe.png. Linux nao disponivel para execucao nesta sessao. Hash historico 0.11.0-rc1 preservado: 7484f90f5de5ca0475c8d73a918a8868b20a089d5a3fb3a12dd2e40f5fc19d01.

Novo ZIP: C:\Users\lucas\OneDrive\Desktop\Spirit_Gate_Prototipo_Jogavel\Projeto-K\dist\SpiritGate-0.12.0-task012-20261018T225125Z-3e5bf0d7-portable.zip

SHA-256: befe84b9d22eb19b7e2b8b2986ccd028da881a34fbee71af3e7d64a16caa6b63

Evidencias: TASK-012-auditoria.json, TASK-012-auditoria-negativa.json, TASK-012-portable-smoke.json, logs, PDF e diff. Checkpoint local em .task-checkpoints; checkout recebido sem Git. Reproducao: build-windows-portable.ps1 -Version 0.12.0-task012 -SkipMaven apos suite limpa; auditar_rc.py --zip ARQUIVO --report RELATORIO; test-windows-portable.ps1 -ZipPath ARQUIVO.

O QUE FOI REALMENTE EXECUTADO: suite Maven, controles Python, build, auditoria original e smoke Windows local. O QUE AINDA EXIGE O USUARIO: jogabilidade e outro Windows x64 sem Java/Maven; sem publicacao ou assinatura.
