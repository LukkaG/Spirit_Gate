package com.projetok.studio;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class NavigationAnalysisTest {
    @Test void wallSeparatesSamplesAndLargeMapIsBounded(){var e=new WorldDocument.Element("player",WorldDocument.Kind.PLAYER,"","",new WorldDocument.Crop(0,0,32,32),48,48,32,32,"interaction",true,false,"","FRONT",Map.of());var d=WorldDocument.empty("mapa",256,256).withElements(List.of(e));assertEquals(0,NavigationAnalysis.analyze(d).isolated());var blocked=d.withHitboxes(List.of(new WorldDocument.Hitbox("wall",112,0,32,256,true,"collision")));assertTrue(NavigationAnalysis.analyze(blocked).isolated()>0);var large=NavigationAnalysis.analyze(WorldDocument.empty("grande",8192,8192));assertTrue(large.cols()*large.rows()<=65536);}
}
