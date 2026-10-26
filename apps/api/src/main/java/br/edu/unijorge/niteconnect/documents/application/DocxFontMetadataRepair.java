package br.edu.unijorge.niteconnect.documents.application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.helpers.DefaultHandler;

/** Restores a source font's missing OOXML fallback alias in an editor export. */
@Component
public class DocxFontMetadataRepair {
    private static final String FONT_TABLE = "word/fontTable.xml";
    private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final Map<String, String> VISUALLY_VERIFIED_ALIASES = Map.of(
            "JetBrainsMono NF ExtraBold", "Cambria Math");
    private final DocxIntegrity integrity;

    public DocxFontMetadataRepair(DocxIntegrity integrity) { this.integrity = integrity; }

    public byte[] restoreMissingAlternateNames(byte[] source, byte[] candidate) {
        integrity.validate(source);
        try {
            integrity.validate(candidate);
            Map<String, byte[]> sourceParts = unpack(source);
            Map<String, byte[]> candidateParts = unpack(candidate);
            byte[] sourceFontTable = sourceParts.get(FONT_TABLE);
            byte[] candidateFontTable = candidateParts.get(FONT_TABLE);
            if (sourceFontTable == null || candidateFontTable == null) return candidate;

            var sourceDocument = parse(sourceFontTable);
            var candidateDocument = parse(candidateFontTable);
            Map<String, String> aliases = uniqueAliases(sourceDocument.getDocumentElement());
            boolean changed = false;
            var fonts = candidateDocument.getElementsByTagNameNS(WORD, "font");
            for (int i = 0; i < fonts.getLength(); i++) {
                Element font = (Element) fonts.item(i);
                String name = attribute(font, "name");
                String alias = aliases.get(name);
                if (name.isBlank() || alias == null || hasAlternateName(font)) continue;
                String prefix = wordPrefix(font);
                Element alternate = candidateDocument.createElementNS(WORD, prefix + ":altName");
                alternate.setAttributeNS(WORD, prefix + ":val", alias);
                Node first = font.getFirstChild();
                if (first == null) font.appendChild(alternate); else font.insertBefore(alternate, first);
                changed = true;
            }
            if (!changed) return candidate;
            candidateParts.put(FONT_TABLE, serialize(candidateDocument));
            byte[] repaired = pack(candidateParts);
            integrity.validate(repaired);
            return repaired;
        } catch (Exception error) {
            return candidate;
        }
    }

    private static Map<String, String> uniqueAliases(Element root) {
        Map<String, String> aliases = new HashMap<>();
        java.util.Set<String> ambiguous = new java.util.HashSet<>();
        var fonts = root.getElementsByTagNameNS(WORD, "font");
        for (int i = 0; i < fonts.getLength(); i++) {
            Element font = (Element) fonts.item(i);
            String name = attribute(font, "name");
            String alias = alternateName(font);
            if (!VISUALLY_VERIFIED_ALIASES.containsKey(name)
                    || !VISUALLY_VERIFIED_ALIASES.get(name).equals(alias)) continue;
            if (ambiguous.contains(name)) continue;
            String existing = aliases.putIfAbsent(name, alias);
            if (existing != null && !existing.equals(alias)) {
                aliases.remove(name);
                ambiguous.add(name);
            }
        }
        return aliases;
    }

    private static boolean hasAlternateName(Element font) {
        return font.getElementsByTagNameNS(WORD, "altName").getLength() > 0;
    }

    private static String alternateName(Element font) {
        var aliases = font.getElementsByTagNameNS(WORD, "altName");
        if (aliases.getLength() == 0) return "";
        return ((Element) aliases.item(0)).getAttributeNS(WORD, "val");
    }

    private static String attribute(Element element, String localName) {
        return element.getAttributeNS(WORD, localName);
    }

    private static String wordPrefix(Element element) {
        String prefix = element.lookupPrefix(WORD);
        return prefix == null || prefix.isBlank() ? "w" : prefix;
    }

    private static org.w3c.dom.Document parse(byte[] xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        return builder.parse(new ByteArrayInputStream(xml));
    }

    private static byte[] serialize(org.w3c.dom.Document document) throws Exception {
        var transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        var output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private static Map<String, byte[]> unpack(byte[] bytes) throws Exception {
        Map<String, byte[]> parts = new java.util.LinkedHashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (parts.putIfAbsent(entry.getName(), zip.readAllBytes()) != null)
                    throw new IllegalArgumentException("Entrada DOCX inválida");
            }
        }
        return parts;
    }

    private static byte[] pack(Map<String, byte[]> parts) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(output)) {
            for (var part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue());
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
