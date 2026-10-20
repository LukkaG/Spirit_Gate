package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import java.nio.file.*;
public class PendingProbe {public static void main(String[] a)throws Exception{try(var r=new RpgRuntime()){var saved=r.saveAndPause(new LocalSaveStore(Path.of(a[0])));r.advanceOneTick();saved.get();var action=r.action(w->true);r.close();System.out.println("P077-01 futureDone="+action.isDone());}}}
