package br.edu.unijorge.experiments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/**
 * Re-bases only a proven body-text-only edit onto its original package. This deliberately
 * supports the small synthetic text-only profile; every unsupported or changed feature falls
 * back to the untouched editor candidate, which the strict gate will reject.
 */
final class TextOnlyDocxRebase {
    record Result(byte[] bytes, String reason) { boolean rebased() { return "REBASED".equals(reason); } }
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String MC = "http://schemas.openxmlformats.org/markup-compatibility/2006";
    private static final String CT = "http://schemas.openxmlformats.org/package/2006/content-types";
    private static final String REL = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final String DOC = "word/document.xml";
    private static final String STYLES = "word/styles.xml";
    private static final String NUMBERING = "word/numbering.xml";
    private static final Set<String> SOURCE_PARTS = Set.of("[Content_Types].xml", "_rels/.rels",
            DOC, "word/_rels/document.xml.rels", STYLES, NUMBERING);
    private static final Set<String> GENERATED_PARTS = Set.of("docProps/app.xml", "docProps/core.xml",
            "word/_rels/comments.xml.rels", "word/_rels/endnotes.xml.rels", "word/_rels/footnotes.xml.rels",
            "word/endnotes.xml", "word/fontTable.xml", "word/footnotes.xml", "word/settings.xml",
            "word/theme/theme1.xml", "word/webSettings.xml");
    private static final Set<String> EMPTY_PROPERTIES = Set.of("pBdr", "spacing", "ind", "rPr");

    private TextOnlyDocxRebase() {}

    static byte[] rebase(byte[] sourceBytes, byte[] candidateBytes, String prefix) {
        return rebaseWithReason(sourceBytes, candidateBytes, prefix).bytes();
    }

    static Result rebaseWithReason(byte[] sourceBytes, byte[] candidateBytes, String prefix) {
        try {
            Map<String, byte[]> source = parts(sourceBytes);
            Map<String, byte[]> candidate = parts(candidateBytes);
            if (!source.keySet().equals(SOURCE_PARTS) || !candidate.keySet().containsAll(SOURCE_PARTS)) return skipped(candidateBytes,"UNSUPPORTED_SOURCE_OR_MISSING_PART");
            var added = new HashSet<>(candidate.keySet());
            added.removeAll(SOURCE_PARTS);
            if (!GENERATED_PARTS.containsAll(added)) return skipped(candidateBytes,"UNEXPECTED_ADDED_PART");
            if (!generatedPartsAreInert(candidate)) return skipped(candidateBytes,"GENERATED_PART_HAS_CONTENT");
            if (!contentTypesAreAdditive(source, candidate)) return skipped(candidateBytes,"CONTENT_TYPES_CHANGED");
            if (!relationshipsAreAdditive(source, candidate)) return skipped(candidateBytes,"RELATIONSHIP_REMOVED_OR_UNEXPECTED");

            Document sourceDocument = parse(source.get(DOC));
            Document candidateDocument = parse(candidate.get(DOC));
            if (!simpleTextProfile(sourceDocument, candidateDocument)) return skipped(candidateBytes,"UNSUPPORTED_BODY_FEATURE");
            Map<String, String> styleIds = styleIdsByKey(source.get(STYLES), candidate.get(STYLES));
            if (styleIds == null) return skipped(candidateBytes,"STYLE_IDENTITY_CHANGED");
            if (!stylesAreEquivalent(source.get(STYLES), candidate.get(STYLES), styleIds, latinOnly(sourceDocument)))
                return skipped(candidateBytes,"USED_STYLE_CHANGED");
            if (!usedNumberingIsEquivalent(source.get(NUMBERING), candidate.get(NUMBERING), sourceDocument,
                    candidateDocument)) return skipped(candidateBytes,"USED_NUMBERING_CHANGED");

            normalizeDocument(sourceDocument, Map.of(), false);
            normalizeDocument(candidateDocument, styleIds, true);
            if (!sameStructure(sourceDocument, candidateDocument)) return skipped(candidateBytes,"BODY_STRUCTURE_OR_FORMAT_CHANGED");
            Document rebasedDocument = parse(source.get(DOC));
            if (!prefix.isEmpty()) {
                var texts = rebasedDocument.getElementsByTagNameNS(W, "t");
                if (texts.getLength() == 0) return skipped(candidateBytes,"BODY_TEXT_MISSING");
                texts.item(0).setTextContent(prefix + texts.item(0).getTextContent());
            }
            byte[] documentBytes = serialize(rebasedDocument);
            return new Result(replaceDocument(sourceBytes, documentBytes),"REBASED");
        } catch (Exception unsupported) {
            return skipped(candidateBytes,"UNSUPPORTED_PACKAGE:"+unsupported.getClass().getSimpleName());
        }
    }

