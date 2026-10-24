package com.projetok.multiplayer;
import java.net.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RadminAccessTest {
    @Test void rangeAloneDoesNotAuthorizeAnInterface()throws Exception{
        var vpn=InetAddress.getByName("26.10.20.30");assertTrue(RadminAccess.address(vpn));assertFalse(vpn.isSiteLocalAddress());
        assertTrue(RadminAccess.adapter("Famatech Radmin VPN",true,vpn));assertFalse(RadminAccess.adapter("Ethernet",true,vpn));assertFalse(RadminAccess.adapter("Radmin VPN",false,vpn));
        for(String ip:new String[]{"0.0.0.0","127.0.0.1","192.168.1.2","8.8.8.8","224.0.0.1","::1"})assertFalse(RadminAccess.adapter("Radmin VPN",true,InetAddress.getByName(ip)));
        assertThrows(IllegalArgumentException.class,()->RadminAccess.requireLocal(InetAddress.getLoopbackAddress()));
    }
}
