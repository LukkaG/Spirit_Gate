param([string]$InstallDir,[string]$FeedUrl,[switch]$NoGui)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$allowed=@('Iniciar-SpiritGate.ps1','Launcher-SpiritGate.ps1','Atualizar-SpiritGate.ps1','Jogar-SpiritGate.cmd','SpiritGate-Launcher.exe')
function Hash-File([string]$Path){return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()}
function Assert-Regular([string]$Path,[bool]$Directory=$false){
    $item=Get-Item -LiteralPath $Path -Force
    if(($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -or $item.PSIsContainer -ne $Directory){throw 'Arquivo ou pasta invalida; links nao sao aceitos.'}
}
function Check-Feed([string]$Value){
    $uri=$null
    if(!$Value -or $Value -match '[\s\\]' -or ![uri]::TryCreate($Value,[UriKind]::Absolute,[ref]$uri) -or
        $uri.Scheme -ne 'https' -or !$uri.Host -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.Port -ne 443){
        throw 'O canal deve ser uma URL HTTPS publica, sem usuario, senha, query ou fragmento.'
    }
    return $uri.AbsoluteUri
}
function Write-Atomic([string]$Path,[string]$Text){
    $temporary=$Path+'.'+[guid]::NewGuid().ToString('N')+'.tmp'
    $previous=$temporary+'.bak'
    [IO.File]::WriteAllText($temporary,$Text,(New-Object Text.UTF8Encoding($false)))
    try{[IO.File]::Replace($temporary,$Path,$previous)}finally{
        if([IO.File]::Exists($temporary)){[IO.File]::Delete($temporary)}
        if([IO.File]::Exists($previous)){[IO.File]::Delete($previous)}
    }
}
function Message([string]$Text,[bool]$Failure=$false){
    if($NoGui){Write-Host $Text;return}
    Add-Type -AssemblyName System.Windows.Forms
    $icon=if($Failure){[Windows.Forms.MessageBoxIcon]::Error}else{[Windows.Forms.MessageBoxIcon]::Information}
    [void][Windows.Forms.MessageBox]::Show($Text,'Spirit Gate - ativar atualizacoes',[Windows.Forms.MessageBoxButtons]::OK,$icon)
}
$locks=New-Object 'Collections.Generic.List[IDisposable]'
$backup=$null;$stage=$null;$changed=New-Object 'Collections.Generic.List[string]'
$original=@{};$root=$null
try{
    Assert-Regular $PSScriptRoot $true
    $payload=Join-Path $PSScriptRoot 'payload'
    Assert-Regular $payload $true
    $manifestPath=Join-Path $PSScriptRoot 'manifest.json';Assert-Regular $manifestPath
    $manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if($manifest.schemaVersion -ne 1 -or @($manifest.files).Count -ne $allowed.Count){throw 'Manifesto do kit incompativel.'}
    $names=New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    foreach($file in $manifest.files){
        $name=[string]$file.path
        if($name -notin $allowed -or !$names.Add($name) -or $file.sha256 -notmatch '^[a-f0-9]{64}$' -or
            $file.bytes -isnot [ValueType] -or [long]$file.bytes -lt 1 -or [long]$file.bytes -gt 5242880){throw 'Arquivo inesperado no kit.'}
        $source=Join-Path $payload $name;Assert-Regular $source
        if((Get-Item -LiteralPath $source).Length -ne $file.bytes -or (Hash-File $source) -ne $file.sha256){throw 'Kit alterado ou incompleto. Baixe novamente.'}
    }
    if(@(Get-ChildItem -LiteralPath $payload -Force).Count -ne $allowed.Count){throw 'Kit possui arquivos extras no payload.'}
    if(!$FeedUrl){
        $channelPath=Join-Path $PSScriptRoot 'canal.json';Assert-Regular $channelPath
        $channel=Get-Content -LiteralPath $channelPath -Raw | ConvertFrom-Json
        $FeedUrl=[string]$channel.feedUrl
    }
    if(!$FeedUrl){
        if($NoGui){throw 'Informe -FeedUrl para ativar o canal.'}
        Add-Type -AssemblyName Microsoft.VisualBasic
        $FeedUrl=[Microsoft.VisualBasic.Interaction]::InputBox('Cole a URL HTTPS publica do canal latest.json.','Spirit Gate - canal de atualizacoes','')
        if(!$FeedUrl){return}
    }
    $FeedUrl=Check-Feed $FeedUrl
    if(!$InstallDir){
        if($NoGui){throw 'Informe -InstallDir com a instalacao existente.'}
        Add-Type -AssemblyName System.Windows.Forms
        $dialog=New-Object Windows.Forms.FolderBrowserDialog
        $dialog.Description='Selecione a pasta que contem SpiritGate-Launcher.exe e atualizacoes.json.'
        $dialog.ShowNewFolderButton=$false
        try{if($dialog.ShowDialog() -ne [Windows.Forms.DialogResult]::OK){return};$InstallDir=$dialog.SelectedPath}finally{$dialog.Dispose()}
    }
    $root=[IO.Path]::GetFullPath($InstallDir)
    Assert-Regular $root $true
    Assert-Regular (Join-Path $root 'versions') $true
    $configPath=Join-Path $root 'atualizacoes.json';Assert-Regular $configPath
    if((Get-Item -LiteralPath $configPath).Length -gt 1048576){throw 'Configuracao da instalacao excessiva.'}
    $config=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
    if($null -eq $config -or $config -is [Array]){throw 'Configuracao invalida.'}
    if($config.PSObject.Properties.Name -contains 'role' -and $config.role -notin @('anfitriao','convidado')){throw 'Papel da instalacao invalido.'}
    $installed=$false
    foreach($pointer in @('active.txt','previous.txt')){
        $path=Join-Path $root $pointer
        if(Test-Path -LiteralPath $path){
            Assert-Regular $path;$identity=[IO.File]::ReadAllText($path).Trim()
            if($identity -match '^[a-f0-9]{64}$' -and (Test-Path -LiteralPath (Join-Path $root ('versions/'+$identity)) -PathType Container)){$installed=$true}
        }
    }
    if(!$installed){throw 'Selecione uma instalacao atualizavel com versao instalada valida.'}
    foreach($name in @('launcher.lock','interface.lock')){
        $path=Join-Path $root $name
        if(Test-Path -LiteralPath $path){Assert-Regular $path}
        try{$locks.Add([IO.File]::Open($path,[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None))}
        catch{throw 'Feche o jogo e o launcher dessa pasta antes de ativar as atualizacoes.'}
    }
    $stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
    $backupRoot=Join-Path $root 'launcher-backups'
    if(Test-Path -LiteralPath $backupRoot){Assert-Regular $backupRoot $true}
    else{New-Item -ItemType Directory -Path $backupRoot | Out-Null}
    $backup=Join-Path $backupRoot ('ativacao-'+$stamp)
    New-Item -ItemType Directory -Path $backup | Out-Null
    foreach($name in @($allowed)+@('atualizacoes.json')){
        $target=Join-Path $root $name
        $exists=Test-Path -LiteralPath $target
        if($exists){Assert-Regular $target;[IO.File]::Copy($target,(Join-Path $backup $name),$false)}
        $original[$name]=[bool]$exists
    }
    [IO.File]::WriteAllText((Join-Path $backup 'arquivos-originais.json'),($original | ConvertTo-Json))
    $stage=Join-Path $root ('.activation-stage-'+$stamp)
    New-Item -ItemType Directory -Path $stage | Out-Null
    foreach($file in $manifest.files){
        $name=[string]$file.path;$temporary=Join-Path $stage $name
        [IO.File]::Copy((Join-Path $payload $name),$temporary,$false)
        if((Hash-File $temporary) -ne $file.sha256){throw 'Falha ao preparar novo launcher.'}
    }
    # A configuracao e ativada por ultimo; nenhuma versao, save ou runtime e substituido.
    foreach($file in $manifest.files){
        $name=[string]$file.path;$changed.Add($name)
        [IO.File]::Copy((Join-Path $stage $name),(Join-Path $root $name),$true)
        if((Hash-File (Join-Path $root $name)) -ne $file.sha256){throw 'Falha ao conferir novo launcher.'}
    }
    if($config.PSObject.Properties.Name -contains 'feedUrl'){$config.feedUrl=$FeedUrl}
    else{$config | Add-Member -NotePropertyName feedUrl -NotePropertyValue $FeedUrl}
    $changed.Add('atualizacoes.json')
    Write-Atomic $configPath ($config | ConvertTo-Json -Depth 32)
    Message 'Atualizacoes ativadas. Abra SpiritGate-Launcher.exe nessa mesma pasta. Saves, personagens e versoes foram preservados.'
    Write-Output ('Ativacao concluida. Backup: '+$backup)
}catch{
    $failure=$_.Exception.Message
    $rollbackFailed=$false
    if($backup -and $root){
        foreach($name in $changed){
            try{
                $target=Join-Path $root $name
                if($original[$name]){
                    $saved=Join-Path $backup $name
                    if(!(Test-Path -LiteralPath $target) -or (Hash-File $target) -ne (Hash-File $saved)){[IO.File]::Copy($saved,$target,$true)}
                }elseif(Test-Path -LiteralPath $target){[IO.File]::Delete($target)}
            }catch{$rollbackFailed=$true}
        }
    }
    $text='Ativacao nao concluida: '+$failure
    if($rollbackFailed){$text+=' Feche os processos e restaure os arquivos da pasta de backup: '+$backup}
    elseif($changed.Count -gt 0){$text+=' Os arquivos anteriores foram restaurados.'}
    Message $text $true
    exit 1
}finally{
    foreach($handle in $locks){$handle.Dispose()}
    if($stage -and $root){
        $resolved=[IO.Path]::GetFullPath($stage)
        if($resolved.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -and
            [IO.Path]::GetFileName($resolved).StartsWith('.activation-stage-') -and (Test-Path -LiteralPath $resolved)){
            Remove-Item -LiteralPath $resolved -Recurse -Force
        }
    }
}
