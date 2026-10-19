package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EnvironmentCycleTest {
    @Test void deterministicOptionalAndPeriodic(){assertEquals(1f,EnvironmentCycle.light(0,true));assertEquals(EnvironmentCycle.light(100,true),EnvironmentCycle.light(2500,true));assertTrue(EnvironmentCycle.light(1200,true)>=0.82f);assertEquals(1,EnvironmentCycle.light(1200,false));}
}