    private static Result skipped(byte[] candidate, String reason) { return new Result(candidate, reason); }

    private static boolean simpleTextProfile(Document source, Document candidate) {
        for (Document document : List.of(source, candidate)) {
            Element body = first(document.getDocumentElement(), W, "body");
            if (body == null || body.getElementsByTagNameNS(W, "tbl").getLength() != 0
                    || body.getElementsByTagNameNS(W, "drawing").getLength() != 0
                    || body.getElementsByTagNameNS(W, "pict").getLength() != 0
                    || body.getElementsByTagNameNS(W, "instrText").getLength() != 0
                    || body.getElementsByTagNameNS(W, "fldChar").getLength() != 0
                    || body.getElementsByTagNameNS(W, "fldSimple").getLength() != 0
                    || body.getElementsByTagNameNS(W, "hyperlink").getLength() != 0
                    || body.getElementsByTagNameNS(W, "commentReference").getLength() != 0
                    || body.getElementsByTagNameNS(W, "footnoteReference").getLength() != 0
                    || body.getElementsByTagNameNS(W, "endnoteReference").getLength() != 0) return false;
            if (document.getElementsByTagNameNS(W, "ins").getLength() != 0
                    || document.getElementsByTagNameNS(W, "del").getLength() != 0) return false;
        }
        return source.getElementsByTagNameNS(W, "t").getLength() > 0;
    }

    private static boolean latinOnly(Document document) {
        var texts = document.getElementsByTagNameNS(W, "t");
        for (int i = 0; i < texts.getLength(); i++) {
            String value = texts.item(i).getTextContent();
            for (int offset = 0; offset < value.length();) {
                int codePoint = value.codePointAt(offset);
                Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
                if (script != Character.UnicodeScript.LATIN && script != Character.UnicodeScript.COMMON
                        && script != Character.UnicodeScript.INHERITED) return false;
                offset += Character.charCount(codePoint);
            }
        }
        return true;
    }

    private static Map<String, String> styleIdsByKey(byte[] sourceBytes, byte[] candidateBytes) throws Exception {
        Map<String, Element> source = stylesByKey(parse(sourceBytes));
        Map<String, Element> candidate = stylesByKey(parse(candidateBytes));
        if (source == null || candidate == null) return null;
        var result = new HashMap<String, String>();
        for (var style : source.entrySet()) {
            Element counterpart = candidate.get(style.getKey());
            if (counterpart == null) return null;
            String sourceId = style.getValue().getAttributeNS(W, "styleId");
            String candidateId = counterpart.getAttributeNS(W, "styleId");
            if (sourceId.isEmpty() || candidateId.isEmpty()) return null;
            result.put(candidateId, sourceId);
        }
        return result;
    }

    private static Map<String, Element> stylesByKey(Document document) {
        var result = new HashMap<String, Element>();
        var styles = document.getElementsByTagNameNS(W, "style");
        for (int i = 0; i < styles.getLength(); i++) {
            Element style = (Element) styles.item(i);
            Element name = first(style, W, "name");
            String type = style.getAttributeNS(W, "type");
            String value = name == null ? "" : normalizeStyleName(name.getAttributeNS(W, "val"));
            if (value.isEmpty() || type.isEmpty()) return null;
            if (result.put(type + ":" + value, style) != null) return null;
        }
        return result;
    }

