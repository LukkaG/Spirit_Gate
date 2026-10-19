<#
.SYNOPSIS
Gera a arena portatil Windows x64 com Java 17 integrado, sem instalador.
.DESCRIPTION
Executa os testes por padrao. O JAR e montado apenas com domain e prototype:
as entradas legadas de servidor, banco e Kryonet nao pertencem a arena local.
Nao apaga diretorios e nunca sobrescreve uma distribuicao anterior.
.EXAMPLE
.\tools\build-windows-portable.cmd -Version 0.7.0-task007
.EXAMPLE
.\tools\build-windows-portable.ps1 -Version 0.11.0-rc1 -SkipMaven
.EXAMPLE
.\tools\build-windows-portable.cmd -Version 0.11.0-rc1 -TesterKitDirectory .\docs\kit-homologacao
#>
[CmdletBinding()]
param(
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$')]
    [string] $Version = '0.7.0-task007',
    [string] $JavaHome = '',
    [string] $TesterKitDirectory = '',
    [string] $MavenRepository = '',
    [string] $VisualManifest = '',
    [string] $VisualRoot = '',
    [switch] $SkipMaven
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$transcriptStarted = $false

# --- Ferramentas e caminhos: somente entradas locais explicitamente permitidas. ---

function Invoke-CheckedTool {
    param(
        [Parameter(Mandatory = $true)][string] $Program,
        [string[]] $Arguments = @(),
        [switch] $Capture
    )
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $lines = @(& $Program @Arguments 2>&1)
        $toolExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousPreference
    }
    foreach ($line in $lines) { Write-Host $line.ToString() }
    if ($toolExitCode -ne 0) {
        throw "Ferramenta terminou com codigo ${toolExitCode}: $Program"
    }
    if ($Capture) { return @($lines | ForEach-Object { $_.ToString() }) }
}

