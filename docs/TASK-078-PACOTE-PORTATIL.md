# Pacote portátil de teste — TASK-078

Gerado por solicitação explícita do responsável após a integração experimental, em 19/10/2026 (America/Sao_Paulo). Pacotes anteriores preservados. Sem push ou publicação externa.

## Arquivo para compartilhar

`dist/SpiritGate-RPG-Sprites-Teste-0.78.0-rc1-20261020T003645Z-593b5c5d.zip`

Tamanho: 29.832.783 bytes, aproximadamente 28,5 MiB. Plataforma: Windows 64 bits. Java integrado; não precisa instalar Java, Maven ou Python.

Enviar o ZIP inteiro. O amigo deve extrair todo o conteúdo e abrir `SpiritGateRPG/SpiritGateRPG.exe`. As sprites experimentais já vêm habilitadas. Para comparar a apresentação antiga, abrir `Abrir-Geometrico.cmd` na mesma pasta. Não executar diretamente de dentro do ZIP nem separar o EXE de `app`, `runtime` e `assets`.

Saves novos ficam em `saves/` ao lado do EXE. Nenhum save pessoal foi incluído. Trata-se de RPG offline, não de sessão Coop entre computadores. Aprovação artística e homologação humana continuam a cargo do responsável e dos testadores.

## Construção e verificação

Novo builder: `tools/build-rpg-sprites-portable.ps1`, baseado no builder RPG existente, sem modificar o script ou os pacotes antigos. Versão de teste 0.78.0-rc1, identificação única por horário UTC e sufixo aleatório. O carimbo UTC do nome está em 20/10, correspondente à noite local de 19/10.

Antes do build: `python tools/qualidade.py --offline --task 078-portatil`, exit 0, 307 testes Java e 25 Python aprovados, sem falhas/erros/ignorados. Logs próprios com prefixo `docs/TASK-078-portatil-`.

ZIP validado por `tools/auditar_rpg.py`: integridade, 223 classes de produção Java 17, recursos, hashes de cada arquivo do manifesto e runtime integrado. Verificação adicional confirmou exatamente os 12 PNGs esperados, byte a byte iguais aos originais, opção experimental no launcher e ausência de pasta de saves no arquivo. Sidecars `.manifest.json` e `.sha256` acompanham o ZIP.

SHA-256: `7ee27742d91db25779583dc7b774822f3dbafc8ad2880f6d167127f547df2024`.

## Smoke do pacote extraído

O EXE extraído foi executado em `.task-checkpoints/TASK-078-portatil-smoke/`, com PATH sem Java externo. Janela real aberta, 62 ticks, sprites experimentais visíveis na captura de componentes; stderr vazio e encerramento com código 0. A inspeção de módulos do processo não retornou caminho da JVM; não se atribui uma comprovação por DLL a essa execução. O runtime consta do pacote e o teste do modo geométrico também chama diretamente seu `runtime/bin/java.exe`.

Evidências: `exe-smoke.json`, `sprites-portatil.png.json`, `sprites-portatil.png.components.png`, `archive-check.json`, `geometry-smoke.json` e capturas na pasta de smoke. Captura de componentes foi examinada; a captura de desktop não é usada como homologação visual. Arquivo final auditado em `docs/TASK-078-portatil-auditoria.json`. O ZIP de entrega foi fechado antes dos smokes e continua sem dados desses ensaios.
