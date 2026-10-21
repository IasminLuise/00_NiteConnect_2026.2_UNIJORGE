package br.edu.unijorge.experiments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.zip.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

class DocumentStoreTest {
    @TempDir Path root;
    static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    final UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    static class MutableClock extends Clock {
        Instant instant=NOW;
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return instant;}
    }
    final MutableClock clock=new MutableClock();
    DocumentStore store() throws IOException { return new DocumentStore(root, clock); }
    static byte[] docx(String text) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            entry(zip, "[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            entry(zip, "word/document.xml", "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>"+text+"</w:t></w:r></w:p></w:body></w:document>");
        }
        return bytes.toByteArray();
    }
    static void entry(ZipOutputStream zip, String name, String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); zip.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); zip.closeEntry();
    }
    static void status(int expected, org.junit.jupiter.api.function.Executable action) {
        assertEquals(expected, assertThrows(DocumentStore.Failure.class, action).status());
    }
    @Test void tokenCannotReadAnotherDocument() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));s.seed(b,"F02.docx",docx("two"));
        var t=s.issue(a,true,Duration.ofMinutes(10));status(404,()->s.read(b,t.token()));
    }
    @Test void readOnlyAndRevokedSessionsCannotSaveOrDownload() throws Exception {
        var s=store();var original=docx("one");s.seed(a,"F01.docx",original);
        var ro=s.issue(a,false,Duration.ofMinutes(10));var info=s.info(a,ro.token());
        status(403,()->s.save(a,ro.token(),docx("two"),info.modified(),null));
        s.revoke(ro.id());status(401,()->s.read(a,ro.token()));assertArrayEquals(original,s.current(a));
    }
    @Test void expiredSessionIsDenied() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofSeconds(1));
        clock.instant=NOW.plusSeconds(1);status(401,()->s.read(a,t.token()));
    }
    @Test void saveCreatesImmutableVersionAndSurvivesRestart() throws Exception {
        var s=store();var first=docx("one");var second=docx("two");s.seed(a,"F01.docx",first);
        var t=s.issue(a,true,Duration.ofMinutes(10));var before=s.info(a,t.token());
        s.save(a,t.token(),second,before.modified(),null);
        assertEquals(2,s.versions(a).size());assertArrayEquals(first,s.version(a,before.version()));
        assertArrayEquals(second,new DocumentStore(root,Clock.systemUTC()).current(a));
        assertNotEquals(before.modified(),s.info(a,t.token()).modified());
    }
    @Test void staleOrMissingBaseCannotOverwriteExistingContent() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofMinutes(10));var before=s.info(a,t.token());
        s.save(a,t.token(),docx("two"),before.modified(),null);
        status(409,()->s.save(a,t.token(),docx("three"),before.modified(),null));
        status(409,()->s.save(a,t.token(),docx("three"),null,null));assertEquals(2,s.versions(a).size());
    }
    @Test void mismatchedLockConflictsAndRefreshUnlockAreChecked() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofMinutes(10));
        s.lock(a,t.token(),"LOCK","lock-a",null);
        status(409,()->s.lock(a,t.token(),"REFRESH_LOCK","other",null));
        status(409,()->s.save(a,t.token(),docx("two"),s.info(a,t.token()).modified(),"other"));
        s.lock(a,t.token(),"REFRESH_LOCK","lock-a",null);
        s.lock(a,t.token(),"LOCK","lock-b","lock-a");
        s.lock(a,t.token(),"UNLOCK","lock-b",null);assertEquals("",s.lockValue(a));
    }
    @Test void expiredLockDoesNotBlockNewLock() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofHours(1));
        s.lock(a,t.token(),"LOCK","old",null);clock.instant=NOW.plusSeconds(1801);
        s.lock(a,t.token(),"LOCK","new",null);assertEquals("new",s.lockValue(a));
    }
    @Test void twoSavesWithSameBaseHaveExactlyOneWinner() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofMinutes(10));var base=s.info(a,t.token()).modified();
        var start=new CountDownLatch(1);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Integer> save=()->{start.await();try{s.save(a,t.token(),docx("new"),base,null);return 200;}catch(DocumentStore.Failure f){return f.status();}};
            var one=executor.submit(save);var two=executor.submit(save);start.countDown();
            assertEquals(java.util.Set.of(200,409),java.util.Set.of(one.get(),two.get()));assertEquals(2,s.versions(a).size());
        }
    }
    @Test void brokenStoragePreservesPreviousPointer() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofMinutes(10));var before=s.info(a,t.token());
        Path versions=root.resolve(a.toString()).resolve("versions");Path old=root.resolve(a.toString()).resolve("old-versions");
        Files.move(versions,old);Files.writeString(versions,"blocked");
        status(503,()->s.save(a,t.token(),docx("two"),before.modified(),null));
        Files.delete(versions);Files.move(old,versions);assertEquals(before.version(),s.info(a,t.token()).version());
    }
    @Test void invalidDocxAndActiveContentAreRejected() throws Exception {
        var s=store();status(415,()->s.seed(a,"F01.docx","not zip".getBytes()));
        var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)) {entry(zip,"word/vbaProject.bin","bad");}
        status(415,()->s.seed(a,"F01.docx",bytes.toByteArray()));
    }
    @Test void highlyCompressedArchiveIsBounded() throws Exception {
        var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){entry(zip,"word/document.xml","a".repeat(17_000_000));}
        var s=store();status(413,()->s.seed(a,"bomb.docx",bytes.toByteArray()));
    }
    @Test void historicalVersionFromAnotherDocumentIsNotReturned() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));s.seed(b,"F02.docx",docx("two"));
        status(404,()->s.version(a,s.versions(b).getFirst()));
    }
    @Test void equivalentTimestampFormattingDoesNotCauseFalseConflict() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofMinutes(10));
        s.save(a,t.token(),docx("two"),"2026-10-17T12:00:00.000000Z",null);
        assertEquals(2,s.versions(a).size());
    }
    @Test void codeMicrosecondTimestampCanSaveSourceCreatedWithNanosecondClock() throws Exception {
        clock.instant=NOW.plusNanos(123456789);
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofMinutes(10));
        s.save(a,t.token(),docx("two"),"2026-10-17T12:00:00.123456Z",null);
        assertEquals(2,s.versions(a).size());
    }
    @Test void truncatedZipSavePreservesPreviousVersion() throws Exception {
        var s=store();s.seed(a,"F01.docx",docx("one"));var t=s.issue(a,true,Duration.ofMinutes(10));var before=s.info(a,t.token());
        byte[] valid=docx("two");int central=-1;
        for(int i=0;i<valid.length-4;i++)if(valid[i]==0x50&&valid[i+1]==0x4b&&valid[i+2]==1&&valid[i+3]==2){central=i;break;}
        assertTrue(central>0);byte[] truncated=java.util.Arrays.copyOf(valid,central);
        status(415,()->s.save(a,t.token(),truncated,before.modified(),null));
        assertEquals(before.version(),s.info(a,t.token()).version());
    }
    @Test void emptyZipEntriesStillCountTowardsResourceLimit() throws Exception {
        var bytes=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(bytes);var input=new ZipInputStream(new ByteArrayInputStream(docx("one")))) {
            ZipEntry e;while((e=input.getNextEntry())!=null){zip.putNextEntry(new ZipEntry(e.getName()));input.transferTo(zip);zip.closeEntry();}
            for(int i=0;i<600;i++)entry(zip,"empty-"+i,"");
        }
        status(413,()->store().seed(a,"F01.docx",bytes.toByteArray()));
    }
    static byte[] docxWithRelationship(String type,String target)throws IOException {
        var bytes=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(bytes);var input=new ZipInputStream(new ByteArrayInputStream(docx("synthetic-link")))) {
            ZipEntry e;while((e=input.getNextEntry())!=null){zip.putNextEntry(new ZipEntry(e.getName()));input.transferTo(zip);zip.closeEntry();}
            entry(zip,"word/_rels/document.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"link1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/"+type+"\" TargetMode=\"External\" Target=\""+target+"\"/></Relationships>");
        }
        return bytes.toByteArray();
    }
    @Test void inertHttpsHyperlinkIsPreservedWithoutFetchingTarget()throws Exception {
        var original=docxWithRelationship("hyperlink","https://example.invalid/reference");
        var s=store();s.seed(a,"F01.docx",original);assertArrayEquals(original,s.current(a));
    }
    @Test void externalImagesAndNonHttpsOrCredentialedLinksStayBlocked()throws Exception {
        var s=store();
        status(415,()->s.seed(a,"F01.docx",docxWithRelationship("image","https://example.invalid/tracker.png")));
        for(String target:java.util.List.of("file:///private","javascript:alert(1)","http://example.invalid/","https://user:password@example.invalid/private"))
            status(415,()->s.seed(a,"F01.docx",docxWithRelationship("hyperlink",target)));
    }
}
