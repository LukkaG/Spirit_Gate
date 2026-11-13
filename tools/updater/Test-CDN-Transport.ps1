param([string]$EvidenceDir)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1') -FunctionsOnly
if(!$EvidenceDir){$EvidenceDir=Join-Path ([IO.Path]::GetTempPath()) ('SpiritGate-cdn-test-'+[guid]::NewGuid().ToString('N'))}
$EvidenceDir=[IO.Path]::GetFullPath($EvidenceDir)
New-Item -ItemType Directory -Path $EvidenceDir -Force | Out-Null

# Transporte simulado, sem acessar rede ou relaxar certificados/TLS.
Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Cache;

public static class CdnFixture {
    public sealed class Plan {
        public byte[] Data;
        public Uri ResponseUri;
        public long Length;
        public bool Timeout;
    }
    public sealed class Seen {
        public string Url;
        public RequestCacheLevel? CacheLevel;
        public string CacheControl;
        public string Pragma;
        public int Timeout;
        public int ReadWriteTimeout;
    }
    public static readonly Dictionary<string,Plan> Plans = new Dictionary<string,Plan>();
    public static readonly List<Seen> Requests = new List<Seen>();
    public static void Add(string url, byte[] data, string finalUrl, long length) {
        Plans[url] = new Plan {Data=data,ResponseUri=new Uri(String.IsNullOrEmpty(finalUrl)?url:finalUrl),Length=length == -2?data.LongLength:length};
    }
    public sealed class Factory : IWebRequestCreate {
        public WebRequest Create(Uri uri) {return new Request(uri);}
    }
    public sealed class Request : WebRequest {
        private readonly Uri uri;
        private WebHeaderCollection headers = new WebHeaderCollection();
        private RequestCachePolicy policy;
        private int timeout;
        public int ReadWriteTimeout { get; set; }
        public Request(Uri value) {uri=value;}
        public override WebHeaderCollection Headers {get {return headers;} set {headers=value;}}
        public override RequestCachePolicy CachePolicy {get {return policy;} set {policy=value;}}
        public override int Timeout {get {return timeout;} set {timeout=value;}}
        public override WebResponse GetResponse() {
            Requests.Add(new Seen {Url=uri.AbsoluteUri,CacheLevel=policy == null?(RequestCacheLevel?)null:policy.Level,
                CacheControl=headers[HttpRequestHeader.CacheControl],Pragma=headers[HttpRequestHeader.Pragma],Timeout=timeout,ReadWriteTimeout=ReadWriteTimeout});
            Plan plan;
            if(!Plans.TryGetValue(uri.AbsoluteUri,out plan)) throw new WebException("Fixture URL not configured");
            if(plan.Timeout)throw new WebException("Fixture timeout",WebExceptionStatus.Timeout);
            return new Response(plan);
        }
    }
    public sealed class Response : WebResponse {
        private readonly Plan plan;
        public Response(Plan value) {plan=value;}
        public override long ContentLength {get {return plan.Length;} set {throw new NotSupportedException();}}
        public override Uri ResponseUri {get {return plan.ResponseUri;}}
        public override Stream GetResponseStream() {return new MemoryStream(plan.Data,false);}
        public override void Close() {}
    }
}
'@
if(![Net.WebRequest]::RegisterPrefix('https://cdn-test.invalid/',(New-Object CdnFixture+Factory))){throw 'Prefixo de teste ja registrado'}
$checks=New-Object 'Collections.Generic.List[string]'
function Assert-True([bool]$Condition,[string]$Name){if(!$Condition){throw "FAIL: $Name"};$checks.Add($Name)}
function Must-Reject([scriptblock]$Block,[string]$Name){$rejected=$false;try{& $Block}catch{$rejected=$true};Assert-True $rejected $Name}
function Bytes([string]$Text){return [Text.Encoding]::UTF8.GetBytes($Text)}
function Byte-Hash([byte[]]$Data){$sha=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($sha.ComputeHash($Data))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}}
function Destination([string]$Name){return Join-Path $EvidenceDir $Name}

