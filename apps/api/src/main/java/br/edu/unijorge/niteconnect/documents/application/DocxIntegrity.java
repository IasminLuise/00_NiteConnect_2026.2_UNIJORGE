package br.edu.unijorge.niteconnect.documents.application;

import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.helpers.DefaultHandler;

/** Strict experimental text-only gate, not a visual-fidelity certification or official-template support.
 * Non-main parts must remain byte-identical, except for one empty orphan comments relationship part added by the editor;
 * only existing core metadata may otherwise change (64 KiB).
 * Main XML must preserve its entire tree and attributes, except text inside existing w:t nodes.
 * HTTPS hyperlink relationships are preserved but never dereferenced by the server.
 */
@Component
public class DocxIntegrity {
    private static final int MAX_ARCHIVE = 8 * 1024 * 1024;
    private static final int MAX_ENTRY = 8 * 1024 * 1024;
    private static final int MAX_TOTAL = 32 * 1024 * 1024;
    private static final int MAX_ENTRIES = 1024;
    private static final String MAIN = "word/document.xml";
    private static final String CORE = "docProps/core.xml";
    private static final String ORPHAN_COMMENTS_RELS = "word/_rels/comments.xml.rels";
    private static final String COMMENTS = "word/comments.xml";
    private static final String PACKAGE_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String WORD = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final Set<String> REQUIRED = Set.of("[Content_Types].xml", "_rels/.rels", MAIN);

    public record Verdict(boolean accepted, List<String> reasons) {
        public Verdict { reasons = List.copyOf(reasons); }
    }

    public void validate(byte[] bytes) { unpack(bytes); }

    public Verdict assess(byte[] source, byte[] candidate) {
        try {
            Map<String, byte[]> original = unpack(source);
            Map<String, byte[]> edited = unpack(candidate);
            if (!samePartsExceptEmptyOrphanCommentsRels(original, edited)) return rejected();
            for (String name : original.keySet()) {
                if (name.equals(CORE)) continue;
                if (name.equals(MAIN)) {
                    if (!sameTree(xml(original.get(name)), xml(edited.get(name)))) return rejected();
                } else if (!Arrays.equals(original.get(name), edited.get(name))) return rejected();
            }
            return new Verdict(true, List.of());
        } catch (Exception e) { return rejected(); }
    }

    private static Verdict rejected() { return new Verdict(false, List.of("Documento fora das alterações textuais permitidas pelo experimento")); }
    private static ApiException invalid() { return ApiException.invalid("Pacote DOCX inválido ou não suportado pelo experimento"); }

    private static boolean samePartsExceptEmptyOrphanCommentsRels(Map<String, byte[]> original, Map<String, byte[]> edited) {
        if (original.keySet().equals(edited.keySet())) return true;
        if (original.containsKey(ORPHAN_COMMENTS_RELS) || original.containsKey(COMMENTS)
                || edited.containsKey(COMMENTS) || !edited.containsKey(ORPHAN_COMMENTS_RELS)) return false;
        Set<String> expected = new java.util.HashSet<>(original.keySet());
        expected.add(ORPHAN_COMMENTS_RELS);
        return expected.equals(edited.keySet()) && isEmptyOrphanRelationships(edited.get(ORPHAN_COMMENTS_RELS));
    }

