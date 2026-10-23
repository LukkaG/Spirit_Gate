package com.projetok.multiplayer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Protocolo experimental V1: frames limitados e DTOs explicitos, sem serializacao Java. */
public final class Protocol {
    public static final int VERSION=1,MAX_FRAME=4096,MAX_ACTORS=8;
    public static final int COSMETIC_APPLICATION=2;
    public sealed interface Message permits Hello,Welcome,Intent,State,Bye,Error,AppearanceRequest,SessionState { }
    /** Pedido sem ID de outro jogador, raça, domínio ou caminho de arquivo. */
    public record AppearanceRequest(long sequence,String hair,String clothing)implements Message {public AppearanceRequest{if(sequence<1||sequence>1_000_000_000_000L)throw new IllegalArgumentException("Sequência inválida");new com.projetok.prototype.rpg.CosmeticCatalog.Selection(hair,clothing);}}
    public record Appearance(int id,String race,String hair,String clothing){public Appearance{if(id<1)throw new IllegalArgumentException("Sessão inválida");com.projetok.rpg.CharacterRaces.get(race);new com.projetok.prototype.rpg.CosmeticCatalog.Selection(hair,clothing);}}
    /** Extensão negociada; nunca enviada a um peer de aplicação V1. */
    public record SessionState(State world,List<Appearance> appearances)implements Message {public SessionState{appearances=List.copyOf(appearances);if(appearances.size()>2||appearances.stream().map(Appearance::id).distinct().count()!=appearances.size()||appearances.stream().anyMatch(a->world.actors().stream().noneMatch(actor->actor.id()==a.id()&&!actor.boss())))throw new IllegalArgumentException("Aparências de sessão inválidas");}}
    public enum Action { MOVE,ATTACK,LIGHTNING_STEP,CRYOSTASIS }
    public record Hello(int version,String name)implements Message{public Hello{if(version<0||version>65535)throw new IllegalArgumentException("Versao invalida");Protocol.name(name);}}
    public record Welcome(int id)implements Message{public Welcome{if(id<1)throw new IllegalArgumentException("ID invalido");}}
    public record Intent(long sequence,Action action,float x,float y)implements Message{public Intent{if(sequence<1||sequence>1_000_000_000_000L||action==null||!Float.isFinite(x)||!Float.isFinite(y)||Math.abs(x)>8192||Math.abs(y)>8192)throw new IllegalArgumentException("Intencao invalida");}}
    public record Actor(int id,String name,float x,float y,int hp,int maxHp,float stamina,int coins,boolean boss,float facingX,float facingY,int flags) {
        public Actor(int id,String name,float x,float y,int hp,int maxHp,float stamina,int coins,boolean boss){this(id,name,x,y,hp,maxHp,stamina,coins,boss,1,0,0);}
        public Actor{Protocol.name(name);if(id<1||!Float.isFinite(x)||!Float.isFinite(y)||x<18||x>942||y<18||y>622||hp<0||hp>maxHp||maxHp<1||maxHp>10000||!Float.isFinite(stamina)||stamina<0||stamina>1000||coins<0||coins>1_000_000||!Float.isFinite(facingX)||!Float.isFinite(facingY)||Math.hypot(facingX,facingY)>1.001||flags<0||flags>7)throw new IllegalArgumentException("Ator invalido");}
    }
    public record State(long tick,List<Actor> actors)implements Message{
        public State{actors=List.copyOf(actors);if(tick<0||tick>1_000_000_000||actors.size()>MAX_ACTORS||actors.stream().map(Actor::id).distinct().count()!=actors.size())throw new IllegalArgumentException("Snapshot invalido");}
    }
    public record Bye()implements Message{ }
    public record Error(String reason)implements Message{public Error{if(reason==null||reason.length()>120)throw new IllegalArgumentException("Erro invalido");}}
    private Protocol(){ }
    private static void name(String value){if(value==null||!value.matches("[\\p{L}][\\p{L}\\p{N} _-]{2,23}"))throw new IllegalArgumentException("Nome invalido");}
    public static void write(OutputStream stream,Message message)throws IOException{
        var buffer=new ByteArrayOutputStream();var out=new DataOutputStream(buffer);out.writeByte(VERSION);
        if(message instanceof Hello m){out.writeByte(1);out.writeInt(m.version());text(out,m.name());}
        else if(message instanceof Welcome m){out.writeByte(2);out.writeInt(m.id());}
        else if(message instanceof Intent m){out.writeByte(3);out.writeLong(m.sequence());out.writeByte(m.action().ordinal()+1);out.writeFloat(m.x());out.writeFloat(m.y());}
        else if(message instanceof State m){out.writeByte(4);out.writeLong(m.tick());out.writeByte(m.actors().size());for(Actor a:m.actors()){out.writeInt(a.id());text(out,a.name());out.writeFloat(a.x());out.writeFloat(a.y());out.writeInt(a.hp());out.writeInt(a.maxHp());out.writeFloat(a.stamina());out.writeInt(a.coins());out.writeBoolean(a.boss());out.writeFloat(a.facingX());out.writeFloat(a.facingY());out.writeByte(a.flags());}}
        else if(message instanceof Bye){out.writeByte(5);}
        else if(message instanceof Error m){out.writeByte(6);text(out,m.reason());}
        else if(message instanceof AppearanceRequest m){out.writeByte(7);out.writeLong(m.sequence());text(out,m.hair());text(out,m.clothing());}
        else if(message instanceof SessionState m){out.writeByte(8);write(out,m.world());out.writeByte(m.appearances().size());for(var a:m.appearances()){out.writeInt(a.id());text(out,a.race());text(out,a.hair());text(out,a.clothing());}}
        else throw new IOException("Mensagem desconhecida");
        byte[] bytes=buffer.toByteArray();if(bytes.length>MAX_FRAME)throw new IOException("Frame excessivo");var target=new DataOutputStream(stream);target.writeInt(bytes.length);target.write(bytes);target.flush();
    }
    public static Message read(InputStream stream)throws IOException{
        return read(stream,true);
    }
    private static Message read(InputStream stream,boolean allowExtended)throws IOException{
        var source=new DataInputStream(stream);int size=source.readInt();if(size<2||size>MAX_FRAME)throw new IOException("Tamanho de frame invalido");byte[] bytes=source.readNBytes(size);if(bytes.length!=size)throw new EOFException("Frame interrompido");
        var in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readUnsignedByte()!=VERSION)throw new IOException("Versao de envelope incompativel");
        try{
            Message message=switch(in.readUnsignedByte()){
                case 1->new Hello(in.readInt(),text(in));case 2->new Welcome(in.readInt());
                case 3->{long sequence=in.readLong();int action=in.readUnsignedByte();if(action<1||action>4)throw new IOException("Acao desconhecida");yield new Intent(sequence,Action.values()[action-1],in.readFloat(),in.readFloat());}
                case 4->{long tick=in.readLong();int count=in.readUnsignedByte();if(count>MAX_ACTORS)throw new IOException("Atores excessivos");List<Actor> actors=new ArrayList<>();for(int i=0;i<count;i++)actors.add(new Actor(in.readInt(),text(in),in.readFloat(),in.readFloat(),in.readInt(),in.readInt(),in.readFloat(),in.readInt(),in.readBoolean(),in.readFloat(),in.readFloat(),in.readUnsignedByte()));yield new State(tick,actors);}
                case 5->new Bye();case 6->new Error(text(in));
                case 7->new AppearanceRequest(in.readLong(),text(in),text(in));
                case 8->{if(!allowExtended)throw new IOException("Extensão aninhada recusada");var nested=read(in,false);if(!(nested instanceof State state))throw new IOException("Snapshot aninhado inválido");int count=in.readUnsignedByte();if(count>2)throw new IOException("Aparências excessivas");var appearances=new ArrayList<Appearance>();for(int i=0;i<count;i++)appearances.add(new Appearance(in.readInt(),text(in),text(in),text(in)));yield new SessionState(state,appearances);}
                default->throw new IOException("Tipo desconhecido");};
            if(in.available()!=0)throw new IOException("Dados excedentes");return message;
        }catch(IllegalArgumentException error){throw new IOException("Mensagem malformada",error);}
    }
    private static void text(DataOutputStream out,String text)throws IOException{byte[] bytes=text.getBytes(StandardCharsets.UTF_8);if(bytes.length>240)throw new IOException("Texto excessivo");out.writeByte(bytes.length);out.write(bytes);}
    private static String text(DataInputStream in)throws IOException{int length=in.readUnsignedByte();if(length>240)throw new IOException("Texto excessivo");byte[] bytes=in.readNBytes(length);if(bytes.length!=length)throw new EOFException();return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
}
