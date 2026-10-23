package com.projetok.studio;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/** Formato SG World Studio V1: Properties UTF-8, registros delimitados e textos Base64URL. */
public final class DocumentStore {
    private static final int LIMIT=32_000_000;
    private static final java.util.concurrent.atomic.AtomicLong TEMP_SEQUENCE=new java.util.concurrent.atomic.AtomicLong();
    private DocumentStore(){}
    private static String text(String s){return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String unt(String s){try{return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(Base64.getUrlDecoder().decode(s))).toString();}catch(CharacterCodingException ex){throw new IllegalArgumentException("Texto Base64 nao UTF-8",ex);}}
    private static String map(Map<String,String> values){return values.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->text(e.getKey())+":"+text(e.getValue())).collect(java.util.stream.Collectors.joining(","));}
    private static Map<String,String> unmap(String value){Map<String,String> result=new LinkedHashMap<>();if(!value.isEmpty())for(String item:value.split(",",-1)){var p=item.split(":",-1);if(p.length!=2||result.put(unt(p[0]),unt(p[1]))!=null)throw new IllegalArgumentException("Propriedade duplicada/invalida");}return result;}
    private static String row(Object... fields){return Arrays.stream(fields).map(Object::toString).collect(java.util.stream.Collectors.joining("|"));}
    private static boolean bool(String value){if(!Set.of("true","false").contains(value))throw new IllegalArgumentException("Booleano invalido");return Boolean.parseBoolean(value);}
    private static int integer(String s){return Integer.parseInt(s);}
    private static String take(Properties p,String key){String value=(String)p.remove(key);if(value==null)throw new IllegalArgumentException("Campo ausente: "+key);return value;}
    private static String[] fields(Properties p,String key,int count){String[] values=take(p,key).split("\\|",-1);if(values.length!=count)throw new IllegalArgumentException("Registro invalido: "+key);return values;}
    private static int count(Properties p,String key,int max){int n=integer(take(p,key+".count"));if(n<0||n>max)throw new IllegalArgumentException("Contagem excessiva: "+key);return n;}
    /** Serializa somente dados, sem usar serializacao Java ou executar scripts. */
    public static byte[] encode(WorldDocument d)throws IOException {
        Properties p=new Properties();p.setProperty("format","spirit-gate-world-studio");p.setProperty("schema","1");p.setProperty("id",d.id());p.setProperty("name",d.name());p.setProperty("region",d.region());p.setProperty("width",""+d.width());p.setProperty("height",""+d.height());p.setProperty("grid",""+d.grid());p.setProperty("metadata",map(d.metadata()));
        p.setProperty("layer.count",""+d.layers().size());for(int i=0;i<d.layers().size();i++){var l=d.layers().get(i);p.setProperty("layer."+i,row(l.id(),text(l.name()),l.role(),l.order(),l.visible(),l.locked()));}
        p.setProperty("element.count",""+d.elements().size());for(int i=0;i<d.elements().size();i++){var e=d.elements().get(i);var c=e.crop();p.setProperty("element."+i,row(e.id(),e.kind(),e.asset(),e.sha(),c.x(),c.y(),c.width(),c.height(),e.x(),e.y(),e.width(),e.height(),e.layer(),e.visible(),e.locked(),text(e.reference()),e.direction(),map(e.properties())));}
        p.setProperty("hitbox.count",""+d.hitboxes().size());for(int i=0;i<d.hitboxes().size();i++){var h=d.hitboxes().get(i);p.setProperty("hitbox."+i,row(h.id(),h.x(),h.y(),h.width(),h.height(),h.solid(),h.layer()));}
        p.setProperty("portal.count",""+d.portals().size());for(int i=0;i<d.portals().size();i++){var x=d.portals().get(i);p.setProperty("portal."+i,row(x.id(),x.x(),x.y(),x.destination(),x.arrivalX(),x.arrivalY(),text(x.quest()),x.direction()));}
        p.setProperty("trigger.count",""+d.triggers().size());for(int i=0;i<d.triggers().size();i++){var t=d.triggers().get(i);p.setProperty("trigger."+i,row(t.id(),t.when(),t.action(),text(t.target()),text(t.message()),t.x(),t.y(),t.width(),t.height(),t.once()));}
        StringWriter writer=new StringWriter();p.store(writer,"Spirit Gate World Studio V1");byte[] bytes=writer.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>LIMIT)throw new IOException("Documento excede 32 MB");return bytes;
    }
    /** Valida versao, contagens, chaves duplicadas/desconhecidas e invariantes antes de abrir. */
    public static WorldDocument decode(byte[] bytes)throws IOException {
        if(bytes.length>LIMIT)throw new IOException("Documento excessivo");
        Properties p=new Properties(){@Override public synchronized Object put(Object key,Object value){if(containsKey(key))throw new IllegalArgumentException("Chave duplicada: "+key);return super.put(key,value);}};
        try {
            p.load(new StringReader(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()));
            if(!take(p,"format").equals("spirit-gate-world-studio")||!take(p,"schema").equals("1"))throw new IllegalArgumentException("Formato/versao nao suportado");
            String id=take(p,"id"),name=take(p,"name"),region=take(p,"region");int width=integer(take(p,"width")),height=integer(take(p,"height")),grid=integer(take(p,"grid"));var metadata=unmap(take(p,"metadata"));
            var layers=new ArrayList<WorldDocument.Layer>();for(int n=count(p,"layer",32),i=0;i<n;i++){var v=fields(p,"layer."+i,6);layers.add(new WorldDocument.Layer(v[0],unt(v[1]),WorldDocument.Role.valueOf(v[2]),integer(v[3]),bool(v[4]),bool(v[5])));}
            var elements=new ArrayList<WorldDocument.Element>();for(int n=count(p,"element",100000),i=0;i<n;i++){var v=fields(p,"element."+i,18);elements.add(new WorldDocument.Element(v[0],WorldDocument.Kind.valueOf(v[1]),v[2],v[3],new WorldDocument.Crop(integer(v[4]),integer(v[5]),integer(v[6]),integer(v[7])),integer(v[8]),integer(v[9]),integer(v[10]),integer(v[11]),v[12],bool(v[13]),bool(v[14]),unt(v[15]),v[16],unmap(v[17])));}
            var hitboxes=new ArrayList<WorldDocument.Hitbox>();for(int n=count(p,"hitbox",4096),i=0;i<n;i++){var v=fields(p,"hitbox."+i,7);hitboxes.add(new WorldDocument.Hitbox(v[0],integer(v[1]),integer(v[2]),integer(v[3]),integer(v[4]),bool(v[5]),v[6]));}
            var portals=new ArrayList<WorldDocument.Portal>();for(int n=count(p,"portal",64),i=0;i<n;i++){var v=fields(p,"portal."+i,8);portals.add(new WorldDocument.Portal(v[0],integer(v[1]),integer(v[2]),v[3],integer(v[4]),integer(v[5]),unt(v[6]),v[7]));}
            var triggers=new ArrayList<WorldDocument.Trigger>();for(int n=count(p,"trigger",256),i=0;i<n;i++){var v=fields(p,"trigger."+i,10);triggers.add(new WorldDocument.Trigger(v[0],v[1],v[2],unt(v[3]),unt(v[4]),integer(v[5]),integer(v[6]),integer(v[7]),integer(v[8]),bool(v[9])));}
            if(!p.isEmpty())throw new IllegalArgumentException("Campos desconhecidos: "+p.keySet());return new WorldDocument(1,id,name,region,width,height,grid,layers,elements,hitboxes,portals,triggers,metadata);
        }catch(IllegalArgumentException error){throw new IOException("Mapa invalido: "+error.getMessage(),error);}
    }
    /** Le no maximo 32 MB, inclusive quando o arquivo muda durante a leitura. */
    public static WorldDocument load(Path file)throws IOException {try(var in=Files.newInputStream(file)){return decode(in.readNBytes(LIMIT+1));}}
    /** Grava temporario validado e backup somente de uma versao anterior integra. */
    public static void save(Path file,WorldDocument d)throws IOException {
        byte[] bytes=encode(d);decode(bytes);Path path=file.toAbsolutePath();Files.createDirectories(path.getParent());if(Files.exists(path))load(path);
        Path temp=temporary(path.getParent(),"studio-");try{Files.write(temp,bytes);load(temp);if(Files.exists(path)){Path backup=temporary(path.getParent(),"studio-backup-");try{Files.copy(path,backup,StandardCopyOption.REPLACE_EXISTING);move(backup,Path.of(path+".bak"));}finally{Files.deleteIfExists(backup);}}move(temp,path);}finally{Files.deleteIfExists(temp);}
    }
    /** CREATE_NEW evita colisão/sobrescrita sem inicializar o gerador criptográfico do JDK. */
    private static Path temporary(Path folder,String prefix)throws IOException {
        for(int attempt=0;attempt<256;attempt++){
            Path candidate=folder.resolve(prefix+ProcessHandle.current().pid()+"-"+Long.toUnsignedString(System.nanoTime(),36)+"-"+TEMP_SEQUENCE.incrementAndGet()+".tmp");
            try{return Files.createFile(candidate);}catch(FileAlreadyExistsException collision){/* Tenta outro nome, sem tocar no arquivo existente. */}
        }
        throw new IOException("Não foi possível reservar temporário exclusivo");
    }
    private static void move(Path from,Path to)throws IOException {try{Files.move(from,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException error){Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);}}
}
