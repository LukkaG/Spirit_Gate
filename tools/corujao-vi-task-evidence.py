"""Complementa checkpoints novos com diff real, regressões e hashes, sem sobrescrever evidência."""
from pathlib import Path
import runpy, sys, difflib
root=Path.cwd()
helper=runpy.run_path(str(root/'.task-checkpoints/corujao-vi-session-20261025T221534Z-be629798/session.py'))
task,action=sys.argv[1:3]
out=helper['checkpoint'](task)
if action=='regress':
    helper['suite'](task,'com.projetok.rpg','rpg')
    helper['suite'](task,'com.projetok.multiplayer','multiplayer')
elif action=='diff':
    baseline=Path(sys.argv[3])
    before=helper['read'](baseline/'sources-before.json')
    after=helper['sources']()
    helper['write'](out/'source-delta.json',dict(before=before,after=after,changed=[n for n in after if before.get(n)!=after[n]]))
    diff=[]
    for name in after:
        if before.get(name)==after[name]:continue
        prior=baseline/'isolated'/name
        old=prior.read_text(encoding='utf-8').splitlines(True) if prior.exists() else []
        diff.extend(difflib.unified_diff(old,(root/name).read_text(encoding='utf-8').splitlines(True),fromfile='a/'+name,tofile='b/'+name))
    (out/'implementation.patch').write_text(''.join(diff),encoding='utf-8')
elif action=='full':
    helper['suite'](task,'com.projetok','java-full-headless')
    helper['suite'](task,'com.projetok','java-full-graphics','false')
    import json
    prior=helper['read'](root/'.task-checkpoints/TASK-192/corujao-vi/run-20261025T232547Z-87c81ea2/python.command.json')
    # Compilar o parser da árvore atual para o caminho esperado pela ferramenta Python.
    classes=out/'isolated/server/target/classes'
    classes.mkdir(parents=True)
    source=out/'isolated/server/src/main/java/com/projetok/prototype/LocalArenaMap.java'
    helper['command'](out,'python-map-compile',['javac','--release','17','-encoding','UTF-8','-sourcepath',str(out/'isolated/server/src/main/java'),'-d',str(classes),str(source)],root,180)
    args=prior['command']; args[-1]=str(out/'isolated')
    helper['command'](out,'python',args,root,180)
    helper['integrity'](out,'full-final',True)
