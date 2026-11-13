<# Inicia, consulta ou solicita a parada do monitor local de erros do Spirit Gate. #>
[CmdletBinding()]
param(
    [ValidateSet('Iniciar', 'Status', 'Parar')][string]$Acao = 'Iniciar',
    [switch]$SomenteAvisar
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$script = Join-Path $PSScriptRoot 'monitorar_erros.py'
$monitorFolder = Join-Path $project 'logs\monitor'
$launchFile = Join-Path $monitorFolder 'iniciador.json'
$statusFile = Join-Path $monitorFolder 'status.json'
$stopFile = Join-Path $monitorFolder 'parar'

# Confere tambem a linha de comando para nao confundir um PID reutilizado.
function Get-SpiritGateMonitor {
    if (-not (Test-Path -LiteralPath $launchFile -PathType Leaf)) { return $null }
    $launch = Get-Content -LiteralPath $launchFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $([int]$launch.pid)"
    if ($process -and $process.CommandLine -and
        $process.CommandLine.IndexOf($script, [StringComparison]::OrdinalIgnoreCase) -ge 0 -and
        $process.CommandLine.IndexOf($project, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        return $process
    }
    return $null
}

$running = Get-SpiritGateMonitor
switch ($Acao) {
    'Status' {
        if ($running) { Write-Host "[Spirit Gate] Monitor ativo, PID $($running.ProcessId)." }
        else { Write-Host '[Spirit Gate] Monitor iniciado por este atalho esta parado.' }
        if (Test-Path -LiteralPath $statusFile -PathType Leaf) {
            Get-Content -LiteralPath $statusFile -Raw -Encoding UTF8
        }
    }
    'Parar' {
        if (-not $running) { Write-Host '[Spirit Gate] Nenhum monitor deste atalho esta ativo.'; break }
        [IO.File]::WriteAllText($stopFile, 'parar', (New-Object Text.UTF8Encoding($false)))
        Write-Host '[Spirit Gate] Parada solicitada. O monitor terminara depois do atendimento em andamento.'
    }
    'Iniciar' {
        if ($running) { Write-Host "[Spirit Gate] Monitor ja ativo, PID $($running.ProcessId)."; break }
        $python = Get-Command python.exe -ErrorAction SilentlyContinue
        if (-not $python) { throw 'Python nao encontrado no PATH. Nenhuma ferramenta foi instalada.' }
        New-Item -ItemType Directory -Path $monitorFolder -Force | Out-Null
        if (Test-Path -LiteralPath $stopFile -PathType Leaf) { Remove-Item -LiteralPath $stopFile }
        $stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
        $arguments = @('-u', ('"' + $script + '"'), '--root', ('"' + $project + '"'))
        if (-not $SomenteAvisar) { $arguments += '--codex' }
        $process = Start-Process -FilePath $python.Source -ArgumentList $arguments -WorkingDirectory $project `
            -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $monitorFolder "inicializacao-$stamp-saida.log") `
            -RedirectStandardError (Join-Path $monitorFolder "inicializacao-$stamp-erros.log")
        $launch = [ordered]@{ pid = $process.Id; iniciado = [DateTimeOffset]::Now.ToString('o'); automatico = -not $SomenteAvisar }
        [IO.File]::WriteAllText($launchFile, ($launch | ConvertTo-Json), (New-Object Text.UTF8Encoding($false)))
        Start-Sleep -Milliseconds 800
        $process.Refresh()
        if ($process.HasExited) {
            throw "Monitor encerrou com codigo $($process.ExitCode). Consulte logs\monitor\inicializacao-$stamp-erros.log."
        }
        Write-Host "[Spirit Gate] Monitor iniciado, PID $($process.Id). Relatorios em logs\erros; respostas em logs\analises."
    }
}
