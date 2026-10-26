package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.documents.application.DocxIntegrity;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DocxIntegrityTest {
    private final DocxIntegrity guard = new DocxIntegrity();
    private static final String DOCUMENT = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:pPr><w:spacing w:after=\"100\"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii=\"Arial\"/></w:rPr><w:t>Original</w:t></w:r><w:r><w:instrText> PAGE </w:instrText></w:r></w:p></w:body></w:document>";

    @Test void permitsOnlyMainTextAndBoundedCoreMetadataChanges() throws Exception {
        var source = parts();
        var candidate = parts();
        candidate.put("word/document.xml", DOCUMENT.replace("Original", "Edited &amp; safe"));
        candidate.put("docProps/core.xml", "<core>updated</core>");
        assertThat(guard.assess(zip(source), zip(candidate)).accepted()).isTrue();
    }

    @Test void rejectsFormattingSpacingFieldsStructureAndLostParts() throws Exception {
        byte[] source = zip(parts());
        for (String changed : new String[]{DOCUMENT.replace("Arial", "Calibri"), DOCUMENT.replace("100", "101"),
                DOCUMENT.replace(" PAGE ", " DATE "), DOCUMENT.replace("<w:rPr>", "<w:rPr><w:b/>"),
                DOCUMENT.replace("<w:t>Original</w:t>", "<w:t>Original</w:t><w:t>added node</w:t>"),
                DOCUMENT.replace("<w:body>", "<w:body><w:tbl/>")}) {
            var candidate = parts(); candidate.put("word/document.xml", changed);
            assertThat(guard.assess(source, zip(candidate)).accepted()).isFalse();
        }
        var candidate = parts(); candidate.remove("word/header1.xml");
        assertThat(guard.assess(source, zip(candidate)).accepted()).isFalse();
        candidate = parts(); candidate.put("word/header1.xml", "<header>Changed</header>");
        assertThat(guard.assess(source, zip(candidate)).accepted()).isFalse();
    }

    @Test void failsClosedForMalformedOrMissingPackages() throws Exception {
        assertThatThrownBy(() -> guard.validate(new byte[]{1,2})).isInstanceOf(ApiException.class);
        var missing = parts(); missing.remove("[Content_Types].xml");
        assertThatThrownBy(() -> guard.validate(zip(missing))).isInstanceOf(ApiException.class);
        assertThat(guard.assess(zip(parts()), new byte[]{1}).accepted()).isFalse();
    }

    @Test void rejectsTraversalMacrosExternalRelationshipsAndXmlEntities() throws Exception {
        for (String path : new String[]{"../escape", "/absolute", "word/../escape", "word\\escape", "word/vbaProject.bin"}) {
            var bad = parts(); bad.put(path, "bad");
            assertThatThrownBy(() -> guard.validate(zip(bad))).isInstanceOf(ApiException.class);
        }
        var external = parts();
        external.put("_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"r1\" TargetMode=\"External\" Target=\"https://example.org\" Type=\"link\"/></Relationships>");
        assertThatThrownBy(() -> guard.validate(zip(external))).isInstanceOf(ApiException.class);
        var entity = parts(); entity.put("word/document.xml", "<!DOCTYPE x [<!ENTITY x SYSTEM 'file:///private'>]><x>&x;</x>");
        assertThatThrownBy(() -> guard.validate(zip(entity))).isInstanceOf(ApiException.class);
    }

    @Test void preservesOnlySecureExternalHyperlinksWithoutDereferencingThem() throws Exception {
        var linked = parts();
        linked.put("word/_rels/document.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"link1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\" Target=\"https://example.org/path\" TargetMode=\"External\"/></Relationships>");
        byte[] original = zip(linked);

        assertThatCode(() -> guard.validate(original)).doesNotThrowAnyException();
        assertThat(guard.assess(original, original).accepted()).isTrue();

        for (String relation : new String[]{
                "<Relationship Id=\"link1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\" Target=\"http://example.org\" TargetMode=\"External\"/>",
                "<Relationship Id=\"link1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink\" Target=\"javascript:alert(1)\" TargetMode=\"External\"/>",
                "<Relationship Id=\"link1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"https://example.org/image.png\" TargetMode=\"External\"/>"}) {
            var unsafe = parts();
            unsafe.put("word/_rels/document.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" + relation + "</Relationships>");
            assertThatThrownBy(() -> guard.validate(zip(unsafe))).isInstanceOf(ApiException.class);
        }
    }

    @Test void rejectsInflatedEntryAndExcessiveEntryCount() {
        var large = parts(); large.put("word/large.xml", "a".repeat(8 * 1024 * 1024 + 1));
        assertThatThrownBy(() -> guard.validate(zip(large))).isInstanceOf(ApiException.class);
        var many = parts(); for (int i=0; i<1025; i++) many.put("word/item"+i, "x");
        assertThatThrownBy(() -> guard.validate(zip(many))).isInstanceOf(ApiException.class);
        var total = parts();
        for (int i=0; i<5; i++) total.put("word/blob"+i, "a".repeat(7 * 1024 * 1024));
        assertThatThrownBy(() -> guard.validate(zip(total))).isInstanceOf(ApiException.class);
        var metadata = parts(); metadata.put("docProps/core.xml", "<core>"+"a".repeat(65536)+"</core>");
        assertThatThrownBy(() -> guard.validate(zip(metadata))).isInstanceOf(ApiException.class);
    }

    @Test void rejectsDuplicateEntryAndTruncatedCentralDirectory() throws Exception {
        var entries = parts(); entries.put("word/aaa", "1"); entries.put("word/bbb", "2");
        byte[] duplicate = zip(entries);
        byte[] from = "word/bbb".getBytes(StandardCharsets.UTF_8);
        byte[] to = "word/aaa".getBytes(StandardCharsets.UTF_8);
        for (int i=0; i<=duplicate.length-from.length; i++) {
            if (java.util.Arrays.equals(java.util.Arrays.copyOfRange(duplicate, i, i+from.length), from))
                System.arraycopy(to,0,duplicate,i,to.length);
        }
        assertThatThrownBy(() -> guard.validate(duplicate)).isInstanceOf(ApiException.class);
        byte[] valid = zip(parts());
        assertThatThrownBy(() -> guard.validate(java.util.Arrays.copyOf(valid, valid.length-22))).isInstanceOf(ApiException.class);
    }

    @Test void protectsRelationshipsNotesAndMetadataPresence() throws Exception {
        var source = parts(); source.put("word/footnotes.xml", "<notes>protected</notes>");
        source.put("word/_rels/document.xml.rels", "<Relationships/>");
        for (String part : new String[]{"word/footnotes.xml", "word/_rels/document.xml.rels"}) {
            var candidate = new LinkedHashMap<>(source); candidate.put(part, "<changed/>");
            assertThat(guard.assess(zip(source), zip(candidate)).accepted()).isFalse();
        }
        var missing = parts(); missing.remove("docProps/core.xml");
        assertThat(guard.assess(zip(parts()), zip(missing)).accepted()).isFalse();
    }

    @Test void protectsCommentsAndProcessingInstructionsOutsideMainRoot() throws Exception {
        var candidate = parts(); candidate.put("word/document.xml", "<?changed value?>"+DOCUMENT);
        assertThat(guard.assess(zip(parts()), zip(candidate)).accepted()).isFalse();
        candidate.put("word/document.xml", DOCUMENT+"<!--added-->");
        assertThat(guard.assess(zip(parts()), zip(candidate)).accepted()).isFalse();
    }

    @Test void rejectsCentralDirectoryThatDisagreesWithLocalEntries() throws Exception {
        byte[] malformed = zip(parts());
        for (int i=0; i<malformed.length-46; i++) {
            if (malformed[i]==0x50 && malformed[i+1]==0x4b && malformed[i+2]==1 && malformed[i+3]==2) {
                malformed[i+46] = 'X';
                break;
            }
        }
        assertThatThrownBy(() -> guard.validate(malformed)).isInstanceOf(ApiException.class);
        byte[] badChecksum = zip(parts());
        for (int i=0; i<badChecksum.length-46; i++) {
            if (badChecksum[i]==0x50 && badChecksum[i+1]==0x4b && badChecksum[i+2]==1 && badChecksum[i+3]==2) {
                badChecksum[i+16] ^= 1;
                break;
            }
        }
        assertThatThrownBy(() -> guard.validate(badChecksum)).isInstanceOf(ApiException.class);
    }

    private static Map<String,String> parts() {
        Map<String,String> entries = new LinkedHashMap<>();
        entries.put("[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
        entries.put("_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"r1\" Target=\"word/document.xml\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\"/></Relationships>");
        entries.put("word/document.xml", DOCUMENT);
        entries.put("word/header1.xml", "<header>Protected</header>");
        entries.put("docProps/core.xml", "<core>original</core>");
        return entries;
    }

    private static byte[] zip(Map<String,String> parts) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (var entry : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
