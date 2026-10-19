<#
.SYNOPSIS
Valida o ZIP portatil e abre o executavel extraido fora da pasta de fontes.
.DESCRIPTION
Nao precisa de Java ou Maven no PATH. Confere estrutura, arquitetura e ponto de
entrada; captura a janela real e fecha somente o processo iniciado pelo teste.
Uma execucao local nao substitui a homologacao em outro Windows sem Java.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ZipPath,
    [string]$OutputDirectory,
    [string]$EvidencePath,
    [string[]]$LauncherArguments = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Drawing
if (-not ('SpiritGatePortableSmoke.Native' -as [type])) {
    Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Runtime.InteropServices;
namespace SpiritGatePortableSmoke {
    public static class Native {
        [StructLayout(LayoutKind.Sequential)]
        public struct RECT { public int Left, Top, Right, Bottom; }
        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
        private struct PROCESSENTRY32 {
            public uint dwSize, cntUsage, th32ProcessID;
            public IntPtr th32DefaultHeapID;
            public uint th32ModuleID, cntThreads, th32ParentProcessID;
            public int pcPriClassBase;
            public uint dwFlags;
            [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 260)] public string szExeFile;
        }
        [DllImport("kernel32.dll", SetLastError = true)] private static extern IntPtr CreateToolhelp32Snapshot(uint flags, uint process);
        [DllImport("kernel32.dll", CharSet = CharSet.Auto)] private static extern bool Process32First(IntPtr snapshot, ref PROCESSENTRY32 entry);
        [DllImport("kernel32.dll", CharSet = CharSet.Auto)] private static extern bool Process32Next(IntPtr snapshot, ref PROCESSENTRY32 entry);
        [DllImport("kernel32.dll")] private static extern bool CloseHandle(IntPtr handle);
        [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr window, out RECT rect);
        [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr window, IntPtr target, uint flags);
        [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr window, uint message, IntPtr w, IntPtr l);
        public static int[] DescendantIds(int root) {
            IntPtr snapshot = CreateToolhelp32Snapshot(2, 0);
            if (snapshot == new IntPtr(-1)) throw new Win32Exception();
            var parents = new Dictionary<int, int>();
            try {
                var entry = new PROCESSENTRY32();
                entry.dwSize = (uint)Marshal.SizeOf(typeof(PROCESSENTRY32));
                if (Process32First(snapshot, ref entry)) do {
                    parents[(int)entry.th32ProcessID] = (int)entry.th32ParentProcessID;
                } while (Process32Next(snapshot, ref entry));
            } finally { CloseHandle(snapshot); }
            var owned = new HashSet<int>();
            owned.Add(root);
            bool changed;
            do {
                changed = false;
                foreach (var pair in parents)
                    if (owned.Contains(pair.Value) && owned.Add(pair.Key)) changed = true;
            } while (changed);
            owned.Remove(root);
            var result = new int[owned.Count];
            owned.CopyTo(result);
            Array.Sort(result);
            return result;
        }
    }
}
'@
}

