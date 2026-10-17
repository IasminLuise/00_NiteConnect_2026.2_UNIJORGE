package br.edu.unijorge.experiments;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Strict scientific policy: independent expected text prefixes, no learned output whitelist.
 * Unknown reserialization of layout is unverified and therefore rejected, not called corruption.
 * This deliberately does not claim a general DOCX equivalence algorithm.
 */
public final class ScriptedDocxGate implements CandidateGate {
    private static final String W="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private record Baseline(String name, Map<String,String> invariants, List<String> body) {}
    private final Map<String,List<String>> scripts;
    private final Map<UUID,Baseline> sources=new HashMap<>();
    public ScriptedDocxGate(Map<String,List<String>> scripts){this.scripts=Map.copyOf(scripts);}
    @Override public void register(UUID id,String name,byte[] source) {
        DocxValidator.validate(source);
        sources.put(id,new Baseline(name,snapshot(source),body(source)));
    }
    @Override public void validate(UUID id,byte[] candidate,long nextNumber) {
        var baseline=sources.get(id);
        var script=baseline==null?null:scripts.get(baseline.name());
        long index=nextNumber-2;
        if(script==null||index<0||index>=script.size())reject("UNAUTHORIZED_EDIT_STEP");
        var expected=new ArrayList<>(baseline.body());
        if(expected.isEmpty())reject("BODY_UNSUPPORTED");
        expected.set(0,script.get((int)index)+expected.get(0));
        if(!expected.equals(body(candidate)))reject("UNEXPECTED_BODY");
        if(!baseline.invariants().equals(snapshot(candidate)))reject("UNVERIFIED_FIELDS_OR_LAYOUT");
    }
    private static void reject(String reason){throw new DocumentStore.Failure(500,"SAVE_REJECTED_"+reason);}
    private static Map<String,Document> parse(byte[] data) {
        DocxValidator.validate(data);
        try {
            var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            var result=new TreeMap<String,Document>();
            try(var zip=new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry entry;while((entry=zip.getNextEntry())!=null) {
                    if(entry.getName().endsWith(".xml")||entry.getName().endsWith(".rels"))
                        result.put(entry.getName(),factory.newDocumentBuilder().parse(new ByteArrayInputStream(zip.readAllBytes())));
                }
            }
            return result;
        }catch(Exception error){reject("INVALID_PACKAGE");return Map.of();}
    }
    private static List<String> body(byte[] data) {
        var doc=parse(data).get("word/document.xml");var paragraphs=doc.getElementsByTagNameNS(W,"p");
        var result=new ArrayList<String>();
        for(int i=0;i<paragraphs.getLength();i++) {
            var text=new StringBuilder();collectText(paragraphs.item(i),text);result.add(text.toString());
        }
        return result;
    }
    private static void collectText(Node node,StringBuilder text) {
        if(W.equals(node.getNamespaceURI())) {
            if("t".equals(node.getLocalName())){text.append(node.getTextContent());return;}
            if("tab".equals(node.getLocalName()))text.append('\t');
            if("br".equals(node.getLocalName()))text.append('\n');
        }
        for(Node child=node.getFirstChild();child!=null;child=child.getNextSibling())collectText(child,text);
    }
    private static Map<String,String> snapshot(byte[] data) {
        var result=new TreeMap<String,String>();var xml=parse(data);
        for(var part:xml.entrySet()) {
            // Volatile application/provenance metadata is audited separately, never a layout oracle.
            if(part.getKey().equals("docProps/core.xml")||part.getKey().equals("docProps/app.xml"))continue;
            var value=new StringBuilder();canonical(part.getValue().getDocumentElement(),value,part.getKey().equals("word/document.xml"));
            result.put(part.getKey(),value.toString());
        }
        try(var zip=new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;while((entry=zip.getNextEntry())!=null) {
                if(!entry.getName().endsWith(".xml")&&!entry.getName().endsWith(".rels"))
                    result.put(entry.getName(),Base64.getEncoder().encodeToString(zip.readAllBytes()));
            }
        }catch(IOException error){reject("INVALID_PACKAGE");}
        return result;
    }
    private static void token(StringBuilder out,String value){out.append(value.length()).append(':').append(value);}
    private static void canonical(Element element,StringBuilder out,boolean body) {
        token(out,"{"+element.getNamespaceURI()+"}"+element.getLocalName());
        var attrs=new TreeMap<String,String>();
        for(int i=0;i<element.getAttributes().getLength();i++) {
            var attr=element.getAttributes().item(i);
            if(XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attr.getNamespaceURI()))continue;
            if(W.equals(attr.getNamespaceURI())&&attr.getLocalName().startsWith("rsid"))continue;
            attrs.put("{"+attr.getNamespaceURI()+"}"+attr.getLocalName(),attr.getNodeValue());
        }
        attrs.forEach((key,value)->{token(out,key);token(out,value);});out.append('|');
        for(Node child=element.getFirstChild();child!=null;child=child.getNextSibling()) {
            if(child instanceof Element nested)canonical(nested,out,body);
            else if(child.getNodeType()==Node.TEXT_NODE) {
                boolean wordText=W.equals(element.getNamespaceURI())&&Set.of("t","instrText","delText").contains(element.getLocalName());
                if((wordText||!child.getNodeValue().isBlank())&&!(body&&W.equals(element.getNamespaceURI())&&"t".equals(element.getLocalName())))token(out,child.getNodeValue());
            }
        }
        out.append(';');
    }
}
