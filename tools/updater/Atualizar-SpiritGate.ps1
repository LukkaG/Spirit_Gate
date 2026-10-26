param(
    [string]$InstallDir=$PSScriptRoot,
    [string]$LocalFeed,
    [switch]$NoLaunch,
    [switch]$FunctionsOnly
)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
function Hash-File([string]$Path) { (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }
function Write-Atomic([string]$Path,[string]$Text) {
    $temporary=$Path+'.'+[guid]::NewGuid().ToString('N')+'.tmp'
    [IO.File]::WriteAllText($temporary,$Text,(New-Object Text.UTF8Encoding($false)))
    try {
        if([IO.File]::Exists($Path)){
            $backup=$temporary+'.bak'
            [IO.File]::Replace($temporary,$Path,$backup)
            [IO.File]::Delete($backup)
        }
        else{[IO.File]::Move($temporary,$Path)}
    } finally {if([IO.File]::Exists($temporary)){[IO.File]::Delete($temporary)}}
}
function Check-Manifest($Manifest) {
    if($Manifest.schemaVersion -ne 1 -or $Manifest.version -notmatch '^[A-Za-z0-9._-]{1,80}$'){throw 'Manifesto incompativel'}
    $files=@($Manifest.files)
    if($files.Count -lt 1 -or $files.Count -gt 2000){throw 'Contagem de arquivos invalida'}
    $names=New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    [long]$total=0
    foreach($file in $files){
        $name=[string]$file.path
        if($name -notmatch '^[A-Za-z0-9_. /-]+$' -or $name.Contains('\')){throw 'Caminho de arquivo invalido'}
        foreach($part in ($name -split '/')){
            if(!$part -or $part -eq '.' -or $part -eq '..' -or $part.TrimEnd(' ','.') -ne $part -or $part -match '^(?i:CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])([.]|$)'){throw 'Caminho inseguro'}
        }
        if($name -match '^(?i:saves|data|versions|staging)(/|$)' -or $name -ieq 'installed-manifest.json' -or !$names.Add($name)){throw 'Caminho reservado ou duplicado'}
        if($file.sha256 -notmatch '^[a-f0-9]{64}$' -or $file.bytes -isnot [ValueType] -or [long]$file.bytes -lt 0 -or [long]$file.bytes -gt 134217728 -or [long]$file.bytes -ne $file.bytes){throw 'Hash/tamanho invalido'}
        $total += [long]$file.bytes
    }
    if($total -gt 536870912){throw 'Atualizacao excessiva'}
    foreach($required in @('runtime/bin/java.exe','app/spirit-gate-rpg.jar')){if(!$names.Contains($required)){throw "Arquivo essencial ausente: $required"}}
}
function Fetch-File([string]$Source,[string]$Destination,[long]$Limit,[bool]$Local) {
    if($Local){
        if((Get-Item -LiteralPath $Source).Length -gt $Limit){throw 'Arquivo excede limite'}
        [IO.File]::Copy($Source,$Destination,$false);return
    }
    $uri=[uri]$Source
    if($uri.Scheme -ne 'https' -or $uri.UserInfo){throw 'Canal remoto exige HTTPS sem credenciais no endereco'}
    [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12
    $request=[Net.WebRequest]::Create($uri);$request.Timeout=15000;$request.ReadWriteTimeout=15000
    $response=$null;$inputStream=$null;$outputStream=$null
    try {
        $response=$request.GetResponse()
        if($response.ResponseUri.Scheme -ne 'https' -or $response.ContentLength -gt $Limit){throw 'Resposta de atualizacao invalida'}
        $inputStream=$response.GetResponseStream();$outputStream=[IO.File]::Create($Destination)
        $buffer=New-Object byte[] 65536;[long]$received=0
        while(($count=$inputStream.Read($buffer,0,$buffer.Length)) -gt 0){
            $received+=$count;if($received -gt $Limit){throw 'Download excedeu tamanho permitido'}
            $outputStream.Write($buffer,0,$count)
        }
    } finally {
        if($outputStream){$outputStream.Dispose()};if($inputStream){$inputStream.Dispose()};if($response){$response.Dispose()}
    }
}
function Read-Installed([string]$Root,[string]$Pointer) {
    $pointerPath=Join-Path $Root $Pointer
    if(!(Test-Path -LiteralPath $pointerPath)){return $null}
    $id=[IO.File]::ReadAllText($pointerPath).Trim()
    if($id -notmatch '^[a-f0-9]{64}$'){throw 'Referencia de versao invalida'}
    $folder=Join-Path $Root "versions/$id";$manifestPath=Join-Path $folder 'installed-manifest.json'
    if((Hash-File $manifestPath) -ne $id){throw 'Manifesto instalado alterado'}
    $manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json;Check-Manifest $manifest
    foreach($file in $manifest.files){
        $path=Join-Path $folder $file.path;$item=Get-Item -LiteralPath $path
        if(($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -or $item.Length -ne $file.bytes -or (Hash-File $path) -ne $file.sha256){throw 'Instalacao incompleta ou alterada'}
    }
    return @{id=$id;folder=$folder;manifest=$manifest}
}
function Update-Installed([string]$Root,[string]$Feed,[bool]$Local,$Current) {
    $stage=Join-Path $Root ('staging/'+[guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $stage -Force | Out-Null
    try {
    $manifestPath=Join-Path $stage 'installed-manifest.json'
    Fetch-File $Feed $manifestPath 1048576 $Local
    $id=Hash-File $manifestPath
    $manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json;Check-Manifest $manifest
    if($Current -and $Current.id -eq $id){return $Current}
    if($Local){
        $base=Split-Path -Parent ([IO.Path]::GetFullPath($Feed))
        if($manifest.basePath -notmatch '^[A-Za-z0-9_-]+$'){throw 'basePath invalido'}
        $base=Join-Path $base $manifest.basePath
    } else {
        if($manifest.basePath -notmatch '^[A-Za-z0-9_-]+$'){throw 'basePath invalido'}
        $base=[uri]::new([uri]$Feed,$manifest.basePath+'/')
    }
    [int]$downloaded=0;[int]$reused=0
    foreach($file in $manifest.files){
        $destination=Join-Path $stage $file.path
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        $old=if($Current){Join-Path $Current.folder $file.path}else{$null}
        if($old -and (Test-Path -LiteralPath $old) -and (Hash-File $old) -eq $file.sha256){
            [IO.File]::Copy($old,$destination,$false);$reused++
        } else {
            $source=if($Local){Join-Path $base $file.path}else{[uri]::new($base,($file.path -split '/' | ForEach-Object {[uri]::EscapeDataString($_)}) -join '/').AbsoluteUri}
            Fetch-File $source $destination ([long]$file.bytes) $Local;$downloaded++
        }
        if((Get-Item -LiteralPath $destination).Length -ne $file.bytes -or (Hash-File $destination) -ne $file.sha256){throw "Integridade invalida: $($file.path)"}
    }
    $version=Join-Path $Root "versions/$id"
    # Each attempt has a unique staging directory. Existing versions are never overwritten.
    if(Test-Path -LiteralPath $version){throw 'Versao ja existe; preservada para diagnostico'}
    New-Item -ItemType Directory -Path (Join-Path $Root 'versions') -Force | Out-Null
    [IO.Directory]::Move($stage,$version)
    if($Current){Write-Atomic (Join-Path $Root 'previous.txt') $Current.id}
    Write-Atomic (Join-Path $Root 'active.txt') $id
    Write-Host "Versao $($manifest.version): $downloaded arquivos baixados, $reused reutilizados."
    return @{id=$id;folder=$version;manifest=$manifest}
    } finally {
        $resolved=[IO.Path]::GetFullPath($stage)
        $allowed=[IO.Path]::GetFullPath((Join-Path $Root 'staging'))+[IO.Path]::DirectorySeparatorChar
        if(!$resolved.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'Limpeza fora da pasta temporaria recusada'}
        if(Test-Path -LiteralPath $resolved){Remove-Item -LiteralPath $resolved -Recurse -Force}
    }
}
function Get-GameLaunch([string]$Root,[string]$VersionFolder) {
    $role='anfitriao'
    $configPath=Join-Path $Root 'atualizacoes.json'
    if(Test-Path -LiteralPath $configPath){
        $config=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
        if($config.PSObject.Properties.Name -contains 'role'){$role=[string]$config.role}
    }
    if($role -notin @('anfitriao','convidado')){throw 'Papel da instalacao invalido'}
    $app=Join-Path $VersionFolder 'SpiritGate.exe'
    if(!(Test-Path -LiteralPath $app)){$app=Join-Path $VersionFolder 'SpiritGateRPG.exe'}
    $data=Join-Path $Root 'data'
    $arguments=@('-Dspiritgate.rpg.experimentalSprites=true',"-Dspiritgate.rpg.dataDir=$data","-Djpackage.app-path=$app")
    if($role -eq 'convidado'){$arguments+='-Dspiritgate.guest=true'}
    $arguments+=@('-cp',(Join-Path $VersionFolder 'app/spirit-gate-rpg.jar'),'com.projetok.prototype.rpg.RpgLauncher')
    return @{java=(Join-Path $VersionFolder 'runtime/bin/java.exe');arguments=$arguments;data=$data;role=$role}
}
if($FunctionsOnly){return}
$root=[IO.Path]::GetFullPath($InstallDir)
New-Item -ItemType Directory -Path $root -Force | Out-Null
$lock=$null
try {
    try{$lock=[IO.File]::Open((Join-Path $root 'launcher.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)}
    catch{throw 'O jogo ou atualizador ja esta aberto nesta pasta'}
    $current=$null
    try{$current=Read-Installed $root 'active.txt'}catch{Write-Host 'Versao atual invalida; tentando anterior.'}
    if(!$current){try{$current=Read-Installed $root 'previous.txt'}catch{Write-Host 'Versao anterior indisponivel.'}}
    try {
        $feed=$LocalFeed
        if(!$feed){$config=Get-Content -LiteralPath (Join-Path $root 'atualizacoes.json') -Raw | ConvertFrom-Json;$feed=[string]$config.feedUrl}
        if($feed){$current=Update-Installed $root $feed ([bool]$LocalFeed) $current}
        else{Write-Host 'Canal ainda nao publicado; usando versao instalada.'}
    } catch {
        # Do not log remote URLs, tokens, or server response bodies.
        Write-Host 'Atualizacao indisponivel ou invalida; mantendo a versao instalada.'
        [IO.File]::WriteAllText((Join-Path $root 'ultima-atualizacao.log'),'Falha: '+$_.Exception.GetType().Name)
    }
    if(!$current){throw 'Nenhuma versao valida instalada. Extraia novamente o pacote inicial.'}
    if(!$NoLaunch){
        $launch=Get-GameLaunch $root $current.folder
        New-Item -ItemType Directory -Path $launch.data -Force | Out-Null
        Write-Host "Abrindo Spirit Gate $($current.manifest.version)..."
        $arguments=$launch.arguments
        & $launch.java @arguments
        if($LASTEXITCODE -ne 0){throw 'O jogo encerrou com erro; arquivos e saves foram preservados'}
    }
} finally {if($lock){$lock.Dispose()}}
