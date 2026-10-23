package com.projetok.studio;
import java.nio.file.*;
import java.util.Locale;
/** Isola o custo do primeiro temporário JDK em processo novo. */
public final class StudioTempProbe {
    public static void main(String[] args)throws Exception {
        Path folder=Path.of(args[0]);Files.createDirectory(folder);long start=System.nanoTime();Path first=Files.createTempFile(folder,"first-",".tmp");double firstMs=(System.nanoTime()-start)/1e6;start=System.nanoTime();Path second=Files.createTempFile(folder,"second-",".tmp");double secondMs=(System.nanoTime()-start)/1e6;Files.delete(first);Files.delete(second);String json=String.format(Locale.ROOT,"{\"firstJdkTempMs\":%.4f,\"secondJdkTempMs\":%.4f}",firstMs,secondMs);Files.writeString(folder.resolve("metrics.json"),json);System.out.println(json);
    }
}
