param([Parameter(Mandatory=$true)][string]$FixtureRoot,[ValidateSet('success','invalid-executable','exit-early','ack-failure')][string]$Case='success',[ValidateSet('anfitriao','convidado')][string]$Role='convidado')
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Windows.Forms
$fixture=[IO.Path]::GetFullPath($FixtureRoot)
New-Item -ItemType Directory -Path $fixture -Force | Out-Null
$version=Join-Path $fixture 'payload'
New-Item -ItemType Directory -Path (Join-Path $version 'runtime/bin'),(Join-Path $version 'app') -Force | Out-Null
$java=Join-Path $version 'runtime/bin/java.exe'
if($Case -eq 'invalid-executable'){
    [IO.File]::WriteAllText($java,'fixture invalida')
}else{
    $source=@'
using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
internal static class FixtureGame {
    private static void Main(string[] args) {
        string folder=Environment.GetEnvironmentVariable("SPIRITGATE_LAUNCH_FIXTURE");
        File.WriteAllLines(Path.Combine(folder,"argv.txt"),args);
        File.WriteAllText(Path.Combine(folder,"cwd.txt"),Environment.CurrentDirectory);
        File.WriteAllText(Path.Combine(folder,"pid.txt"),Process.GetCurrentProcess().Id.ToString());
        if(Environment.GetEnvironmentVariable("SPIRITGATE_LAUNCH_FIXTURE_MODE")!="exit-early") Thread.Sleep(6000);
        File.WriteAllText(Path.Combine(folder,"exited.txt"),"finished");
    }
}
'@
    Add-Type -TypeDefinition $source -OutputAssembly $java -OutputType ConsoleApplication
}
[IO.File]::WriteAllText((Join-Path $version 'app/spirit-gate-rpg.jar'),'jar fixture')
$files=@('runtime/bin/java.exe','app/spirit-gate-rpg.jar') | ForEach-Object {
    $file=Join-Path $version $_
    @{path=$_;bytes=(Get-Item -LiteralPath $file).Length;sha256=(Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()}
}
$manifest=@{schemaVersion=1;version='fixture-201';basePath='release-fixture';files=@($files)} | ConvertTo-Json -Depth 5
$manifestPath=Join-Path $version 'installed-manifest.json'
[IO.File]::WriteAllText($manifestPath,$manifest,(New-Object Text.UTF8Encoding($false)))
$identity=(Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash.ToLowerInvariant()
$versions=Join-Path $fixture 'versions';New-Item -ItemType Directory -Path $versions | Out-Null
Move-Item -LiteralPath $version -Destination (Join-Path $versions $identity)
[IO.File]::WriteAllText((Join-Path $fixture 'active.txt'),$identity)
[IO.File]::WriteAllText((Join-Path $fixture 'atualizacoes.json'),(@{feedUrl='';role=$Role} | ConvertTo-Json))
foreach($name in @('Launcher-SpiritGate.ps1','Atualizar-SpiritGate.ps1')){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $fixture $name)}
$env:SPIRITGATE_LAUNCH_FIXTURE=$fixture
$env:SPIRITGATE_LAUNCH_FIXTURE_MODE=$Case
if($Case -eq 'ack-failure'){
    . (Join-Path $fixture 'Atualizar-SpiritGate.ps1') -FunctionsOnly -InstallDir $fixture
    $current=Read-Installed $fixture 'active.txt'
    $launch=Get-GameLaunch $fixture $current.folder
    $peerFolder=Join-Path $fixture 'preexisting';New-Item -ItemType Directory -Path $peerFolder | Out-Null
    $env:SPIRITGATE_LAUNCH_FIXTURE=$peerFolder
    $peer=Start-GameProcess $launch $fixture
    $env:SPIRITGATE_LAUNCH_FIXTURE=$fixture
    # Injecao somente no teste: o mesmo helper de producao e chamado via FunctionsOnly.
    function Write-Atomic([string]$Path,[string]$Text){throw [IO.IOException]::new('fixture: falha de escrita do ACK')}
    $result=@{case=$Case;role=$Role;failureObserved=$false;newChildStopped=$false;preexistingChildPreserved=$false;lockHeldBeforeFailureReturned=$false}
    $ownedLock=[IO.File]::Open((Join-Path $fixture 'launcher.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try {
        try {Invoke-GameHandoff $launch $fixture (Join-Path $fixture ('.launch-ready-'+[guid]::NewGuid().ToString('N')+'.json'))}
        catch {
            if($_.Exception.Message -ne 'fixture: falha de escrita do ACK'){throw}
            $result.failureObserved=$true
            $newChildId=[int][IO.File]::ReadAllText((Join-Path $fixture 'pid.txt'))
            $child=$null
            try {$child=[Diagnostics.Process]::GetProcessById($newChildId);$result.newChildStopped=$child.HasExited}
            catch {$result.newChildStopped=$true}
            finally {if($child){$child.Dispose()}}
            $result.preexistingChildPreserved=!$peer.HasExited
            $probeLock=$null
            try {$probeLock=[IO.File]::Open((Join-Path $fixture 'launcher.lock'),[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)}
            catch {$result.lockHeldBeforeFailureReturned=$true}
            finally {if($probeLock){$probeLock.Dispose()}}
        }
    } finally {$ownedLock.Dispose();$peer.WaitForExit();$peer.Dispose()}
    $result.remainingLaunchSignals=@(Get-ChildItem -LiteralPath $fixture -Filter '.launch-ready-*.json').Count
    if(!$result.failureObserved -or !$result.newChildStopped -or !$result.preexistingChildPreserved -or !$result.lockHeldBeforeFailureReturned){throw 'Falha do ACK deixou processo abandonado ou atingiu processo anterior'}
    # Kill negado: mock complementar verifica espera e descarte somente apos a saida.
    $fake=[pscustomobject]@{Id=123;HasExited=$false;WaitCalls=0;KillAttempted=$false;DisposedAfterExit=$false}
    $fake | Add-Member -MemberType ScriptMethod -Name Kill -Value {$this.KillAttempted=$true;throw 'fixture: Kill negado'}
    $fake | Add-Member -MemberType ScriptMethod -Name WaitForExit -Value {param([int]$Milliseconds)$this.WaitCalls++;$this.HasExited=$true;return $true}
    $fake | Add-Member -MemberType ScriptMethod -Name Dispose -Value {$this.DisposedAfterExit=$this.HasExited}
    function Start-GameProcess($Launch,[string]$Root){return $fake}
    $failed=$false
    try {Invoke-GameHandoff $launch $fixture (Join-Path $fixture ('.launch-ready-'+[guid]::NewGuid().ToString('N')+'.json'))}
    catch {$failed=$_.Exception.Message -eq 'fixture: falha de escrita do ACK'}
    $result.killFailureWaited=$failed -and $fake.KillAttempted -and $fake.WaitCalls -gt 0 -and $fake.DisposedAfterExit
    if(!$result.killFailureWaited){throw 'Kill negado nao manteve supervisao ate a saida'}
    $result | ConvertTo-Json -Depth 5
    return
}
$global:handoffCase=$Case
$global:handoffState='waiting'
$global:handoffFailure=$null
$global:handoffRecovery=$null
$global:handoffStarted=[DateTime]::UtcNow
$driver=New-Object Windows.Forms.Timer
$driver.Interval=50
$driver.Add_Tick({
    try {
        if(([DateTime]::UtcNow-$global:handoffStarted).TotalSeconds -gt 25){throw 'Timeout do teste de abertura'}
        $fixtureWindow=@([Windows.Forms.Application]::OpenForms | Where-Object {$_.Text -eq 'Spirit Gate | Launcher'}) | Select-Object -First 1
        if(!$fixtureWindow){return}
        $fixturePlay=@($fixtureWindow.Controls | Where-Object {$_ -is [Windows.Forms.Button] -and $_.Text -eq 'JOGAR'}) | Select-Object -First 1
        $fixtureRetry=@($fixtureWindow.Controls | Where-Object {$_ -is [Windows.Forms.Button] -and $_.Text -eq 'Verificar atualizacoes'}) | Select-Object -First 1
        if($global:handoffState -eq 'waiting' -and $fixturePlay.Enabled){
            $global:handoffState='opening'
            $fixturePlay.PerformClick()
        }elseif($global:handoffState -eq 'opening' -and $global:handoffCase -ne 'success' -and $fixtureRetry.Enabled -and $fixturePlay.Enabled){
            $failureLabel=@($fixtureWindow.Controls | Where-Object {$_ -is [Windows.Forms.Label] -and $_.Text -match 'Nao foi possivel concluir|encerrou antes de confirmar'}) | Select-Object -First 1
            if(!$failureLabel){return}
            $global:handoffRecovery=@{visible=$fixtureWindow.Visible;playEnabled=$fixturePlay.Enabled;retryEnabled=$fixtureRetry.Enabled;status=$failureLabel.Text}
            $fixtureWindow.Close()
        }
    }catch{
        $global:handoffFailure=$_.Exception.Message
        # Somente este processo de teste; o helper tem vida limitada e nao e o jogo.
        [Environment]::Exit(2)
    }
})
$driver.Start()
try {& (Join-Path $fixture 'Launcher-SpiritGate.ps1') -InstallDir $fixture -NoDispatch}
finally {$driver.Stop();$driver.Dispose()}
if($global:handoffFailure){throw $global:handoffFailure}
$result=@{case=$Case;role=$Role;closed=$true;recovery=$global:handoffRecovery;remainingLaunchSignals=@(Get-ChildItem -LiteralPath $fixture -Filter '.launch-ready-*.json').Count}
if($Case -eq 'success'){
    $childId=[int][IO.File]::ReadAllText((Join-Path $fixture 'pid.txt'))
    $child=[Diagnostics.Process]::GetProcessById($childId)
    try {$result.childAlive=!$child.HasExited;$result.childPid=$childId}finally{$child.Dispose()}
    $gameLock=$null
    try {$gameLock=[IO.File]::Open((Join-Path $fixture 'launcher.lock'),[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None);$result.gameLockHeld=$false}
    catch {$result.gameLockHeld=$true}
    finally {if($gameLock){$gameLock.Dispose()}}
    $interfaceLock=[IO.File]::Open((Join-Path $fixture 'interface.lock'),[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    $interfaceLock.Dispose();$result.interfaceLockReleased=$true
    if(!$result.childAlive -or !$result.gameLockHeld){throw 'Launcher fechou sem manter o jogo independente e protegido'}
}elseif(!$result.recovery -or !$result.recovery.visible -or !$result.recovery.playEnabled -or !$result.recovery.retryEnabled){throw 'Falha de abertura nao preservou uma interface recuperavel'}
$result | ConvertTo-Json -Depth 5
