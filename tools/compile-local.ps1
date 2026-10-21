<# Compila o cliente desktop e o World Studio a partir das fontes locais, sem Maven ou rede. #>
[CmdletBinding()]
param([string]$JavaHome = '', [switch]$Force, [switch]$PassThru)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$buildRoot = Join-Path $project 'server\target\launcher'
$classes = Join-Path $buildRoot 'classes'
$sourceRoot = Join-Path $project 'server\src\main\java'
$resourceRoot = Join-Path $project 'server\src\main\resources'

function Assert-BuildPath([string]$path) {
    $absolute = [IO.Path]::GetFullPath($path)
    $prefix = [IO.Path]::GetFullPath($buildRoot).TrimEnd('\') + '\'
    if (-not $absolute.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Saida de compilacao fora da pasta permitida: $absolute"
    }
    if (Test-Path -LiteralPath $absolute) {
        if ((Get-Item -LiteralPath $absolute -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw "Saida de compilacao nao pode ser um link: $absolute"
        }
    }
    return $absolute
}

if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
if ($JavaHome) {
    $jdkBin = Join-Path ([IO.Path]::GetFullPath($JavaHome)) 'bin'
} else {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if (-not $compiler) { throw 'Instale um JDK 17 ou superior e configure JAVA_HOME ou javac no PATH.' }
    $jdkBin = Split-Path -Parent $compiler.Source
}
$java = Join-Path $jdkBin 'java.exe'
$javac = Join-Path $jdkBin 'javac.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf) -or -not (Test-Path -LiteralPath $javac -PathType Leaf)) {
    throw "JDK incompleto em $jdkBin. Sao necessarios java.exe e javac.exe (17 ou superior)."
}
$compilerVersion = (& $javac --version | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $compilerVersion -notmatch '^javac (\d+)') { throw 'Nao foi possivel verificar a versao do JDK.' }
if ([int]$Matches[1] -lt 17) { throw 'O launcher exige um JDK 17 ou superior.' }

$sources = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java' | Sort-Object FullName)
$resources = @(Get-ChildItem -LiteralPath $resourceRoot -Recurse -File | Sort-Object FullName)
if (-not $sources.Count) { throw 'Nenhuma fonte Java encontrada.' }
$fingerprintLines = @('compile-local-v2', '--release=17', $javac, $compilerVersion)
foreach ($file in @($sources) + @($resources)) {
    $relative = $file.FullName.Substring($project.Length + 1).Replace('\', '/')
    $fingerprintLines += $relative + ':' + (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
}
$sha = [Security.Cryptography.SHA256]::Create()
try {
    $fingerprint = [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($fingerprintLines -join "`n")))).Replace('-', '').ToLowerInvariant()
} finally { $sha.Dispose() }
$manifestFile = Join-Path $buildRoot 'build.json'
$needsBuild = $true
if (-not $Force -and (Test-Path -LiteralPath $manifestFile -PathType Leaf) -and
    (Test-Path -LiteralPath (Join-Path $classes 'com\projetok\prototype\rpg\RpgLauncher.class') -PathType Leaf)) {
    try {
        $manifest = Get-Content -LiteralPath $manifestFile -Raw | ConvertFrom-Json
        $needsBuild = $manifest.fingerprint -ne $fingerprint
        if (-not $needsBuild) {
            foreach ($entry in $manifest.files) {
                $output = Assert-BuildPath (Join-Path $classes $entry.path)
                if (-not (Test-Path -LiteralPath $output -PathType Leaf) -or
                    (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash -ne $entry.sha256) {
                    $needsBuild = $true
                    break
                }
            }
        }
    } catch { $needsBuild = $true }
}
if ($needsBuild) {
    New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
    $staging = Assert-BuildPath (Join-Path $buildRoot ('compile-' + [Guid]::NewGuid().ToString('N')))
    New-Item -ItemType Directory -Path $staging | Out-Null
    $argumentsFile = Assert-BuildPath (Join-Path $buildRoot ('sources-' + [Guid]::NewGuid().ToString('N') + '.args'))
    try {
        $sourceArguments = @($sources | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
        [IO.File]::WriteAllLines($argumentsFile, $sourceArguments, (New-Object Text.UTF8Encoding($false)))
        Write-Host "[Spirit Gate] Compilando $($sources.Count) fontes para Java 17..."
        & $javac --release 17 -encoding UTF-8 -d $staging ('@' + $argumentsFile)
        if ($LASTEXITCODE -ne 0) { throw 'Compilacao falhou; o launcher nao usara classes antigas.' }
        foreach ($file in $resources) {
            $destination = Join-Path $staging $file.FullName.Substring($resourceRoot.Length + 1)
            New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
            Copy-Item -LiteralPath $file.FullName -Destination $destination
        }
        $inventory = @(Get-ChildItem -LiteralPath $staging -Recurse -File | Sort-Object FullName | ForEach-Object {
            [ordered]@{ path = $_.FullName.Substring($staging.Length + 1).Replace('\', '/'); sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
        })
        $previous = Assert-BuildPath (Join-Path $buildRoot ('previous-' + [Guid]::NewGuid().ToString('N')))
        $safeClasses = Assert-BuildPath $classes
        if (Test-Path -LiteralPath $safeClasses) { Move-Item -LiteralPath $safeClasses -Destination $previous }
        try { Move-Item -LiteralPath $staging -Destination $safeClasses }
        catch {
            if (Test-Path -LiteralPath $previous) { Move-Item -LiteralPath $previous -Destination $safeClasses }
            throw
        }
        if (Test-Path -LiteralPath $previous) { Remove-Item -LiteralPath (Assert-BuildPath $previous) -Recurse -Force }
        $manifest = [ordered]@{ schemaVersion = 1; fingerprint = $fingerprint; java = $compilerVersion; sources = $sources.Count; files = $inventory }
        [IO.File]::WriteAllText($manifestFile, ($manifest | ConvertTo-Json -Depth 5), (New-Object Text.UTF8Encoding($false)))
    } finally {
        if (Test-Path -LiteralPath $argumentsFile) { Remove-Item -LiteralPath (Assert-BuildPath $argumentsFile) -Force }
        if (Test-Path -LiteralPath $staging) { Remove-Item -LiteralPath (Assert-BuildPath $staging) -Recurse -Force }
    }
}
if ($PassThru) {
    [pscustomobject]@{ Classes = $classes; Java = $java; JdkBin = $jdkBin; Fingerprint = $fingerprint; Rebuilt = $needsBuild }
} else { Write-Host "[Spirit Gate] Cliente atualizado: $classes" }
