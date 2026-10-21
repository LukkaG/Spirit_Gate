param([string]$Version='1.0.0-rc1', [string]$RuntimeImage='')
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
if ($RuntimeImage) {
    # Uma imagem anterior so e reutilizada com os mesmos binarios e modulos do JDK.
    $RuntimeImage = [IO.Path]::GetFullPath($RuntimeImage)
    foreach ($binary in @('bin\java.exe', 'bin\server\jvm.dll')) {
        $runtimeBinary = Join-Path $RuntimeImage $binary
        $jdkBinary = Join-Path (Split-Path -Parent $jdkBin) $binary
        if (-not (Test-Path -LiteralPath $runtimeBinary -PathType Leaf) -or
            (Get-FileHash -LiteralPath $runtimeBinary -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $jdkBinary -Algorithm SHA256).Hash) {
            throw 'RuntimeImage nao corresponde ao JDK selecionado.'
        }
    }
    $expectedModules = @(& (Join-Path $jdkBin 'java.exe') --limit-modules $modules --list-modules | Sort-Object)
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao verificar os modulos do JDK.' }
    $runtimeModules = @(& (Join-Path $RuntimeImage 'bin\java.exe') --list-modules | Sort-Object)
    if ($LASTEXITCODE -ne 0 -or ($expectedModules -join "`n") -ne ($runtimeModules -join "`n")) {
        throw 'RuntimeImage possui modulos diferentes dos solicitados.'
    }
    Copy-Item -LiteralPath $RuntimeImage -Destination $runtimeDir -Recurse
} else {
    Checked 'jlink.exe' @('--add-modules',$modules,'--strip-debug','--no-man-pages','--no-header-files','--compress=2','--output',$runtimeDir)
}
$windowsVersion=($Version -split '-')[0]
Checked 'jpackage.exe' @('--type','app-image','--dest',$destination,'--name','SpiritGateRPG','--input',$inputDir,'--main-jar','spirit-gate-rpg.jar','--main-class','com.projetok.prototype.rpg.RpgLauncher','--runtime-image',$runtimeDir,'--app-version',$windowsVersion,'--vendor','Spirit Gate','--description','Spirit Gate RPG offline - teste privado')
$image=Join-Path $destination 'SpiritGateRPG'
foreach($file in Get-ChildItem -LiteralPath $build -Recurse -File){if($file.IsReadOnly){$file.IsReadOnly=$false}}
$notice="SPIRIT GATE RPG OFFLINE $Version`r`nCandidato local: extraia todo o ZIP e abra SpiritGateRPG.exe. Java integrado; sem banco ou rede.`r`nWASD mover; F interagir/portal; SPACE atacar; Q habilidade; 1 pocao; F5 salvar.`r`nRacas escolhidas na criacao; dominio no nivel 15 e Shunpo no nivel 20. Arte geometrica provisoria.`r`nNao ha compra de cash ou servidor de pagamento neste cliente local. Nenhum save pessoal foi incluido."
[IO.File]::WriteAllText((Join-Path $image 'LEIA-ME.txt'),$notice,(New-Object Text.UTF8Encoding($false)))
Copy-Item -LiteralPath (Join-Path $project 'docs\ROTEIRO-RPG-OFFLINE.md') -Destination (Join-Path $image 'ROTEIRO-RPG-OFFLINE.md')
Copy-Item -LiteralPath (Join-Path $project 'docs\SISTEMA-DE-RACAS-E-DOMINIOS.md') -Destination (Join-Path $image 'RACAS-E-DOMINIOS.md')
Copy-Item -LiteralPath (Join-Path $project 'docs\Spirit_Gate_Racas_e_Dominios.pdf') -Destination (Join-Path $image 'RACAS-E-DOMINIOS.pdf')
Copy-Item -LiteralPath (Join-Path $project 'server\target\launcher\build.json') -Destination (Join-Path $image 'source-build.json')
$manifest=@(Get-ChildItem -LiteralPath $image -Recurse -File | Sort-Object FullName | ForEach-Object { [ordered]@{path=$_.FullName.Substring($image.Length+1).Replace('\','/');bytes=$_.Length;sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()} })
$zip=Join-Path $project ('dist\SpiritGate-RPG-'+$buildId+'.zip')
New-Item -ItemType Directory -Path (Split-Path -Parent $zip) -Force | Out-Null
Compress-Archive -LiteralPath $image -DestinationPath $zip -CompressionLevel Optimal
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath ($zip+'.manifest.json') -Encoding UTF8
(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant() | Set-Content -LiteralPath ($zip+'.sha256') -Encoding ASCII
Write-Output "RPG_EXE=$image\SpiritGateRPG.exe"
Write-Output "RPG_ZIP=$zip"
