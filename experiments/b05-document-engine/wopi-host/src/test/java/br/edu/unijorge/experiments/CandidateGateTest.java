package br.edu.unijorge.experiments;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CandidateGateTest {
    @TempDir Path root;
    @Test void rejectedCandidateCannotAdvanceCurrentOrCreateCommittedVersion() throws Exception {
        CandidateGate gate=(id, after, number)->{throw new DocumentStore.Failure(500,"CANDIDATE_REJECTED");};
        var store=new DocumentStore(root,Clock.systemUTC(),gate);
        var id=UUID.randomUUID();byte[] original=DocumentStoreTest.docx("one");store.seed(id,"F01.docx",original);
        var session=store.issue(id,true,Duration.ofMinutes(10));var info=store.info(id,session.token());
        DocumentStoreTest.status(500,()->store.save(id,session.token(),DocumentStoreTest.docx("two"),info.modified(),null));
        assertEquals(info.version(),store.currentVersion(id));assertEquals(1,store.versions(id).size());
        assertArrayEquals(original,store.current(id));
    }
    @Test void acceptedIndependentEditCreatesNewVersionAndOldBytesRemain() throws Exception {
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("XYZ")));
        var store=new DocumentStore(root,Clock.systemUTC(),gate);
        var id=UUID.randomUUID();byte[] original=DocumentStoreTest.docx("one");store.seed(id,"F01.docx",original);
        var session=store.issue(id,true,Duration.ofMinutes(10));var info=store.info(id,session.token());
        store.save(id,session.token(),DocumentStoreTest.docx("XYZone"),info.modified(),null);
        assertNotEquals(info.version(),store.currentVersion(id));assertEquals(2,store.versions(id).size());
        assertArrayEquals(original,store.version(id,info.version()));
        DocumentStoreTest.status(500,()->store.save(id,session.token(),DocumentStoreTest.docx("wrong"),store.info(id,session.token()).modified(),null));
        assertEquals(2,store.versions(id).size());
    }
    @Test void missingScriptDeniesSaveAndStaleBaseNeverInvokesGate() throws Exception {
        var store=new DocumentStore(root,Clock.systemUTC(),new ScriptedDocxGate(Map.of()));
        var id=UUID.randomUUID();store.seed(id,"F01.docx",DocumentStoreTest.docx("one"));
        var session=store.issue(id,true,Duration.ofMinutes(10));var info=store.info(id,session.token());
        DocumentStoreTest.status(409,()->store.save(id,session.token(),DocumentStoreTest.docx("one"),"2000-01-01T00:00:00Z",null));
        DocumentStoreTest.status(500,()->store.save(id,session.token(),DocumentStoreTest.docx("one"),info.modified(),null));
    }
    static byte[] withHeader(String instruction,String extra,String... cache) throws Exception {
        var out=new java.io.ByteArrayOutputStream();
        try(var zip=new java.util.zip.ZipOutputStream(out)) {
            DocumentStoreTest.entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            DocumentStoreTest.entry(zip,"word/document.xml","<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>one</w:t></w:r></w:p></w:body></w:document>");
            DocumentStoreTest.entry(zip,"word/header1.xml","<w:hdr xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:p><w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText>"+instruction+"</w:instrText></w:r><w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>"+(cache.length==0?"date cache":cache[0])+"</w:t></w:r><w:r><w:fldChar w:fldCharType=\"end\"/></w:r></w:p></w:hdr>");
            DocumentStoreTest.entry(zip,"word/media/image.png",extra);
        }
        return out.toByteArray();
    }
    @Test void adaptedSyntheticAcceptsExpectedNoopButRefAndMissingImageAreRejected() throws Exception {
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("","")));
        var store=new DocumentStore(root,Clock.systemUTC(),gate);var id=UUID.randomUUID();
        byte[] original=withHeader("SAVEDATE \\* MERGEFORMAT","synthetic image");store.seed(id,"F01.docx",original);
        var grant=store.issue(id,true,Duration.ofMinutes(10));var info=store.info(id,grant.token());
        store.save(id,grant.token(),original,info.modified(),null);String good=store.currentVersion(id);
        DocumentStoreTest.status(500,()->store.save(id,grant.token(),withHeader("REF \\* MERGEFORMAT","synthetic image"),store.info(id,grant.token()).modified(),null));
        DocumentStoreTest.status(500,()->store.save(id,grant.token(),withHeader("SAVEDATE \\* MERGEFORMAT","changed image"),store.info(id,grant.token()).modified(),null));
        assertEquals(good,store.currentVersion(id));assertArrayEquals(original,store.version(id,info.version()));
    }
    @Test void whitespaceOnlyHeaderTextCannotBeSilentlyRemoved() throws Exception {
        var store=new DocumentStore(root,Clock.systemUTC(),new ScriptedDocxGate(Map.of("F01.docx",List.of(""))));
        var id=UUID.randomUUID();store.seed(id,"F01.docx",withHeader("SAVEDATE","image"," "));
        var grant=store.issue(id,true,Duration.ofMinutes(10));var info=store.info(id,grant.token());
        DocumentStoreTest.status(500,()->store.save(id,grant.token(),withHeader("SAVEDATE","image",""),info.modified(),null));
        assertEquals(info.version(),store.currentVersion(id));
    }
    static byte[] withBody(String body) throws Exception {
        var out=new java.io.ByteArrayOutputStream();
        try(var zip=new java.util.zip.ZipOutputStream(out)) {
            DocumentStoreTest.entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            DocumentStoreTest.entry(zip,"word/document.xml","<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"+body+"</w:body></w:document>");
        }
        return out.toByteArray();
    }
    @Test void sameParagraphTextCannotMoveBetweenDifferentlyStyledRuns() throws Exception {
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("")));var id=UUID.randomUUID();
        gate.register(id,"F01.docx",withBody("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t>A</w:t></w:r><w:r><w:t>B</w:t></w:r></w:p>"));
        DocumentStoreTest.status(500,()->gate.validate(id,withBody("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t>AB</w:t></w:r><w:r><w:t></w:t></w:r></w:p>"),2));
    }
    @Test void sameParagraphTextCannotMoveIntoFieldCache() throws Exception {
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("")));var id=UUID.randomUUID();
        String start="<w:p><w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText>SAVEDATE</w:instrText></w:r><w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>";
        String between="</w:t></w:r><w:r><w:fldChar w:fldCharType=\"end\"/></w:r><w:r><w:t>";
        String end="</w:t></w:r></w:p>";
        gate.register(id,"F01.docx",withBody(start+"A"+between+"B"+end));
        DocumentStoreTest.status(500,()->gate.validate(id,withBody(start+"AB"+between+""+end),2));
    }
    @Test void cdataHeaderCacheChangesAreRejectedAndEquivalentSegmentsAccepted() throws Exception {
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("")));var id=UUID.randomUUID();
        gate.register(id,"F01.docx",withHeader("SAVEDATE","image","<![CDATA[cache A]]>"));
        DocumentStoreTest.status(500,()->gate.validate(id,withHeader("SAVEDATE","image","<![CDATA[cache B]]>"),2));
        gate.validate(id,withHeader("SAVEDATE","image","cache <![CDATA[A]]>"),2);
    }
}
