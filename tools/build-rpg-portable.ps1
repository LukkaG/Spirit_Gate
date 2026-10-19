param([string]$Version='0.58.0-rc1')
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
if($Version -notmatch '^0[.]\d+[.]\d+-rc\d+$'){throw 'Versao invalida'}
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$jdkBin=Split-Path (Get-Command java.exe).Source
$buildId=$Version+'-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8)
$build=Join-Path $project ('server\target\rpg-portable-'+$buildId)
$inputDir=Join-Path $build 'input'
$destination=Join-Path $build 'output'
$runtimeDir=Join-Path $build 'runtime'
$classes=Join-Path $project 'server\target\classes'
New-Item -ItemType Directory -Path $inputDir,$destination | Out-Null
function Checked([string]$tool,[string[]]$arguments){ & (Join-Path $jdkBin $tool) @arguments; if($LASTEXITCODE -ne 0){throw "$tool falhou: $LASTEXITCODE"} }
$jar=Join-Path $inputDir 'spirit-gate-rpg.jar'
Checked 'jar.exe' @('--create','--file',$jar,'--main-class','com.projetok.prototype.rpg.RpgLauncher','-C',$classes,'com/projetok/domain','-C',$classes,'com/projetok/prototype','-C',$classes,'com/projetok/rpg','-C',$classes,'rpg')
Checked 'jdeps.exe' @('--multi-release','17','--print-module-deps',$jar)
Checked 'jlink.exe' @('--add-modules','java.base,java.desktop,java.logging,jdk.random','--strip-debug','--no-man-pages','--no-header-files','--compress=2','--output',$runtimeDir)
$windowsVersion=($Version -split '-')[0]
Checked 'jpackage.exe' @('--type','app-image','--dest',$destination,'--name','SpiritGateRPG','--input',$inputDir,'--main-jar','spirit-gate-rpg.jar','--main-class','com.projetok.prototype.rpg.RpgLauncher','--runtime-image',$runtimeDir,'--app-version',$windowsVersion,'--vendor','Spirit Gate','--description','Spirit Gate RPG offline - teste privado')
$image=Join-Path $destination 'SpiritGateRPG'
foreach($file in Get-ChildItem -LiteralPath $build -Recurse -File){if($file.IsReadOnly){$file.IsReadOnly=$false}}
$notice="SPIRIT GATE RPG OFFLINE $Version`r`nExtraia todo o ZIP. Abra SpiritGateRPG.exe. Java integrado; sem banco ou rede.`r`nWASD mover; F interagir/portal; SPACE atacar; Q habilidade; 1 pocao; F5 salvar.`r`nArena historica continua disponivel separadamente. Arte geometrica provisoria.`r`nBuild usa classes da ultima suite completa; execute qualidade.py antes de empacotar."
[IO.File]::WriteAllText((Join-Path $image 'LEIA-ME.txt'),$notice,(New-Object Text.UTF8Encoding($false)))
Copy-Item -LiteralPath (Join-Path $project 'docs\ROTEIRO-RPG-OFFLINE.md') -Destination (Join-Path $image 'ROTEIRO-RPG-OFFLINE.md')
$manifest=@(Get-ChildItem -LiteralPath $image -Recurse -File | Sort-Object FullName | ForEach-Object { [ordered]@{path=$_.FullName.Substring($image.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()} })
$zip=Join-Path $project ('dist\SpiritGate-RPG-'+$buildId+'.zip')
Compress-Archive -LiteralPath $image -DestinationPath $zip -CompressionLevel Optimal
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath ($zip+'.manifest.json') -Encoding UTF8
(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() | Set-Content -LiteralPath ($zip+'.sha256') -Encoding ASCII
Write-Output "RPG_EXE=$image\SpiritGateRPG.exe"
Write-Output "RPG_ZIP=$zip"
