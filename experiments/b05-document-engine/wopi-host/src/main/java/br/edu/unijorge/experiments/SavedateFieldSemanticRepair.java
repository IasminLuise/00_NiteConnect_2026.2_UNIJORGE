package br.edu.unijorge.experiments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Restores only the verified SAVEDATE rename and NUMPAGES Arabic-switch loss in simple fields. */
final class SavedateFieldSemanticRepair {
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String XML = "http://www.w3.org/XML/1998/namespace";
    private static final String INSTRUCTION = "instr";

    private SavedateFieldSemanticRepair() {}

    static byte[] repairKnownInstructionLoss(byte[] source, byte[] candidate) {
        DocxValidator.validate(source);
        DocxValidator.validate(candidate);
        try {
            Map<String, Document> sourceStories = stories(source);
            Map<String, Document> candidateStories = stories(candidate);
            Map<String, byte[]> replacements = new LinkedHashMap<>();
            boolean changed = false;

            for (var entry : sourceStories.entrySet()) {
                String part = entry.getKey();
                Document sourceStory = entry.getValue();
                List<Element> sourceFields = simpleFields(sourceStory);
                boolean containsRepairableField = sourceFields.stream().anyMatch(field ->
                        isSavedate(field) || hasArabicNumpagesSwitch(field.getAttributeNS(W, INSTRUCTION)));
                if (!containsRepairableField) continue;

                Document candidateStory = candidateStories.get(part);
                if (candidateStory == null) return candidate;
                List<Element> candidateFields = simpleFields(candidateStory);
                if (sourceFields.size() != candidateFields.size()) return candidate;
                boolean partChanged = false;

                for (int index = 0; index < sourceFields.size(); index++) {
                    Element sourceField = sourceFields.get(index);
                    Element candidateField = candidateFields.get(index);
                    String sourceInstruction = sourceField.getAttributeNS(W, INSTRUCTION);
                    String candidateInstruction = candidateField.getAttributeNS(W, INSTRUCTION);
                    boolean sourceSavedate = isSavedate(sourceField);
                    boolean sourceArabicNumpages = hasArabicNumpagesSwitch(sourceInstruction);
                    if (!sourceSavedate && !sourceArabicNumpages) continue;

                    if (sourceInstruction.equals(candidateInstruction)) continue;
                    boolean renamedSavedate = sourceSavedate && isEmptyRef(candidateInstruction);
                    boolean droppedArabicSwitch = sourceArabicNumpages
                            && isNumpagesWithoutArabicSwitch(sourceInstruction, candidateInstruction);
                    if ((!renamedSavedate && !droppedArabicSwitch)
                            || !sameFieldShape(sourceField, candidateField)) return candidate;

                    candidateField.setAttributeNS(W, "w:" + INSTRUCTION, sourceInstruction);
                    changed = true;
                    partChanged = true;
                }
                if (partChanged) replacements.put(part, serialize(candidateStory));
            }

            return changed ? replaceParts(candidate, replacements) : candidate;
        } catch (Exception unproven) {
            // Fail closed: an unparseable or structurally different field remains subject to the gate.
            return candidate;
        }
    }

    private static boolean isSavedate(Element field) {
        return firstToken(field.getAttributeNS(W, INSTRUCTION)).equalsIgnoreCase("SAVEDATE");
    }

    private static boolean isEmptyRef(String instruction) {
        String normalized = instruction.trim().replaceAll("\\s+", " ");
        return normalized.equalsIgnoreCase("REF")
                || normalized.equalsIgnoreCase("REF \\* MERGEFORMAT");
    }

    private static boolean hasArabicNumpagesSwitch(String instruction) {
        List<String> tokens = normalizedTokens(instruction);
        return equalsTokens(tokens, "NUMPAGES", "\\*", "Arabic")
                || equalsTokens(tokens, "NUMPAGES", "\\*", "Arabic", "\\*", "MERGEFORMAT");
    }

    private static boolean isNumpagesWithoutArabicSwitch(String sourceInstruction, String candidateInstruction) {
        List<String> sourceTokens = normalizedTokens(sourceInstruction);
        List<String> expectedTokens = new ArrayList<>(sourceTokens);
        for (int index = 1; index + 1 < expectedTokens.size(); index++) {
            if (expectedTokens.get(index).equalsIgnoreCase("\\*")
                    && expectedTokens.get(index + 1).equalsIgnoreCase("Arabic")) {
                expectedTokens.subList(index, index + 2).clear();
                return equalsTokens(normalizedTokens(candidateInstruction), expectedTokens.toArray(String[]::new));
            }
        }
        return false;
    }

    private static List<String> normalizedTokens(String instruction) {
        String normalized = instruction.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? List.of() : List.of(normalized.split(" "));
    }

    private static boolean equalsTokens(List<String> actual, String... expected) {
        if (actual.size() != expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if (!actual.get(index).equalsIgnoreCase(expected[index])) return false;
        }
        return true;
    }

    private static String firstToken(String instruction) {
        String trimmed = instruction.trim();
        int separator = trimmed.indexOf(' ');
        if (separator < 0) separator = trimmed.indexOf('\t');
        return separator < 0 ? trimmed : trimmed.substring(0, separator);
    }

    private static boolean sameFieldShape(Element left, Element right) {
        return sameFieldShape(left, right, true);
    }

