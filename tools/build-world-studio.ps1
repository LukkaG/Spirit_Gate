param([string]$Version='0.114.0-rc1',[switch]$Multiplayer)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
if($Version -notmatch '^0[.]\d+[.]\d+-rc\d+$'){throw 'Versao invalida'}
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$compiled = & (Join-Path $PSScriptRoot 'compile-local.ps1') -PassThru
$jdk=$compiled.JdkBin
$buildId=$Version+'-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8)
$build=Join-Path $project ('server/target/studio-'+$buildId)
$inputDir=Join-Path $build 'input'; $output=Join-Path $build 'output'; $runtime=Join-Path $build 'runtime'
New-Item -ItemType Directory -Path $inputDir,$output | Out-Null
function Checked([string]$tool,[string[]]$parameters){& (Join-Path $jdk $tool) @parameters;if($LASTEXITCODE -ne 0){throw "$tool falhou"}}
$classes=$compiled.Classes
Checked 'jar.exe' @('--create','--file',(Join-Path $inputDir 'world-studio.jar'),'--main-class','com.projetok.studio.WorldStudio','-C',$classes,'com/projetok','-C',$classes,'rpg')
$modules = 'java.base,java.desktop,java.logging'
if (@(& (Join-Path $jdk 'java.exe') --list-modules) -match '^jdk[.]random@') { $modules += ',jdk.random' }
Checked 'jlink.exe' @('--add-modules',$modules,'--strip-debug','--no-man-pages','--no-header-files','--compress=2','--output',$runtime)
$packageArguments=@('--type','app-image','--dest',$output,'--name','SpiritGateWorldStudio','--input',$inputDir,'--main-jar','world-studio.jar','--main-class','com.projetok.studio.WorldStudio','--runtime-image',$runtime,'--app-version',($Version -split '-')[0],'--vendor','Spirit Gate')
if($Multiplayer){
    $launchers=@(
        @('SpiritGateRpgServer','com.projetok.multiplayer.LocalSimulationServer','--rpg --cosmetics --bind 127.0.0.1 --port 54557'),
        @('SpiritGateRpgClientA','com.projetok.multiplayer.RpgNetworkClientLauncher','127.0.0.1 54557 JogadorA --cosmetics --hair hair_short --clothing tunic'),
        @('SpiritGateRpgClientB','com.projetok.multiplayer.RpgNetworkClientLauncher','127.0.0.1 54557 JogadorB --cosmetics --hair hair_spiky --clothing jacket'))
    foreach($launcher in $launchers){
        $config=Join-Path $build ($launcher[0]+'.properties')
        $properties=@('main-jar=world-studio.jar',('main-class='+$launcher[1]),('arguments='+$launcher[2]))
        if($launcher[0] -eq 'SpiritGateRpgServer'){$properties+='win-console=true'}
        [IO.File]::WriteAllLines($config,$properties,[Text.Encoding]::ASCII)
        $packageArguments+=@('--add-launcher',($launcher[0]+'='+$config))
    }
}
Checked 'jpackage.exe' $packageArguments
$image=Join-Path $output 'SpiritGateWorldStudio'
foreach($file in Get-ChildItem -LiteralPath $build -Recurse -File){if($file.IsReadOnly){$file.IsReadOnly=$false}}
$assets=Join-Path $project 'assets'
foreach($file in Get-ChildItem -LiteralPath $assets -Recurse -File | Where-Object {$_.Extension -in @('.png','.properties') -or $_.Name -in @('studio_tutorial.sgmap','README.txt')}){
    $relative=$file.FullName.Substring($assets.Length+1);$destination=Join-Path (Join-Path $image 'assets') $relative
    New-Item -ItemType Directory -Path (Split-Path $destination) -Force | Out-Null
    Copy-Item -LiteralPath $file.FullName -Destination $destination
    if((Get-FileHash -LiteralPath $file.FullName).Hash -ne (Get-FileHash -LiteralPath $destination).Hash){throw 'Hash divergente na copia de assets'}
}
Copy-Item -LiteralPath (Join-Path $project 'docs/WORLD-EDITOR-MANUAL.md') -Destination (Join-Path $image 'MANUAL.md')
$packageDocs=Join-Path $image 'docs'
New-Item -ItemType Directory -Path $packageDocs | Out-Null
foreach($name in @('WORLD-STUDIO-TUTORIAL.md','WORLD-STUDIO-FUNCIONALIDADES.md','WORLD-STUDIO-PERFORMANCE.md','WORLD-EDITOR-FORMATO-MAPA.md','CORUJAO-II-HOMOLOGACAO.md')){Copy-Item -LiteralPath (Join-Path $project ('docs/'+$name)) -Destination (Join-Path $packageDocs $name)}
[IO.File]::WriteAllText((Join-Path $image 'LEIA-ME.txt'),"Spirit Gate World Studio $Version`r`nExtraia todo o ZIP e abra SpiritGateWorldStudio.exe. Java integrado, sem PostgreSQL ou multiplayer.`r`nAssets de trabalho experimentais; nenhuma aprovacao artistica presumida. Projetos novos em studio-projects. Manual em MANUAL.md. Exemplo em assets/maps/studio_tutorial.sgmap.",[Text.Encoding]::UTF8)
$manifest=@(Get-ChildItem -LiteralPath $image -Recurse -File | Sort-Object FullName | ForEach-Object {[ordered]@{path=$_.FullName.Substring($image.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}})
$zip=Join-Path $project ('dist/SpiritGate-WorldStudio-'+$buildId+'.zip')
New-Item -ItemType Directory -Path (Split-Path -Parent $zip) -Force | Out-Null
Compress-Archive -LiteralPath $image -DestinationPath $zip -CompressionLevel Optimal
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath ($zip+'.manifest.json') -Encoding UTF8
(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() | Set-Content -LiteralPath ($zip+'.sha256') -Encoding ASCII
Write-Output "STUDIO_EXE=$image\SpiritGateWorldStudio.exe"
Write-Output "STUDIO_ZIP=$zip"
