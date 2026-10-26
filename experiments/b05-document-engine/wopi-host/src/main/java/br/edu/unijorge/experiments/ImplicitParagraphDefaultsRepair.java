package br.edu.unijorge.experiments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Narrow, source-aware repair for the synthetic empty paragraph defaults emitted by Docs API. */
final class ImplicitParagraphDefaultsRepair {
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String STYLES = "word/styles.xml";

    private ImplicitParagraphDefaultsRepair() {}

    static byte[] repairIfSynthesized(byte[] source, byte[] candidate) {
        DocxValidator.validate(source);
        DocxValidator.validate(candidate);
        try {
            Document sourceStyles = styles(source);
            Document candidateStyles = styles(candidate);
            if (sourceStyles == null || candidateStyles == null
                    || sourceStyles.getElementsByTagNameNS(W, "pPrDefault").getLength() != 0) {
                return candidate;
            }
            var defaults = candidateStyles.getElementsByTagNameNS(W, "pPrDefault");
            if (defaults.getLength() != 1) return candidate;
            Element generated = (Element) defaults.item(0);
            if (!isExactNoOpDefault(generated)) return candidate;
            generated.getParentNode().removeChild(generated);

            byte[] repairedStyles = serialize(candidateStyles);
            return replacePart(candidate, repairedStyles);
        } catch (Exception failure) {
            // Fail closed: if the narrow transformation cannot be proven, leave the candidate intact.
            return candidate;
        }
    }

    private static boolean isExactNoOpDefault(Element defaults) {
        if (defaults.hasAttributes()) return false;
        Element pPr = onlyElementChild(defaults);
        if (pPr == null || !isWordElement(pPr, "pPr") || !hasOnlyElementsAndWhitespace(defaults)) return false;
        if (pPr.hasAttributes()) return false;
        Element border = nextElement(pPr, 0);
        Element spacing = nextElement(pPr, 1);
        Element indent = nextElement(pPr, 2);
        if (border == null || spacing == null || indent == null
                || !isWordElement(border, "pBdr") || !isWordElement(spacing, "spacing")
                || !isWordElement(indent, "ind") || elementChildCount(pPr) != 3
                || !hasOnlyElementsAndWhitespace(pPr)) return false;
        return isEmptyElement(border) && isEmptyElement(spacing) && isEmptyElement(indent);
    }

    private static boolean isEmptyElement(Element element) {
        return !element.hasAttributes() && elementChildCount(element) == 0 && hasOnlyWhitespace(element);
    }

    private static Element onlyElementChild(Element parent) {
        if (elementChildCount(parent) != 1) return null;
        return nextElement(parent, 0);
    }

    private static Element nextElement(Element parent, int index) {
        int current = 0;
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element) {
                if (current++ == index) return element;
            } else if (child.getNodeType() != Node.TEXT_NODE || !child.getTextContent().isBlank()) {
                return null;
            }
        }
        return null;
    }

    private static int elementChildCount(Element parent) {
        int count = 0;
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) count++;
        }
        return count;
    }

    private static boolean hasOnlyElementsAndWhitespace(Element element) {
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) continue;
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) continue;
            return false;
        }
        return true;
    }

    private static boolean hasOnlyWhitespace(Element element) {
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) continue;
            return false;
        }
        return true;
    }

    private static boolean isWordElement(Element element, String localName) {
        return W.equals(element.getNamespaceURI()) && localName.equals(element.getLocalName());
    }

    private static Document styles(byte[] packageBytes) throws Exception {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(packageBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (STYLES.equals(entry.getName())) return secureFactory().newDocumentBuilder().parse(zip);
            }
        }
        return null;
    }

    private static byte[] serialize(Document document) throws Exception {
        var factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        var output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private static byte[] replacePart(byte[] packageBytes, byte[] replacement) throws IOException {
        var output = new ByteArrayOutputStream(packageBytes.length);
        boolean replaced = false;
        try (var input = new ZipInputStream(new ByteArrayInputStream(packageBytes));
             var zip = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] content = input.readAllBytes();
                ZipEntry copied = new ZipEntry(entry.getName());
                if (entry.getTime() >= 0) copied.setTime(entry.getTime());
                if (entry.getComment() != null) copied.setComment(entry.getComment());
                if (entry.getExtra() != null) copied.setExtra(entry.getExtra());
                if (entry.getMethod() == ZipEntry.STORED) {
                    byte[] stored = STYLES.equals(entry.getName()) ? replacement : content;
                    var crc = new CRC32();
                    crc.update(stored);
                    copied.setMethod(ZipEntry.STORED);
                    copied.setSize(stored.length);
                    copied.setCompressedSize(stored.length);
                    copied.setCrc(crc.getValue());
                } else {
                    copied.setMethod(ZipEntry.DEFLATED);
                }
                zip.putNextEntry(copied);
                if (STYLES.equals(entry.getName())) {
                    if (replaced) throw new IOException("Duplicate styles part");
                    zip.write(replacement);
                    replaced = true;
                } else {
                    zip.write(content);
                }
                zip.closeEntry();
            }
        }
        if (!replaced) throw new IOException("Missing styles part");
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
