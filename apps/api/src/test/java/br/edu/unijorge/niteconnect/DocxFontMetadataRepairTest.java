package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.documents.application.DocxFontMetadataRepair;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocxFontMetadataRepairTest {
    private static final String FONT_TABLE = "word/fontTable.xml";
    private static final String SOURCE_FONTS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <w:fonts xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:font w:name="JetBrainsMono NF ExtraBold"><w:altName w:val="Cambria Math"/></w:font>
            </w:fonts>
            """;
    private static final String CANDIDATE_FONTS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <w:fonts xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:font w:name="JetBrainsMono NF ExtraBold"/>
            </w:fonts>
            """;
    private final DocxFontMetadataRepair repair = new DocxFontMetadataRepair(new br.edu.unijorge.niteconnect.documents.application.DocxIntegrity());

    @Test void restoresOnlyMissingAlternateNameForAnExistingFont() {
        byte[] source = packageWith(SOURCE_FONTS, document("source"));
        byte[] candidate = packageWith(CANDIDATE_FONTS, document("edited"));

        byte[] result = repair.restoreMissingAlternateNames(source, candidate);

        assertThat(xmlPart(result, FONT_TABLE)).contains("w:val=\"Cambria Math\"");
        assertThat(xmlPart(result, "word/document.xml")).contains("edited");
    }

    @Test void doesNotOverwriteAnExistingAlternateNameOrAddUnknownFonts() {
        byte[] source = packageWith(SOURCE_FONTS, document("source"));
        String candidateFonts = """
                <w:fonts xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:font w:name="JetBrainsMono NF ExtraBold"><w:altName w:val="Candidate Alias"/></w:font>
                  <w:font w:name="New Font"/>
                </w:fonts>
                """;
        byte[] candidate = packageWith(candidateFonts, document("edited"));

        byte[] result = repair.restoreMissingAlternateNames(source, candidate);

        assertThat(xmlPart(result, FONT_TABLE)).contains("Candidate Alias").doesNotContain("Cambria Math");
        assertThat(xmlPart(result, FONT_TABLE)).contains("New Font");
    }

    @Test void returnsTheOriginalBytesWhenNoSafeRepairApplies() {
        byte[] source = packageWith(SOURCE_FONTS, document("source"));
        byte[] candidate = packageWith("<w:fonts xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:font w:name=\"Other Font\"/></w:fonts>",
                document("edited"));

        assertThat(repair.restoreMissingAlternateNames(source, candidate)).isSameAs(candidate);
    }

    @Test void doesNotRestoreAliasesWithoutVisualEvidence() {
        byte[] source = packageWith("<w:fonts xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:font w:name=\"Another Font\"><w:altName w:val=\"Arial\"/></w:font></w:fonts>", document("source"));
        byte[] candidate = packageWith("<w:fonts xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:font w:name=\"Another Font\"/></w:fonts>", document("edited"));

        assertThat(repair.restoreMissingAlternateNames(source, candidate)).isSameAs(candidate);
    }

    @Test void leavesMalformedCandidateForTheExistingIntegrityGateToReject() {
        byte[] source = packageWith(SOURCE_FONTS, document("source"));
        byte[] malformedCandidate = new byte[]{1, 2, 3};

        assertThat(repair.restoreMissingAlternateNames(source, malformedCandidate)).isSameAs(malformedCandidate);
    }

    @Test void officialAv3CandidateReceivesOnlyTheMissingSourceAlias() throws Exception {
        String sourcePath = System.getProperty("b05.visual.sourceDocx");
        String candidatePath = System.getProperty("b05.visual.candidateDocx");
        org.junit.jupiter.api.Assumptions.assumeTrue(sourcePath != null && candidatePath != null,
                "Set private official-document paths to run the M2P8 Word evidence check");
        for (String suffix : new String[]{"", "2", "3"}) {
            sourcePath = System.getProperty("b05.visual.sourceDocx" + suffix, sourcePath);
            candidatePath = System.getProperty("b05.visual.candidateDocx" + suffix, candidatePath);
            byte[] source = Files.readAllBytes(Path.of(sourcePath));
            byte[] candidate = Files.readAllBytes(Path.of(candidatePath));
            byte[] repaired = repair.restoreMissingAlternateNames(source, candidate);

            assertThat(repaired).isNotSameAs(candidate);
            assertThat(xmlPart(repaired, FONT_TABLE)).contains("w:name=\"JetBrainsMono NF ExtraBold\"")
                    .contains("w:val=\"Cambria Math\"").doesNotContain("宋体", "w:val=\"Arial\"");
            assertThat(xmlPart(candidate, FONT_TABLE)).doesNotContain("w:val=\"Cambria Math\"");
            assertThat(xmlPart(repaired, "word/document.xml")).isEqualTo(xmlPart(candidate, "word/document.xml"));
            String outputPath = System.getProperty("b05.visual.repairedOutputDocx" + suffix,
                    suffix.isEmpty() ? null : System.getProperty("b05.visual.repairedOutputDocx"));
            if (outputPath != null) {
                Path output = Path.of(outputPath).toAbsolutePath().normalize();
                assertThat(output.toString()).contains(".private");
                Files.write(output, repaired, java.nio.file.StandardOpenOption.CREATE_NEW);
            }
        }
    }

    private static byte[] packageWith(String fontTable, String document) {
        Map<String, String> parts = Map.of(
                "[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"xml\" ContentType=\"application/xml\"/></Types>",
                "_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>",
                "word/document.xml", document,
                FONT_TABLE, fontTable);
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            for (var part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (Exception error) { throw new IllegalStateException(error); }
        return output.toByteArray();
    }

    private static String document(String text) {
        return "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>" + text + "</w:t></w:r></w:p></w:body></w:document>";
    }

    private static String xmlPart(byte[] bytes, String target) {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (target.equals(entry.getName())) return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new AssertionError("Missing DOCX part: " + target);
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
}
