# Envio ao Git e teste portátil — 19/11/2026

Pedido: enviar todas as alterações locais e disponibilizar o teste de skills para outros computadores. Branch `beco`, remoto `origin` em `LukkaG/Spirit_Gate`. Antes do trabalho, HEAD e `origin/beco` estavam em `c3d484e`, sem divergência.

## Escopo do envio

Inclui as alterações locais já existentes de servidor/combate, os testes ALPHA-002 e de prazo de handshake, documentos e ferramentas locais versionáveis de auditoria/ORG-03, além da integração das sprites REA e suas evidências. A seleção segue o pedido de enviar todas as mudanças; não se descartou trabalho anterior.

Dados ignorados pelo Git — saves, checkpoints, caches, classes, logs locais e saídas temporárias — permanecem locais. A verificação dos arquivos pendentes não encontrou candidatos nos padrões examinados de chaves privadas ou tokens de acesso. Isso não representa uma auditoria integral de segredos.

`git diff --cached --check` apontou espaços em branco e linhas vazias finais em documentos, testes e evidências que já estavam locais. Os arquivos `.diff` de evidência foram preservados como recebidos; não foi feita uma limpeza de formatação em massa durante este envio.

## Pacote portátil

O empacotador `tools/build-skills-portable.ps1` foi ajustado para incluir o manifesto `rea-v1` e suas imagens. Sem esse ajuste, o pacote omitiria as sprites novas.

Artefato: `SpiritGate-Skills-Windows64-20261119T030548Z-91f83ee2.zip`, em `testes-portateis/skills-windows-x64/`. Tamanho: **57.235.024 bytes**, aproximadamente **54,58 MiB**. SHA-256:

```text
eac64ac0717e0d456d4dc393e5014533c3003980e496bbe692baab8ea09a2bd9
```

Foi compilado a partir do checkout completo antes do commit; o manifesto registra `sourceIncludesUncommittedChanges=true`, HEAD de origem e fingerprint da compilação. O pacote anterior foi preservado.

Validações executadas:

1. Build com `powershell -NoProfile -ExecutionPolicy Bypass -File tools/build-skills-portable.ps1`.
2. Integridade do ZIP, extração em diretório novo e verificação de todos os **667 arquivos** contra o manifesto, com tamanho e SHA-256.
3. **264 PNGs de skills**, incluindo os 219 PNGs REA e os assets anteriores efetivos. As revisões REA incluem 40 habilidades e 5.766 frames conferidos na origem.
4. `Verificar-Pacote.cmd` da extração, com o runtime Java embutido: `SkillsLocalPreviewProbe` e `SkillsLiveSwitchProbe` aprovados, resultado **SKILLS_PACKAGE_PASS**. Oito kits e troca entre dez domínios, com e sem auxiliar.
5. Carregamento adicional pelo próprio `NetworkSkillPresentation.load`, com o Java/JAR extraídos: **PORTABLE_REA_ASSETS_PASS skills=40 phases=219 frames=5766**. Isso confirma que os novos recortes, e não apenas o fallback, carregam no pacote.

Os resultados estão em [VALIDACAO-20261119.json](../testes-portateis/skills-windows-x64/VALIDACAO-20261119.json). Não foi instalado Java nem alterada a configuração de outro computador. O teste físico em outra máquina e o Radmin continuam pendentes.

## Suíte geral: resultado e pendências

Comando executado:

```powershell
mvn -o -f server/pom.xml '-Djava.awt.headless=true' test
```

Resultado em 284 classes: **1.290 casos reportados; 1.214 aprovados, 16 falhas, 3 erros e 57 ignorados**. O Maven terminou com código 1. A suíte geral **não está verde**. Os 57 ignorados incluem testes que exigem ambiente gráfico; os probes do ZIP foram executados separadamente com janelas reais.

As falhas se concentram nas expectativas históricas de intangibilidade, passagem de aliado pelo corpo invisível, espera por apoio livre, read models e texto da quinta técnica de raio. Exemplos: `Task225R1MaterializationTest` ainda exige `skillIntangible()==true`; `LightningFieldServerTest` espera que o aliado atravesse o caster. O checkout já continha o novo contrato ALPHA-002: corpo fixo com colisão normal, imunidade finita e término sem aguardar espaço livre. Essa discrepância exige conciliação dos testes/read models com o contrato aprovado; não foi feita uma mudança de gameplay para forçar a suíte a passar durante um pedido de envio.

Classes com falha/erro, com os casos individuais registrados no JSON de validação:

- `LightningFieldServerTest`.
- `OwnerCharacterCaptureTest`.
- `SkillTooltipTest`.
- `Task225R1PhaseObserversTest`.
- `Task225R2ShutdownTcpLabTest`.
- `CharacterReadModelsTest`.
- `CharacterSnapshotCaptureTest`.
- `Task225R1MaterializationTest`.
- `Task225R2DeadlineLabTest`.

Os testes relacionados ao contrato atual passaram na mesma execução:

| Classe | Aprovados |
| --- | ---: |
| RpgSkillEngineTest | 104 |
| Task225MaterializationTest | 12 |
| Alpha002LightningTest | 13 |
| Alpha002LightningSessionTest | 2 |
| Alpha002LightningTcpTest | 3 |
| ServerHandshakeDeadlineTest | 4 |

Total desse recorte: **138 aprovados**, sem falhas/erros/ignorados. A integração visual REA também já tinha 75 testes focados aprovados, incluindo os testes gráficos registrados em [SKILLS-INTEGRACAO-REA.md](SKILLS-INTEGRACAO-REA.md).

As capturas históricas que a execução da suíte regenerou foram preservadas em `server/target/teste-completo-evidencias/`, e somente esses arquivos, inicialmente limpos, foram restaurados. Os fontes e alterações anteriores não foram restaurados nem descartados.

## Uso e limites

Baixe o ZIP atual pelo [diretório de testes portáteis](../testes-portateis/skills-windows-x64/README.md), extraia tudo em pasta gravável e execute `SpiritGateSkills/Abrir-Skills.cmd`. Windows x64; Java incluído; sem Maven ou banco; treino local efêmero. Não é conexão entre computadores nem atualização oficial do launcher.

Este envio é commit/push da branch de desenvolvimento e disponibilização do teste portátil solicitados pelo desenvolvedor. Não cria release, não promove conteúdo e não publica no canal `updates`.
