package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.documents.application.DocxIntegrity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Opt-in regression check against private official NITE DOCX files; never commits or embeds those files. */
class DocxIntegrityOfficialDocumentsTest {
    private static final String RELATIONSHIPS = "word/_rels/comments.xml.rels";
    private static final String COMMENTS = "word/comments.xml";
    private static final String PACKAGE_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private final DocxIntegrity guard = new DocxIntegrity();

    @Test void officialDocumentsConfirmNarrowOrphanRuleAndArabicSwitchPreservation() throws Exception {
        Path av3SourcePath = requiredPath("b05.official.av3Source");
        Path av3ExportPath = requiredPath("b05.official.av3Export");
        Path reportSourcePath = requiredPath("b05.official.reportSource");
        assumeTrue(av3SourcePath != null && av3ExportPath != null && reportSourcePath != null,
                "Set b05.official.av3Source, b05.official.av3Export and b05.official.reportSource for local official-document validation");

        byte[] av3Source = Files.readAllBytes(av3SourcePath);
        byte[] av3Export = Files.readAllBytes(av3ExportPath);
        byte[] reportSource = Files.readAllBytes(reportSourcePath);
        guard.validate(av3Source);
        guard.validate(av3Export);
        guard.validate(reportSource);

        Map<String, byte[]> reportParts = unpack(reportSource);
        assertThat(reportParts).doesNotContainKeys(RELATIONSHIPS, COMMENTS);
        assertThat(externalHttpsHyperlinkCount(reportParts)).isEqualTo(4);

        Map<String, byte[]> reportWithEditorPart = new LinkedHashMap<>(reportParts);
        reportWithEditorPart.put(RELATIONSHIPS, emptyRelationships());
        assertThat(guard.assess(reportSource, zip(reportWithEditorPart)).accepted())
                .as("real report remains text-editable when ONLYOFFICE adds only its empty orphan part")
                .isTrue();

        reportWithEditorPart.put(RELATIONSHIPS,
                ("<Relationships xmlns=\"" + PACKAGE_RELATIONSHIPS + "\"><Relationship Id=\"r1\" "
                        + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/comments\" "
                        + "Target=\"comments.xml\"/></Relationships>").getBytes(StandardCharsets.UTF_8));
        assertThat(guard.assess(reportSource, zip(reportWithEditorPart)).accepted())
                .as("a relationship-bearing comments part is not covered by the exception")
                .isFalse();

        reportWithEditorPart = new LinkedHashMap<>(reportParts);
        reportWithEditorPart.put(RELATIONSHIPS, emptyRelationships());
        reportWithEditorPart.put(COMMENTS, "<w:comments xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"/>"
                .getBytes(StandardCharsets.UTF_8));
        assertThat(guard.assess(reportSource, zip(reportWithEditorPart)).accepted())
                .as("comments content is not covered by the exception")
                .isFalse();

        Map<String, byte[]> av3ExportParts = unpack(av3Export);
        assertThat(av3ExportParts).containsKey(RELATIONSHIPS).doesNotContainKey(COMMENTS);
        assertThat(isEmptyRelationships(av3ExportParts.get(RELATIONSHIPS))).isTrue();

        String sourceArabic = fieldInstructions(unpack(av3Source));
        String exportArabic = fieldInstructions(av3ExportParts);
        assertThat(sourceArabic).contains("NUMPAGES  \\* Arabic");
        assertThat(exportArabic).contains("NUMPAGES  \\* Arabic");
        assertThat(guard.assess(av3Source, av3Export).accepted())
                .as("the full ONLYOFFICE export remains subject to all other strict gate checks")
                .isFalse();
    }

    private static Path requiredPath(String property) {
        String value = System.getProperty(property);
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    private static Map<String, byte[]> unpack(byte[] docx) throws Exception {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                parts.put(entry.getName(), zip.readAllBytes());
            }
        }
        return parts;
    }

    private static byte[] zip(Map<String, byte[]> parts) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            for (var part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] emptyRelationships() {
        return ("<Relationships xmlns=\"" + PACKAGE_RELATIONSHIPS + "\"/>").getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isEmptyRelationships(byte[] xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml)).getDocumentElement();
        if (!PACKAGE_RELATIONSHIPS.equals(root.getNamespaceURI()) || !"Relationships".equals(root.getLocalName())) return false;
        var children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            var child = children.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE
                    || child.getNodeType() == org.w3c.dom.Node.TEXT_NODE && !child.getNodeValue().isBlank()) return false;
        }
        return true;
    }

    private static long externalHttpsHyperlinkCount(Map<String, byte[]> parts) {
        return parts.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith(".rels"))
                .flatMap(entry -> java.util.regex.Pattern.compile("<Relationship\\b[^>]*>")
                        .matcher(new String(entry.getValue(), StandardCharsets.UTF_8)).results())
                .map(java.util.regex.MatchResult::group)
                .filter(relationship -> relationship.contains("TargetMode=\"External\"")
                        && relationship.contains("/hyperlink") && relationship.contains("Target=\"https://"))
                .count();
    }

    private static String fieldInstructions(Map<String, byte[]> parts) {
        StringBuilder instructions = new StringBuilder();
        parts.forEach((name, bytes) -> {
            if (name.startsWith("word/") && name.endsWith(".xml")) {
                String xml = new String(bytes, StandardCharsets.UTF_8);
                java.util.regex.Pattern.compile("NUMPAGES[^\"<]*")
                        .matcher(xml).results().forEach(match -> instructions.append(match.group()).append('\n'));
            }
        });
        return instructions.toString();
    }
}
