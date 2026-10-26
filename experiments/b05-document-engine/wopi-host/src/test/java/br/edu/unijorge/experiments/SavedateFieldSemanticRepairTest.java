package br.edu.unijorge.experiments;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

final class SavedateFieldSemanticRepairTest {
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String HEADER = "word/header1.xml";

    @Test
    void restoresOnlyRenamedSavedateAndKeepsEngineResult() throws Exception {
        byte[] source = packageWithField("SAVEDATE \\* MERGEFORMAT", "old result", "");
        byte[] candidate = packageWithField("REF  \\* MERGEFORMAT", "recalculated result", "");

        byte[] repaired = SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate);

        assertEquals("SAVEDATE \\* MERGEFORMAT", instruction(repaired));
        assertEquals("recalculated result", result(repaired));
        assertNotEquals(instruction(candidate), instruction(repaired));
    }

    @Test
    void restoresFieldWhenOnlyWordRunPropertiesWereDropped() throws Exception {
        byte[] source = packageWithField("SAVEDATE  \\* MERGEFORMAT", "23/09/2026 23:56:00",
                "<w:rPr><w:noProof/></w:rPr>", false);
        byte[] candidate = packageWithField("REF  \\* MERGEFORMAT", "23/09/2026 23:56:00", "", true);

        byte[] repaired = SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate);

        assertEquals("SAVEDATE  \\* MERGEFORMAT", instruction(repaired));
        assertEquals("23/09/2026 23:56:00", result(repaired));
    }

    @Test
    void restoresFieldWhenOnlyWordRevisionSessionMetadataWasDropped() throws Exception {
        byte[] source = packageWithFieldAndRunAttributes("SAVEDATE \\* MERGEFORMAT", "23/09/2026 23:56:00", "",
                false, " w:rsidR=\"004D097E\"");
        byte[] candidate = packageWithField("REF  \\* MERGEFORMAT", "23/09/2026 23:56:00", "");

        byte[] repaired = SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate);

        assertEquals("SAVEDATE \\* MERGEFORMAT", instruction(repaired));
        assertEquals("23/09/2026 23:56:00", result(repaired));
    }

    @Test
    void restoresArabicSwitchWhenOnlyNumpagesSwitchWasDropped() throws Exception {
        byte[] source = packageWithField("NUMPAGES  \\* Arabic  \\* MERGEFORMAT", "5", "");
        byte[] candidate = packageWithField("NUMPAGES \\* MERGEFORMAT", "5", "");

        byte[] repaired = SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate);

        assertEquals("NUMPAGES  \\* Arabic  \\* MERGEFORMAT", instruction(repaired));
        assertEquals("5", result(repaired));
    }

    @Test
    void leavesNumpagesUnchangedWhenCandidateHasAnotherSwitch() {
        byte[] source = packageWithField("NUMPAGES  \\* Arabic  \\* MERGEFORMAT", "5", "");
        byte[] candidate = packageWithField("NUMPAGES \\* Roman \\* MERGEFORMAT", "V", "");

        assertArrayEquals(candidate, SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate));
    }

    @Test
    void leavesNumpagesUnchangedWhenCandidateDroppedAnotherSwitchToo() {
        byte[] source = packageWithField("NUMPAGES  \\* Arabic  \\* MERGEFORMAT", "5", "");
        byte[] candidate = packageWithField("NUMPAGES", "5", "");

        assertArrayEquals(candidate, SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate));
    }

    @Test
    void leavesCandidateUnchangedWhenRefHasARealTarget() {
        byte[] source = packageWithField("SAVEDATE \\* MERGEFORMAT", "old result", "");
        byte[] candidate = packageWithField("REF ExistingBookmark \\* MERGEFORMAT", "new result", "");

        assertArrayEquals(candidate, SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate));
    }

    @Test
    void leavesCandidateUnchangedWhenFieldFormattingChanged() {
        byte[] source = packageWithField("SAVEDATE \\* MERGEFORMAT", "old result", "");
        byte[] candidate = packageWithField("REF  \\* MERGEFORMAT", "new result", "<w:rPr><w:b/></w:rPr>");

        assertArrayEquals(candidate, SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate));
    }

    @Test
    void leavesCandidateUnchangedWhenFieldCountChanged() {
        byte[] source = packageWithField("SAVEDATE \\* MERGEFORMAT", "old result", "");
        byte[] candidate = packageWithField("REF  \\* MERGEFORMAT", "new result", "", "<w:fldSimple w:instr=\"REF OtherBookmark\"><w:r><w:t>other</w:t></w:r></w:fldSimple>");

        assertArrayEquals(candidate, SavedateFieldSemanticRepair.repairKnownInstructionLoss(source, candidate));
    }

    private static byte[] packageWithField(String instruction, String value, String runProperties, String... extraFields) {
        return packageWithField(instruction, value, runProperties, false, extraFields);
    }

    private static byte[] packageWithField(String instruction, String value, String runProperties,
            boolean preserveXmlSpace, String... extraFields) {
        return packageWithFieldAndRunAttributes(instruction, value, runProperties, preserveXmlSpace, "", extraFields);
    }

    private static byte[] packageWithFieldAndRunAttributes(String instruction, String value, String runProperties,
            boolean preserveXmlSpace, String runAttributes, String... extraFields) {
        StringBuilder fields = new StringBuilder("<w:fldSimple w:instr=\"").append(instruction)
                .append("\"><w:r").append(runAttributes).append(">").append(runProperties).append("<w:t")
                .append(preserveXmlSpace ? " xml:space=\"preserve\"" : "").append(">").append(value)
                .append("</w:t></w:r></w:fldSimple>");
        for (String extra : extraFields) fields.append(extra);
        Map<String, String> parts = new HashMap<>();
        parts.put("[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/><Override PartName=\"/word/header1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml\"/></Types>");
        parts.put("_rels/.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
        parts.put("word/document.xml", "<w:document xmlns:w=\"" + W + "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><w:body><w:p/><w:sectPr><w:headerReference w:type=\"default\" r:id=\"rIdHeader\"/></w:sectPr></w:body></w:document>");
        parts.put("word/_rels/document.xml.rels", "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rIdHeader\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/header\" Target=\"header1.xml\"/></Relationships>");
        parts.put(HEADER, "<w:hdr xmlns:w=\"" + W + "\"><w:p>" + fields + "</w:p></w:hdr>");
        try {
            var output = new ByteArrayOutputStream();
            try (var zip = new ZipOutputStream(output)) {
                for (var part : parts.entrySet()) {
                    zip.putNextEntry(new ZipEntry(part.getKey()));
                    zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return output.toByteArray();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String instruction(byte[] docx) throws Exception {
        var field = xmlPart(docx).getElementsByTagNameNS(W, "fldSimple").item(0);
        return ((org.w3c.dom.Element) field).getAttributeNS(W, "instr");
    }

    private static String result(byte[] docx) throws Exception {
        var texts = xmlPart(docx).getElementsByTagNameNS(W, "t");
        return texts.item(0).getTextContent();
    }

    private static Document xmlPart(byte[] docx) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (var zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (HEADER.equals(entry.getName())) return factory.newDocumentBuilder().parse(zip);
            }
        }
        throw new IllegalArgumentException("Header part not found");
    }
}