    private static String normalizeStyleName(String value) {
        return value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static boolean stylesAreEquivalent(byte[] sourceBytes, byte[] candidateBytes,
            Map<String, String> styleIds, boolean latin) throws Exception {
        Document source = parse(sourceBytes);
        Document candidate = parse(candidateBytes);
        Element sourceDefaults = first(source.getDocumentElement(), W, "docDefaults");
        Element candidateDefaults = first(candidate.getDocumentElement(), W, "docDefaults");
        if ((sourceDefaults == null) != (candidateDefaults == null)) return false;
        if (sourceDefaults != null) {
            Element sourceCopy = (Element) sourceDefaults.cloneNode(true);
            Element candidateCopy = (Element) candidateDefaults.cloneNode(true);
            if (latin) removeLatinFallbackFonts(candidateCopy);
            normalizeProperties(sourceCopy);
            normalizeProperties(candidateCopy);
            if (!canonical(sourceCopy, false, Map.of()).equals(canonical(candidateCopy, false, styleIds))) return false;
        }
        Map<String, Element> sourceStyles = stylesByKey(source);
        Map<String, Element> candidateStyles = stylesByKey(candidate);
        if (sourceStyles == null || candidateStyles == null) return false;
        var sourceIds = new HashMap<String, String>();
        for (Element style : source.getElementsByTagNameNS(W, "style").getLength() == 0
                ? List.<Element>of() : elements(source, W, "style")) {
            String id = style.getAttributeNS(W, "styleId");
            if (!id.isEmpty()) sourceIds.put(id, id);
        }
        for (var entry : sourceStyles.entrySet()) {
            Element left = (Element) entry.getValue().cloneNode(true);
            Element right = (Element) candidateStyles.get(entry.getKey()).cloneNode(true);
            normalizeStyle(left, sourceIds);
            normalizeStyle(right, styleIds);
            if (!canonical(left, false, Map.of()).equals(canonical(right, false, Map.of()))) return false;
        }
        return true;
    }

    private static void removeLatinFallbackFonts(Element defaults) {
        var fonts = defaults.getElementsByTagNameNS(W, "rFonts");
        for (int i = 0; i < fonts.getLength(); i++) {
            Element font = (Element) fonts.item(i);
            for (String name : List.of("cs", "eastAsia", "hint")) font.removeAttributeNS(W, name);
        }
    }

    private static void normalizeStyle(Element style, Map<String, String> ids) {
        style.removeAttributeNS(W, "styleId");
        Element name = first(style, W, "name");
        if (name != null) name.setAttributeNS(W, "w:val", normalizeStyleName(name.getAttributeNS(W, "val")));
        for (String local : List.of("basedOn", "next", "link")) {
            Element reference = first(style, W, local);
            if (reference != null) {
                String id = reference.getAttributeNS(W, "val");
                String mapped = ids.get(id);
                if (mapped != null) reference.setAttributeNS(W, "w:val", "source:" + mapped);
            }
        }
        normalizeProperties(style);
        removeChildren(style, Set.of("qFormat", "uiPriority", "semiHidden", "unhideWhenUsed"));
    }

    private static boolean usedNumberingIsEquivalent(byte[] sourceBytes, byte[] candidateBytes,
            Document sourceDocument, Document candidateDocument) throws Exception {
        Document source = parse(sourceBytes);
        Document candidate = parse(candidateBytes);
        Map<String, String> sourceNums = numberToAbstract(source);
        Map<String, String> candidateNums = numberToAbstract(candidate);
        Map<String, Set<String>> sourceUsed = usedLevels(sourceDocument);
        Map<String, Set<String>> candidateUsed = usedLevels(candidateDocument);
        if (!sourceUsed.equals(candidateUsed)) return false;
        for (var num : sourceUsed.entrySet()) {
            String sourceAbstractId = sourceNums.get(num.getKey());
            String candidateAbstractId = candidateNums.get(num.getKey());
            if (sourceAbstractId == null || candidateAbstractId == null) return false;
            Element sourceAbstract = abstractById(source, sourceAbstractId);
            Element candidateAbstract = abstractById(candidate, candidateAbstractId);
            if (sourceAbstract == null || candidateAbstract == null) return false;
            for (String level : num.getValue()) {
                Element sourceLevel = levelById(sourceAbstract, level);
                Element candidateLevel = levelById(candidateAbstract, level);
                if (sourceLevel == null || candidateLevel == null || !levelsEqual(sourceLevel, candidateLevel)) return false;
            }
        }
        return true;
    }

    private static boolean levelsEqual(Element source, Element candidate) {
        for (String local : List.of("start", "numFmt", "lvlText"))
            if (!value(source, local, "1").equals(value(candidate, local, "1"))) return false;
        if (!value(source, "lvlJc", "left").equals(value(candidate, "lvlJc", "left"))) return false;
        if (!value(source, "suff", "tab").equals(value(candidate, "suff", "tab"))) return false;
        if (!value(source, "isLgl", "false").equals(value(candidate, "isLgl", "false"))) return false;
        for (String local : List.of("pPr", "rPr")) {
            Element a = first(source, W, local), b = first(candidate, W, local);
            Element ac = a == null ? null : (Element) a.cloneNode(true);
            Element bc = b == null ? null : (Element) b.cloneNode(true);
            if (ac != null) { normalizeProperties(ac); if (emptyElement(ac)) ac = null; }
            if (bc != null) { normalizeProperties(bc); if (emptyElement(bc)) bc = null; }
            if (!Objects.equals(ac == null ? null : canonical(ac, false, Map.of()),
                    bc == null ? null : canonical(bc, false, Map.of()))) return false;
        }
        return true;
    }

    private static String value(Element parent, String local, String fallback) {
        Element child = first(parent, W, local);
        return child == null ? fallback : child.getAttributeNS(W, "val");
    }

    private static Map<String, String> numberToAbstract(Document document) {
        var result = new HashMap<String, String>();
        var nums = document.getElementsByTagNameNS(W, "num");
        for (int i = 0; i < nums.getLength(); i++) {
            Element num = (Element) nums.item(i);
            Element abstractRef = first(num, W, "abstractNumId");
            if (abstractRef != null) result.put(num.getAttributeNS(W, "numId"), abstractRef.getAttributeNS(W, "val"));
        }
        return result;
    }

    private static Map<String, Set<String>> usedLevels(Document document) {
        var result = new HashMap<String, Set<String>>();
        var nums = document.getElementsByTagNameNS(W, "numPr");
        for (int i = 0; i < nums.getLength(); i++) {
            Element num = (Element) nums.item(i);
            Element id = first(num, W, "numId"), level = first(num, W, "ilvl");
            if (id != null) result.computeIfAbsent(id.getAttributeNS(W, "val"), ignored -> new HashSet<>())
                    .add(level == null ? "0" : level.getAttributeNS(W, "val"));
        }
        return result;
    }

    private static Element abstractById(Document document, String id) {
        var nodes = document.getElementsByTagNameNS(W, "abstractNum");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (id.equals(node.getAttributeNS(W, "abstractNumId"))) return node;
        }
        return null;
    }

