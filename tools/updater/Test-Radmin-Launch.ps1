param([string]$InstallDir,[string]$ProbeClasses,[string]$OutputDir)
$ErrorActionPreference='Stop'
. (Join-Path $InstallDir 'Atualizar-SpiritGate.ps1') -FunctionsOnly
$current=Read-Installed $InstallDir 'active.txt'
$launch=Get-GameLaunch $InstallDir $current.folder
New-Item -ItemType Directory -Path $launch.data -Force | Out-Null
$arguments=@($launch.arguments)
$classpathIndex=[Array]::IndexOf($arguments,'-cp')+1
$arguments[$classpathIndex]+=[IO.Path]::PathSeparator+$ProbeClasses
$arguments[$arguments.Length-1]='UpdatableRadminUiProbe'
$env:SPIRITGATE_PROBE_OUTPUT=$OutputDir
$java=$launch.java
& $java @arguments
if($LASTEXITCODE -ne 0){throw 'Radmin UI probe failed'}
