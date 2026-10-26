package br.edu.unijorge.experiments;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Conservative synthetic-corpus policy; not an institutional upload policy. */
final class DocxValidator {
    static final int MAX_BYTES=8*1024*1024, MAX_ENTRY=16*1024*1024, MAX_TOTAL=64*1024*1024;
    static void validate(byte[] bytes) {
        if(bytes.length>MAX_BYTES)throw new DocumentStore.Failure(413,"Arquivo excede limite técnico");
        var names=new HashSet<String>();var xml=new HashMap<String,byte[]>();var checksums=new HashMap<String,Long>();long total=0;
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry e;
            while((e=zip.getNextEntry())!=null) {
                String n=e.getName();
                if(!names.add(n)||n.startsWith("/")||n.contains("..")||n.contains("\\")||n.contains(":"))invalid();
                if(names.size()>512)throw new DocumentStore.Failure(413,"Pacote excede limite técnico");
                String lower=n.toLowerCase(Locale.ROOT);
                if(lower.contains("vba")||lower.contains("embeddings/")||lower.endsWith(".bin"))invalid();
                var out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int read;int size=0;
                while((read=zip.read(buffer))!=-1) {
                    size+=read;total+=read;
                    if(size>MAX_ENTRY||total>MAX_TOTAL||names.size()>512)throw new DocumentStore.Failure(413,"Pacote excede limite técnico");
                    if(n.endsWith(".xml")||n.endsWith(".rels"))out.write(buffer,0,read);
                }
                if(n.endsWith(".xml")||n.endsWith(".rels"))xml.put(n,out.toByteArray());
                checksums.put(n,e.getCrc());
            }
            validateDirectory(bytes,names,checksums);
            if(!names.contains("[Content_Types].xml")||!names.contains("word/document.xml"))invalid();
            var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            var parser=factory.newDocumentBuilder();parser.setErrorHandler(new DefaultHandler(){@Override public void fatalError(org.xml.sax.SAXParseException e)throws SAXException{throw e;}});
            for(var part:xml.entrySet()) {
                var doc=parser.parse(new ByteArrayInputStream(part.getValue()));
                if(part.getKey().equals("[Content_Types].xml")) {
                    var types=doc.getElementsByTagNameNS("*","Override");boolean main=false;
                    for(int i=0;i<types.getLength();i++) {
                        var element=(org.w3c.dom.Element)types.item(i);String type=element.getAttribute("ContentType");
                        if(type.toLowerCase(Locale.ROOT).contains("macroenabled"))invalid();
                        if(element.getAttribute("PartName").equals("/word/document.xml")&&type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"))main=true;
                    }
                    if(!main)invalid();
                }
                if(part.getKey().endsWith(".rels")) {
                    var rels=doc.getElementsByTagNameNS("*","Relationship");
                    for(int i=0;i<rels.getLength();i++) {
                        var relationship=(org.w3c.dom.Element)rels.item(i);
                        if(relationship.getAttribute("TargetMode").equalsIgnoreCase("External")) {
                            // A hyperlink is inert until the reader activates it; never fetch it here.
                            // Linked images, templates and other external parts remain prohibited.
                            if(!relationship.getAttribute("Type").equals("http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink"))invalid();
                            var target=java.net.URI.create(relationship.getAttribute("Target"));
                            if(!"https".equalsIgnoreCase(target.getScheme())||target.getHost()==null||target.getUserInfo()!=null)invalid();
                        }
                    }
                }
                if(part.getKey().equals("word/document.xml")&&!doc.getDocumentElement().getLocalName().equals("document"))invalid();
            }
        }catch(DocumentStore.Failure f){throw f;}catch(Exception e){invalid();}
    }
    /** Small ZIP-only corpus: reject ZIP64, split archives, absent/truncated central directory. */
    private static void validateDirectory(byte[] bytes,Set<String> names,Map<String,Long> checksums) {
        var data=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);int end=-1;
        for(int i=bytes.length-22;i>=Math.max(0,bytes.length-65557);i--)
            if(data.getInt(i)==0x06054b50&&i+22+u16(data,i+20)==bytes.length){end=i;break;}
        if(end<0||u16(data,end+4)!=0||u16(data,end+6)!=0)invalid();
        int count=u16(data,end+10);long size=u32(data,end+12),offset=u32(data,end+16);
        if(count!=names.size()||u16(data,end+8)!=count||offset+size!=end||offset>Integer.MAX_VALUE)invalid();
        int cursor=(int)offset;var centralNames=new HashSet<String>();
        for(int i=0;i<count;i++) {
            if(cursor+46>end||data.getInt(cursor)!=0x02014b50)invalid();
            int nameSize=u16(data,cursor+28),extraSize=u16(data,cursor+30),commentSize=u16(data,cursor+32);
            if(cursor+46L+nameSize+extraSize+commentSize>end||u16(data,cursor+34)!=0)invalid();
            var charset=(u16(data,cursor+8)&0x800)!=0?java.nio.charset.StandardCharsets.UTF_8:java.nio.charset.Charset.forName("IBM437");
            String name=new String(bytes,cursor+46,nameSize,charset);
            long local=u32(data,cursor+42),compressed=u32(data,cursor+20);
            if(!centralNames.add(name)||!names.contains(name)||u32(data,cursor+16)!=checksums.get(name)||local+30>offset)invalid();
            int localOffset=(int)local;
            if(data.getInt(localOffset)!=0x04034b50||local+30+u16(data,localOffset+26)+u16(data,localOffset+28)+compressed>offset)invalid();
            cursor+=46+nameSize+extraSize+commentSize;
        }
        if(cursor!=end)invalid();
    }
    private static int u16(ByteBuffer b,int i){return Short.toUnsignedInt(b.getShort(i));}
    private static long u32(ByteBuffer b,int i){return Integer.toUnsignedLong(b.getInt(i));}
    private static void invalid(){throw new DocumentStore.Failure(415,"DOCX inválido ou conteúdo não permitido no laboratório");}
}
