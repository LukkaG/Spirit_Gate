param([Parameter(Mandatory=$true)][string]$Destination,[string]$EntryScript='Iniciar-SpiritGate.ps1')
$ErrorActionPreference='Stop'
if($EntryScript -notmatch '^[A-Za-z0-9_-]+[.]ps1$'){throw 'Entrada do launcher invalida'}
$source=@'
using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;
internal static class Entry {
    [STAThread] private static void Main() {
        try {
            string script = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "__ENTRY_SCRIPT__");
            if (!File.Exists(script)) throw new FileNotFoundException("Extraia todo o pacote antes de abrir o launcher.");
            var start = new ProcessStartInfo(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), @"WindowsPowerShell\v1.0\powershell.exe"));
            start.Arguments = "-NoProfile -STA -ExecutionPolicy Bypass -WindowStyle Hidden -File \"" + script + "\"";
            start.WorkingDirectory = AppDomain.CurrentDomain.BaseDirectory;
            start.UseShellExecute = false;
            start.CreateNoWindow = true;
            Process.Start(start);
        } catch (Exception error) { MessageBox.Show(error.Message, "Spirit Gate", MessageBoxButtons.OK, MessageBoxIcon.Error); }
    }
}
'@
$source=$source.Replace('__ENTRY_SCRIPT__',$EntryScript)
Add-Type -TypeDefinition $source -ReferencedAssemblies System.Windows.Forms -OutputAssembly ([IO.Path]::GetFullPath($Destination)) -OutputType WindowsApplication
