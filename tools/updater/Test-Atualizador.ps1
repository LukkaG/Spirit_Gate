param([string]$EvidenceDir)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1') -FunctionsOnly
if(!$EvidenceDir){$EvidenceDir=Join-Path ([IO.Path]::GetTempPath()) ('SpiritGate-updater-test-'+[guid]::NewGuid().ToString('N'))}
$EvidenceDir=[IO.Path]::GetFullPath($EvidenceDir)
New-Item -ItemType Directory -Path $EvidenceDir -Force | Out-Null
$root=Join-Path $EvidenceDir 'install';$feedRoot=Join-Path $EvidenceDir 'feed'
New-Item -ItemType Directory -Path $root,$feedRoot | Out-Null
$checks=New-Object 'Collections.Generic.List[string]'
function Assert-True([bool]$Condition,[string]$Name){if(!$Condition){throw "FAIL: $Name"};$checks.Add($Name)}
function Must-Reject([scriptblock]$Block,[string]$Name){$rejected=$false;try{& $Block}catch{$rejected=$true};Assert-True $rejected $Name}
function Make-Feed([string]$Release,[string]$Payload){
    $folder=Join-Path $feedRoot $Release;New-Item -ItemType Directory -Path $folder | Out-Null
    $files=@()
    foreach($name in @('runtime/bin/java.exe','app/spirit-gate-rpg.jar','assets/dev/player-base/test.png')){
        $path=Join-Path $folder $name;New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null
        [IO.File]::WriteAllText($path,$(if($name -eq 'app/spirit-gate-rpg.jar'){$Payload}else{'same'}))
        $files+=@{path=$name;bytes=(Get-Item -LiteralPath $path).Length;sha256=(Hash-File $path)}
    }
    $manifest=@{schemaVersion=1;version=$Release;basePath=$Release;files=$files}
    $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $feedRoot 'latest.json') -Encoding UTF8
}
Make-Feed 'v1' 'first'
$feed=Join-Path $feedRoot 'latest.json'
$current=Update-Installed $root $feed $true $null
Assert-True ((Read-Installed $root 'active.txt').id -eq $current.id) 'Initial install and full verification'
$data=Join-Path $root 'data/saves/characters';New-Item -ItemType Directory -Path $data -Force | Out-Null
$save=Join-Path $data 'personal.properties';[IO.File]::WriteAllText($save,'do not modify');$saveHash=Hash-File $save
$oldId=$current.id
Make-Feed 'v2' 'second'
# Deleting unchanged files from the source proves they are reused from the verified installation.
[IO.File]::Delete((Join-Path $feedRoot 'v2/runtime/bin/java.exe'))
[IO.File]::Delete((Join-Path $feedRoot 'v2/assets/dev/player-base/test.png'))
$current=Update-Installed $root $feed $true $current
Assert-True ($oldId -ne $current.id -and (Read-Installed $root 'previous.txt').id -eq $oldId) 'Incremental update and previous version retained'
Assert-True ((Hash-File $save) -eq $saveHash) 'Saves unchanged outside versions'
$same=Update-Installed $root $feed $true $current
Assert-True ($same.id -eq $current.id) 'Unchanged feed is a no-op'
Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $root 'staging')).Count -eq 0) 'Staging cleaned after successful and no-op updates'
Make-Feed 'v3' 'third'
[IO.File]::WriteAllText((Join-Path $feedRoot 'v3/app/spirit-gate-rpg.jar'),'broken')
Must-Reject {Update-Installed $root $feed $true $current} 'Wrong download hash rejected'
Assert-True ((Read-Installed $root 'active.txt').id -eq $current.id) 'Failed update does not activate partial version'
Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $root 'staging')).Count -eq 0) 'Staging cleaned after failed download'
$manifest=Get-Content -LiteralPath $feed -Raw | ConvertFrom-Json
$original=$manifest.files[0].path
foreach($bad in @('../outside','/absolute','data/saves/test','saves/test','CON.txt','app/../evil','app/space.','app\evil','installed-manifest.json')){
    $manifest.files[0].path=$bad;Must-Reject {Check-Manifest $manifest} "Unsafe path rejected: $bad"
}
$manifest.files[0].path=$original
$manifest.files[0].sha256='bad';Must-Reject {Check-Manifest $manifest} 'Invalid hash rejected'
$manifest.files[0].sha256=('a'*64);$manifest.files[0].bytes=134217729
Must-Reject {Check-Manifest $manifest} 'Excessive download rejected'
$manifest.files[0].bytes=4;$manifest.files[1].path=$manifest.files[0].path.ToUpperInvariant()
Must-Reject {Check-Manifest $manifest} 'Case-insensitive duplicate rejected'
$dummy=Join-Path $EvidenceDir 'remote.tmp'
Must-Reject {Fetch-File 'http://example.invalid/feed' $dummy 1024 $false} 'Remote HTTP rejected without a network call'
$activePath=Join-Path $root 'active.txt';$activeBefore=[IO.File]::ReadAllText($activePath)
& powershell.exe -NoProfile -File (Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1') -InstallDir $root -LocalFeed (Join-Path $feedRoot 'absent.json') -NoLaunch
Assert-True ($LASTEXITCODE -eq 0 -and [IO.File]::ReadAllText($activePath) -eq $activeBefore) 'Unavailable feed keeps installed game'
Write-Atomic $activePath 'invalid-pointer'
& powershell.exe -NoProfile -File (Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1') -InstallDir $root -LocalFeed (Join-Path $feedRoot 'absent.json') -NoLaunch
Assert-True ($LASTEXITCODE -eq 0) 'Corrupt active pointer falls back to verified previous version'
Write-Atomic $activePath $activeBefore
$lock=[IO.File]::Open((Join-Path $root 'launcher.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try {
    $ErrorActionPreference='Continue'
    & powershell.exe -NoProfile -File (Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1') -InstallDir $root -LocalFeed $feed -NoLaunch 2> (Join-Path $EvidenceDir 'expected-lock-error.log')
    $ErrorActionPreference='Stop'
    Assert-True ($LASTEXITCODE -ne 0) 'Concurrent updater rejected'
} finally {$lock.Dispose()}
Assert-True ((Hash-File $save) -eq $saveHash) 'All failure scenarios preserve saves'
$legacyLaunch=Get-GameLaunch $root $current.folder
Assert-True ($legacyLaunch.role -eq 'anfitriao' -and $legacyLaunch.arguments -contains 'com.projetok.prototype.rpg.RpgLauncher') 'Legacy install opens the shared launcher by default'
$configPath=Join-Path $root 'atualizacoes.json'
[IO.File]::WriteAllText($configPath,'{"feedUrl":"","role":"convidado"}')
$guestLaunch=Get-GameLaunch $root $current.folder
Assert-True ($guestLaunch.arguments -contains '-Dspiritgate.guest=true') 'Guest role is passed explicitly to Java'
Assert-True ($guestLaunch.arguments -contains "-Dspiritgate.rpg.dataDir=$(Join-Path $root 'data')") 'Persistent data stays outside the active version'
Assert-True ($guestLaunch.arguments -contains (Join-Path $current.folder 'app/spirit-gate-rpg.jar')) 'Historical JAR path stays compatible'
Make-Feed 'v4' 'role-preserved'
$current=Update-Installed $root $feed $true $current
Assert-True ((Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json).role -eq 'convidado') 'Game updates preserve installation role'
[IO.File]::WriteAllText($configPath,'{"feedUrl":"","role":"invalid"}')
Must-Reject {Get-GameLaunch $root $current.folder} 'Invalid installation role rejected'
[IO.File]::WriteAllText($configPath,'{"feedUrl":"","role":"anfitriao"}')
Assert-True ((Get-GameLaunch $root $current.folder).arguments -notcontains '-Dspiritgate.guest=true') 'Host retains offline and hosting options'
@{passed=$checks.Count;checks=@($checks)} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $EvidenceDir 'results.json') -Encoding UTF8
Write-Host "PASS: $($checks.Count) checks"
