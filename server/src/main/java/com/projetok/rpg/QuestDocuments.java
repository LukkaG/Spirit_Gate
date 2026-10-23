package com.projetok.rpg;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

/** Documento .sgquest V1 em Properties UTF-8, limitado e estritamente declarativo. */
public final class QuestDocuments {
    private static final int LIMIT=1_000_000;
    private QuestDocuments() { }
    public static byte[] encode(QuestBook book)throws IOException {
        book.requireValid();Properties p=new Properties();p.setProperty("format","spirit-gate-quests");p.setProperty("version","1");p.setProperty("count",""+book.quests().size());
        for(int i=0;i<book.quests().size();i++){
            var q=book.quests().get(i);String prefix="quest."+i+".";var d=q.definition();
            String[] keys={"id","name","description","npc","region","race","level","domain","unchosen","prerequisites","xp","coins","item","quantity","reset","objectives"};
            String[] values={q.id(),q.name(),q.description(),q.npc(),q.region(),q.race(),""+q.minimumLevel(),q.domain(),""+q.unchosenDomain(),String.join(",",q.prerequisites()),""+d.xp(),""+d.coins(),d.item(),""+d.quantity(),""+q.resetContract(),""+d.objectives().size()};
            for(int k=0;k<keys.length;k++)p.setProperty(prefix+keys[k],values[k]);
            for(int j=0;j<d.objectives().size();j++){var o=d.objectives().get(j);p.setProperty(prefix+"objective."+j,o.type()+"|"+o.target()+"|"+o.count());}
        }
        StringWriter out=new StringWriter();p.store(out,"Spirit Gate Quest Builder V1 - autoria, nao save");byte[] bytes=out.toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>LIMIT)throw new IOException("Documento excessivo");return bytes;
    }
    private static String take(Properties p,String key){String v=(String)p.remove(key);if(v==null)throw new IllegalArgumentException("Campo ausente: "+key);return v;}
    private static boolean bool(String value){if(!Set.of("true","false").contains(value))throw new IllegalArgumentException("Booleano inválido");return Boolean.parseBoolean(value);}
    public static QuestBook decode(byte[] bytes)throws IOException {
        if(bytes.length>LIMIT)throw new IOException("Documento excessivo");
        try {
            Properties p=new Properties(){@Override public synchronized Object put(Object k,Object v){if(containsKey(k))throw new IllegalArgumentException("Campo duplicado: "+k);return super.put(k,v);}};
            p.load(new StringReader(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()));
            if(!take(p,"format").equals("spirit-gate-quests")||!take(p,"version").equals("1"))throw new IllegalArgumentException("Formato/versão incompatível");
            int count=Integer.parseInt(take(p,"count"));if(count<1||count>64)throw new IllegalArgumentException("Contagem inválida");var quests=new ArrayList<QuestBook.Draft>();
            for(int i=0;i<count;i++){
                String prefix="quest."+i+".";String id=take(p,prefix+"id"),name=take(p,prefix+"name"),description=take(p,prefix+"description"),npc=take(p,prefix+"npc"),region=take(p,prefix+"region"),race=take(p,prefix+"race");
                int level=Integer.parseInt(take(p,prefix+"level"));String domain=take(p,prefix+"domain");boolean unchosen=bool(take(p,prefix+"unchosen"));String required=take(p,prefix+"prerequisites");
                int xp=Integer.parseInt(take(p,prefix+"xp")),coins=Integer.parseInt(take(p,prefix+"coins"));String item=take(p,prefix+"item");int quantity=Integer.parseInt(take(p,prefix+"quantity"));boolean reset=bool(take(p,prefix+"reset"));
                int n=Integer.parseInt(take(p,prefix+"objectives"));if(n<1||n>8)throw new IllegalArgumentException("Contagem de objetivos inválida");var objectives=new ArrayList<QuestService.Objective>();
                for(int j=0;j<n;j++){String[] row=take(p,prefix+"objective."+j).split("\\|",-1);if(row.length!=3)throw new IllegalArgumentException("Objetivo inválido");objectives.add(new QuestService.Objective(row[0],row[1],Integer.parseInt(row[2])));}
                quests.add(new QuestBook.Draft(id,name,description,npc,region,race,level,domain,unchosen,required.isEmpty()?List.of():List.of(required.split(",",-1)),new QuestService.Definition(objectives,xp,coins,item,quantity),reset));
            }
            if(!p.isEmpty())throw new IllegalArgumentException("Campos desconhecidos: "+p.keySet());QuestBook book=new QuestBook(1,quests);book.requireValid();return book;
        }catch(IllegalArgumentException|NullPointerException e){throw new IOException("Missões inválidas: "+e.getMessage(),e);}
    }
    public static QuestBook load(Path path)throws IOException {try(var in=Files.newInputStream(path)){return decode(in.readNBytes(LIMIT+1));}}
    /** Valida bytes gravados antes de substituir e conserva backup íntegro. */
    public static void save(Path file,QuestBook book)throws IOException {
        byte[] bytes=encode(book);decode(bytes);Path path=file.toAbsolutePath();Files.createDirectories(path.getParent());if(Files.isSymbolicLink(path)||Files.isSymbolicLink(Path.of(path+".bak")))throw new IOException("Arquivo de missões/backup não pode ser link");if(Files.exists(path))load(path);
        Path temp=Files.createTempFile(path.getParent(),"quest-",".tmp");try{Files.write(temp,bytes);load(temp);if(Files.exists(path))Files.copy(path,Path.of(path+".bak"),StandardCopyOption.REPLACE_EXISTING);try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
}
