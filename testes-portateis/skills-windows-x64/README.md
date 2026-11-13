# Teste portátil das 40 skills — Windows x64

[Baixar ZIP (52,48 MiB)](SpiritGate-Skills-Windows64-20261113T003427Z-e989a97f.zip?raw=true) ·
[SHA-256](SpiritGate-Skills-Windows64-20261113T003427Z-e989a97f.zip.sha256) ·
[Validação local](VALIDACAO.json)

Este diretório contém um ZIP de teste com Java integrado, os sprites efetivamente
usados e a mesma prévia `SkillsLocalPreview` do desenvolvimento. Não é uma
release nem uma atualização do launcher. Não requer Java, Maven ou PostgreSQL
instalados e não usa conta ou save pessoal.

## Abrir em outro computador

1. Baixe o arquivo `SpiritGate-Skills-Windows64-*.zip` desta pasta. No GitHub,
   abra o arquivo e use **Download raw**; o navegador não mostra seu conteúdo.
   Baixe também o `.zip.sha256` correspondente.
2. Compare o SHA-256, no PowerShell: `Get-FileHash -Algorithm SHA256 .\NOME-DO-ARQUIVO.zip`.
   O resultado deve coincidir com o arquivo `.sha256`.
3. Extraia **todo** o ZIP em pasta gravável. Não abra diretamente dentro do ZIP
   nem copie só o EXE. Execute `SpiritGateSkills\Abrir-Skills.cmd`.
4. Escolha o kit e, se desejar, habilite o aliado auxiliar. Troque os kits pelo
   seletor no topo. Fechar a janela principal encerra clientes e servidor.

O treino usa TCP somente em loopback com porta temporária, na mesma máquina.
Isso permite validar sprites/skills localmente; não é um ensaio de Radmin ou
de conexão entre dois computadores. O executável não é assinado; mantenha as
proteções do Windows e confira a origem/hash antes de abri-lo.

## Roteiro visual

- Percorra Água, Vento, Raio, Fogo, Spirit, Aetheri, Luz e Artista Marcial.
- Use F5–F9 para técnicas, TAB para alvo, SPACE para ataque e WASD para mover.
  O roteiro completo `ROTEIRO-SKILLS.md` acompanha o ZIP.
- Confira direção, pivô, formação/dissipação, impactos, rastros e apresentação
  no auxiliar. Faça a troca de kit e teste foco, teclado e fechamento.
- Em Luz, teste aliado ferido, aliado derrotado e revive. Em Raio, observe as
  quatro fases da quinta técnica; o timeout A-04 não foi implementado.
- `Verificar-Pacote.cmd` executa os dois probes gráficos: abre/fecha janelas
  automaticamente e grava resultados/capturas locais em `.task-checkpoints`.
  Esses arquivos não são enviados automaticamente.

Para reportar: nome completo do ZIP/build-id, versão do Windows, kit/técnica,
passos, esperado/obtido e captura. Não envie credenciais nem saves pessoais.
Sprites e gameplay ainda exigem avaliação humana; execução automática local
não comprova homologação física em outro PC.

Validação de 12/11/2026: 446 arquivos do manifest conferidos por SHA-256;
o `Verificar-Pacote.cmd` distribuído executou os dois probes com sucesso numa
extração nova usando o runtime embutido, com oito
kits e troca entre dez domínios, com/sem auxiliar. Suíte Java headless:
1.196 aprovados, 57 excluídos, zero falhas/erros. Mais 12 testes Python de assets
e revisão de sprites aprovados. O teste físico em outro computador está pendente.

## Reproduzir o pacote no desenvolvimento

Na raiz de um checkout completo, com JDK 17 contendo javac, jar, jdeps, jlink e
jpackage: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/build-skills-portable.ps1`.
Não instala dependências nem publica. Cria uma saída nova, manifest de arquivos
e SHA-256; os manifests V1–V8 selecionam o conjunto visual efetivo. O refinamento
de comportamento V9 já está nos fontes compilados.

Para abrir pelos fontes: `tools\testar-skills.cmd`, sem Maven/cache local.
Os demais testes reproduzíveis e scripts de empacotamento também acompanham o
repositório; dados pessoais, caches, checkpoints e builds históricos ficam locais.
