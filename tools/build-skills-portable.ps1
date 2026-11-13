<# Empacota a previa existente de skills para outro Windows, sem alterar o jogo. #>
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$compiled = & (Join-Path $PSScriptRoot 'compile-local.ps1') -PassThru
$buildId = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8)
$build = Join-Path $project ('server/target/skills-portable-' + $buildId)
$inputDir = Join-Path $build 'input'
$previewClasses = Join-Path $build 'preview-classes'
$runtimeDir = Join-Path $build 'runtime'
$destination = Join-Path $build 'image'
New-Item -ItemType Directory -Path $inputDir,$previewClasses,$destination | Out-Null

# Toda saida usa diretorio novo. O empacotador nao apaga pacotes anteriores.
function Invoke-Jdk([string]$name, [string[]]$toolArguments) {
    & (Join-Path $compiled.JdkBin $name) @toolArguments
    if ($LASTEXITCODE -ne 0) { throw "$name falhou: $LASTEXITCODE" }
}
function Write-Utf8([string]$path, [string]$content) {
    [IO.File]::WriteAllText($path,$content,(New-Object Text.UTF8Encoding($false)))
}
Invoke-Jdk 'javac.exe' @('--release','17','-encoding','UTF-8','-cp',$compiled.Classes,'-d',$previewClasses,
    (Join-Path $PSScriptRoot 'SkillsLocalPreview.java'),
    (Join-Path $PSScriptRoot 'SkillsLocalPreviewProbe.java'),
    (Join-Path $PSScriptRoot 'SkillsLiveSwitchProbe.java'))
$jar = Join-Path $inputDir 'spiritgate-skills.jar'
Invoke-Jdk 'jar.exe' @('--create','--file',$jar,'--main-class','SkillsLocalPreview','-C',$compiled.Classes,'.','-C',$previewClasses,'.')
$dependencyText = & (Join-Path $compiled.JdkBin 'jdeps.exe') --multi-release 17 --print-module-deps $jar
if ($LASTEXITCODE -ne 0) { throw 'Falha ao determinar modulos do runtime.' }
$modules = (($dependencyText | Out-String).Trim() + ',jdk.random')
Invoke-Jdk 'jlink.exe' @('--add-modules',$modules,'--strip-debug','--no-man-pages','--no-header-files','--compress=2','--output',$runtimeDir)
Invoke-Jdk 'jpackage.exe' @('--type','app-image','--dest',$destination,'--name','SpiritGateSkills',
    '--input',$inputDir,'--main-jar','spiritgate-skills.jar','--main-class','SkillsLocalPreview',
    '--runtime-image',$runtimeDir,'--app-version','0.0.0','--vendor','Spirit Gate',
    '--description','Previa de teste das 40 habilidades; sem progresso pessoal')
$image = Join-Path $destination 'SpiritGateSkills'

