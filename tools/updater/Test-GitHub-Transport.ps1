param([string]$EvidenceDir)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1') -FunctionsOnly
if(!$EvidenceDir){$EvidenceDir=Join-Path ([IO.Path]::GetTempPath()) ('SpiritGate-github-test-'+[guid]::NewGuid().ToString('N'))}
$EvidenceDir=[IO.Path]::GetFullPath($EvidenceDir)
New-Item -ItemType Directory -Path $EvidenceDir -Force | Out-Null

# Somente simulacao de HTTPS: os dominios GitHub abaixo sao interceptados
# neste processo, sem acessar a rede ou alterar validacao de certificados.
Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Cache;

public static class GitHubTransportFixture {
    public sealed class Plan {
        public byte[] Data;
        public Uri ResponseUri;
        public bool Unavailable;
    }
    public sealed class Seen {
        public string Url;
        public RequestCacheLevel? CacheLevel;
        public string CacheControl;
        public string Authorization;
        public string Cookie;
    }
    public static readonly Dictionary<string,Plan> Plans = new Dictionary<string,Plan>();
    public static readonly List<Seen> Requests = new List<Seen>();
    public static void Add(string url, byte[] data, string finalUrl) {
        Plans[url]=new Plan {Data=data,ResponseUri=new Uri(String.IsNullOrEmpty(finalUrl)?url:finalUrl)};
    }
    public sealed class Factory : IWebRequestCreate {
        public WebRequest Create(Uri uri) {return new Request(uri);}
    }
    public sealed class Request : WebRequest {
        private readonly Uri uri;
        private WebHeaderCollection headers=new WebHeaderCollection();
        private RequestCachePolicy policy;
        private int timeout;
        public int ReadWriteTimeout {get;set;}
        public Request(Uri value){uri=value;}
        public override WebHeaderCollection Headers {get{return headers;}set{headers=value;}}
        public override RequestCachePolicy CachePolicy {get{return policy;}set{policy=value;}}
        public override int Timeout {get{return timeout;}set{timeout=value;}}
        public override WebResponse GetResponse() {
            Requests.Add(new Seen {Url=uri.AbsoluteUri,CacheLevel=policy==null?(RequestCacheLevel?)null:policy.Level,
                CacheControl=headers[HttpRequestHeader.CacheControl],Authorization=headers[HttpRequestHeader.Authorization],Cookie=headers[HttpRequestHeader.Cookie]});
            Plan plan;
            if(!Plans.TryGetValue(uri.AbsoluteUri,out plan)||plan.Unavailable)throw new WebException("Fixture GitHub unavailable");
            return new Response(plan);
        }
    }
    public sealed class Response : WebResponse {
        private readonly Plan plan;
        public Response(Plan value){plan=value;}
        public override long ContentLength {get{return plan.Data.LongLength;}set{throw new NotSupportedException();}}
        public override Uri ResponseUri {get{return plan.ResponseUri;}}
        public override Stream GetResponseStream(){return new MemoryStream(plan.Data,false);}
        public override void Close(){}
    }
}
'@
foreach($prefix in @('https://raw.githubusercontent.com/','https://github.com/')){
    if(![Net.WebRequest]::RegisterPrefix($prefix,(New-Object GitHubTransportFixture+Factory))){throw 'Prefixo de teste ja registrado'}
}
$checks=New-Object 'Collections.Generic.List[string]'
function Assert-True([bool]$Condition,[string]$Name){if(!$Condition){throw "FAIL: $Name"};$checks.Add($Name)}
function Must-Reject([scriptblock]$Block,[string]$Name){$rejected=$false;try{& $Block}catch{$rejected=$true};Assert-True $rejected $Name}
function Bytes([string]$Text){return [Text.Encoding]::UTF8.GetBytes($Text)}
function Byte-Hash([byte[]]$Data){$sha=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($sha.ComputeHash($Data))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}}

