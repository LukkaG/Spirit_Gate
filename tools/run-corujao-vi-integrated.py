"""Prova 199 com probe novo derivado do cenário auditado e jar/runtime verificados da 198."""
from pathlib import Path
import runpy, shutil, os
root=Path.cwd()
h=runpy.run_path(str(root/'.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/session.py'))
out=h['checkpoint']('199');previous=h['checkpoint']('198')
if h['sources']()!=h['read'](previous/'compiled-sources.json'):raise RuntimeError('Fontes mudaram desde o pacote')
host=root/h['read'](previous/'packages.json')[0]['image'];jar=host/'app/spiritgate.jar'
expected=h['read'](previous/'packages.json')[0]['jar_sha256']
if h['sha'](jar)!=expected:raise RuntimeError('Jar divergente')
project=out/'project';project.mkdir()
for relative in ('assets/dev/player-base','assets/dev/generated/corujao-iv-v1'):shutil.copytree(root/relative,project/relative)
output=out/'integrated';output.mkdir()
source=(root/'tools/Task180PublishingProbe.java').read_text(encoding='utf-8').replace('Task180PublishingProbe','Task199PublishingProbe')
needle='capture(output,"rollback-confirmed.png");'
addition='''
            b.close();waitFor(()->server.players()==1);
            String reconnectHash;
            try(var c=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),"Reconexao",false,false,null,assets)){
                SwingUtilities.invokeAndWait(()->{second.dispose();second=RpgNetworkClientLauncher.open(c,spritesB,"127.0.0.1:"+server.port(),cosmetics);second.setSize(1400,830);});
                waitFor(()->!c.revisions().blocked()&&c.snapshot()!=null&&server.players()==2);
                reconnectHash=c.revisions().hash();if(!baseline.equals(reconnectHash)||!baseline.equals(a.revisions().hash()))throw new AssertionError("Reconnect diverged");
                capture(output,"reconnect-confirmed.png");
            }
'''
if source.count(needle)!=1:raise RuntimeError('Ancora do probe mudou')
source=source.replace(needle,needle+addition)
needle='Files.writeString(output.resolve("metrics.json"),json);'
addition='''json=json.substring(0,json.length()-1)+String.format(Locale.ROOT,",\\"baselineHash\\":\\"%s\\",\\"publishedHash\\":\\"%s\\",\\"finalServerHash\\":\\"%s\\",\\"finalClientAHash\\":\\"%s\\",\\"finalClientBHash\\":\\"%s\\",\\"reconnectHash\\":\\"%s\\",\\"disconnectReconnectAfterRollback\\":true,\\"gamePort\\":%d,\\"previewPort\\":%d}",baseline,prepared.hash(),managed.target().activeHash(),a.revisions().hash(),b.revisions().hash(),reconnectHash,server.port(),studio.liveTools.preview.port());'''
if source.count(needle)!=1:raise RuntimeError('Ancora de metricas mudou')
source=source.replace(needle,addition+needle)
probe=out/'Task199PublishingProbe.java';probe.write_text(source,encoding='utf-8')
shutil.copyfile(probe,root/'tools/Task199PublishingProbe.java')
classes=out/'probe-classes';classes.mkdir()
h['command'](out,'probe-compile',['javac','--release','17','-encoding','UTF-8','-cp',str(jar),'-d',str(classes),str(probe)],root)
h['write'](out/'implementation.json',dict(current_sources_match_packaged=True,jar_sha256=expected,probe_sha256=h['sha'](probe),original_probe_sha256=h['sha'](root/'tools/Task180PublishingProbe.java')))
h['command'](out,'integrated',[str(host/'runtime/bin/java.exe'),'-Xmx768m','-Djava.awt.headless=false','-Dspiritgate.network.debug=true','-cp',os.pathsep.join([str(jar),str(classes)]),'com.projetok.studio.Task199PublishingProbe',str(project),str(output)],project,240)
metrics=h['read'](output/'metrics.json')
assert metrics['activationConfirmed'] and metrics['rollbackConfirmed'] and metrics['disconnectReconnectAfterRollback']
assert metrics['gamePort']!=metrics['previewPort']
assert all(metrics[key]==metrics['baselineHash'] for key in ['finalServerHash','finalClientAHash','finalClientBHash','reconnectHash'])
print(metrics,flush=True)
