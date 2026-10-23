param([Parameter(Mandatory=$true)][string]$Image,[Parameter(Mandatory=$true)][string]$Evidence)
$ErrorActionPreference='Stop'
& python (Join-Path $PSScriptRoot 'smoke_private_multiplayer.py') $Image $Evidence
if($LASTEXITCODE -ne 0){throw 'Validacao multiplayer nativo falhou; consulte os logs preservados'}
