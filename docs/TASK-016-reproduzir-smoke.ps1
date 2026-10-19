param([Parameter(Mandatory=$true)][string]$ZipPath)
$ErrorActionPreference = 'Stop'
# Bypass apenas no processo que executa este script; nenhuma politica global alterada.
& "$PSScriptRoot\..\tools\test-windows-portable.ps1" -ZipPath $ZipPath -EvidencePath "$PSScriptRoot\TASK-016-portable-smoke.json"
& "$PSScriptRoot\..\tools\test-windows-portable.ps1" -ZipPath $ZipPath -EvidencePath "$PSScriptRoot\TASK-016-map-smoke.json" -LauncherArguments @('--map','SpiritGate/mapa-demo.properties')