# --- Destino novo: o teste nao apaga nem sobrescreve pastas existentes. ---
$projectDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$workspaceDirectory = [IO.Path]::GetDirectoryName($projectDirectory)
$archive = (Resolve-Path -LiteralPath $ZipPath).Path
if (-not $OutputDirectory) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $OutputDirectory = Join-Path $workspaceDirectory ("Validacao Portatil\$stamp-" + [Guid]::NewGuid().ToString('N').Substring(0, 8))
}
$destination = [IO.Path]::GetFullPath($OutputDirectory)
if ($destination.Equals($projectDirectory, [StringComparison]::OrdinalIgnoreCase) -or
        $destination.StartsWith($projectDirectory + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'A extracao de verificacao deve ficar fora da pasta Projeto-K.'
}
if (Test-Path -LiteralPath $destination) { throw "O destino ja existe: $destination" }
if ($destination -notmatch ' ') { throw 'Use um destino com espacos para validar caminhos portateis.' }
if (-not $EvidencePath) { $EvidencePath = Join-Path $destination 'smoke-portable.json' }
$evidenceFile = [IO.Path]::GetFullPath($EvidencePath)

$result = [ordered]@{
    status = 'EXECUCAO LOCAL EM ANDAMENTO'
    startedAt = [DateTimeOffset]::Now.ToString('o')
    zip = $archive
    zipSha256 = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    extractionDirectory = $destination
    environment = [Environment]::OSVersion.VersionString
    cleanWindowsWithoutJava = 'PENDENTE DE HOMOLOGACAO HUMANA'
}
$launched = $null
$ownedProcesses = @{}
$oldPath = $env:PATH
$oldJavaHome = $env:JAVA_HOME

try {
    # --- Validacao das entradas antes da extracao (inclusive caminhos relativos). ---
    $zip = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        $seenEntries = @{}
        foreach ($entry in $zip.Entries) {
            # FullName e consultado antes de qualquer substituicao/extracao.
            $rawName = $entry.FullName
            if ($rawName.Contains('\') -or $rawName -match '(^/|:|\x00|(^|/)\.{1,2}(/|$)|//|[<>"|?*])' -or
                $rawName -match '(^|/)(CON|PRN|AUX|NUL|CONIN\$|CONOUT\$|COM[1-9\u00b9\u00b2\u00b3]|LPT[1-9\u00b9\u00b2\u00b3])(\.|/|$)' -or
                $rawName -match '[ .](/|$)' -or $seenEntries.ContainsKey($rawName.TrimEnd('/')) -or
                (($entry.ExternalAttributes -shr 16) -band 0xF000) -eq 0xA000) {
                throw "Nome original inseguro no ZIP: $rawName"
            }
            $seenEntries[$rawName.TrimEnd('/')] = $true
            $relative = $entry.FullName.Replace('/', [IO.Path]::DirectorySeparatorChar)
            $expanded = [IO.Path]::GetFullPath((Join-Path $destination $relative))
            if (-not $expanded.StartsWith($destination + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
                throw "Entrada fora da pasta de extracao: $($entry.FullName)"
            }
            if ($entry.FullName -match '(^|/)(\.env([^/]*|$)|\.git(/|$)|target(/|$)|[^/]*\.(pem|key|p12|pfx)$)') {
                throw "Entrada privada ou de desenvolvimento no pacote: $($entry.FullName)"
            }
        }
        $result.archiveEntries = $zip.Entries.Count
    } finally { $zip.Dispose() }
    [IO.Directory]::CreateDirectory($destination) | Out-Null
    [IO.Compression.ZipFile]::ExtractToDirectory($archive, $destination)
    $executables = @(Get-ChildItem -LiteralPath $destination -Filter 'SpiritGate.exe' -File -Recurse)
    if ($executables.Count -ne 1) { throw 'Esperado exatamente um SpiritGate.exe no ZIP.' }
    $exe = $executables[0].FullName
    $appDirectory = $executables[0].Directory.FullName
    $configuration = Join-Path $appDirectory 'app\SpiritGate.cfg'
    $jvm = Join-Path $appDirectory 'runtime\bin\server\jvm.dll'
    $release = Join-Path $appDirectory 'runtime\release'
    foreach ($required in @($configuration, $jvm, $release)) {
        if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "Arquivo obrigatorio ausente: $required" }
    }
    $cfg = Get-Content -LiteralPath $configuration -Raw
    if ($cfg -notmatch '(?m)^app\.mainclass=com\.projetok\.prototype\.DebugLauncher\s*$') {
        throw 'O app-image nao aponta para DebugLauncher.'
    }
    $releaseText = Get-Content -LiteralPath $release -Raw
    if ($releaseText -notmatch '(?m)^JAVA_VERSION="17(?:\.|\")') { throw 'O runtime integrado deve ser Java 17.' }
    $reader = New-Object IO.BinaryReader([IO.File]::OpenRead($exe))
    try {
        $reader.BaseStream.Position = 0x3c
        $reader.BaseStream.Position = $reader.ReadInt32()
        if ($reader.ReadUInt32() -ne 0x00004550 -or $reader.ReadUInt16() -ne 0x8664) {
            throw 'O executavel nao e um PE Windows x64.'
        }
    } finally { $reader.Dispose() }
    $result.launcher = $exe
    $result.launcherArchitecture = 'PE x64 (0x8664)'
    $result.mainClass = 'com.projetok.prototype.DebugLauncher'
    $result.runtimeRelease = $releaseText.Trim()
    $result.configuration = $cfg.Trim()

    # --- O manifest cobre cada arquivo distribuido, inclusive o inventario textual. ---
    $manifestPath = Join-Path $appDirectory 'manifest.json'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) { throw 'manifest.json ausente.' }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.schemaVersion -ne 1) { throw 'Schema de manifest desconhecido.' }
    if ($manifest.entrypoint -ne 'com.projetok.prototype.DebugLauncher') { throw 'Ponto de entrada inesperado no manifest.' }
    if ($manifest.mainJar -ne 'app/spirit-gate-arena.jar') { throw 'JAR principal inesperado no manifest.' }
    if ($manifest.javaModules -notcontains 'jdk.random' -or $releaseText -notmatch '(?m)^MODULES="[^"]*\bjdk\.random\b') {
        throw 'O runtime precisa do modulo jdk.random para o provedor de RandomGenerator.'
    }
    $listed = @{}
    foreach ($item in $manifest.files) {
        $relative = $item.path.Replace('/', [IO.Path]::DirectorySeparatorChar)
        $filePath = [IO.Path]::GetFullPath((Join-Path $appDirectory $relative))
        if (-not $filePath.StartsWith($appDirectory + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Manifest referencia arquivo externo: $($item.path)"
        }
        if ($listed.ContainsKey($filePath)) { throw "Arquivo duplicado no manifest: $($item.path)" }
        $listed[$filePath] = $true
        $file = Get-Item -LiteralPath $filePath
        if ($file.Length -ne $item.sizeBytes) { throw "Tamanho divergente: $($item.path)" }
        if ((Get-FileHash -LiteralPath $filePath -Algorithm SHA256).Hash -ne $item.sha256) {
            throw "SHA-256 divergente: $($item.path)"
        }
    }
    foreach ($file in Get-ChildItem -LiteralPath $appDirectory -File -Recurse) {
        if ($file.FullName -ne $manifestPath -and -not $listed.ContainsKey($file.FullName)) {
            throw "Arquivo nao inventariado: $($file.FullName)"
        }
    }
    $sidecar = $archive + '.sha256'
    if (-not (Test-Path -LiteralPath $sidecar -PathType Leaf)) { throw 'SHA-256 externo do ZIP ausente.' }
    $expectedZipHash = ((Get-Content -LiteralPath $sidecar -Raw).Trim() -split '\s+')[0]
    if ($expectedZipHash -ne $result.zipSha256) { throw 'SHA-256 externo divergente do ZIP.' }
    $result.manifestFilesVerified = $listed.Count
    $result.packageVersion = $manifest.version
    $result.javaModules = $manifest.javaModules
    $result.manifestSha256 = (Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash
    $result.zipSidecarVerified = $true

    # --- O teste usa a JVM integrada, sem Java/Maven do ambiente de desenvolvimento. ---
    $env:PATH = "$env:SystemRoot\System32;$env:SystemRoot"
    $env:JAVA_HOME = Join-Path $destination 'java-externo-ausente'
    $launchOptions = @{ FilePath=$exe; WorkingDirectory=$destination; WindowStyle='Hidden'; PassThru=$true }
    if ($LauncherArguments.Count -gt 0) { $launchOptions.ArgumentList = $LauncherArguments }
    $result.launcherArguments = @($LauncherArguments)
    $launched = Start-Process @launchOptions
    $ownedProcesses[$launched.Id] = $launched
    $result.processId = $launched.Id
    $window = [IntPtr]::Zero
    $windowProcess = $null
    $deadline = [DateTime]::UtcNow.AddSeconds(30)
    while ([DateTime]::UtcNow -lt $deadline) {
        foreach ($childId in [SpiritGatePortableSmoke.Native]::DescendantIds($launched.Id)) {
            if (-not $ownedProcesses.ContainsKey($childId)) {
                try { $ownedProcesses[$childId] = [Diagnostics.Process]::GetProcessById($childId) }
                catch [ArgumentException] { } # O filho pode encerrar entre snapshot e leitura.
            }
        }
        foreach ($candidate in $ownedProcesses.Values) {
            $candidate.Refresh()
            if (-not $candidate.HasExited -and $candidate.MainWindowHandle -ne [IntPtr]::Zero -and
                    $candidate.MainWindowTitle -like '*Spirit Gate*') {
                $window = $candidate.MainWindowHandle
                $windowProcess = $candidate
                break
            }
        }
        if ($window -ne [IntPtr]::Zero) { break }
        $launched.Refresh()
        if ($launched.HasExited -and $ownedProcesses.Count -eq 1) { throw "O launcher encerrou antes de criar janela (codigo $($launched.ExitCode))." }
        Start-Sleep -Milliseconds 200
    }
    if ($window -eq [IntPtr]::Zero) { throw 'O executavel nao apresentou janela em 30 segundos.' }
    $result.windowTitle = $windowProcess.MainWindowTitle
    $result.windowProcessId = $windowProcess.Id
    $result.ownedProcessIds = @($ownedProcesses.Keys | Sort-Object)
    # Preserva o handle antes do encerramento para obter ExitCode do filho nativo.
    [void]$windowProcess.Handle
    # A DLL carregada comprova o runtime usado, alem do PATH reduzido do teste.
    $loadedModules = $null
    try { $loadedModules = @($windowProcess.Modules) }
    catch {
        $result.bundledJvmVerified = $false
        $result.loadedJvmInspection = 'INDISPONIVEL: ' + $_.Exception.Message
    }
    if ($null -ne $loadedModules) {
        $jvmModules = @($loadedModules | Where-Object { $_.ModuleName -ieq 'jvm.dll' })
        if ($jvmModules.Count -ne 1) { throw 'Esperada exatamente uma jvm.dll no processo da janela.' }
        $loadedJvm = [IO.Path]::GetFullPath($jvmModules[0].FileName)
        $bundledRuntime = [IO.Path]::GetFullPath((Join-Path $appDirectory 'runtime')) + [IO.Path]::DirectorySeparatorChar
        if (-not $loadedJvm.StartsWith($bundledRuntime, [StringComparison]::OrdinalIgnoreCase)) {
            throw "O processo carregou uma JVM fora do app-image: $loadedJvm"
        }
        $result.loadedJvmPath = $loadedJvm
        $result.bundledJvmVerified = $true
    }
    Start-Sleep -Milliseconds 900

    # --- Captura da propria janela, sem controlar outras aplicacoes. ---
    $rect = New-Object SpiritGatePortableSmoke.Native+RECT
    if (-not [SpiritGatePortableSmoke.Native]::GetWindowRect($window, [ref]$rect)) { throw 'Nao foi possivel obter as dimensoes da janela.' }
    $bitmap = New-Object Drawing.Bitmap(($rect.Right - $rect.Left), ($rect.Bottom - $rect.Top))
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    try {
        $dc = $graphics.GetHdc()
        try { $captured = [SpiritGatePortableSmoke.Native]::PrintWindow($window, $dc, 0) }
        finally { $graphics.ReleaseHdc($dc) }
        if (-not $captured) { throw 'PrintWindow nao conseguiu capturar o app-image.' }
        $screenshot = Join-Path $destination 'SpiritGate-exe-smoke.png'
        $bitmap.Save($screenshot, [Drawing.Imaging.ImageFormat]::Png)
        $result.screenshot = $screenshot
        $result.windowSize = "$($bitmap.Width)x$($bitmap.Height)"
    } finally { $graphics.Dispose(); $bitmap.Dispose() }

    # --- Observacao objetiva do processo aberto; nao afirma inspecao de trafego historico. ---
    try {
        $tcp = @(Get-NetTCPConnection -ErrorAction Stop | Where-Object { $ownedProcesses.ContainsKey([int]$_.OwningProcess) })
        $udp = @(Get-NetUDPEndpoint -ErrorAction Stop | Where-Object { $ownedProcesses.ContainsKey([int]$_.OwningProcess) })
        $result.tcpConnectionsObserved = $tcp.Count
        $result.udpEndpointsObserved = $udp.Count
        if ($tcp.Count -ne 0 -or $udp.Count -ne 0) { throw 'Foram observados sockets pertencentes ao launcher local.' }
    } catch {
        if ($_.Exception.Message -like 'Foram observados sockets*') { throw }
        $result.networkInspection = 'INDISPONIVEL: ' + $_.Exception.Message
    }
    $result.pathDuringLaunch = $env:PATH
    [SpiritGatePortableSmoke.Native]::PostMessage($window, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null
    if (-not $windowProcess.WaitForExit(10000)) { throw 'O processo da janela nao encerrou normalmente depois de WM_CLOSE.' }
    if (-not $launched.WaitForExit(10000)) { throw 'A janela nao encerrou normalmente depois de WM_CLOSE.' }
    $result.windowExitCode = $windowProcess.ExitCode
    $result.exitCode = $launched.ExitCode
    if ($launched.ExitCode -ne 0 -or $windowProcess.ExitCode -ne 0) { throw "Saida nao-zero ao fechar a janela: $($launched.ExitCode)/$($windowProcess.ExitCode)" }
    $result.status = 'EXECUCAO LOCAL CONFERIDA'
} catch {
    $result.status = 'FALHA'
    $result.error = $_.Exception.Message
    throw
} finally {
    $env:PATH = $oldPath
    $env:JAVA_HOME = $oldJavaHome
    $allExited = $true
    foreach ($owned in @($ownedProcesses.Values)) {
        $owned.Refresh()
        if (-not $owned.HasExited) {
            Stop-Process -Id $owned.Id -Force
            $owned.WaitForExit(5000) | Out-Null
            $result.forcedClose = $true
        }
        $allExited = $allExited -and $owned.HasExited
        $owned.Dispose()
    }
    $result.processExited = $allExited
    $result.finishedAt = [DateTimeOffset]::Now.ToString('o')
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($evidenceFile)) | Out-Null
    $result | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $evidenceFile -Encoding UTF8
}
Write-Output "EXECUCAO LOCAL CONFERIDA: $evidenceFile"
