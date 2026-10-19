package com.projetok.prototype;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Diagnóstico local por lista permitida; nunca lê ambiente, nomes ou caminhos pessoais. */
public final class DiagnosticReport {
    public static final String VERSION="0.36.0-rc1";
    public enum Stage { ARENA, MAP_LOADING, RENDERING }
    private DiagnosticReport() { }

    /** Não inclui lastEvent, nomes de atores, mensagens/stack de exceção ou nome do mapa. */
    public static String json(DebugSnapshot snapshot,int width,int height,boolean localMap,Stage stage,Throwable error) {
        if(width<=0 || height<=0 || width>32768 || height>32768 || stage==null)
            throw new IllegalArgumentException("Dimensoes/etapa invalidas");
        // Valores de propriedades passam por listas fechadas, sem exportar propriedades arbitrárias.
        String os=System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT);
        os=os.startsWith("windows")?"Windows":os.startsWith("linux")?"Linux":os.startsWith("mac")?"macOS":"Other";
        String arch=System.getProperty("os.arch","");
        arch=arch.equals("amd64") || arch.equals("x86_64")?"x64":arch.equals("aarch64")?"arm64":"other";
        String jvm=System.getProperty("java.version","");
        if(!jvm.matches("[0-9][0-9.+_-]{0,40}"))jvm="unknown";
        return "{\n  \"schemaVersion\": 1,\n  \"version\": \""+VERSION+"\",\n  \"os\": \""+os+"\",\n"+
                "  \"arch\": \""+arch+"\",\n  \"jvm\": \""+jvm+"\",\n  \"window\": ["+width+","+height+"],\n"+
                "  \"map\": \""+(localMap?"LOCAL_MAP":"DEFAULT_ARENA")+"\",\n  \"stage\": \""+stage+"\",\n"+
                "  \"error\": \""+(error==null?"NONE":"REPORTED_ERROR_DETAILS_OMITTED")+"\",\n"+
                "  \"ticks\": "+snapshot.ticks()+",\n  \"actors\": "+snapshot.actors().size()+",\n"+
                "  \"hp\": "+snapshot.playerHp()+",\n  \"gameOver\": "+snapshot.gameOver()+",\n"+
                "  \"cryostasisState\": \""+snapshot.cryostasisHud().state()+"\",\n"+
                "  \"dashState\": \""+snapshot.lightningStepHud().state()+"\"\n}\n";
    }

    /** Exportação explícita e exclusiva; um único JSON revisável, sem arquivos externos. */
    public static void export(Path destination,DebugSnapshot snapshot,int width,int height,boolean localMap,Stage stage,Throwable error) throws IOException {
        byte[] data=json(snapshot,width,height,localMap,stage,error).getBytes(StandardCharsets.UTF_8);
        try(var zip=new ZipOutputStream(Files.newOutputStream(destination,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE))) {
            var entry=new ZipEntry("diagnostico.json");entry.setTime(0);zip.putNextEntry(entry);
            zip.write(data);zip.closeEntry();
        }
    }
}