# Conserva os manifests originais, copiando apenas imagens do conjunto efetivo.
$properties = @{}
foreach ($suffix in @('','/refined-v2','/refined-v3','/refined-v4','/refined-v5','/refined-v6','/refined-v7','/refined-v8')) {
    $relative = 'assets/skills' + $suffix + '/manifest.properties'
    $source = Join-Path $project $relative
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Manifesto ausente: $relative" }
    $target = Join-Path $image $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $target
    foreach ($line in Get-Content -LiteralPath $source -Encoding UTF8) {
        if ($line -match '^\s*([^#!][^=]*?)\s*=(.*)$') { $properties[$Matches[1].Trim()] = $Matches[2].Trim() }
    }
    foreach ($notice in @('provenance.json','README.md')) {
        $noticeSource = Join-Path (Split-Path -Parent $source) $notice
        if (Test-Path -LiteralPath $noticeSource -PathType Leaf) {
            Copy-Item -LiteralPath $noticeSource -Destination (Join-Path (Split-Path -Parent $target) $notice)
        }
    }
}
$assetPaths = @($properties.Keys | Where-Object { $_ -match '[.]frames$' } | ForEach-Object {
    $prefix = $_.Substring(0,$_.Length - 6)
    if ($properties.ContainsKey($prefix+'path')) { $properties[$prefix+'path'] } else { $properties[$prefix+'image'] }
} | Sort-Object -Unique)
if ($assetPaths.Count -lt 40) { throw 'Conjunto de imagens de skills incompleto.' }
foreach ($relative in $assetPaths) {
    $source = [IO.Path]::GetFullPath((Join-Path $project $relative))
    $assetRoot = [IO.Path]::GetFullPath((Join-Path $project 'assets/skills')) + '\'
    if (-not $source.StartsWith($assetRoot,[StringComparison]::OrdinalIgnoreCase)) { throw 'Imagem fora de assets/skills.' }
    $target = Join-Path $image $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Copy-Item -LiteralPath $source -Destination $target
}
foreach ($relative in @('assets/dev/player-base','assets/dev/reaper','assets/dev/generated/corujao-iv-v1')) {
    $target = Join-Path $image $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $project $relative) -Destination $target -Recurse
}
Copy-Item -LiteralPath (Join-Path $project 'docs/SKILLS-COMO-TESTAR.md') -Destination (Join-Path $image 'ROTEIRO-SKILLS.md')
$open = '@echo off' + "`r`n" + 'cd /d "%~dp0"' + "`r`n" + '"%~dp0runtime\bin\java.exe" -jar "%~dp0app\spiritgate-skills.jar" "%~dp0"' + "`r`n" + 'if errorlevel 1 pause' + "`r`n"
Write-Utf8 (Join-Path $image 'Abrir-Skills.cmd') $open
$verify = '@echo off' + "`r`n" + 'cd /d "%~dp0"' + "`r`n" + '"%~dp0runtime\bin\java.exe" -cp "%~dp0app\spiritgate-skills.jar" SkillsLocalPreviewProbe "%~dp0"' + "`r`n" + 'if errorlevel 1 goto failed' + "`r`n" + '"%~dp0runtime\bin\java.exe" -cp "%~dp0app\spiritgate-skills.jar" SkillsLiveSwitchProbe "%~dp0"' + "`r`n" + ':failed' + "`r`n" + 'pause' + "`r`n"
Write-Utf8 (Join-Path $image 'Verificar-Pacote.cmd') $verify
Write-Utf8 (Join-Path $image 'LEIA-ME.txt') @"
SPIRIT GATE - TESTE DAS 40 SKILLS - $buildId
Windows x64; runtime Java integrado. Nao precisa instalar Java/Maven/banco.
Extraia TODO o ZIP em pasta gravavel e execute Abrir-Skills.cmd.
Escolha um kit; o auxiliar opcional permite testar curas/revive.
Troque o kit pelo seletor no topo. F5-F9: skills; TAB: alvo; SPACE: ataque.
Consulte ROTEIRO-SKILLS.md para os demais controles e casos visuais.
O treino usa servidor efemero somente em loopback, sem conta ou save pessoal.
Feche a janela principal para encerrar. Preferencias de treino sao temporarias.
Verificar-Pacote.cmd abre/fecha as janelas automaticamente para diagnostico.
Ele grava evidencias locais em .task-checkpoints; nao envia nada.
Arte e gameplay continuam sujeitos a homologacao humana. Este ZIP nao e release.
Ao reportar: build-id, Windows, kit/skill, passos e captura; nunca credenciais.
"@
$files = @(Get-ChildItem -LiteralPath $image -Recurse -File | Sort-Object FullName | ForEach-Object {
    [ordered]@{path=$_.FullName.Substring($image.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
})
$manifest = [ordered]@{buildId=$buildId;sourceHead=(& git -C $project rev-parse HEAD);sourceIncludesUncommittedChanges=$true;compileFingerprint=$compiled.Fingerprint;skillsImages=$assetPaths.Count;files=$files}
$output = Join-Path $project 'testes-portateis/skills-windows-x64'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$zip = Join-Path $output ('SpiritGate-Skills-Windows64-' + $buildId + '.zip')
Compress-Archive -LiteralPath $image -DestinationPath $zip -CompressionLevel Optimal
if ((Get-Item -LiteralPath $zip).Length -ge 100MB) { throw 'ZIP excede o limite para Git; conservar localmente e revisar a distribuicao.' }
Write-Utf8 ($zip+'.manifest.json') ($manifest | ConvertTo-Json -Depth 6)
Write-Utf8 ($zip+'.sha256') ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + [IO.Path]::GetFileName($zip) + "`n")
Write-Output "SKILLS_IMAGE=$image"
Write-Output "SKILLS_ZIP=$zip"
