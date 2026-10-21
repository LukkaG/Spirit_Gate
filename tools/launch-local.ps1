[CmdletBinding()]
param(
    [ValidateSet('Rpg', 'Sprites', 'Studio', 'Map')][string]$Mode = 'Rpg',
    [string]$MapFolder = '',
    [switch]$Geometric,
    [switch]$BuildOnly
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
try {
    $project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    Set-Location -LiteralPath $project
    $build = & (Join-Path $PSScriptRoot 'compile-local.ps1') -PassThru
    if ($BuildOnly) { Write-Host '[Spirit Gate] Compilacao concluida.'; exit 0 }
    $javaArguments = @()
    switch ($Mode) {
        'Studio' { $entry = 'com.projetok.studio.WorldStudio'; $clientArguments = @('--project', $project) }
        'Map' {
            if (-not $MapFolder -or -not (Test-Path -LiteralPath $MapFolder -PathType Container)) {
                throw 'Informe a pasta de mapa exportada: testar-mapa-rpg.cmd "pasta-exportada"'
            }
            $entry = 'com.projetok.studio.StudioPlaytest'
            $clientArguments = @([IO.Path]::GetFullPath($MapFolder), $project)
        }
        default {
            $entry = 'com.projetok.prototype.rpg.RpgLauncher'
            $clientArguments = @()
            if ($Mode -eq 'Sprites' -and -not $Geometric) {
                $scale = if ($env:SPIRIT_GATE_SPRITE_SCALE) { $env:SPIRIT_GATE_SPRITE_SCALE } else { '0.5' }
                $frame = if ($env:SPIRIT_GATE_FRAME_MS) { $env:SPIRIT_GATE_FRAME_MS } else { '250' }
                $javaArguments += @("-Dspiritgate.rpg.spriteScale=$scale", "-Dspiritgate.rpg.spriteFrameMillis=$frame")
                $clientArguments = @('--experimental-player-sprites')
            }
        }
    }
    Write-Host '[Spirit Gate] Abrindo cliente...'
    & $build.Java @javaArguments -cp $build.Classes $entry @clientArguments
    if ($LASTEXITCODE -ne 0) { throw "O cliente terminou com codigo $LASTEXITCODE." }
    exit 0
} catch { Write-Host "[Spirit Gate] ERRO: $($_.Exception.Message)" -ForegroundColor Red; exit 1 }
