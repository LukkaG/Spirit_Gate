param([string]$InstallDir=$PSScriptRoot,[string]$LocalFeed,[switch]$SmokeTest,[switch]$NoDispatch)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($InstallDir)
if(!$NoDispatch -and $root -eq [IO.Path]::GetFullPath($PSScriptRoot) -and (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'Iniciar-SpiritGate.ps1'))){
    $argsList=@('-InstallDir',$root)
    if($LocalFeed){$argsList+=@('-LocalFeed',$LocalFeed)}
    if($SmokeTest){$argsList+='-SmokeTest'}
    & (Join-Path $PSScriptRoot 'Iniciar-SpiritGate.ps1') @argsList
    exit $LASTEXITCODE
}
$enginePath=Join-Path $PSScriptRoot 'Atualizar-SpiritGate.ps1'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[Windows.Forms.Application]::EnableVisualStyles()
$root=[IO.Path]::GetFullPath($InstallDir)
$uiLock=$null
try {$uiLock=[IO.File]::Open((Join-Path $root 'interface.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)}
catch {[Windows.Forms.MessageBox]::Show('O launcher ja esta aberto nesta pasta.','Spirit Gate') | Out-Null;exit 1}
. $enginePath -FunctionsOnly -InstallDir $root -LocalFeed $LocalFeed
$script:worker=$null
$script:mode=''
$script:ready=$false
$script:smokeOk=$false
$form=New-Object Windows.Forms.Form
$form.Text='Spirit Gate | Launcher'
$form.ClientSize=New-Object Drawing.Size(640,390)
$form.StartPosition='CenterScreen'
$form.FormBorderStyle='FixedSingle'
$form.MaximizeBox=$false
$form.BackColor=[Drawing.Color]::FromArgb(20,25,36)
$form.ForeColor=[Drawing.Color]::White
$form.Font=New-Object Drawing.Font('Segoe UI',11)
function Label-At($text,$x,$y,$w,$h) {
    $label=New-Object Windows.Forms.Label
    $label.Text=$text;$label.Location=New-Object Drawing.Point($x,$y);$label.Size=New-Object Drawing.Size($w,$h)
    $form.Controls.Add($label);return $label
}
$title=Label-At 'SPIRIT GATE' 30 26 580 50
$title.Font=New-Object Drawing.Font('Segoe UI',27,[Drawing.FontStyle]::Bold)
$subtitle=Label-At 'Atualize, conecte e entre na sua jornada.' 33 84 570 28
$subtitle.ForeColor=[Drawing.Color]::LightSteelBlue
$role='anfitriao'
try {$config=Get-Content (Join-Path $root 'atualizacoes.json') -Raw | ConvertFrom-Json;if($config.PSObject.Properties.Name -contains 'role'){$role=$config.role}}catch{}
$versionLabel=Label-At ('Instalacao: '+$role) 33 139 570 28
$status=Label-At 'Preparando verificacao...' 33 179 570 50
$bar=New-Object Windows.Forms.ProgressBar
$bar.Location=New-Object Drawing.Point(33,237);$bar.Size=New-Object Drawing.Size(574,16)
$form.Controls.Add($bar)
$note=Label-At 'Seus saves ficam preservados em data/saves.' 33 267 574 25
$note.Font=New-Object Drawing.Font('Segoe UI',9)
$play=New-Object Windows.Forms.Button
$play.Text='JOGAR';$play.Location=New-Object Drawing.Point(408,310);$play.Size=New-Object Drawing.Size(199,50)
$play.BackColor=[Drawing.Color]::FromArgb(37,116,155);$play.FlatStyle='Flat';$play.Enabled=$false
$form.Controls.Add($play)
$retry=New-Object Windows.Forms.Button
$retry.Text='Verificar atualizacoes';$retry.Location=New-Object Drawing.Point(33,310);$retry.Size=New-Object Drawing.Size(230,50)
$retry.FlatStyle='Flat';$form.Controls.Add($retry)
$timer=New-Object Windows.Forms.Timer
$timer.Interval=250
function Start-Worker([string]$Mode) {
    $script:mode=$Mode;$script:ready=$false;$play.Enabled=$false;$retry.Enabled=$false
    $bar.Style='Marquee'
    $status.Text=$(if($Mode -eq 'update'){'Verificando e atualizando arquivos...'}else{'Jogo em execucao. Boa jornada!'})
    $start=New-Object Diagnostics.ProcessStartInfo
    $start.FileName=Join-Path $PSHOME 'powershell.exe'
    $start.Arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$enginePath+'" -InstallDir "'+$root+'"'
    if($Mode -eq 'update'){$start.Arguments+=' -NoLaunch';if($LocalFeed){$start.Arguments+=' -LocalFeed "'+[IO.Path]::GetFullPath($LocalFeed)+'"'}}else{$start.Arguments+=' -SkipUpdate'}
    $start.WorkingDirectory=$root;$start.UseShellExecute=$false;$start.CreateNoWindow=$true
    $start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
    $script:worker=New-Object Diagnostics.Process
    $script:worker.StartInfo=$start
    [void]$script:worker.Start()
    # Drenar ambos os streams sem bloquear a interface ou o processo do jogo.
    $script:stdout=$script:worker.StandardOutput.ReadToEndAsync()
    $script:stderr=$script:worker.StandardError.ReadToEndAsync()
    $timer.Start()
}
$timer.Add_Tick({
    if(!$script:worker -or !$script:worker.HasExited){return}
    $timer.Stop()
    $exitCode=$script:worker.ExitCode
    $output=$script:stdout.GetAwaiter().GetResult()
    $errors=$script:stderr.GetAwaiter().GetResult()
    $script:worker.Dispose();$script:worker=$null
    $bar.Style='Continuous';$bar.Value=100;$retry.Enabled=$true
    try {
        if($exitCode -ne 0){throw 'Nao foi possivel concluir. Feche outras instancias e tente novamente.'}
        $current=Read-Installed $root 'active.txt'
        if(!$current){throw 'Nenhuma versao instalada. Extraia novamente o pacote completo.'}
        $versionLabel.Text='Versao '+$current.manifest.version+' | '+$role
        $script:ready=$true;$play.Enabled=$true
        if($output -match 'Atualizacao indisponivel'){$status.Text='Atualizacao indisponivel. Voce pode jogar a versao instalada.'}
        elseif($output -match 'Canal ainda nao publicado'){$status.Text='Versao instalada pronta. Canal de atualizacoes ainda nao configurado.'}
        else{$status.Text='Tudo pronto! Clique em JOGAR.'}
        $script:smokeOk=$true
    } catch {$status.Text=$_.Exception.Message;$bar.Value=0;$script:ready=$false;$play.Enabled=$false}
    if($SmokeTest){$form.Close()}
})
$retry.Add_Click({try {Start-Worker 'update'}catch{$status.Text=$_.Exception.Message;$retry.Enabled=$true}})
$play.Add_Click({if($script:ready){try {Start-Worker 'game'}catch{$status.Text=$_.Exception.Message;$retry.Enabled=$true}}})
$form.Add_Shown({try {Start-Worker 'update'}catch{$status.Text=$_.Exception.Message;$retry.Enabled=$true;if($SmokeTest){$form.Close()}}})
$form.Add_FormClosing({
    param($sender,$eventArgs)
    if($script:worker -and !$script:worker.HasExited){$eventArgs.Cancel=$true;$status.Text='Aguarde a atualizacao terminar ou feche o jogo para sair.'}
})
try {[void]$form.ShowDialog()}finally{$timer.Dispose();$form.Dispose();if($uiLock){$uiLock.Dispose()}}
if($SmokeTest -and !$script:smokeOk){exit 1}