$base='https://raw.githubusercontent.com/LukkaG/Spirit_Gate/updates/'
$feed=$base+'latest.json'
function Make-GitHubFeed([string]$Release,[string]$Game){
    $files=@()
    foreach($name in @('runtime/bin/java.exe','app/spirit-gate-rpg.jar','assets/dev/player-base/fixture frame.png','launcher/Launcher-SpiritGate.ps1','launcher/Atualizar-SpiritGate.ps1')){
        $data=Bytes $(if($name -eq 'app/spirit-gate-rpg.jar'){$Game}else{'unchanged fixture'})
        $files+=@{path=$name;bytes=$data.Length;sha256=(Byte-Hash $data)}
        $url=$base+$Release+'/'+(($name -split '/' | ForEach-Object {[uri]::EscapeDataString($_)}) -join '/')
        [GitHubTransportFixture]::Add($url,$data,'')
    }
    $raw=Bytes (@{schemaVersion=1;version=$Release;basePath=$Release;files=$files} | ConvertTo-Json -Depth 8)
    [GitHubTransportFixture]::Add($feed,$raw,'')
    return $raw
}
$install=Join-Path $EvidenceDir 'install'
New-Item -ItemType Directory -Path $install -Force | Out-Null
$configPath=Join-Path $install 'atualizacoes.json'
[IO.File]::WriteAllText($configPath,(@{feedUrl=$feed;role='convidado'} | ConvertTo-Json))
$configHash=Hash-File $configPath
$save=Join-Path $install 'data/saves/github-sentinel.txt'
New-Item -ItemType Directory -Path (Split-Path -Parent $save) -Force | Out-Null
[IO.File]::WriteAllText($save,'personal save must remain unchanged');$saveHash=Hash-File $save

$firstManifest=Make-GitHubFeed 'release-github-a' 'first game'
$current=Update-Installed $install $feed $false $null
Assert-True ((Read-Installed $install 'active.txt').id -eq $current.id) 'Canonical raw GitHub URL installs verified payload'
Assert-True ([GitHubTransportFixture]::Requests.Count -eq 6) 'Cold install fetches one manifest and all five files'
$manifestRequest=[GitHubTransportFixture]::Requests[0]
Assert-True ($manifestRequest.Url -eq $feed -and $manifestRequest.CacheLevel -eq [Net.Cache.RequestCacheLevel]::Reload -and $manifestRequest.CacheControl -eq 'no-cache') 'Mutable GitHub manifest explicitly revalidates cache'
Assert-True (@([GitHubTransportFixture]::Requests | Where-Object {$_.Url.Contains('fixture%20frame.png')}).Count -eq 1) 'File names with spaces retain URL encoding under the raw branch path'
Assert-True (@([GitHubTransportFixture]::Requests | Where-Object {$_.Url -ne $feed -and ($null -ne $_.CacheLevel -or $_.CacheControl)}).Count -eq 0) 'Hashed release payload keeps normal cache policy'
Assert-True (@([GitHubTransportFixture]::Requests | Where-Object {$_.Authorization -or $_.Cookie}).Count -eq 0) 'Public GitHub downloads do not embed user authentication or cookies'
Assert-True (@([GitHubTransportFixture]::Requests | Where-Object {([uri]$_.Url).Host -ne 'raw.githubusercontent.com'}).Count -eq 0) 'GitHub workflow never requests another hosting provider'
Assert-True ((Hash-File (Join-Path $current.folder 'launcher/Launcher-SpiritGate.ps1')) -eq (Byte-Hash (Bytes 'unchanged fixture'))) 'Versioned launcher files are delivered by the same GitHub channel'