    private static Element levelById(Element abstractNum, String id) {
        var levels = abstractNum.getElementsByTagNameNS(W, "lvl");
        for (int i = 0; i < levels.getLength(); i++) {
            Element level = (Element) levels.item(i);
            if (id.equals(level.getAttributeNS(W, "ilvl"))) return level;
        }
        return null;
    }

    private static void normalizeDocument(Document document, Map<String, String> styleIds, boolean generatedDefaults) {
        Element root = document.getDocumentElement();
        root.removeAttributeNS(MC, "Ignorable");
        var refs = document.getElementsByTagNameNS(W, "*");
        for (int i = 0; i < refs.getLength(); i++) {
            Element element = (Element) refs.item(i);
            if (Set.of("pStyle", "rStyle", "tblStyle", "basedOn", "next", "link").contains(element.getLocalName())) {
                String old = element.getAttributeNS(W, "val");
                String mapped = styleIds.get(old);
                if (mapped != null) element.setAttributeNS(W, "w:val", mapped);
            }
        }
        normalizeProperties(root);
        var textNodes = document.getElementsByTagNameNS(W, "t");
        for (int i = 0; i < textNodes.getLength(); i++) {
            Element text = (Element) textNodes.item(i);
            String value = text.getTextContent();
            if (!value.isEmpty() && !Character.isWhitespace(value.charAt(0))
                    && !Character.isWhitespace(value.charAt(value.length() - 1))) {
                text.removeAttributeNS(XMLConstants.XML_NS_URI, "space");
            }
        }
        if (generatedDefaults) normalizeGeneratedSectionDefaults(document);
    }

    private static void normalizeGeneratedSectionDefaults(Document document) {
        var sections = document.getElementsByTagNameNS(W, "sectPr");
        for (int i = 0; i < sections.getLength(); i++) {
            Element section = (Element) sections.item(i);
            for (Node child = section.getFirstChild(); child != null;) {
                Node next = child.getNextSibling();
                if (child instanceof Element element && W.equals(element.getNamespaceURI())) {
                    String local = element.getLocalName();
                    if ((local.equals("type") && "nextPage".equals(element.getAttributeNS(W, "val")))
                            || ((local.equals("footnotePr") || local.equals("endnotePr")) && emptyElement(element))
                            || (local.equals("cols") && "1".equals(element.getAttributeNS(W, "num")))) {
                        section.removeChild(element);
                    } else if (local.equals("pgSz") && "portrait".equals(element.getAttributeNS(W, "orient"))) {
                        element.removeAttributeNS(W, "orient");
                    } else if (local.equals("pgMar")) {
                        for (String attr : List.of("header", "footer", "gutter")) element.removeAttributeNS(W, attr);
                    }
                }
                child = next;
            }
        }
    }

