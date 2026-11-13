<# Registra uma falha dos scripts locais, sem impedir a exibicao original no console. #>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Contexto,
    [Parameter(Mandatory = $true)][string]$Detalhes
)
try {
    $project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $folder = Join-Path $project 'logs\erros'
    New-Item -ItemType Directory -Path $folder -Force | Out-Null
    $name = 'script-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '-' + [Guid]::NewGuid().ToString('N')
    $temporary = Join-Path $folder ($name + '.tmp')
    $destination = Join-Path $folder ($name + '.txt')
    $report = "Spirit Gate - falha local`nData: $([DateTimeOffset]::Now.ToString('o'))`nOrigem: $Contexto`nPID: $PID`n`n$Detalhes`n"
    [IO.File]::WriteAllText($temporary, $report, (New-Object Text.UTF8Encoding($false)))
    Move-Item -LiteralPath $temporary -Destination $destination
    Write-Host "[Spirit Gate] Relatorio de erro: $destination"
} catch {
    Write-Warning "Nao foi possivel gravar o relatorio local: $($_.Exception.Message)"
}
