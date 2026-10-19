param([Parameter(Mandatory=$true)][string]$OfflineDirectory,[Parameter(Mandatory=$true)][string]$CoopDirectory)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'));$docs=Join-Path $project 'docs'
$offline=[IO.Path]::GetFullPath($OfflineDirectory);$coop=[IO.Path]::GetFullPath($CoopDirectory)
foreach($directory in @($offline,$coop)){if(-not $directory.StartsWith((Join-Path $project 'dist')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Smoke exige pacote extraido dentro de dist'}}
$rpgCapture=Join-Path $docs 'TASK-076-rpg-desktop.png'
$server=$null;$first=$null;$second=$null
try{
    # A janela do jogo e interativa; o smoke captura seus componentes reais.
    $rpg=Start-Process -FilePath (Join-Path $offline 'SpiritGateRPG.exe') -ArgumentList @('--smoke','"Teste Offline"',('"'+$rpgCapture+'"')) -WindowStyle Normal -PassThru
    $null=$rpg.Handle
    if(-not $rpg.WaitForExit(15000)){Stop-Process -Id $rpg.Id;throw 'RPG nao encerrou smoke'}
    $offlineEvidence=Get-Content -LiteralPath ($rpgCapture+'.json') -Raw | ConvertFrom-Json
    if(-not $offlineEvidence.windowShowing -or $offlineEvidence.ticks -le 0){throw 'Janela RPG nao comprovada'}
    $stdout=Join-Path $docs 'TASK-076-server-smoke.log';$stderr=Join-Path $docs 'TASK-076-server-smoke-error.log'
    $server=Start-Process -FilePath (Join-Path $coop 'SpiritGateCoopServer.exe') -ArgumentList @('--port','0','--run-seconds','12') -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $null=$server.Handle
    $port=$null
    for($i=0;$i -lt 50;$i++){if(Test-Path -LiteralPath $stdout){$text=Get-Content -LiteralPath $stdout -Raw;if($text -match '127[.]0[.]0[.]1:(\d+)'){$port=$Matches[1];break}};Start-Sleep -Milliseconds 100}
    if(-not $port){throw 'Servidor nao informou bind localhost'}
    $java=Join-Path $coop 'runtime\bin\java.exe';$jar=Join-Path $coop 'app\spirit-gate-coop.jar'
    $captureOne=Join-Path $docs 'TASK-076-coop-um-components.png';$captureTwo=Join-Path $docs 'TASK-076-coop-dois-components.png'
    $first=Start-Process -FilePath $java -ArgumentList @('-cp',('"'+$jar+'"'),'com.projetok.multiplayer.NetworkClientLauncher','127.0.0.1',$port,'"Coop Um"',('"'+$captureOne+'"')) -WindowStyle Hidden -PassThru
    $null=$first.Handle
    $second=Start-Process -FilePath $java -ArgumentList @('-cp',('"'+$jar+'"'),'com.projetok.multiplayer.NetworkClientLauncher','127.0.0.1',$port,'"Coop Dois"',('"'+$captureTwo+'"')) -WindowStyle Hidden -PassThru
    $null=$second.Handle
    if(-not $first.WaitForExit(10000)-or -not $second.WaitForExit(10000)){throw 'Clientes nao encerraram smoke'}
    $one=Get-Content -LiteralPath ($captureOne+'.json') -Raw | ConvertFrom-Json;$two=Get-Content -LiteralPath ($captureTwo+'.json') -Raw | ConvertFrom-Json
    if(-not $one.windowShowing -or -not $two.windowShowing -or $one.players -ne 2 -or $two.players -ne 2 -or $one.session -eq $two.session){throw 'Duas sessoes visuais nao comprovadas'}
    if(-not $server.WaitForExit(15000)){throw 'Servidor nao encerrou automaticamente'}
    if($server.ExitCode -ne 0 -or $first.ExitCode -ne 0 -or $second.ExitCode -ne 0){throw "Processo de smoke falhou: server=$($server.ExitCode), one=$($first.ExitCode), two=$($second.ExitCode)"}
    [ordered]@{schemaVersion=1;offlineExecutable=$true;offline=$offlineEvidence;serverBind='127.0.0.1';serverPort=[int]$port;serverExitCode=$server.ExitCode;clients=@($one,$two);integratedJava=$true;captureKind='componentes Swing de janelas em execucao';humanApproved=$false;lanBetweenMachines=$false;automatedApproved=$true} | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath (Join-Path $docs 'TASK-076-smoke.json') -Encoding UTF8
}finally{foreach($owned in @($server,$first,$second)){if($null -ne $owned -and -not $owned.HasExited){Stop-Process -Id $owned.Id}}}
