package br.edu.unijorge.experiments;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ImplicitParagraphDefaultsRepairTest {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String NO_OP_DEFAULT="<w:pPrDefault><w:pPr><w:pBdr/><w:spacing/><w:ind/></w:pPr></w:pPrDefault>";
    private static final String SOURCE_STYLES="<w:styles xmlns:w=\""+W+"\"><w:docDefaults><w:rPrDefault><w:rPr/></w:rPrDefault></w:docDefaults></w:styles>";
    private static final String DOCUMENT="<w:document xmlns:w=\""+W+"\"><w:body><w:p><w:r><w:t>synthetic text</w:t></w:r></w:p></w:body></w:document>";

    @Test void removesOnlyGeneratedNoOpParagraphDefaultsWhenSourceOmittedThem() throws Exception {
        byte[] source=docx(SOURCE_STYLES);
        byte[] candidate=docx(SOURCE_STYLES.replace("</w:docDefaults>",NO_OP_DEFAULT+"</w:docDefaults>"));

        byte[] repaired=repair(source,candidate);

        assertFalse(styles(repaired).getElementsByTagNameNS(W,"pPrDefault").getLength()>0);
        assertEquals("synthetic text",stylesDocument(repaired).getElementsByTagNameNS(W,"t").item(0).getTextContent());
        assertFalse(java.util.Arrays.equals(candidate,repaired));
        Map<String,byte[]> originalParts=parts(candidate), repairedParts=parts(repaired);
        assertEquals(originalParts.keySet(),repairedParts.keySet());
        for(String name:originalParts.keySet())if(!name.equals("word/styles.xml"))assertArrayEquals(originalParts.get(name),repairedParts.get(name),name);
    }

    @Test void retainsDefaultsWhenSourceHadExplicitParagraphDefaults() throws Exception {
        String explicit=SOURCE_STYLES.replace("</w:docDefaults>","<w:pPrDefault><w:pPr><w:spacing w:after=\"160\"/></w:pPr></w:pPrDefault></w:docDefaults>");
        assertArrayEquals(docx(SOURCE_STYLES.replace("</w:docDefaults>",NO_OP_DEFAULT+"</w:docDefaults>")),
                repair(docx(explicit),docx(SOURCE_STYLES.replace("</w:docDefaults>",NO_OP_DEFAULT+"</w:docDefaults>"))));
    }

    @Test void retainsCandidateWithExplicitParagraphSpacing() throws Exception {
        String explicit=SOURCE_STYLES.replace("</w:docDefaults>","<w:pPrDefault><w:pPr><w:spacing w:after=\"0\"/></w:pPr></w:pPrDefault></w:docDefaults>");
        byte[] candidate=docx(explicit);
        assertArrayEquals(candidate,repair(docx(SOURCE_STYLES),candidate));
    }

    @Test void retainsCandidateWithUnrecognizedDefaultProperties() throws Exception {
        String unknown=SOURCE_STYLES.replace("</w:docDefaults>","<w:pPrDefault><w:pPr><w:pBdr/><w:spacing/><w:ind/><w:jc w:val=\"center\"/></w:pPr></w:pPrDefault></w:docDefaults>");
        byte[] candidate=docx(unknown);
        assertArrayEquals(candidate,repair(docx(SOURCE_STYLES),candidate));
    }

    @Test void retainsCandidateWithCommentsInsideParagraphDefaults() throws Exception {
        String commented=SOURCE_STYLES.replace("</w:docDefaults>","<w:pPrDefault><!--preserve--><w:pPr><w:pBdr/><w:spacing/><w:ind/></w:pPr></w:pPrDefault></w:docDefaults>");
        byte[] candidate=docx(commented);
        assertArrayEquals(candidate,repair(docx(SOURCE_STYLES),candidate));
    }

    @Test void narrowRepairAllowsOnlyTheSynthesizedNoOpInStrictGate() throws Exception {
        byte[] source=docx(SOURCE_STYLES);
        byte[] candidate=docx(SOURCE_STYLES.replace("</w:docDefaults>",NO_OP_DEFAULT+"</w:docDefaults>"),
                DOCUMENT.replace("synthetic text","XYZsynthetic text"));
        byte[] repaired=repair(source,candidate);
        var gate=new ScriptedDocxGate(Map.of("synthetic.docx",List.of("XYZ")));
        var id=UUID.randomUUID();gate.register(id,"synthetic.docx",source);
        assertDoesNotThrow(()->gate.validate(id,repaired,2));
    }

    @Test void realFontChangeStillFailsStrictGateAfterNarrowRepair() throws Exception {
        byte[] source=docx(SOURCE_STYLES);
        String changedFont=SOURCE_STYLES.replace("<w:rPr/>","<w:rPr><w:rFonts w:ascii=\"Arial\"/></w:rPr>")
                .replace("</w:docDefaults>",NO_OP_DEFAULT+"</w:docDefaults>");
        byte[] candidate=docx(changedFont,DOCUMENT.replace("synthetic text","XYZsynthetic text"));
        byte[] repaired=repair(source,candidate);
        var gate=new ScriptedDocxGate(Map.of("synthetic.docx",List.of("XYZ")));
        var id=UUID.randomUUID();gate.register(id,"synthetic.docx",source);
        var rejection=assertThrows(DocumentStore.Failure.class,()->gate.validate(id,repaired,2));
        assertEquals("SAVE_REJECTED_UNVERIFIED_FIELDS_OR_LAYOUT",rejection.getMessage());
        assertEquals("Arial",styles(repaired).getElementsByTagNameNS(W,"rFonts").item(0)
                .getAttributes().getNamedItemNS(W,"ascii").getNodeValue());
    }

    @Test void editorSerializationIsRebasedForVerifiedTextOnlyEditAndStrictGatePasses() throws Exception {
        byte[] source=profileDocx(false,false);
        byte[] candidate=profileDocx(true,false);
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("XYZ")));
        var id=UUID.randomUUID();gate.register(id,"F01.docx",source);

        byte[] rebased=gate.normalizeCandidate(id,candidate,2);

        assertDoesNotThrow(()->gate.validate(id,rebased,2));
        assertTrue(parts(rebased).keySet().equals(parts(source).keySet()));
        assertEquals("XYZTitle",textOf(rebased,"word/document.xml",0));
    }

    @Test void rebaseRefusesFontMutationAndStrictGateStillRejectsIt() throws Exception {
        byte[] source=profileDocx(false,false);
        byte[] candidate=profileDocx(true,true);
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("XYZ")));
        var id=UUID.randomUUID();gate.register(id,"F01.docx",source);

        byte[] unchanged=gate.normalizeCandidate(id,candidate,2);

        assertArrayEquals(candidate,unchanged);
        var rejection=assertThrows(DocumentStore.Failure.class,()->gate.validate(id,unchanged,2));
        assertEquals("SAVE_REJECTED_UNVERIFIED_FIELDS_OR_LAYOUT",rejection.getMessage());
    }

    @Test void rebaseRefusesExplicitSpacingChange() throws Exception {
        byte[] source=profileDocx(false,false);
        byte[] candidate=profileDocx(true,false);
        String document=new String(parts(candidate).get("word/document.xml"),StandardCharsets.UTF_8)
                .replace("<w:spacing/><w:ind/><w:rPr/>","<w:spacing w:after=\"120\"/><w:ind/><w:rPr/>");
        candidate=replacePart(candidate,"word/document.xml",document);
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("XYZ")));
        var id=UUID.randomUUID();gate.register(id,"F01.docx",source);

        byte[] unchanged=gate.normalizeCandidate(id,candidate,2);

        assertArrayEquals(candidate,unchanged);
        var rejection=assertThrows(DocumentStore.Failure.class,()->gate.validate(id,unchanged,2));
        assertEquals("SAVE_REJECTED_UNVERIFIED_FIELDS_OR_LAYOUT",rejection.getMessage());
    }

    @Test void rebaseRefusesFieldInsertionAndProtectedPartRemoval() throws Exception {
        byte[] source=profileDocx(false,false);
        var gate=new ScriptedDocxGate(Map.of("F01.docx",List.of("XYZ")));
        var id=UUID.randomUUID();gate.register(id,"F01.docx",source);
        byte[] withField=profileDocx(true,false);
        String document=new String(parts(withField).get("word/document.xml"),StandardCharsets.UTF_8)
                .replace("<w:r><w:t>Body</w:t></w:r>","<w:r><w:t>Body</w:t></w:r><w:fldSimple w:instr=\" SAVEDATE \"></w:fldSimple>");
        withField=replacePart(withField,"word/document.xml",document);
        final byte[] fieldCandidate=withField;
        assertArrayEquals(fieldCandidate,gate.normalizeCandidate(id,fieldCandidate,2));
        assertThrows(DocumentStore.Failure.class,()->gate.validate(id,fieldCandidate,2));

        byte[] missingNumbering=removePart(profileDocx(true,false),"word/numbering.xml");
        assertArrayEquals(missingNumbering,gate.normalizeCandidate(id,missingNumbering,2));
        assertThrows(DocumentStore.Failure.class,()->gate.validate(id,missingNumbering,2));
    }

    private static byte[] repair(byte[] source,byte[] candidate) {
        return ImplicitParagraphDefaultsRepair.repairIfSynthesized(source,candidate);
    }

    private static byte[] docx(String styleXml) throws IOException {
        return docx(styleXml,DOCUMENT);
    }

    private static byte[] docx(String styleXml,String documentXml) throws IOException {
        var out=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(out)) {
            entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
            entry(zip,"word/document.xml",documentXml);
            entry(zip,"word/styles.xml",styleXml);
        }
        return out.toByteArray();
    }

    private static void entry(ZipOutputStream zip,String name,String value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));zip.write(value.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
    }

    private static Document styles(byte[] docx) throws Exception { return xmlPart(docx,"word/styles.xml"); }
    private static Document stylesDocument(byte[] docx) throws Exception { return xmlPart(docx,"word/document.xml"); }
    private static Document xmlPart(byte[] docx,String wanted) throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        try(var zip=new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry item;while((item=zip.getNextEntry())!=null)if(item.getName().equals(wanted))return factory.newDocumentBuilder().parse(zip);
        }
        throw new IOException("Missing package part: "+wanted);
    }

    private static Map<String,byte[]> parts(byte[] docx) throws IOException {
        var result=new TreeMap<String,byte[]>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(docx))){ZipEntry entry;while((entry=zip.getNextEntry())!=null)result.put(entry.getName(),zip.readAllBytes());}
        return result;
    }

    private static byte[] profileDocx(boolean editorOutput,boolean fontMutation) throws IOException {
        String types="<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/><Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/><Override PartName=\"/word/numbering.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml\"/></Types>";
        String rels="<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>";
        String documentRels="<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"styles\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/><Relationship Id=\"numbering\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering\" Target=\"numbering.xml\"/></Relationships>";
        String defaults="<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii=\"Liberation Serif\" w:hAnsi=\"Liberation Serif\""+(editorOutput?" w:cs=\"Times New Roman\" w:eastAsia=\"Times New Roman\" w:hint=\"default\"":"")+"/><w:sz w:val=\"24\"/></w:rPr></w:rPrDefault></w:docDefaults>";
        String styles;
        if(editorOutput) {
            styles="<w:styles xmlns:w=\""+W+"\">"+defaults
                    +"<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"664\"><w:name w:val=\"Normal\"/><w:pPr><w:pBdr/><w:spacing/><w:ind/></w:pPr></w:style>"
                    +"<w:style w:type=\"paragraph\" w:styleId=\"665\"><w:name w:val=\"Heading 1\"/><w:basedOn w:val=\"664\"/><w:pPr><w:pBdr/><w:spacing/><w:ind/><w:outlineLvl w:val=\"0\"/></w:pPr><w:rPr><w:b/><w:sz w:val=\"32\"/>"+(fontMutation?"<w:rFonts w:ascii=\"Arial\"/>":"")+"</w:rPr></w:style>"
                    +"<w:style w:type=\"paragraph\" w:styleId=\"700\"><w:name w:val=\"Unused Editor Style\"/></w:style></w:styles>";
        } else {
            styles="<w:styles xmlns:w=\""+W+"\">"+defaults
                    +"<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\"><w:name w:val=\"Normal\"/></w:style>"
                    +"<w:style w:type=\"paragraph\" w:styleId=\"Heading1\"><w:name w:val=\"heading 1\"/><w:basedOn w:val=\"Normal\"/><w:pPr><w:outlineLvl w:val=\"0\"/></w:pPr><w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr></w:style></w:styles>";
        }
        String numbering=editorOutput
                ?"<w:numbering xmlns:w=\""+W+"\"><w:abstractNum w:abstractNumId=\"0\"><w:lvl w:ilvl=\"0\"><w:isLgl w:val=\"false\"/><w:lvlJc w:val=\"left\"/><w:lvlText w:val=\"%1.\"/><w:numFmt w:val=\"decimal\"/><w:pPr><w:pBdr/><w:spacing/><w:ind/></w:pPr><w:rPr/><w:start w:val=\"1\"/><w:suff w:val=\"tab\"/></w:lvl><w:lvl w:ilvl=\"1\"><w:numFmt w:val=\"bullet\"/></w:lvl></w:abstractNum><w:num w:numId=\"1\"><w:abstractNumId w:val=\"0\"/></w:num></w:numbering>"
                :"<w:numbering xmlns:w=\""+W+"\"><w:abstractNum w:abstractNumId=\"0\"><w:lvl w:ilvl=\"0\"><w:start w:val=\"1\"/><w:numFmt w:val=\"decimal\"/><w:lvlText w:val=\"%1.\"/></w:lvl></w:abstractNum><w:num w:numId=\"1\"><w:abstractNumId w:val=\"0\"/></w:num></w:numbering>";
        String paragraphExtra=editorOutput?"<w:spacing/><w:ind/><w:rPr/>":"";
        String titleStyle=editorOutput?"665":"Heading1";
        String document=editorOutput
                ?"<w:document xmlns:w=\""+W+"\" xmlns:mc=\""+"http://schemas.openxmlformats.org/markup-compatibility/2006"+"\" mc:Ignorable=\"w14 w15\"><w:body><w:p><w:pPr><w:pStyle w:val=\""+titleStyle+"\"/>"+paragraphExtra+"</w:pPr><w:r><w:t>XYZTitle</w:t></w:r><w:r/></w:p><w:p><w:pPr>"+paragraphExtra+"</w:pPr><w:r><w:t>Body</w:t></w:r><w:r/></w:p><w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr>"+paragraphExtra+"</w:pPr><w:r><w:t>Item</w:t></w:r><w:r/></w:p><w:sectPr/></w:body></w:document>"
                :"<w:document xmlns:w=\""+W+"\"><w:body><w:p><w:pPr><w:pStyle w:val=\""+titleStyle+"\"/></w:pPr><w:r><w:t>Title</w:t></w:r></w:p><w:p><w:r><w:t>Body</w:t></w:r></w:p><w:p><w:pPr><w:numPr><w:ilvl w:val=\"0\"/><w:numId w:val=\"1\"/></w:numPr></w:pPr><w:r><w:t>Item</w:t></w:r></w:p><w:sectPr/></w:body></w:document>";
        var out=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(out)) {
            entry(zip,"[Content_Types].xml",types);entry(zip,"_rels/.rels",rels);entry(zip,"word/document.xml",document);
            entry(zip,"word/_rels/document.xml.rels",documentRels);entry(zip,"word/styles.xml",styles);entry(zip,"word/numbering.xml",numbering);
            if(editorOutput) {
                entry(zip,"docProps/core.xml","<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\"/>");
                entry(zip,"docProps/app.xml","<Properties xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/extended-properties\"/>");
                entry(zip,"word/_rels/comments.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>");
                entry(zip,"word/_rels/endnotes.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>");
                entry(zip,"word/_rels/footnotes.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"/>");
                entry(zip,"word/endnotes.xml","<w:endnotes xmlns:w=\""+W+"\"><w:endnote w:type=\"separator\" w:id=\"-1\"><w:p><w:r><w:separator/></w:r></w:p></w:endnote><w:endnote w:type=\"continuationSeparator\" w:id=\"0\"><w:p><w:r><w:continuationSeparator/></w:r></w:p></w:endnote></w:endnotes>");
                entry(zip,"word/footnotes.xml","<w:footnotes xmlns:w=\""+W+"\"><w:footnote w:type=\"separator\" w:id=\"-1\"><w:p><w:r><w:separator/></w:r></w:p></w:footnote><w:footnote w:type=\"continuationSeparator\" w:id=\"0\"><w:p><w:r><w:continuationSeparator/></w:r></w:p></w:footnote></w:footnotes>");
                entry(zip,"word/settings.xml","<w:settings xmlns:w=\""+W+"\"><w:defaultTabStop w:val=\"709\"/></w:settings>");
                entry(zip,"word/fontTable.xml","<w:fonts xmlns:w=\""+W+"\"/>");entry(zip,"word/theme/theme1.xml","<a:theme xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"/>");entry(zip,"word/webSettings.xml","<w:webSettings xmlns:w=\""+W+"\"/>");
            }
        }
        return out.toByteArray();
    }

    private static String textOf(byte[] docx,String part,int paragraph) throws Exception {
        Document document=xmlPart(docx,part);var paragraphs=document.getElementsByTagNameNS(W,"p");
        var out=new StringBuilder();
        var texts=((org.w3c.dom.Element)paragraphs.item(paragraph)).getElementsByTagNameNS(W,"t");
        for(int i=0;i<texts.getLength();i++)out.append(texts.item(i).getTextContent());
        return out.toString();
    }

    private static byte[] replacePart(byte[] docx,String wanted,String replacement) throws IOException {
        var entries=parts(docx);entries.put(wanted,replacement.getBytes(StandardCharsets.UTF_8));return writeParts(entries);
    }

    private static byte[] removePart(byte[] docx,String unwanted) throws IOException {
        var entries=parts(docx);entries.remove(unwanted);return writeParts(entries);
    }

    private static byte[] writeParts(Map<String,byte[]> entries) throws IOException {
        var output=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(output)){for(var part:entries.entrySet()){zip.putNextEntry(new ZipEntry(part.getKey()));zip.write(part.getValue());zip.closeEntry();}}
        return output.toByteArray();
    }
}
