package com.projetok.multiplayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
class TwoProcessTest {
    @TempDir Path folder;
    @Test void twoSeparateClientProcessesShareOneLocalServer()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)){server.start();
            String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();String cp=Path.of(CoopWorld.class.getProtectionDomain().getCodeSource().getLocation().toURI())+System.getProperty("path.separator")+Path.of(TwoProcessProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Process a=new ProcessBuilder(java,"-cp",cp,TwoProcessProbe.class.getName(),""+server.port(),"Processo Um").redirectErrorStream(true).redirectOutput(folder.resolve("one.log").toFile()).start();Process b=new ProcessBuilder(java,"-cp",cp,TwoProcessProbe.class.getName(),""+server.port(),"Processo Dois").redirectErrorStream(true).redirectOutput(folder.resolve("two.log").toFile()).start();
            try{assertTrue(a.waitFor(10,TimeUnit.SECONDS));assertTrue(b.waitFor(10,TimeUnit.SECONDS));String one=Files.readString(folder.resolve("one.log")),two=Files.readString(folder.resolve("two.log"));assertEquals(0,a.exitValue(),one);assertEquals(0,b.exitValue(),two);assertTrue(one.contains("\"players\":2"));assertTrue(two.contains("\"moved\":true"));System.out.println("TWO_PROCESS_LOCAL "+one.strip()+" "+two.strip());}finally{if(a.isAlive())a.destroyForcibly();if(b.isAlive())b.destroyForcibly();}
            assertNull(server.failure());
        }
    }
    @Test void lanNeverAllowsWildcardOrPublicAddresses()throws Exception{assertThrows(IllegalArgumentException.class,()->LocalSimulationServer.lan(InetAddress.getByName("0.0.0.0"),0));assertThrows(IllegalArgumentException.class,()->LocalSimulationServer.lan(InetAddress.getByName("8.8.8.8"),0));}
}
