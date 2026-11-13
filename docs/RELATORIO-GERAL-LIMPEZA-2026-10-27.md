# Relatorio geral de limpeza ? 27/10/2026

Estado anterior: branch `beco`, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`.

## Entrega preservada

Spirit Gate **0.201.0**, atualizacoes pelo GitHub, commit de distribuicao
`097cdbbaafcd9f902f6ce5b9f7dee46d63eb9780` na branch `updates`.
A entrega atual permanece em `dist/SpiritGate-GitHub-0.201.0`.
O recibo de publicacao esta em `publicacao.json` nessa pasta. A entrada de
empacotamento original foi preservada em `Integracao/Fonte-Anfitriao-0.201.0.zip`.

A verificacao online anterior confirmou manifesto, dois instaladores, ativador
e checksum. Na migracao real 0.200.8 -> 0.201.0, foram baixados **3 arquivos**
e reutilizados **388**; saves, papel Convidado e Java foram preservados.
Os dois launchers passaram no teste de abertura. Esses resultados sao
historicos; nao constituem nova homologacao remota via Radmin.

## Criterios da limpeza

Remover pacotes antigos, builds recompilaveis, caches descartaveis e evidencias
brutas de execucoes passadas, conforme solicitado. Manter fontes Java/Python/
PowerShell, testes, arte editavel, assets, documentacao tecnica, mapas e saves.
O projeto web auxiliar conserva suas fontes; apenas suas saidas geradas e
dependencias reinstalaveis foram limpas. A preparacao futura da CDN permanece
em tools/deploy; a distribuicao atual continua usando somente GitHub.

O cache Maven local (~22 MiB) permanece por ser usado nos testes offline.
O auxiliar de sessao Corujao VI foi removido na etapa complementar, apos a
exclusao dos scripts historicos que o utilizavam.
As credenciais locais da Cloudflare permanecem em seu diretorio ignorado;
nenhuma credencial foi copiada para este relatorio ou para a distribuicao.

## Dados recuperados

Foram recuperados **301 arquivos**, totalizando 134.12 MiB antes da compressao,
de pastas de saves, projetos do Studio e mapas encontrados nos artefatos antigos.
Estao em `saves/recuperados-limpeza-20261027/dados-recuperados.zip`, com indice
local. Incluem dados sinteticos de testes, preservados por precaucao. Os saves
e projetos principais na raiz permanecem intactos.

## Historico consolidado

Foram localizados 100 registros de comandos de validacao. A tabela abaixo
conta execucoes, incluindo repeticoes, tentativas intermediarias e controles
negativos. Saida diferente de zero nao significa regressao atual.
Os relatorios JUnit encontrados somam 482 execucoes de testes,
0 falhas, 0 erros e 0 ignorados.
Esses totais historicos nao representam testes unicos nem o estado da suite atual.

| Grupo | Saida 0 | Outras saidas |
|---|---:|---:|
| TASK-187 | 5 | 2 |
| TASK-188 | 3 | 2 |
| TASK-189 | 5 | 0 |
| TASK-190 | 7 | 3 |
| TASK-191 | 9 | 2 |
| TASK-192 | 9 | 0 |
| TASK-193 | 2 | 0 |
| TASK-194 | 6 | 1 |
| TASK-195 | 4 | 0 |
| TASK-196 | 6 | 1 |
| TASK-197 | 3 | 0 |
| TASK-198 | 22 | 0 |
| TASK-199 | 2 | 0 |
| TASK-200 | 6 | 0 |

## Inventario dos artefatos selecionados

Volume inicial selecionado: **23.46 GiB**. O resultado efetivo sera registrado apos a remocao.

| Caminho | MiB | Arquivos |
|---|---:|---:|
| `server/target` | 5757.33 | 192264 |
| `.task-checkpoints/radmin-atualizavel` | 1994.27 | 12555 |
| `.task-checkpoints/TASK-198` | 1072.71 | 6285 |
| `.task-checkpoints/github-publication-0.201.0` | 911.65 | 1187 |
| `Validacao Portatil` | 638.90 | 2243 |
| `.task-checkpoints/TASK-176` | 562.59 | 3838 |
| `.task-checkpoints/TASK-134` | 551.83 | 11 |
| `.task-checkpoints/TASK-160` | 548.63 | 9 |
| `.task-checkpoints/launcher-oficial-0.201.0` | 428.33 | 1253 |
| `.task-checkpoints/TASK-180` | 359.59 | 3870 |
| `dist/SpiritGate-Oficial-0.201.0` | 305.31 | 1593 |
| `dist/SpiritGate-Teste-Zoom-0.200.8` | 305.21 | 1581 |
| `dist/SpiritGate-Teste-Camera-0.200.7` | 305.21 | 1581 |
| `dist/SpiritGate-Launcher-Grafico-0.200.6` | 305.20 | 1581 |
| `dist/SpiritGate-Launcher-Grafico-0.200.5` | 305.20 | 1581 |
| `.task-checkpoints/launcher-online-0.200.2` | 305.16 | 1579 |
| `.task-checkpoints/cdn-preparer-audit-0.200.8` | 262.82 | 399 |
| `.task-checkpoints/github-launcher-0.201.0` | 222.46 | 1606 |
| `.task-checkpoints/TASK-190` | 201.74 | 7336 |
| `dist/SpiritGate-Launcher-Grafico-0.200.4` | 194.01 | 792 |
| `.task-checkpoints/teste-zoom-0.200.8` | 166.31 | 824 |
| `.task-checkpoints/melhorias-multiplayer-0.200.4` | 165.91 | 829 |
| `.task-checkpoints/melhorias-multiplayer-0.200.5` | 165.90 | 823 |
| `.task-checkpoints/teste-camera-0.200.7` | 165.86 | 814 |
| `.task-checkpoints/cloudflare-tools` | 163.47 | 1635 |
| `.task-checkpoints/github-updates-publicacao` | 158.49 | 365 |
| `.task-checkpoints/TASK-145` | 150.80 | 78 |
| `.task-checkpoints/TASK-188` | 149.38 | 7223 |
| `.task-checkpoints/TASK-133` | 140.12 | 6 |
| `.task-checkpoints/TASK-194` | 139.54 | 7276 |
| `.task-checkpoints/TASK-191` | 139.37 | 7273 |
| `.task-checkpoints/TASK-187` | 139.03 | 7224 |
| `.task-checkpoints/TASK-132` | 133.98 | 6 |
| `.task-checkpoints/TASK-131` | 133.92 | 6 |
| `.task-checkpoints/TASK-130` | 133.69 | 6 |
| `.task-checkpoints/TASK-129` | 133.38 | 6 |
| `.task-checkpoints/TASK-128` | 133.19 | 6 |
| `.task-checkpoints/TASK-127` | 132.57 | 6 |
| `.task-checkpoints/TASK-178` | 127.57 | 1243 |
| `.task-checkpoints/TASK-126` | 127.15 | 6 |
| `.task-checkpoints/TASK-192` | 124.36 | 5713 |
| `.task-checkpoints/TASK-125` | 122.33 | 6 |
| `.task-checkpoints/TASK-124` | 122.31 | 6 |
| `.task-checkpoints/TASK-123` | 122.29 | 6 |
| `.task-checkpoints/TASK-122` | 122.27 | 6 |
| `.task-checkpoints/TASK-115` | 121.14 | 12 |
| `.task-checkpoints/npm-cache` | 119.99 | 258 |
| `.task-checkpoints/SpiritGate-TASK-076-checkpoint-recuperavel-r2.zip` | 118.11 | 1 |
| `.task-checkpoints/SpiritGate-TASK-076-checkpoint-recuperavel.zip` | 118.10 | 1 |
| `.task-checkpoints/TASK-121` | 117.45 | 6 |
| `.task-checkpoints/TASK-177` | 113.43 | 850 |
| `.task-checkpoints/TASK-196` | 108.44 | 4995 |
| `.task-checkpoints/TASK-120` | 107.83 | 6 |
| `.task-checkpoints/TASK-079` | 107.19 | 9 |
| `.task-checkpoints/TASK-186` | 105.71 | 5685 |
| `.task-checkpoints/TASK-119` | 102.73 | 6 |
| `.task-checkpoints/SpiritGate-TASK-037-base-recuperavel.zip` | 102.18 | 1 |
| `.task-checkpoints/TASK-118` | 102.03 | 6 |
| `.task-checkpoints/TASK-117` | 101.12 | 6 |
| `.task-checkpoints/launcher-grafico-0.200.2` | 98.50 | 565 |
| `.task-checkpoints/TASK-114` | 97.49 | 3 |
| `.task-checkpoints/TASK-113` | 96.86 | 4 |
| `.task-checkpoints/TASK-112` | 96.82 | 2 |
| `.task-checkpoints/TASK-111` | 96.80 | 2 |
| `.task-checkpoints/TASK-110` | 96.78 | 2 |
| `.task-checkpoints/TASK-109` | 96.76 | 2 |
| `.task-checkpoints/TASK-108` | 96.73 | 2 |
| `.task-checkpoints/TASK-107` | 96.71 | 2 |
| `.task-checkpoints/TASK-106` | 96.69 | 2 |
| `.task-checkpoints/TASK-105` | 96.67 | 2 |
| `.task-checkpoints/TASK-104` | 96.65 | 2 |
| `.task-checkpoints/TASK-103` | 96.63 | 2 |
| `.task-checkpoints/TASK-102` | 96.60 | 2 |
| `.task-checkpoints/TASK-101` | 96.57 | 3 |
| `.task-checkpoints/TASK-100` | 96.54 | 2 |
| `.task-checkpoints/TASK-099` | 96.51 | 2 |
| `.task-checkpoints/TASK-098` | 96.49 | 2 |
| `.task-checkpoints/TASK-097` | 96.47 | 2 |
| `.task-checkpoints/TASK-096` | 96.45 | 2 |
| `.task-checkpoints/TASK-095` | 96.43 | 2 |
| `.task-checkpoints/TASK-094` | 96.40 | 2 |
| `.task-checkpoints/TASK-093` | 96.37 | 2 |
| `.task-checkpoints/TASK-092` | 96.35 | 2 |
| `.task-checkpoints/TASK-091` | 96.32 | 2 |
| `.task-checkpoints/TASK-078` | 95.67 | 10 |
| `.task-checkpoints/TASK-200` | 66.10 | 2597 |
| `.task-checkpoints/TASK-182` | 64.11 | 3235 |
| `.task-checkpoints/reaper-multiplayer-0.200.3` | 54.25 | 15 |
| `.task-checkpoints/proporcao-sprites-0.200.6` | 54.12 | 10 |
| `.task-checkpoints/TASK-189` | 53.83 | 2429 |
| `.task-checkpoints/TASK-197` | 53.59 | 2445 |
| `.task-checkpoints/TASK-195` | 53.55 | 2441 |
| `.task-checkpoints/TASK-077` | 42.79 | 12 |
| `.task-checkpoints/github-update-live-check` | 28.79 | 2 |
| `.task-checkpoints/negativo-task036.zip` | 28.34 | 1 |
| `.task-checkpoints/negativo-task036-final.zip` | 28.34 | 1 |
| `.task-checkpoints/negativo-task021.zip` | 28.33 | 1 |
| `.task-checkpoints/negativo-task016.zip` | 28.32 | 1 |
| `.task-checkpoints/negativo-task017.zip` | 28.32 | 1 |
| `.task-checkpoints/TASK-199` | 22.94 | 239 |
| `.task-checkpoints/TASK-193` | 20.78 | 25 |
| `.task-checkpoints/TASK-193-approval` | 20.68 | 8 |
| `.task-checkpoints/TASK-179` | 16.26 | 1315 |
| `.task-checkpoints/TASK-090` | 12.97 | 15 |
| `.task-checkpoints/TASK-088` | 11.44 | 9 |
| `.task-checkpoints/TASK-089` | 11.43 | 2 |
| `.task-checkpoints/TASK-087` | 11.42 | 7 |
| `.task-checkpoints/TASK-086` | 11.37 | 2 |
| `.task-checkpoints/TASK-085` | 11.36 | 7 |
| `.task-checkpoints/TASK-084` | 11.33 | 2 |
| `.task-checkpoints/TASK-083` | 11.32 | 2 |
| `.task-checkpoints/TASK-082` | 11.30 | 2 |
| `.task-checkpoints/TASK-080` | 11.29 | 7 |
| `.task-checkpoints/TASK-081` | 11.28 | 2 |
| `.task-checkpoints/TASK-183` | 7.71 | 17 |
| `.task-checkpoints/TASK-175` | 5.89 | 44 |
| `docs.zip` | 4.50 | 1 |
| `.task-checkpoints/TASK-181` | 3.05 | 13 |
| `.task-checkpoints/TASK-157` | 2.89 | 12 |
| `.task-checkpoints/TASK-159` | 2.78 | 54 |
| `.task-checkpoints/TASK-156` | 2.72 | 9 |
| `.task-checkpoints/TASK-158` | 2.16 | 10 |
| `.task-checkpoints/TASK-174` | 1.87 | 11 |
| `.task-checkpoints/TASK-173` | 1.86 | 8 |
| `.task-checkpoints/TASK-172` | 1.86 | 8 |
| `.task-checkpoints/TASK-171` | 1.86 | 8 |
| `.task-checkpoints/TASK-170` | 1.86 | 8 |
| `.task-checkpoints/TASK-169` | 1.86 | 8 |
| `.task-checkpoints/TASK-168` | 1.86 | 8 |
| `.task-checkpoints/TASK-167` | 1.86 | 8 |
| `.task-checkpoints/TASK-166` | 1.86 | 8 |
| `.task-checkpoints/TASK-165` | 1.86 | 8 |
| `.task-checkpoints/TASK-164` | 1.86 | 8 |
| `.task-checkpoints/TASK-163` | 1.86 | 8 |
| `.task-checkpoints/TASK-162` | 1.86 | 8 |
| `.task-checkpoints/TASK-161` | 1.86 | 8 |
| `.task-checkpoints/TASK-153` | 1.77 | 9 |
| `.task-checkpoints/TASK-149` | 1.67 | 21 |
| `.task-checkpoints/TASK-141` | 1.62 | 14 |
| `.task-checkpoints/TASK-148` | 1.58 | 13 |
| `.task-checkpoints/TASK-155` | 1.58 | 6 |
| `.task-checkpoints/TASK-147` | 1.58 | 11 |
| `.task-checkpoints/TASK-154` | 1.56 | 6 |
| `.task-checkpoints/TASK-152` | 1.51 | 6 |
| `.task-checkpoints/TASK-151` | 1.51 | 6 |
| `.task-checkpoints/TASK-150` | 1.48 | 6 |
| `.task-checkpoints/TASK-139` | 1.46 | 11 |
| `.task-checkpoints/TASK-146` | 1.44 | 7 |
| `.task-checkpoints/TASK-135` | 1.41 | 11 |
| `.task-checkpoints/TASK-144` | 1.41 | 7 |
| `.task-checkpoints/TASK-143` | 1.40 | 7 |
| `.task-checkpoints/TASK-142` | 1.38 | 7 |
| `.task-checkpoints/TASK-137` | 1.35 | 9 |
| `.task-checkpoints/TASK-140` | 1.35 | 7 |
| `.task-checkpoints/TASK-136` | 1.35 | 9 |
| `.task-checkpoints/TASK-138` | 1.31 | 7 |
| `.task-checkpoints/TASK-116` | 0.27 | 20 |
| `.task-checkpoints/pixel-scale-probe` | 0.17 | 11 |
| `.task-checkpoints/pixel-camera-test` | 0.06 | 17 |
| `.task-checkpoints/ativador-cdn-review-sem-canal` | 0.05 | 13 |
| `.task-checkpoints/fractional-camera-tests` | 0.05 | 8 |
| `.task-checkpoints/fractional-scale-probe` | 0.05 | 6 |
| `.task-checkpoints/pixel-camera-tests` | 0.05 | 8 |
| `.task-checkpoints/TASK-185` | 0.03 | 11 |
| `.task-checkpoints/urban-login` | 0.03 | 4 |
| `.task-checkpoints/urban-door-v3` | 0.02 | 4 |
| `.task-checkpoints/TASK-184` | 0.02 | 7 |
| `.task-checkpoints/cartoon-backgrounds` | 0.01 | 3 |
| `.task-checkpoints/cdn-updater-tests` | 0.01 | 50 |
| `.task-checkpoints/reaper-scale-1.5` | 0.01 | 5 |
| `.task-checkpoints/github-updater-tests` | 0.00 | 18 |
| `.task-checkpoints/auto-update-tests-final` | 0.00 | 2 |
| `.task-checkpoints/auto-update-tests-3` | 0.00 | 2 |
| `.task-checkpoints/updater-real-smoke` | 0.00 | 3 |
| `.task-checkpoints/task186-correction-current.txt` | 0.00 | 1 |
| `.task-checkpoints/task186-current.txt` | 0.00 | 1 |
| `.task-checkpoints/task182-current.txt` | 0.00 | 1 |
| `.task-checkpoints/task183-current.txt` | 0.00 | 1 |
| `.task-checkpoints/task184-current.txt` | 0.00 | 1 |
| `.task-checkpoints/task185-current.txt` | 0.00 | 1 |
| `.task-checkpoints/corujao-vi-current-session.txt` | 0.00 | 1 |
| `.task-checkpoints/TASK-078-portatil-smoke` | 0.00 | 3 |

## Validacao apos a limpeza

Limpeza concluida, com **25.13 GB (23.41 GiB) de reducao**
no volume dos arquivos do workspace, excluindo o diretorio raiz `.git`.
Foram processados 1021 alvos, sem falhas de remocao.

- 2654 arquivos de fonte, assets e documentacao preservados com SHA-256 conferido; zero alteracoes inesperadas.
- Compilacao local: 239 fontes Java, sucesso.
- Compilacao Maven offline: sucesso. Recriou somente as classes atuais necessarias para as ferramentas de mapas.
- Suite Python completa: **94 testes passaram**.
- Integridade dos canais e ZIPs Anfitriao/Convidado: aprovada; ambos apontam para GitHub.
- ZIP fonte de empacotamento e backup de dados recuperados: integridade aprovada.
- `git diff --check`: sem erros de espacos; avisos de conversao LF/CRLF preexistentes.

`server/target` conserva apenas a compilacao atual recriada para validacao.
`dist` conserva somente a entrega GitHub 0.201.0. Os fontes do prototipo web,
os projetos editaveis e a preparacao futura da CDN permanecem disponiveis.
As referencias dos guias atuais foram ajustadas para o recibo preservado e
este relatorio geral. Referencias a capturas e logs em documentos antigos
sao historicas; os artefatos brutos correspondentes foram descartados.

Os detalhes locais da execucao estao em `.task-checkpoints/limpeza-20261027`.
Nao houve publicacao de uma nova versao do jogo durante esta limpeza.

## ZIP redundante adicional

Um `server.zip` de 3.172.454.959 bytes foi identificado apos o inventario
inicial. Seus 463 arquivos de fonte/pom foram comparados com os arquivos
atuais: nenhum ausente ou diferente. As outras 26.184 entradas eram builds
sob `server/target`. O arquivo foi removido e incluido no `.gitignore`.
A reducao acima usa o inventario inicial como referencia; este arquivo
adicional nao fazia parte daquela medicao inicial.

## Etapa complementar: ferramentas de testes antigas

Conforme a escolha de manter testes automaticos reutilizaveis, foram removidos
**86 scripts e probes antigos**, especificos de tarefas, geracoes de pacotes
e capturas avulsas. Nenhum arquivo retirado era importado ou referenciado
pelas ferramentas de codigo mantidas. O auxiliar da antiga sessao Corujao VI
tambem foi removido, pois seus consumidores faziam parte desse conjunto.

Foram preservados todos os testes em `server/src/test`, os 15 modulos
`tools/test_*.py`, as verificacoes do atualizador, a compilacao, o fluxo GitHub,
a preparacao futura da CDN e os geradores reutilizaveis de conteudo.
O gerador de cosmeticos e o benchmark do Studio foram mantidos por serem
ferramentas de trabalho. `gerar_pdf_task002.py` permanece por ser biblioteca
compartilhada de documentacao.

Validacao: **94 testes Python passaram** novamente; 529 arquivos de codigo,
testes e ferramentas mantidas conferidos por SHA-256, sem divergencias.
`git diff --check` passou. A funcao das ferramentas restantes esta em
[tools/README.md](../tools/README.md).

Arquivos removidos nesta etapa:

- `tools/auditar_corujao.py`
- `tools/build-corujao-v.py`
- `tools/build-corujao-vi-private.py`
- `tools/build-task176.py`
- `tools/build-task177.py`
- `tools/build-task178.py`
- `tools/build-task180.py`
- `tools/CameraPixelClientProbe.java`
- `tools/consolidar_expansao.py`
- `tools/consolidar_rodada.py`
- `tools/corujao-vi-approval193.py`
- `tools/corujao-vi-close200.py`
- `tools/corujao-vi-final-audit.py`
- `tools/corujao-vi-task-evidence.py`
- `tools/corujao3_consolidate.py`
- `tools/corujao3_package_audit.py`
- `tools/corujao3_package_smoke.py`
- `tools/corujao3_stage.py`
- `tools/corujao3_verify_ui.py`
- `tools/corujao4_checkpoint.py`
- `tools/corujao4_package.py`
- `tools/corujao4_report.py`
- `tools/corujao4_validate.py`
- `tools/corujao5_checkpoint.py`
- `tools/CorujaoFiveDemo.java`
- `tools/CorujaoFourDemo.java`
- `tools/corujao_checkpoint.py`
- `tools/corujao_relatorio.py`
- `tools/CosmeticPreviewProbe.java`
- `tools/finalize-task176.py`
- `tools/FractionalPixelScaleProbe.java`
- `tools/gerar_pdf_alteracoes_codigo.py`
- `tools/gerar_pdf_guia_fixtures_dash.py`
- `tools/gerar_pdf_task003.py`
- `tools/gerar_pdf_task004.py`
- `tools/gerar_pdf_task005.py`
- `tools/gerar_pdf_task006.py`
- `tools/gerar_pdf_task006_1.py`
- `tools/PixelScaleProbe.java`
- `tools/QuestDemoProbe.java`
- `tools/registrar_integracao_radmin.py`
- `tools/RpgNetworkProbe.java`
- `tools/run-corujao-vi-integrated.py`
- `tools/smoke-corujao-vi-private.py`
- `tools/smoke-expansao.ps1`
- `tools/smoke-task176.py`
- `tools/smoke-task177.py`
- `tools/smoke-task178.py`
- `tools/smoke-task180.py`
- `tools/smoke_atualizavel_radmin.py`
- `tools/smoke_corujao5_package.py`
- `tools/SpiritGateCorujao3Probe.java`
- `tools/SpiritGateCreationProbe.java`
- `tools/SpiritGateFinalJourneyProbe.java`
- `tools/SpiritGateOptionsProbe.java`
- `tools/SpiritGatePerformanceProbe.java`
- `tools/SpiritGateSelectionProbe.java`
- `tools/SpiritGateSelectionVisualProbe.java`
- `tools/SpiritGateTradeProbe.java`
- `tools/SpiritGateWindowsProbe.java`
- `tools/StudioCapture.java`
- `tools/StudioSaveProbe.java`
- `tools/StudioTempProbe.java`
- `tools/Task176PackageClientProbe.java`
- `tools/task177-checkpoint.py`
- `tools/task177-finalize.py`
- `tools/task177-verify.py`
- `tools/Task177NetworkProbe.java`
- `tools/Task177PresentationProbe.java`
- `tools/Task177RenderProbe.java`
- `tools/Task177SpriteProbe.java`
- `tools/task178-finalize.py`
- `tools/task178-verify.py`
- `tools/Task178CollisionProbe.java`
- `tools/Task178NetworkSceneProbe.java`
- `tools/task179-finalize.py`
- `tools/task179-verify.py`
- `tools/Task179LiveProbe.java`
- `tools/task180-finalize.py`
- `tools/task180-verify.py`
- `tools/Task180PackagedProbe.java`
- `tools/Task180PublishingProbe.java`
- `tools/Task190SoakProbe.java`
- `tools/Task193RepositoryAuditProbe.java`
- `tools/Task199PublishingProbe.java`
- `tools/WorldStudioUiProbe.java`