    private static boolean isEmptyOrphanRelationships(byte[] bytes) {
        try {
            Document document = xml(bytes);
            var root = document.getDocumentElement();
            if (!PACKAGE_RELATIONSHIPS.equals(root.getNamespaceURI()) || !"Relationships".equals(root.getLocalName())) return false;
            for (int i = 0; i < root.getAttributes().getLength(); i++) {
                Node attribute = root.getAttributes().item(i);
                if (!XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) return false;
            }
            var children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() != Node.TEXT_NODE || !child.getNodeValue().isBlank()) return false;
            }
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private static Map<String, byte[]> unpack(byte[] bytes) {
        if (bytes == null || bytes.length < 22 || bytes.length > MAX_ARCHIVE) throw invalid();
        Map<String, byte[]> parts = new HashMap<>();
        int total = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                if (parts.size() >= MAX_ENTRIES || name.isEmpty() || name.startsWith("/") || name.contains("\\")
                        || name.contains(":") || Arrays.stream(name.split("/", -1)).anyMatch(p -> p.equals("..") || p.equals("."))
                        || lower.contains("vbaproject") || lower.endsWith(".vba") || parts.containsKey(name)) throw invalid();
                var out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = zip.read(buffer)) != -1) {
                    total += length;
                    if (out.size() + length > MAX_ENTRY || total > MAX_TOTAL) throw invalid();
                    out.write(buffer, 0, length);
                }
                byte[] content = out.toByteArray();
                if (name.equals(CORE) && content.length > 64 * 1024) throw invalid();
                if (!entry.isDirectory() && (lower.endsWith(".xml") || lower.endsWith(".rels"))) {
                    Document parsed = xml(content);
                    rejectUnsafeXml(parsed);
                    if (name.equals(MAIN) && (!WORD.equals(parsed.getDocumentElement().getNamespaceURI())
                            || !"document".equals(parsed.getDocumentElement().getLocalName()))) throw invalid();
                }
                parts.put(name, content);
                zip.closeEntry();
            }
            if (!parts.keySet().containsAll(REQUIRED) || parts.size() != centralEntryCount(bytes, parts)) throw invalid();
            return parts;
        } catch (Exception e) { throw invalid(); }
    }

    private static int centralEntryCount(byte[] bytes, Map<String, byte[]> parts) {
        for (int i = bytes.length - 22; i >= Math.max(0, bytes.length - 65557); i--) {
            if (bytes[i] == 0x50 && bytes[i+1] == 0x4b && bytes[i+2] == 5 && bytes[i+3] == 6) {
                if (i + 22 + u16(bytes, i+20) != bytes.length) continue;
                int count = u16(bytes, i+10);
                long size = u32(bytes, i+12), offset = u32(bytes, i+16);
                if (u16(bytes,i+4) != 0 || u16(bytes,i+6) != 0 || u16(bytes,i+8) != count
                        || count < 1 || count > MAX_ENTRIES || size + offset != i || offset > i) throw invalid();
                int cursor = (int) offset;
                Set<String> centralNames = new java.util.HashSet<>();
                for (int index=0; index<count; index++) {
                    if (cursor + 46 > i || u32(bytes,cursor) != 0x02014b50L) throw invalid();
                    int nameLength = u16(bytes,cursor+28);
                    long localOffset = u32(bytes,cursor+42);
                    if (cursor + 46 + nameLength > i || localOffset + 30 > offset) throw invalid();
                    int local = (int) localOffset;
                    if (u32(bytes,local) != 0x04034b50L || u16(bytes,local+26) != nameLength
                            || local + 30 + nameLength > offset || u16(bytes,local+6) != u16(bytes,cursor+8)
                            || u16(bytes,local+8) != u16(bytes,cursor+10)
                            || !Arrays.equals(Arrays.copyOfRange(bytes,local+30,local+30+nameLength),
                                    Arrays.copyOfRange(bytes,cursor+46,cursor+46+nameLength))) throw invalid();
                    String centralName = new String(bytes,cursor+46,nameLength,java.nio.charset.StandardCharsets.UTF_8);
                    if (!centralNames.add(centralName)) throw invalid();
                    byte[] content = parts.get(centralName);
                    if (content == null || content.length != u32(bytes,cursor+24)) throw invalid();
                    var crc = new java.util.zip.CRC32(); crc.update(content);
                    if (crc.getValue() != u32(bytes,cursor+16)
                            || localOffset + 30 + nameLength + u16(bytes,local+28) + u32(bytes,cursor+20) > offset) throw invalid();
                    cursor += 46 + u16(bytes,cursor+28) + u16(bytes,cursor+30) + u16(bytes,cursor+32);
                    if (cursor > i) throw invalid();
                }
                if (cursor != i) throw invalid();
                return count;
            }
        }
        throw invalid();
    }

    private static int u16(byte[] bytes, int position) {
        return Byte.toUnsignedInt(bytes[position]) | Byte.toUnsignedInt(bytes[position+1]) << 8;
    }
    private static long u32(byte[] bytes, int position) {
        return Integer.toUnsignedLong(u16(bytes, position) | u16(bytes, position+2) << 16);
    }

    private static Document xml(byte[] bytes) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth", "128");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        return builder.parse(new ByteArrayInputStream(bytes));
    }

    private static void rejectUnsafeXml(Document document) {
        var elements = document.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < elements.getLength(); i++) {
            Node node = elements.item(i);
            if (node.hasAttributes()) {
                for (int j=0; j<node.getAttributes().getLength(); j++) {
                    String value = node.getAttributes().item(j).getNodeValue().toLowerCase(Locale.ROOT);
                    if (value.contains("macroenabled") || value.contains("vbaproject")) throw invalid();
                }
            }
            if ("Relationship".equals(node.getLocalName())) {
                var element = (org.w3c.dom.Element) node;
                String target = element.getAttribute("Target");
                String targetMode = element.getAttribute("TargetMode");
                if ("External".equalsIgnoreCase(targetMode)) {
                    validateExternalHyperlink(element);
                } else {
                    if (!targetMode.isEmpty() && !"Internal".equalsIgnoreCase(targetMode)) throw invalid();
                    if (target.contains(":") || target.startsWith("/") || target.contains("\\")) throw invalid();
                }
            }
        }
    }

    private static void validateExternalHyperlink(org.w3c.dom.Element relationship) {
        if (!relationship.getAttribute("Type").endsWith("/hyperlink")) throw invalid();
        URI target;
        try { target = URI.create(relationship.getAttribute("Target")); }
        catch (IllegalArgumentException error) { throw invalid(); }
        if (!target.isAbsolute() || !"https".equalsIgnoreCase(target.getScheme()) || target.getHost() == null
                || target.getRawUserInfo() != null || target.getPort() == 0 || target.getPort() > 65535) throw invalid();
    }

    private static boolean sameTree(Node first, Node second) {
        if (first.getNodeType() != second.getNodeType() || !java.util.Objects.equals(first.getNamespaceURI(), second.getNamespaceURI())
                || !java.util.Objects.equals(first.getLocalName(), second.getLocalName()) || !attributes(first).equals(attributes(second))) return false;
        if (first.getNodeType() != Node.ELEMENT_NODE && !java.util.Objects.equals(first.getNodeName(), second.getNodeName())) return false;
        if (first.getNodeType() == Node.ELEMENT_NODE && WORD.equals(first.getNamespaceURI()) && "t".equals(first.getLocalName())) {
            return textOnly(first) && textOnly(second);
        }
        if (!java.util.Objects.equals(first.getNodeValue(), second.getNodeValue())) return false;
        var a = first.getChildNodes(); var b = second.getChildNodes();
        if (a.getLength() != b.getLength()) return false;
        for (int i=0; i<a.getLength(); i++) if (!sameTree(a.item(i), b.item(i))) return false;
        return true;
    }

    private static boolean textOnly(Node node) {
        for (int i=0; i<node.getChildNodes().getLength(); i++) {
            short type = node.getChildNodes().item(i).getNodeType();
            if (type != Node.TEXT_NODE && type != Node.CDATA_SECTION_NODE) return false;
        }
        return true;
    }

    private static Map<String,String> attributes(Node node) {
        Map<String,String> result = new TreeMap<>();
        if (!node.hasAttributes()) return result;
        for (int i=0; i<node.getAttributes().getLength(); i++) {
            Node attr = node.getAttributes().item(i);
            result.put(String.valueOf(attr.getNamespaceURI()) + ":" + attr.getLocalName(), attr.getNodeValue());
        }
        return result;
    }
}
