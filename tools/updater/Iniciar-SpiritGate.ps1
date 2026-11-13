param([string]$InstallDir=$PSScriptRoot,[string]$LocalFeed,[switch]$SmokeTest,[switch]$ResolveOnly)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($InstallDir)
$engine=Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1'
. $engine -FunctionsOnly -InstallDir $root -LocalFeed $LocalFeed
$current=$null
try{$current=Read-Installed $root 'active.txt'}catch{}
if(!$current){try{$current=Read-Installed $root 'previous.txt'}catch{}}
$ui=Join-Path $PSScriptRoot 'Launcher-SpiritGate.ps1'
if($current){
    $names=@($current.manifest.files | ForEach-Object {[string]$_.path})
    if($names -contains 'launcher/Launcher-SpiritGate.ps1' -and $names -contains 'launcher/Atualizar-SpiritGate.ps1'){
        $ui=Join-Path $current.folder 'launcher/Launcher-SpiritGate.ps1'
    }
}
if($ResolveOnly){Write-Output $ui;return}
$arguments=@('-NoProfile','-STA','-ExecutionPolicy','Bypass','-File',$ui,'-InstallDir',$root,'-NoDispatch')
if($LocalFeed){$arguments+=@('-LocalFeed',$LocalFeed)}
if($SmokeTest){$arguments+='-SmokeTest'}
& (Join-Path $PSHOME 'powershell.exe') @arguments
exit $LASTEXITCODE
