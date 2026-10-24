package com.projetok.multiplayer;

import java.net.*;
import java.util.*;

/** Autorização opt-in de um endereço atribuído ao adaptador Radmin, nunca wildcard. */
public final class RadminAccess {
    private RadminAccess() { }
    public static boolean address(InetAddress address){return address instanceof Inet4Address&&(address.getAddress()[0]&255)==26;}
    static boolean adapter(String name,boolean up,InetAddress address){return up&&name!=null&&name.toLowerCase(Locale.ROOT).contains("radmin")&&address(address);}
    public static InetAddress requireLocal(InetAddress selected){
        try{var nic=NetworkInterface.getByInetAddress(selected);if(nic==null||!adapter(nic.getName()+" "+nic.getDisplayName(),nic.isUp(),selected))throw new IllegalArgumentException("Selecione o IPv4 26.x.x.x de um adaptador Radmin VPN ativo");return selected;}
        catch(SocketException e){throw new IllegalArgumentException("Nao foi possivel verificar o adaptador Radmin",e);}
    }
    public static InetAddress clientSource(){
        try{var addresses=new ArrayList<InetAddress>();for(var nic:Collections.list(NetworkInterface.getNetworkInterfaces()))for(var ip:Collections.list(nic.getInetAddresses()))if(adapter(nic.getName()+" "+nic.getDisplayName(),nic.isUp(),ip))addresses.add(ip);if(addresses.size()!=1)throw new IllegalArgumentException("Cliente exige exatamente um IPv4 Radmin ativo");return addresses.get(0);}
        catch(SocketException e){throw new IllegalArgumentException("Adaptador Radmin indisponivel",e);}
    }
}
