param([Parameter(Mandatory=$true)][string]$Image,[Parameter(Mandatory=$true)][string]$Evidence)
$ErrorActionPreference='Stop'
$Image=[IO.Path]::GetFullPath($Image)
$Evidence=[IO.Path]::GetFullPath($Evidence)
New-Item -ItemType Directory -Path $Evidence -Force | Out-Null
$executable=Join-Path $Image 'SpiritGateWorldStudio.exe'
& (Join-Path $Image 'runtime/bin/java.exe') -cp (Join-Path $Image 'app/world-studio.jar') com.projetok.prototype.rpg.RpgLauncher --verify-install (Join-Path $Evidence 'package-self-check.json')
if($LASTEXITCODE -ne 0){throw 'Self-check do runtime integrado falhou'}
$launcher=Start-Process -FilePath $executable -WorkingDirectory $Image -WindowStyle Hidden -PassThru
$window=$null
for($attempt=0;$attempt -lt 100;$attempt++){
    Start-Sleep -Milliseconds 150
    foreach($process in @(Get-Process -Name SpiritGateWorldStudio -ErrorAction SilentlyContinue)){
        if($process -and $process.Path -eq $executable -and $process.MainWindowHandle -ne 0){$window=$process;break}
    }
    if($window){break}
}
if(-not $window){throw 'Janela do executavel ou processo filho nao apareceu no prazo'}
$title=$window.MainWindowTitle
$closeAccepted=$window.CloseMainWindow()
$closed=$window.WaitForExit(15000)
$parentClosed=$launcher.WaitForExit(15000)
$result=[ordered]@{startup=$true;title=$title;childPid=$window.Id;closeRequested=$closeAccepted;exited=$closed;launcherExited=$parentClosed;exitCode=$(if($parentClosed){$launcher.ExitCode}else{$null});physicalUiApproved=$false}
$result | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $Evidence 'package-smoke.json')
if(-not $closed -or -not $parentClosed -or $launcher.ExitCode -ne 0){throw 'Encerramento do pacote falhou'}
$result | ConvertTo-Json