$sample=Bytes 'cdn-payload'
$url='https://cdn-test.invalid/latest.json'
[CdnFixture]::Add($url,$sample,'',-2)
Fetch-File $url (Destination 'fresh.tmp') 1024 $false $true
$seen=[CdnFixture]::Requests[[CdnFixture]::Requests.Count-1]
Assert-True ($seen.CacheLevel -eq [Net.Cache.RequestCacheLevel]::Reload -and $seen.CacheControl -eq 'no-cache' -and $seen.Pragma -eq 'no-cache') 'Mutable manifest requests force cache revalidation'
Assert-True ($seen.Timeout -eq 15000 -and $seen.ReadWriteTimeout -eq 15000) 'Network request and stream timeout bounds retained'
Assert-True ((Hash-File (Destination 'fresh.tmp')) -eq (Byte-Hash $sample)) 'HTTPS fixture bytes are preserved'

$asset='https://cdn-test.invalid/release-immutable/app/game.jar'
[CdnFixture]::Add($asset,$sample,'',-2)
Fetch-File $asset (Destination 'asset.tmp') 1024 $false
$seen=[CdnFixture]::Requests[[CdnFixture]::Requests.Count-1]
Assert-True ($null -eq $seen.CacheLevel -and !$seen.CacheControl -and !$seen.Pragma) 'Immutable payload fetch leaves normal cache policy intact'

[CdnFixture]::Add($url,$sample,'https://cdn-test.invalid/moved/latest.json',-2)
Must-Reject {Fetch-File $url (Destination 'redirected-manifest.tmp') 1024 $false $true} 'Manifest redirects are refused rather than changing payload base silently'
Assert-True (!(Test-Path -LiteralPath (Destination 'redirected-manifest.tmp'))) 'Manifest redirect is rejected before writing a destination'
[CdnFixture]::Add($asset,$sample,'https://mirror.example.invalid/immutable/game.jar',-2)
Fetch-File $asset (Destination 'redirected-asset.tmp') 1024 $false
Assert-True ((Hash-File (Destination 'redirected-asset.tmp')) -eq (Byte-Hash $sample)) 'Payload HTTPS redirect remains supported'
[CdnFixture]::Add($asset,$sample,'http://cdn-test.invalid/game.jar',-2)
Must-Reject {Fetch-File $asset (Destination 'downgraded.tmp') 1024 $false} 'Redirect to HTTP is refused'
[CdnFixture]::Add($asset,$sample,'https://user:secret@cdn-test.invalid/game.jar',-2)
Must-Reject {Fetch-File $asset (Destination 'credentialed.tmp') 1024 $false} 'Redirect with URL credentials is refused'
$before=[CdnFixture]::Requests.Count
Must-Reject {Fetch-File 'http://cdn-test.invalid/latest.json' (Destination 'http.tmp') 1024 $false $true} 'HTTP source is rejected before transport'
Must-Reject {Fetch-File 'https://user:secret@cdn-test.invalid/latest.json' (Destination 'userinfo.tmp') 1024 $false $true} 'Source URL credentials are rejected before transport'
Assert-True ([CdnFixture]::Requests.Count -eq $before) 'Unsafe source URLs never reach the request factory'

