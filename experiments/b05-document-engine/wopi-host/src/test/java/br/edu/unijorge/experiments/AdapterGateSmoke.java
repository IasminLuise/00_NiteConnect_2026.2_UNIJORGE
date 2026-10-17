package br.edu.unijorge.experiments;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;

/** CLI cross-language synthetic check, called explicitly after Maven by adapter_ci.py. */
public final class AdapterGateSmoke {
    public static void main(String[] args)throws Exception {
        byte[] adapted=Files.readAllBytes(Path.of(args[0]));
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("","")));
        var store=new DocumentStore(Path.of(args[1]),Clock.systemUTC(),gate);var id=UUID.randomUUID();
        store.seed(id,"F01.docx",adapted);var grant=store.issue(id,true,Duration.ofMinutes(10));
        var original=store.info(id,grant.token());
        store.save(id,grant.token(),adapted,original.modified(),null);
        String accepted=store.currentVersion(id);
        var out=new ByteArrayOutputStream();
        try(var source=new ZipInputStream(new ByteArrayInputStream(adapted));var target=new ZipOutputStream(out)) {
            ZipEntry entry;while((entry=source.getNextEntry())!=null) {
                byte[] bytes=source.readAllBytes();
                if(entry.getName().equals("word/header1.xml"))bytes=new String(bytes,java.nio.charset.StandardCharsets.UTF_8).replace("SAVEDATE", "REF").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                target.putNextEntry(new ZipEntry(entry.getName()));target.write(bytes);target.closeEntry();
            }
        }
        try {
            store.save(id,grant.token(),out.toByteArray(),store.info(id,grant.token()).modified(),null);
            throw new AssertionError("REF regression accepted");
        }catch(DocumentStore.Failure failure){if(failure.status()!=500)throw failure;}
        if(!accepted.equals(store.currentVersion(id))||store.versions(id).size()!=2||!Arrays.equals(adapted,store.version(id,original.version())))throw new AssertionError("Committed history changed");
        System.out.println("PASS adapted fixture accepted; REF rejected; pointer/history retained");
    }
}
