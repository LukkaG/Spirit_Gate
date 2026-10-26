# TASK-198 — pacotes privados por papel

VALIDADA COM RESSALVAS, condicionada aos comandos e checks reais do checkpoint. Compilação integral dos fontes de produção atuais para Java 17, jar sem testes, runtime jlink local e launchers nativos jpackage existentes. Anfitrião e Convidado contêm o mesmo jar; convidado recebe spiritgate.guest=true. Java integrado; sem instalador, feed ou publicação. ZIPs com nomes exclusivos dentro do checkpoint, dist e versões anteriores preservados.

Allowlist: classes/recursos de produção; somente assets/dev/player-base e assets/dev/generated/corujao-iv-v1; quatro documentos locais selecionados, LEIA-ME e imagem jpackage. Sem saves, studio-projects, fontes soltos, ferramentas de desenvolvimento, segredos ou pastas online-synthetic. Cinco arquivos .properties externos ao jar são permitidos por caminho exato: manifest visual e configurações/fontes do runtime. Nenhum arquivo de teste/harness 194–196 entra no jar. A classe de laboratório não é chamada pelo launcher.

Três montagens preliminares foram interrompidas pela allowlist genérica de .properties (manifest visual, logging e psfontj2d); diretórios preservados. Não eram falhas do jogo ou rejeições de integridade, nem testes afrouxados: a política final permite somente os cinco arquivos requeridos, após inventário completo da imagem. Nenhum ZIP incompleto foi considerado pacote válido. Montagem final nova e seu stdout completo preservados em package-build.log.

Verificação: SHA ZIP e todas as entradas de manifest, lista exata de arquivos/ausência de traversal/duplicatas, extração limpa por papel, dois self-checks dos executáveis nativos. Harness compilado contra o jar novo e executado com runtimes extraídos: servidor real e dois processos clientes com renderizador de produção, publicação de geometria/asset preinstalado, falha AFTER_ACTIVATION recuperada, rollback e recusa administrativa do convidado. Logs, imagens e result.json em smoke/. Nenhum teste físico Radmin ou aprovação artística inferida. Cosméticos temporários existentes mantêm a classificação original.

Inventário de ZIPs, hashes e contagens em packages.json; manifests dentro de cada ZIP. Compilação e processos sem dependências novas ou alterações de rede. O próximo ensaio 199 verifica Studio/Preview e movimento/PvE/portal pela interface real.


Evidências: `.task-checkpoints/TASK-198/corujao-vi/run-20261026T002307Z-ade93946`.

Integridade: 31629 entradas verificadas; rehash nesta etapa: 0; divergências: 0. Método e hashes em protected-before/after.json.

Fontes alterados nesta execução (before/after): nenhum.