    private static void normalizeProperties(Element root) {
        var elements = new ArrayList<Element>();
        collectElements(root, elements);
        Collections.reverse(elements);
        for (Element element : elements) {
            if (W.equals(element.getNamespaceURI()) && EMPTY_PROPERTIES.contains(element.getLocalName())
                    && element.getParentNode() instanceof Element parent
                    && elementChildCount(element) == 0 && !element.hasAttributes() && onlyWhitespace(element)) {
                parent.removeChild(element);
            } else if (W.equals(element.getNamespaceURI()) && "r".equals(element.getLocalName())
                    && elementChildCount(element) == 0 && onlyWhitespace(element)) {
                element.getParentNode().removeChild(element);
            } else if (W.equals(element.getNamespaceURI()) && "pPr".equals(element.getLocalName())
                    && elementChildCount(element) == 0 && !element.hasAttributes() && onlyWhitespace(element)) {
                if (element.getParentNode() != null) element.getParentNode().removeChild(element);
            }
        }
    }

    private static void collectElements(Element root, List<Element> result) {
        result.add(root);
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element element) collectElements(element, result);
    }

    private static void removeChildren(Element root, Set<String> names) {
        for (Node child = root.getFirstChild(); child != null;) {
            Node next = child.getNextSibling();
            if (child instanceof Element element && W.equals(element.getNamespaceURI())
                    && names.contains(element.getLocalName())) root.removeChild(child);
            child = next;
        }
    }

    private static int elementChildCount(Element root) {
        int count = 0;
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element) count++;
        return count;
    }

    private static boolean emptyElement(Element element) {
        return elementChildCount(element) == 0 && !element.hasAttributes() && onlyWhitespace(element);
    }

    private static List<Element> elements(Document document, String namespace, String local) {
        var result = new ArrayList<Element>();
        var nodes = document.getElementsByTagNameNS(namespace, local);
        for (int i = 0; i < nodes.getLength(); i++) result.add((Element) nodes.item(i));
        return result;
    }

    private static boolean onlyWhitespace(Element root) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
            if ((child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE)
                    && !child.getTextContent().isBlank()) return false;
        return true;
    }

    private static void maskText(Element root) {
        if (W.equals(root.getNamespaceURI()) && "t".equals(root.getLocalName())) root.setTextContent("#TEXT#");
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
            if (child instanceof Element element) maskText(element);
    }

    private static boolean sameStructure(Document source, Document candidate) {
        return canonical(source.getDocumentElement(), true, Map.of())
                .equals(canonical(candidate.getDocumentElement(), true, Map.of()));
    }

    private static String canonical(Element element, boolean maskText, Map<String, String> ignored) {
        var out = new StringBuilder();
        canonical(element, maskText, out);
        return out.toString();
    }

    private static void canonical(Element element, boolean maskText, StringBuilder out) {
        token(out, "{" + element.getNamespaceURI() + "}" + element.getLocalName());
        var attrs = new TreeMap<String, String>();
        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            Node attr = element.getAttributes().item(i);
            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI())) continue;
            if (W.equals(attr.getNamespaceURI()) && attr.getLocalName().startsWith("rsid")) continue;
            attrs.put("{" + attr.getNamespaceURI() + "}" + attr.getLocalName(), attr.getNodeValue());
        }
        attrs.forEach((key, value) -> { token(out, key); token(out, value); });
        out.append('|');
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element nested) canonical(nested, maskText, out);
            else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                String value = maskText && W.equals(element.getNamespaceURI())
                        && "t".equals(element.getLocalName()) ? "#TEXT#" : child.getNodeValue();
                if (!value.isBlank() || (W.equals(element.getNamespaceURI()) && Set.of("t", "instrText", "delText").contains(element.getLocalName()))) token(out, value);
            }
        }
        out.append(';');
    }

    private static void token(StringBuilder out, String value) { out.append(value.length()).append(':').append(value); }

    private static boolean contentTypesAreAdditive(Map<String, byte[]> source, Map<String, byte[]> candidate) throws Exception {
        var a = parse(source.get("[Content_Types].xml"));
        var b = parse(candidate.get("[Content_Types].xml"));
        var entries = new HashSet<String>();
        var candidateEntries = new HashSet<String>();
        collectContentTypes(a.getDocumentElement(), entries);
        collectContentTypes(b.getDocumentElement(), candidateEntries);
        return candidateEntries.containsAll(entries);
    }

    private static void collectContentTypes(Element root, Set<String> entries) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element element)) continue;
            String key = element.hasAttribute("PartName") ? element.getAttribute("PartName") : "." + element.getAttribute("Extension");
            entries.add(element.getLocalName() + "|" + key + "|" + element.getAttribute("ContentType"));
        }
    }

    private static boolean relationshipsAreAdditive(Map<String, byte[]> source, Map<String, byte[]> candidate) throws Exception {
        if (!relationshipSubset(source.get("_rels/.rels"), candidate.get("_rels/.rels"), Set.of("docProps/core.xml", "docProps/app.xml"))) return false;
        return relationshipSubset(source.get("word/_rels/document.xml.rels"), candidate.get("word/_rels/document.xml.rels"),
                Set.of("settings.xml", "webSettings.xml", "fontTable.xml", "theme/theme1.xml", "footnotes.xml", "endnotes.xml"));
    }

    private static boolean relationshipSubset(byte[] source, byte[] candidate, Set<String> generatedTargets) throws Exception {
        var a = relationshipSet(parse(source));
        var b = relationshipSet(parse(candidate));
        if (!b.containsAll(a)) return false;
        for (String relation : b) if (!a.contains(relation)) {
            String target = relation.substring(relation.indexOf('|') + 1);
            int last = target.lastIndexOf('|');
            String path = last < 0 ? target : target.substring(0, last);
            if (!generatedTargets.contains(path)) return false;
        }
        return true;
    }

    private static Set<String> relationshipSet(Document document) {
        var result = new HashSet<String>();
        var relationships = document.getElementsByTagNameNS(REL, "Relationship");
        for (int i = 0; i < relationships.getLength(); i++) {
            Element rel = (Element) relationships.item(i);
            result.add(rel.getAttribute("Type") + "|" + rel.getAttribute("Target") + "|" + rel.getAttribute("TargetMode"));
        }
        return result;
    }

    private static boolean generatedPartsAreInert(Map<String, byte[]> candidate) throws Exception {
        byte[] commentsRels = candidate.get("word/_rels/comments.xml.rels");
        if (commentsRels != null && parse(commentsRels).getElementsByTagNameNS(REL, "Relationship").getLength() != 0) return false;
        for (String name : List.of("word/_rels/endnotes.xml.rels", "word/_rels/footnotes.xml.rels")) {
            byte[] rels = candidate.get(name);
            if (rels != null && parse(rels).getElementsByTagNameNS(REL, "Relationship").getLength() != 0) return false;
        }
        for (String name : List.of("word/footnotes.xml", "word/endnotes.xml")) {
            byte[] notes = candidate.get(name);
            if (notes == null) continue;
            Document doc = parse(notes);
            if (doc.getElementsByTagNameNS(W, "t").getLength() != 0
                    || doc.getElementsByTagNameNS(W, "footnoteRef").getLength() != 0
                    || doc.getElementsByTagNameNS(W, "endnoteRef").getLength() != 0) return false;
            var noteNodes = doc.getElementsByTagNameNS(W, "footnote");
            if (noteNodes.getLength() == 0) noteNodes = doc.getElementsByTagNameNS(W, "endnote");
            for (int i = 0; i < noteNodes.getLength(); i++) {
                Element note = (Element) noteNodes.item(i);
                if (!Set.of("separator", "continuationSeparator").contains(note.getAttributeNS(W, "type"))) return false;
            }
        }
        return true;
    }

    private static Map<String, byte[]> parts(byte[] bytes) throws IOException {
        var result = new LinkedHashMap<String, byte[]>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (result.put(entry.getName(), zip.readAllBytes()) != null) throw new IOException("Duplicate part");
            }
        }
        return result;
    }

    private static Document parse(byte[] bytes) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private static Element first(Element parent, String namespace, String local) {
        var children = parent.getElementsByTagNameNS(namespace, local);
        return children.getLength() == 0 ? null : (Element) children.item(0);
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

    private static byte[] replaceDocument(byte[] packageBytes, byte[] document) throws IOException {
        var output = new ByteArrayOutputStream(packageBytes.length);
        boolean replaced = false;
        try (var input = new ZipInputStream(new ByteArrayInputStream(packageBytes));
             var zip = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] content = input.readAllBytes();
                if (DOC.equals(entry.getName())) {
                    content = document;
                    replaced = true;
                }
                zip.putNextEntry(new ZipEntry(entry.getName()));
                zip.write(content);
                zip.closeEntry();
            }
        }
        if (!replaced) throw new IOException("Missing document part");
        return output.toByteArray();
    }
}
