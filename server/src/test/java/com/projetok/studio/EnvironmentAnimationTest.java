package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EnvironmentAnimationTest {
    @Test void monotonicTimingRoundTripAndExplicitStaticExport()throws Exception{String a="a".repeat(64),b="b".repeat(64);var s=new EnvironmentAnimation.Sequence(List.of(a,b),List.of(a,b),250,true);assertEquals(0,s.index(249_999_999));assertEquals(1,s.index(250_000_000));assertEquals(0,s.index(500_000_000));assertEquals(1,new EnvironmentAnimation.Sequence(s.frames(),s.hashes(),250,false).index(Long.MAX_VALUE));assertThrows(IllegalArgumentException.class,()->new EnvironmentAnimation.Sequence(s.frames(),s.hashes(),0,true));var e=EnvironmentAnimation.configure(EditorOperationsTest.brush(),s);var d=WorldDocument.empty("mapa",128,128).withElements(List.of(e));assertEquals(d,DocumentStore.decode(DocumentStore.encode(d)));assertTrue(EnvironmentAnimation.sequence(EnvironmentAnimation.staticExport(d).elements().get(0)).isEmpty());assertTrue(EnvironmentAnimation.sequence(d.elements().get(0)).isPresent());}
}