[CdnFixture]::Add($asset,$sample,'',2048)
Must-Reject {Fetch-File $asset (Destination 'oversized-header.tmp') 1024 $false} 'Excessive Content-Length is refused before body download'
[CdnFixture]::Add($asset,$sample,'',-1)
Must-Reject {Fetch-File $asset (Destination 'oversized-stream.tmp') 4 $false} 'Unknown-length stream remains bounded by the expected limit'
[CdnFixture]::Add($asset,$sample,'',$sample.Length+1)
Must-Reject {Fetch-File $asset (Destination 'truncated.tmp') 1024 $false} 'Truncated response is refused'
[CdnFixture]::Add($asset,$sample,'',$sample.Length-1)
Must-Reject {Fetch-File $asset (Destination 'wrong-length.tmp') 1024 $false} 'Incorrect smaller declared response length is refused'
[CdnFixture]::Add($asset,$sample,'',-1)
Fetch-File $asset (Destination 'chunked.tmp') 1024 $false
Assert-True ((Hash-File (Destination 'chunked.tmp')) -eq (Byte-Hash $sample)) 'Complete chunked response is accepted'
[CdnFixture]::Plans[$asset].Timeout=$true
Must-Reject {Fetch-File $asset (Destination 'timeout.tmp') 1024 $false} 'Transport timeout is surfaced for installed-version fallback'

$feed='https://cdn-test.invalid/game/latest.json'
function Make-RemoteFeed([string]$Release,[string]$Jar) {
    $files=@()
    foreach($name in @('runtime/bin/java.exe','app/spirit-gate-rpg.jar','assets/dev/player-base/test.png')){
        $data=Bytes $(if($name -eq 'app/spirit-gate-rpg.jar'){$Jar}else{'same'})
        $files+=@{path=$name;bytes=$data.Length;sha256=(Byte-Hash $data)}
        [CdnFixture]::Add(('https://cdn-test.invalid/game/'+$Release+'/'+$name),$data,'',-2)
    }
    $raw=Bytes (@{schemaVersion=1;version=$Release;basePath=$Release;files=$files} | ConvertTo-Json -Depth 8)
    [CdnFixture]::Add($feed,$raw,'',-2)
}
$install=Destination 'install'
Make-RemoteFeed 'release-a' 'first'
$current=Update-Installed $install $feed $false $null
Assert-True ((Read-Installed $install 'active.txt').id -eq $current.id) 'Initial CDN installation validates every file'
$save=Join-Path $install 'data/saves/cdn-sentinel.txt';New-Item -ItemType Directory -Path (Split-Path -Parent $save) -Force | Out-Null
[IO.File]::WriteAllText($save,'preserve-personal-save');$savedHash=Hash-File $save
$old=$current.id
Make-RemoteFeed 'release-b' 'second'
[CdnFixture]::Plans.Remove('https://cdn-test.invalid/game/release-b/runtime/bin/java.exe') | Out-Null
[CdnFixture]::Plans.Remove('https://cdn-test.invalid/game/release-b/assets/dev/player-base/test.png') | Out-Null
$requestsBefore=[CdnFixture]::Requests.Count
$current=Update-Installed $install $feed $false $current
$newRequests=[CdnFixture]::Requests.Count-$requestsBefore
Assert-True ($newRequests -eq 2 -and $current.id -ne $old) 'CDN update fetches only fresh manifest and one changed file'
Assert-True ((Hash-File $save) -eq $savedHash) 'Incremental CDN update preserves saves'
$active=$current.id
Make-RemoteFeed 'release-c' 'third'
[CdnFixture]::Add('https://cdn-test.invalid/game/release-c/app/spirit-gate-rpg.jar',(Bytes 'broken'),'',-2)
Must-Reject {Update-Installed $install $feed $false $current} 'Corrupt CDN payload is rejected by manifest hash'
Assert-True ((Read-Installed $install 'active.txt').id -eq $active) 'CDN integrity failure does not change active version'
Assert-True (@(Get-ChildItem -LiteralPath (Join-Path $install 'staging')).Count -eq 0) 'CDN failure removes temporary staging'
Assert-True ((Hash-File $save) -eq $savedHash) 'All simulated CDN failures preserve saves'

@{passed=$checks.Count;transport='simulated HTTPS WebRequest; no certificate or TLS bypass';checks=@($checks)} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $EvidenceDir 'results.json') -Encoding UTF8
Write-Host "PASS: $($checks.Count) CDN checks"
