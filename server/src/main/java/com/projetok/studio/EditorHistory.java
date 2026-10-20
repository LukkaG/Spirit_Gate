package com.projetok.studio;
import java.util.*;

/** Historico limitado a 64 gestos e 250 mil referencias, compartilhando registros imutaveis. */
public final class EditorHistory {
    private record Change(WorldDocument before,WorldDocument after){int weight(){return before.elements().size()+after.elements().size()+before.hitboxes().size()+after.hitboxes().size()+1;}}
    private final Deque<Change> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private WorldDocument current;private int weight;
    /** Inicia um documento sem criar uma operacao artificial no historico. */
    public EditorHistory(WorldDocument document){current=document;}
    /** Documento confirmado ao fim do ultimo gesto. */
    public WorldDocument current(){return current;}
    /** Confirma um gesto completo; repetir o mesmo documento nao duplica historico. */
    public WorldDocument commit(WorldDocument next){if(current.equals(next))return current;redo.clear();undo.addLast(new Change(current,next));current=next;weight=undo.stream().mapToInt(Change::weight).sum();while((undo.size()>64||weight>250000)&&undo.size()>1)weight-=undo.removeFirst().weight();return current;}
    /** Desfaz atomicamente, sem IO. */
    public WorldDocument undo(){if(!undo.isEmpty()){var c=undo.removeLast();redo.addLast(c);weight-=c.weight();current=c.before();}return current;}
    /** Refaz o ultimo gesto desfeito. */
    public WorldDocument redo(){if(!redo.isEmpty()){var c=redo.removeLast();undo.addLast(c);weight+=c.weight();current=c.after();}return current;}
    /** Abre outro documento com historico proprio. */
    public WorldDocument reset(WorldDocument document){undo.clear();redo.clear();weight=0;return current=document;}
    /** Numero de gestos retidos para diagnostico. */
    public int size(){return undo.size();}
}
