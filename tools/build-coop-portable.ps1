param([string]$Version='0.76.0-rc1')
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
if($Version -notmatch '^0[.]\d+[.]\d+-rc\d+$'){throw 'Versao invalida'}
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$jdkBin=Split-Path (Get-Command java.exe).Source
$buildId=$Version+'-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8)
$build=Join-Path $project ('server\target\coop-portable-'+$buildId)
$inputDir=Join-Path $build 'input';$destination=Join-Path $build 'output';$runtimeDir=Join-Path $build 'runtime';$classes=Join-Path $project 'server\target\classes';$stage=Join-Path $build 'classes'
New-Item -ItemType Directory -Path $inputDir,$destination,(Join-Path $stage 'com\projetok\prototype') | Out-Null
Copy-Item -LiteralPath (Join-Path $classes 'com\projetok\domain') -Destination (Join-Path $stage 'com\projetok\domain') -Recurse
Copy-Item -LiteralPath (Join-Path $classes 'com\projetok\multiplayer') -Destination (Join-Path $stage 'com\projetok\multiplayer') -Recurse
Get-ChildItem -LiteralPath (Join-Path $classes 'com\projetok\prototype') -File | Copy-Item -Destination (Join-Path $stage 'com\projetok\prototype')
function Checked([string]$tool,[string[]]$arguments){& (Join-Path $jdkBin $tool) @arguments;if($LASTEXITCODE -ne 0){throw "$tool falhou: $LASTEXITCODE"}}
$jar=Join-Path $inputDir 'spirit-gate-coop.jar'
Checked 'jar.exe' @('--create','--file',$jar,'--main-class','com.projetok.multiplayer.LocalSimulationServer','-C',$stage,'com/projetok/domain','-C',$stage,'com/projetok/prototype','-C',$stage,'com/projetok/multiplayer')
Checked 'jdeps.exe' @('--multi-release','17','--print-module-deps',$jar)
Checked 'jlink.exe' @('--add-modules','java.base,java.desktop,java.logging,jdk.random','--strip-debug','--no-man-pages','--no-header-files','--compress=2','--output',$runtimeDir)
Checked 'jpackage.exe' @('--type','app-image','--dest',$destination,'--name','SpiritGateCoopServer','--input',$inputDir,'--main-jar','spirit-gate-coop.jar','--main-class','com.projetok.multiplayer.LocalSimulationServer','--runtime-image',$runtimeDir,'--app-version',($Version -split '-')[0],'--win-console','--vendor','Spirit Gate','--description','Coop experimental local - separado do RPG offline')
$image=Join-Path $destination 'SpiritGateCoopServer'
foreach($file in Get-ChildItem -LiteralPath $build -Recurse -File){if($file.IsReadOnly){$file.IsReadOnly=$false}}
$client=@'
@echo off
setlocal
"%~dp0runtime\bin\java.exe" -cp "%~dp0app\spirit-gate-coop.jar" com.projetok.multiplayer.NetworkClientLauncher %*
'@
[IO.File]::WriteAllText((Join-Path $image 'iniciar-cliente.cmd'),$client,(New-Object Text.UTF8Encoding($false)))
$notice="SPIRIT GATE COOP EXPERIMENTAL $Version`r`nPacote separado do RPG offline. Java integrado. Sem contas, banco ou saves compartilhados.`r`n1. Inicie SpiritGateCoopServer.exe (somente localhost:54557). Ctrl+C encerra.`r`n2. Abra iniciar-cliente.cmd duas vezes, com nomes distintos.`r`n3. WASD mover, SPACE golpear, SHIFT Passo Relampago, Q Criostase, mouse mirar.`r`nChefe compartilhado; participantes conectados recebem 20 moedas temporarias uma vez.`r`nLAN somente explicita: SpiritGateCoopServer.exe --bind IP-PRIVADO --allow-lan`r`nMarque LAN no cliente. Sem bind publico, firewall, port forwarding ou servico automatico.`r`nHomologacao entre computadores pendente. Um mapa online; sem transicoes online. Nao e MMORPG completo.`r`nO RPG offline e seus saves permanecem no outro pacote."
[IO.File]::WriteAllText((Join-Path $image 'LEIA-ME.txt'),$notice,(New-Object Text.UTF8Encoding($false)))
Copy-Item -LiteralPath (Join-Path $project 'docs\ROTEIRO-COOP-EXPERIMENTAL.md') -Destination (Join-Path $image 'ROTEIRO-COOP-EXPERIMENTAL.md')
$manifest=@(Get-ChildItem -LiteralPath $image -Recurse -File | Sort-Object FullName | ForEach-Object {[ordered]@{path=$_.FullName.Substring($image.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}})
$zip=Join-Path $project ('dist\SpiritGate-Coop-Experimental-'+$buildId+'.zip')
Compress-Archive -LiteralPath $image -DestinationPath $zip -CompressionLevel Optimal
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath ($zip+'.manifest.json') -Encoding UTF8
(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() | Set-Content -LiteralPath ($zip+'.sha256') -Encoding ASCII
Write-Output "COOP_EXE=$image\SpiritGateCoopServer.exe"
Write-Output "COOP_ZIP=$zip"