    private static boolean sameFieldShape(Element left, Element right, boolean fieldRoot) {
        if (!sameElement(left, right, fieldRoot)) return false;
        List<Node> leftChildren = structuralChildren(left);
        List<Node> rightChildren = structuralChildren(right);
        if (leftChildren.size() != rightChildren.size()) return false;
        for (int index = 0; index < leftChildren.size(); index++) {
            Node a = leftChildren.get(index);
            Node b = rightChildren.get(index);
            if (a instanceof Element ae && b instanceof Element be) {
                if (!sameFieldShape(ae, be, false)) return false;
            } else if (a.getNodeType() != b.getNodeType()) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameElement(Element left, Element right, boolean fieldRoot) {
        if (!left.getNamespaceURI().equals(right.getNamespaceURI())
                || !left.getLocalName().equals(right.getLocalName())) return false;
        Map<String, String> leftAttributes = attributes(left, fieldRoot);
        Map<String, String> rightAttributes = attributes(right, fieldRoot);
        return leftAttributes.equals(rightAttributes);
    }

    private static Map<String, String> attributes(Element element, boolean fieldRoot) {
        var result = new TreeMap<String, String>();
        for (int index = 0; index < element.getAttributes().getLength(); index++) {
            Node attribute = element.getAttributes().item(index);
            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) continue;
            if (fieldRoot && W.equals(attribute.getNamespaceURI()) && INSTRUCTION.equals(attribute.getLocalName())) continue;
            // Word revision-session IDs are volatile metadata and do not affect field rendering or behavior.
            if (W.equals(attribute.getNamespaceURI()) && attribute.getLocalName().startsWith("rsid")) continue;
            if (W.equals(element.getNamespaceURI()) && "t".equals(element.getLocalName())
                    && XML.equals(attribute.getNamespaceURI()) && "space".equals(attribute.getLocalName())) continue;
            result.put("{" + attribute.getNamespaceURI() + "}" + attribute.getLocalName(), attribute.getNodeValue());
        }
        return result;
    }

    private static List<Node> structuralChildren(Element element) {
        var result = new ArrayList<Node>();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element nested) {
                if (!isEmptyProofingProperties(nested)) result.add(child);
            }
            else if (child.getNodeType() == Node.TEXT_NODE && !child.getTextContent().isBlank()) result.add(child);
            else if (child.getNodeType() != Node.TEXT_NODE && child.getNodeType() != Node.COMMENT_NODE) result.add(child);
        }
        return result;
    }

    private static boolean isEmptyProofingProperties(Element element) {
        if (!isWordElement(element, "rPr") || element.hasAttributes()) return false;
        int proofingProperties = 0;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element nested) {
                if (!isWordElement(nested, "noProof") || nested.hasAttributes()) return false;
                proofingProperties++;
            } else if (child.getNodeType() != Node.TEXT_NODE || !child.getTextContent().isBlank()) {
                return false;
            }
        }
        return proofingProperties == 1;
    }

    private static boolean isWordElement(Element element, String localName) {
        return W.equals(element.getNamespaceURI()) && localName.equals(element.getLocalName());
    }

    private static Map<String, Document> stories(byte[] packageBytes) throws Exception {
        var result = new LinkedHashMap<String, Document>();
        var factory = secureFactory();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(packageBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (isStory(entry.getName())) {
                    result.put(entry.getName(), factory.newDocumentBuilder().parse(new ByteArrayInputStream(zip.readAllBytes())));
                }
            }
        }
        return result;
    }

    private static boolean isStory(String part) {
        return part.equals("word/document.xml")
                || part.matches("word/(?:header|footer)\\d+\\.xml")
                || part.equals("word/footnotes.xml")
                || part.equals("word/endnotes.xml");
    }

    private static List<Element> simpleFields(Document story) {
        var nodes = story.getElementsByTagNameNS(W, "fldSimple");
        var result = new ArrayList<Element>(nodes.getLength());
        for (int index = 0; index < nodes.getLength(); index++) result.add((Element) nodes.item(index));
        return result;
    }

    private static byte[] serialize(Document document) throws Exception {
        var factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        var output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private static byte[] replaceParts(byte[] packageBytes, Map<String, byte[]> replacements) throws IOException {
        var output = new ByteArrayOutputStream(packageBytes.length);
        try (var input = new ZipInputStream(new ByteArrayInputStream(packageBytes));
             var zip = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] bytes = replacements.get(entry.getName());
                if (bytes == null) bytes = input.readAllBytes();
                else input.readAllBytes();
                ZipEntry copied = new ZipEntry(entry.getName());
                if (entry.getTime() >= 0) copied.setTime(entry.getTime());
                if (entry.getComment() != null) copied.setComment(entry.getComment());
                if (entry.getExtra() != null) copied.setExtra(entry.getExtra());
                if (entry.getMethod() == ZipEntry.STORED) {
                    var crc = new CRC32();
                    crc.update(bytes);
                    copied.setMethod(ZipEntry.STORED);
                    copied.setSize(bytes.length);
                    copied.setCompressedSize(bytes.length);
                    copied.setCrc(crc.getValue());
                }
                zip.putNextEntry(copied);
                zip.write(bytes);
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private static DocumentBuilderFactory secureFactory() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}
