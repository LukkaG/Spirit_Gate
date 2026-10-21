param([string]$Version='1.0.0-rc1')
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
if($Version -notmatch '^\d+[.]\d+[.]\d+-rc\d+$'){throw 'Versao invalida'}
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$compiled = & (Join-Path $PSScriptRoot 'compile-local.ps1') -PassThru
$jdkBin=$compiled.JdkBin
$buildId=$Version+'-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8)
$build=Join-Path $project ('server\target\rpg-portable-'+$buildId)
$inputDir=Join-Path $build 'input'
$destination=Join-Path $build 'output'
$runtimeDir=Join-Path $build 'runtime'
$classes=$compiled.Classes
New-Item -ItemType Directory -Path $inputDir,$destination | Out-Null
function Checked([string]$tool,[string[]]$arguments){ & (Join-Path $jdkBin $tool) @arguments; if($LASTEXITCODE -ne 0){throw "$tool falhou: $LASTEXITCODE"} }
$jar=Join-Path $inputDir 'spirit-gate-rpg.jar'
Checked 'jar.exe' @('--create','--file',$jar,'--main-class','com.projetok.prototype.rpg.RpgLauncher','-C',$classes,'com/projetok/domain','-C',$classes,'com/projetok/prototype','-C',$classes,'com/projetok/rpg','-C',$classes,'rpg')
Checked 'jdeps.exe' @('--multi-release','17','--print-module-deps',$jar)
$modules = 'java.base,java.desktop,java.logging'
if (@(& (Join-Path $jdkBin 'java.exe') --list-modules) -match '^jdk[.]random@') { $modules += ',jdk.random' }
Checked 'jlink.exe' @('--add-modules',$modules,'--strip-debug','--no-man-pages','--no-header-files','--compress=2','--output',$runtimeDir)
$windowsVersion=($Version -split '-')[0]
Checked 'jpackage.exe' @('--type','app-image','--dest',$destination,'--name','SpiritGateRPG','--input',$inputDir,'--main-jar','spirit-gate-rpg.jar','--main-class','com.projetok.prototype.rpg.RpgLauncher','--runtime-image',$runtimeDir,'--app-version',$windowsVersion,'--vendor','Spirit Gate','--description','Spirit Gate RPG offline - sprites experimentais','--java-options','-Dspiritgate.rpg.experimentalSprites=true')
$image=Join-Path $destination 'SpiritGateRPG'
foreach($file in Get-ChildItem -LiteralPath $build -Recurse -File){if($file.IsReadOnly){$file.IsReadOnly=$false}}
$assetSource=Join-Path $project 'assets/dev/player-base'
$assetTarget=Join-Path $image 'assets/dev/player-base'
New-Item -ItemType Directory -Path $assetTarget | Out-Null
$metadata=Join-Path $classes 'rpg/experimental-player.properties'
$expected=@(Get-Content -LiteralPath $metadata | Where-Object {$_ -match '^(FRONT|BACK|LEFT|RIGHT)[.][0-4]='})
if($expected.Count -ne 20){throw 'Contrato experimental incompleto: esperado Idle e quatro Walk por direcao'}
foreach($line in $expected){
    $fields=($line -split '=',2)[1] -split ','
    $asset=Join-Path $assetSource $fields[0]
    if((Get-FileHash -LiteralPath $asset -Algorithm SHA256).Hash.ToLowerInvariant() -ne $fields[1]){throw "Asset divergente: $($fields[0])"}
    Copy-Item -LiteralPath $asset -Destination (Join-Path $assetTarget $fields[0])
}
$geometry='@echo off' + "`r`n" + 'cd /d "%~dp0"' + "`r`n" + '"%~dp0runtime\bin\java.exe" -Dspiritgate.rpg.experimentalSprites=false "-Djpackage.app-path=%~dp0SpiritGateRPG.exe" -cp "%~dp0app\spirit-gate-rpg.jar" com.projetok.prototype.rpg.RpgLauncher' + "`r`n"
[IO.File]::WriteAllText((Join-Path $image 'Abrir-Geometrico.cmd'),$geometry,[Text.Encoding]::ASCII)
$notice="SPIRIT GATE RPG OFFLINE $Version`r`nExtraia todo o ZIP. Abra SpiritGateRPG.exe. Java integrado; sem banco ou rede.`r`nWASD mover; F interagir/portal; SPACE atacar; Q habilidade; 1 pocao; F5 salvar.`r`nArena historica continua disponivel separadamente. Sprites experimentais ligadas no EXE. Alternativa sem sprites: Abrir-Geometrico.cmd. Teste privado; aprovacao artistica pendente.`r`nWindows 64 bits. Nao precisa instalar Java. Extraia TUDO antes de abrir. Saves novos ficam em saves ao lado do EXE; nenhum save pessoal foi incluido."
[IO.File]::WriteAllText((Join-Path $image 'LEIA-ME.txt'),$notice,(New-Object Text.UTF8Encoding($false)))
Copy-Item -LiteralPath (Join-Path $project 'docs\ROTEIRO-RPG-OFFLINE.md') -Destination (Join-Path $image 'ROTEIRO-RPG-OFFLINE.md')
$manifest=@(Get-ChildItem -LiteralPath $image -Recurse -File | Sort-Object FullName | ForEach-Object { [ordered]@{path=$_.FullName.Substring($image.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()} })
$zip=Join-Path $project ('dist\SpiritGate-RPG-Sprites-Teste-'+$buildId+'.zip')
New-Item -ItemType Directory -Path (Split-Path -Parent $zip) -Force | Out-Null
Compress-Archive -LiteralPath $image -DestinationPath $zip -CompressionLevel Optimal
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath ($zip+'.manifest.json') -Encoding UTF8
(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() | Set-Content -LiteralPath ($zip+'.sha256') -Encoding ASCII
Write-Output "RPG_EXE=$image\SpiritGateRPG.exe"
Write-Output "RPG_ZIP=$zip"