$firstId=$current.id
$secondManifest=Make-GitHubFeed 'release-github-b' 'second game'
# Ausencia de todos os arquivos iguais no falso servidor prova reutilizacao local.
foreach($url in @([GitHubTransportFixture]::Plans.Keys)){
    if($url.StartsWith($base+'release-github-b/') -and !$url.EndsWith('/app/spirit-gate-rpg.jar')){[GitHubTransportFixture]::Plans.Remove($url) | Out-Null}
}
$before=[GitHubTransportFixture]::Requests.Count
$current=Update-Installed $install $feed $false $current
Assert-True ([GitHubTransportFixture]::Requests.Count-$before -eq 2 -and $current.id -ne $firstId) 'Next GitHub update fetches only latest manifest and changed game file'
Assert-True ((Hash-File $configPath) -eq $configHash -and (Hash-File $save) -eq $saveHash) 'GitHub updates preserve feed, guest role and personal saves'
Assert-True ((Get-GameLaunch $install $current.folder).arguments -contains '-Dspiritgate.guest=true') 'Guest role still reaches the Java launcher after GitHub update'

$secondId=$current.id
[GitHubTransportFixture]::Plans[$feed].Unavailable=$true
Must-Reject {Update-Installed $install $feed $false $current} 'Unavailable GitHub channel reports update failure'
Assert-True ((Read-Installed $install 'active.txt').id -eq $secondId) 'Unavailable channel leaves installed active game intact'
Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $install 'staging')).Count -eq 0) 'Unavailable channel removes temporary staging'

# Redirecionamentos de manifesto nao podem mudar implicitamente a base do payload.
$alias='https://github.com/LukkaG/Spirit_Gate/raw/refs/heads/updates/latest.json'
[GitHubTransportFixture]::Add($alias,$secondManifest,$feed)
Must-Reject {Fetch-File $alias (Join-Path $EvidenceDir 'alias-manifest.tmp') 1048576 $false $true} 'GitHub web raw alias redirect requires configuring canonical raw URL'
Assert-True (!(Test-Path -LiteralPath (Join-Path $EvidenceDir 'alias-manifest.tmp'))) 'Redirected manifest is rejected before writing payload metadata'
Assert-True ((Read-Installed $install 'active.txt').id -eq $secondId) 'Alias redirect failure never changes active version'

$asset=$base+'release-github-b/app/spirit-gate-rpg.jar'
$redirectBytes=Bytes 'HTTPS redirected payload'
[GitHubTransportFixture]::Add($asset,$redirectBytes,'https://objects.githubusercontent.com/fixture/game.jar')
$redirected=Join-Path $EvidenceDir 'redirected-payload.tmp'
Fetch-File $asset $redirected 1024 $false
Assert-True ((Hash-File $redirected) -eq (Byte-Hash $redirectBytes)) 'HTTPS payload redirect to GitHub object storage stays supported'

# A release anterior ja instalada pode ser reativada sem reenviar nenhum payload.
foreach($url in @([GitHubTransportFixture]::Plans.Keys)){
    if($url.StartsWith($base+'release-github-a/')){[GitHubTransportFixture]::Plans.Remove($url) | Out-Null}
}
[GitHubTransportFixture]::Add($feed,$firstManifest,'')
$before=[GitHubTransportFixture]::Requests.Count
$current=Update-Installed $install $feed $false $current
Assert-True ($current.id -eq $firstId -and [GitHubTransportFixture]::Requests.Count-$before -eq 1) 'GitHub rollback reuses verified installed release after fetching only manifest'
Assert-True ((Read-Installed $install 'previous.txt').id -eq $secondId) 'Rollback preserves replaced GitHub release as previous'
Assert-True ((Hash-File $configPath) -eq $configHash -and (Hash-File $save) -eq $saveHash) 'All GitHub scenarios preserve local settings and saves'

@{passed=$checks.Count;transport='simulated HTTPS WebRequest; no network or TLS bypass';feedUrl=$feed;checks=@($checks)} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $EvidenceDir 'results.json') -Encoding UTF8
Write-Host "PASS: $($checks.Count) GitHub checks"