function Assert-ChildPath {
    param([string] $Path, [string] $Parent)
    $absolute = [IO.Path]::GetFullPath($Path)
    $prefix = [IO.Path]::GetFullPath($Parent).TrimEnd('\') + '\'
    if (-not $absolute.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Caminho de saida fora da pasta permitida: $absolute"
    }
    return $absolute
}

function Write-Utf8File {
    param([string] $Path, [string] $Content)
    [IO.File]::WriteAllText($Path, $Content, (New-Object Text.UTF8Encoding($false)))
}

function Get-PortableInventory {
    param([string] $Directory)
    $prefix = [IO.Path]::GetFullPath($Directory).TrimEnd('\') + '\'
    return @(Get-ChildItem -LiteralPath $Directory -Recurse -File |
        Sort-Object FullName | ForEach-Object {
            [ordered]@{
                path = $_.FullName.Substring($prefix.Length).Replace('\', '/')
                sizeBytes = $_.Length
                sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            }
        })
}

try {
    if ($env:OS -ne 'Windows_NT' -or -not [Environment]::Is64BitOperatingSystem) {
        throw 'O app-image exige uma maquina de build Windows x64.'
    }
    $projectDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $serverDirectory = Join-Path $projectDirectory 'server'
    $distributionDirectory = Join-Path $projectDirectory 'dist'
    # O kit e opcional e usa somente estes tres nomes, sem copia recursiva.
    $testerKitFileNames = @('MODELO-RELATO-BUG.md', 'ROTEIRO-HOMOLOGACAO.md', 'CHANGELOG-RESUMIDO.md')
    $testerKitSources = @()
    if ($TesterKitDirectory) {
        $kitDirectory = Get-Item -LiteralPath $TesterKitDirectory -ErrorAction Stop
        if (-not $kitDirectory.PSIsContainer) { throw 'TesterKitDirectory deve apontar para uma pasta.' }
        # Mapa opcional de teste local; kits historicos de tres arquivos continuam validos.
        if (Test-Path -LiteralPath (Join-Path $kitDirectory.FullName 'mapa-demo.properties') -PathType Leaf) {
            $testerKitFileNames += 'mapa-demo.properties'
        }
        foreach ($fileName in $testerKitFileNames) {
            $kitFile = Get-Item -LiteralPath (Join-Path $kitDirectory.FullName $fileName) -ErrorAction Stop
            if ($kitFile.PSIsContainer -or ($kitFile.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
                throw "Arquivo de homologacao deve ser um arquivo comum: $fileName"
            }
            # Rejeita codificacao invalida sem regravar nem substituir tokens.
            $strictUtf8 = New-Object Text.UTF8Encoding($false, $true)
            $null = $strictUtf8.GetString([IO.File]::ReadAllBytes($kitFile.FullName))
            $testerKitSources += $kitFile
        }
    }
    if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
    if (-not $JavaHome) {
        $JavaHome = Split-Path -Parent (Split-Path -Parent (Get-Command java.exe -ErrorAction Stop).Source)
    }
    $JavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
    $releaseFile = Join-Path $JavaHome 'release'
    $release = Get-Content -LiteralPath $releaseFile -Raw
    if ($release -notmatch '(?m)^JAVA_VERSION="(17\.[^"]+)"') {
        throw 'Selecione um JDK 17 completo pelo parametro -JavaHome ou JAVA_HOME.'
    }
    $javaVersion = $Matches[1]
    if ($release -notmatch '(?m)^OS_ARCH="(x86_64|amd64)"') {
        throw 'O JDK selecionado deve ser Windows x64.'
    }
    $jdkBin = Join-Path $JavaHome 'bin'
    foreach ($name in @('java.exe', 'jar.exe', 'jdeps.exe', 'jlink.exe', 'jpackage.exe')) {
        if (-not (Test-Path -LiteralPath (Join-Path $jdkBin $name) -PathType Leaf)) {
            throw "Ferramenta ausente no JDK 17: $name"
        }
    }
    $env:JAVA_HOME = $JavaHome
    $env:PATH = $jdkBin + ';' + $env:PATH
    $maven = (Get-Command mvn.cmd -ErrorAction Stop).Source

    # A versao completa identifica o ZIP; o recurso PE do Windows usa x.y.z.
    if ($Version -notmatch '^(\d+)\.(\d+)\.(\d+)') {
        throw 'A versao deve comecar com tres numeros, por exemplo 0.7.0-task007.'
    }
    $windowsVersion = '{0}.{1}.{2}' -f $Matches[1], $Matches[2], $Matches[3]
    $buildId = $Version + '-' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + '-' +
        [Guid]::NewGuid().ToString('N').Substring(0, 8)
    $artifactName = 'SpiritGate-' + $buildId + '-portable'
    New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null
    $logPath = Assert-ChildPath (Join-Path $distributionDirectory ($artifactName + '-build.log')) $distributionDirectory
    Start-Transcript -LiteralPath $logPath -NoClobber | Out-Null
    $transcriptStarted = $true
    Write-Host "Spirit Gate $Version | Windows x64 | Java $javaVersion"
    Write-Host "JDK: $JavaHome"
    $mavenInformation = @(Invoke-CheckedTool $maven @('--version') -Capture)
    $jpackageVersion = @(Invoke-CheckedTool (Join-Path $jdkBin 'jpackage.exe') @('--version') -Capture)

    # --- Compilacao/testes e staging restrito da arena independente. ---

    Push-Location -LiteralPath $serverDirectory
    try {
        if (-not $SkipMaven) {
            $testArguments = @('clean', 'test')
            if ($MavenRepository) { $testArguments = @('-Dmaven.repo.local=' + [IO.Path]::GetFullPath($MavenRepository)) + $testArguments }
            Invoke-CheckedTool $maven $testArguments
        }
        else {
            Write-Host 'SkipMaven: reutilizando classes locais; execute mvn clean test antes deste modo.'
        }
    }
    finally { Pop-Location }
    $classesDirectory = Join-Path $serverDirectory 'target\classes'
    foreach ($package in @('domain', 'prototype')) {
        if (-not (Test-Path -LiteralPath (Join-Path $classesDirectory ('com\projetok\' + $package)) -PathType Container)) {
            throw "Classes da arena ausentes: $package. Execute mvn clean test."
        }
    }
    # A arena atual desenha em Java2D e nao carrega recursos externos. Um recurso
    # futuro exige whitelist explicita; evita omissao silenciosa ou copia de config.
    $resourcesDirectory = Join-Path $serverDirectory 'src\main\resources'
    if ((Test-Path -LiteralPath $resourcesDirectory) -and
        @(Get-ChildItem -LiteralPath $resourcesDirectory -Recurse -File | Where-Object { -not $_.FullName.StartsWith((Join-Path $resourcesDirectory 'rpg') + '\', [StringComparison]::OrdinalIgnoreCase) }).Count -gt 0) {
        throw 'Foram encontrados recursos novos. Revise a whitelist de staging antes de distribuir.'
    }
    $buildRoot = Assert-ChildPath (Join-Path $serverDirectory ('target\portable-build-' + $buildId)) (Join-Path $serverDirectory 'target')
    if (Test-Path -LiteralPath $buildRoot) { throw "Saida ja existe: $buildRoot" }
    $jarInput = Join-Path $buildRoot 'input'
    $packageOutput = Join-Path $buildRoot 'app-image'
    $runtimeImage = Join-Path $buildRoot 'runtime-image'
    New-Item -ItemType Directory -Path $jarInput -Force | Out-Null
    New-Item -ItemType Directory -Path $packageOutput -Force | Out-Null
    $jarName = 'spirit-gate-arena.jar'
    $jarPath = Join-Path $jarInput $jarName
    # O modo legado exclui explicitamente o subpacote RPG e seus recursos.
    $arenaClasses = Join-Path $buildRoot 'arena-classes'
    New-Item -ItemType Directory -Path (Join-Path $arenaClasses 'com\projetok\prototype') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $classesDirectory 'com\projetok\domain') -Destination (Join-Path $arenaClasses 'com\projetok\domain') -Recurse
    Get-ChildItem -LiteralPath (Join-Path $classesDirectory 'com\projetok\prototype') -File | Copy-Item -Destination (Join-Path $arenaClasses 'com\projetok\prototype')
    Invoke-CheckedTool (Join-Path $jdkBin 'jar.exe') @('--create', '--file', $jarPath,
        '--main-class', 'com.projetok.prototype.DebugLauncher',
        '-C', $arenaClasses, 'com/projetok/domain',
        '-C', $arenaClasses, 'com/projetok/prototype')
    # jdeps falha caso uma classe incluida passe a exigir biblioteca externa.
    $dependencyArguments = @('--multi-release', '17', '--print-module-deps', $jarPath)
    $dependencyLines = @(Invoke-CheckedTool (Join-Path $jdkBin 'jdeps.exe') $dependencyArguments -Capture)
    $modules = @($dependencyLines | Where-Object { $_ -match '^(java|jdk)\.[a-z.,]+$' })
    if ($modules.Count -ne 1) { throw 'Nao foi possivel determinar os modulos Java da arena.' }
    # RandomGenerator.getDefault usa um provedor de servico de jdk.random;
    # jdeps nao o descobre por referencias estaticas a classes de java.base.
    $runtimeModules = @($modules[0].Split(',') + 'jdk.random' | Sort-Object -Unique) -join ','
    Invoke-CheckedTool (Join-Path $jdkBin 'jlink.exe') @('--add-modules', $runtimeModules,
        '--strip-debug', '--no-man-pages', '--no-header-files', '--compress=2', '--output', $runtimeImage)
    Invoke-CheckedTool (Join-Path $jdkBin 'jpackage.exe') @('--type', 'app-image',
        '--dest', $packageOutput, '--name', 'SpiritGate', '--input', $jarInput,
        '--main-jar', $jarName, '--main-class', 'com.projetok.prototype.DebugLauncher',
        '--runtime-image', $runtimeImage, '--app-version', $windowsVersion,
        '--vendor', 'Spirit Gate', '--description', 'Spirit Gate - arena local de testes')
    $appImage = Join-Path $packageOutput 'SpiritGate'
    # jpackage marca arquivos como somente leitura no Windows. Normaliza apenas
    # a saida criada por este build para que um futuro mvn clean possa remove-la.
    foreach ($generatedFile in Get-ChildItem -LiteralPath $buildRoot -Recurse -File) {
        $null = Assert-ChildPath $generatedFile.FullName $buildRoot
        if ($generatedFile.IsReadOnly) { $generatedFile.IsReadOnly = $false }
    }
    foreach ($required in @('SpiritGate.exe', 'app\SpiritGate.cfg', ('app\' + $jarName), 'runtime\release')) {
        if (-not (Test-Path -LiteralPath (Join-Path $appImage $required) -PathType Leaf)) {
            throw "App-image incompleto: $required"
        }
    }

    # --- Kit do tester, inventario verificavel e distribuicao sem sobrescrita. ---

    if ($VisualManifest) {
        if (-not $VisualRoot) { throw 'VisualRoot obrigatorio com VisualManifest.' }
        Invoke-CheckedTool (Get-Command python -ErrorAction Stop).Source @(
            (Join-Path $projectDirectory 'tools\pacote_visual.py'),
            ([IO.Path]::GetFullPath($VisualRoot)), ([IO.Path]::GetFullPath($VisualManifest)),
            (Join-Path $appImage 'visual'))
    }

    $releaseNotice = if ($Version -match '(?i)(^|[._-])rc\d*($|[._-])') {
        'CANDIDATO A HOMOLOGACAO; NAO RELEASE PUBLICA'
    } else { 'PACOTE DE TESTES LOCAIS' }
    $readme = @"
SPIRIT GATE - ARENA LOCAL PORTATIL
Versao: $Version | Windows x64
$releaseNotice

1. Extraia o ZIP inteiro para uma pasta gravavel.
2. Abra SpiritGate.exe dentro da pasta SpiritGate. Nao mova somente o EXE.
3. Nao e necessario instalar Java, Maven, PostgreSQL ou conectar a um servidor.
4. F1 mostra ajuda; F2 permite salvar diagnostico ZIP local, sem envio automatico.
   Revise o JSON antes de compartilhar. Escolha nome novo para evitar sobrescrita.

Controles: WASD mover; Espaco atacar; E esforco (20 vigor);
Q Criostase (55 vigor); Shift Passo Relampago (35 vigor), mirando com mouse;
H mostrar hitboxes; R reiniciar. Feche a janela para sair.

Janela redimensionavel: arena logica 960x640, proporcao preservada e margens.
Mira Shift/Q usa coordenadas do mundo; HUD e margens nao sao alvos.
Mapa opcional: .\SpiritGate.exe --map mapa-demo.properties, se incluido no kit.
Camera/zoom de integracao nao altera fisica; mapas maiores apenas em testes.
Arte nao aprovada permanece fora do pacote; fallback geometrico e intencional.

Mantenha app/ e runtime/ junto ao EXE. O runtime Java 17 esta integrado.
O executavel nao e assinado; avisos do Windows podem ocorrer. Nao desative
protecoes. Confira a origem e o SHA-256 fornecido com a distribuicao.

Ao relatar um problema, informe versao, Windows, passos para reproduzir,
resultado esperado/obtido e captura se possivel. Nao envie credenciais.
Este pacote e candidato a testes locais, sem multiplayer e sem instalador.
Teste em outro Windows sem Java/Maven: PENDENTE DE HOMOLOGACAO HUMANA.
"@
    Write-Utf8File (Join-Path $appImage 'LEIA-ME-TESTER.txt') $readme
    Write-Utf8File (Join-Path $appImage 'VERSION.txt') ($Version + "`r`n")
    foreach ($kitFile in $testerKitSources) {
        $kitDestination = Assert-ChildPath (Join-Path $appImage $kitFile.Name) $appImage
        if (Test-Path -LiteralPath $kitDestination) {
            throw "Arquivo de homologacao ja existe na imagem: $($kitFile.Name)"
        }
        Copy-Item -LiteralPath $kitFile.FullName -Destination $kitDestination -ErrorAction Stop
        $copiedKitFile = Get-Item -LiteralPath $kitDestination
        if ($copiedKitFile.IsReadOnly) { $copiedKitFile.IsReadOnly = $false }
    }
    $payloadInventory = @(Get-PortableInventory $appImage)
    $inventoryText = @($payloadInventory | ForEach-Object { $_.sha256 + '  ' + $_.path }) -join "`r`n"
    Write-Utf8File (Join-Path $appImage 'INVENTARIO-SHA256.txt') ($inventoryText + "`r`n")
    $manifest = [ordered]@{
        schemaVersion = 1
        application = 'Spirit Gate - arena local'
        version = $Version
        windowsAppVersion = $windowsVersion
        builtAtUtc = [DateTime]::UtcNow.ToString('o')
        platform = 'windows-x64'
        javaVersion = $javaVersion
        jpackageVersion = ($jpackageVersion -join ' ')
        mavenVersion = $mavenInformation[0]
        entrypoint = 'com.projetok.prototype.DebugLauncher'
        mainJar = 'app/' + $jarName
        javaModules = @($runtimeModules.Split(','))
        runtimeLibraries = @()
        testerKitFiles = @($testerKitSources | ForEach-Object { $_.Name })
        stagingPolicy = 'Somente com/projetok/domain e com/projetok/prototype; sem classes legadas de banco/rede, testes ou configuracoes privadas.'
        inventoryPolicy = 'Manifest lista todos os arquivos exceto si mesmo; INVENTARIO-SHA256.txt exclui si mesmo e manifest.json para evitar circularidade.'
        testsExecutedByBuild = (-not $SkipMaven.IsPresent)
        cleanWindowsValidation = 'PENDENTE DE HOMOLOGACAO HUMANA'
        files = @(Get-PortableInventory $appImage)
    }
    $manifestPath = Join-Path $appImage 'manifest.json'
    Write-Utf8File $manifestPath ($manifest | ConvertTo-Json -Depth 8)
    $zipPath = Assert-ChildPath (Join-Path $distributionDirectory ($artifactName + '.zip')) $distributionDirectory
    if (Test-Path -LiteralPath $zipPath) { throw "ZIP ja existente; nada foi sobrescrito: $zipPath" }
    # ZipArchive recebe nomes POSIX explicitos; Compress-Archive no Windows
    # gravava barras invertidas, recusadas pelo auditor multiplataforma.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    Add-Type -AssemblyName System.IO.Compression
    $zipStream = [IO.File]::Open($zipPath, [IO.FileMode]::CreateNew)
    $canonicalZip = New-Object IO.Compression.ZipArchive($zipStream, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($file in Get-ChildItem -LiteralPath $appImage -Recurse -File | Sort-Object FullName) {
            $entryName = 'SpiritGate/' + $file.FullName.Substring($appImage.Length + 1).Replace('\', '/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($canonicalZip, $file.FullName,
                $entryName, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    } finally { $canonicalZip.Dispose(); $zipStream.Dispose() }
    $zipHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
    Write-Utf8File ($zipPath + '.sha256') ($zipHash + '  ' + [IO.Path]::GetFileName($zipPath) + "`r`n")
    Copy-Item -LiteralPath $manifestPath -Destination ($zipPath + '.manifest.json') -ErrorAction Stop
    # Gate local: so anuncia sucesso depois de auditar o ZIP original, sem normalizacao.
    $python = (Get-Command python.exe -ErrorAction Stop).Source
    Invoke-CheckedTool $python @((Join-Path $PSScriptRoot 'auditar_rc.py'), '--zip', $zipPath,
        '--report', ($zipPath + '.audit.json'))
    Write-Host "Pacote gerado: $zipPath"
    Write-Host ('Tamanho: ' + (Get-Item -LiteralPath $zipPath).Length + ' bytes')
    Write-Output "ZIP_PATH=$zipPath"
    Write-Output "APP_IMAGE_PATH=$appImage"
    Write-Output "SHA256=$zipHash"
}
catch {
    Write-Error -Message $_.Exception.Message -ErrorAction Continue
    exit 1
}
finally {
    if ($transcriptStarted) { Stop-Transcript | Out-Null }
}
