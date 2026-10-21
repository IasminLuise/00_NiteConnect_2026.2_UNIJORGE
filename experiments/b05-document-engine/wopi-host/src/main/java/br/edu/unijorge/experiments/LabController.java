package br.edu.unijorge.experiments;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.web.util.HtmlUtils;

/** Trusted, loopback-only synthetic lab controls. Intentionally not a product API. */
@RestController
@RequestMapping("/lab")
public final class LabController {
    private final DocumentStore store;
    private final String origin=System.getenv().getOrDefault("LAB_ORIGIN","http://localhost:18085");
    private final String collabora=System.getenv().getOrDefault("COLLABORA_INTERNAL","http://collabora:9980");
    private final String publicCollabora=System.getenv().getOrDefault("COLLABORA_PUBLIC","http://localhost:19980");
    public LabController(DocumentStore store){this.store=store;}
    private void origin(HttpServletRequest request){if(!origin.equals(request.getHeader("Origin")))throw new DocumentStore.Failure(403,"Origem de laboratório inválida");}
    @GetMapping("/health") Map<String,String> health(){return Map.of("status","UP","scope","synthetic-lab");}
    @GetMapping("/documents") List<Map<String,Object>> documents() {
        var result=new ArrayList<Map<String,Object>>();
        for(int i=1;i<=9;i++) {
            String name="F%02d.docx".formatted(i);UUID id=UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try{var versions=store.versions(id);result.add(Map.of("id",id,"name",name,"versions",versions));}catch(DocumentStore.Failure f){if(f.status()!=404)throw f;}
        }
        return result;
    }
    @GetMapping("/documents/{id}/versions/{version}") ResponseEntity<byte[]> version(@PathVariable UUID id,@PathVariable UUID version) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(store.version(id,version.toString()));
    }
    @GetMapping("/documents/{id}/current") Map<String,String> current(@PathVariable UUID id){return Map.of("version",store.currentVersion(id));}
    @PostMapping("/revoke/{id}") Map<String,String> revoke(@PathVariable UUID id,HttpServletRequest request){origin(request);store.revoke(id);return Map.of("status","revoked");}
    @PostMapping("/documents/{id}/versions/{version}/pdf")
    ResponseEntity<byte[]> convert(@PathVariable UUID id,@PathVariable UUID version,HttpServletRequest request)throws Exception {
        origin(request);byte[] original=store.version(id,version.toString());
        String boundary="b05-"+UUID.randomUUID();
        var body=new java.io.ByteArrayOutputStream();
        body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"data\"; filename=\"fixture.docx\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        body.write(original);body.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var converted=client.send(HttpRequest.newBuilder(URI.create(collabora+"/cool/convert-to/pdf")).timeout(Duration.ofSeconds(45))
                .header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.ofInputStream());
            try(var stream=converted.body()) {
                byte[] pdf=stream.readNBytes(32*1024*1024+1);
                if(converted.statusCode()!=200||pdf.length>32*1024*1024||pdf.length<5||!new String(pdf,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))throw new DocumentStore.Failure(503,"Conversão indisponível");
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header("X-Source-Version",version.toString()).body(pdf);
            }
        }catch(java.io.IOException e){throw new DocumentStore.Failure(503,"Conversão indisponível");}
    }
    @PostMapping(value="/editor",produces=MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> editor(@RequestParam UUID id,@RequestParam(defaultValue="false")boolean writable,HttpServletRequest request)throws Exception {
        origin(request);
        // Fetch discovery BEFORE issuing a token. Only a configured server can supply the action URL.
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var discovery=client.send(HttpRequest.newBuilder(URI.create(collabora+"/hosting/discovery")).timeout(Duration.ofSeconds(10)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        if(discovery.statusCode()!=200)throw new DocumentStore.Failure(503,"Discovery indisponível");
        var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        String action=null;
        try(var input=discovery.body()) {
            var actions=factory.newDocumentBuilder().parse(input).getElementsByTagName("action");
            for(int i=0;i<actions.getLength();i++) {
                var e=(org.w3c.dom.Element)actions.item(i);
                if(e.getAttribute("ext").equals("docx")&&e.getAttribute("name").equals(writable?"edit":"view")){action=e.getAttribute("urlsrc");break;}
            }
        }
        if(action==null)throw new DocumentStore.Failure(503,"Ação DOCX indisponível");
        String url=actionUrl(action,publicCollabora,System.getenv().getOrDefault("LAB_ENGINE","collabora"),
                "http://wopi-host:8080/wopi/files/"+id);
        var session=store.issue(id,writable,Duration.ofMinutes(30));
        String html="""
            <!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>POC B05 — editor sintético</title>
            <style>body{margin:0;font:14px system-ui}header{padding:8px;overflow-wrap:anywhere}iframe{border:0;width:100%%;height:92vh}</style>
            <header>POC descartável · documento sintético · sessão %s <button id="revoke">Revogar sessão</button> <span id="state"></span></header>
            <form id="launch" method="post" target="office" action="%s"><input type="hidden" name="access_token" value="%s"><input type="hidden" name="access_token_ttl" value="%s"></form>
            <iframe name="office" title="Editor DOCX" referrerpolicy="no-referrer"></iframe>
            <script>document.getElementById('launch').submit();document.getElementById('revoke').onclick=async()=>{let r=await fetch('/lab/revoke/%s',{method:'POST'});document.getElementById('state').textContent=r.ok?'Sessão revogada':'Falha ao revogar';};</script></html>
            """.formatted(session.id(),HtmlUtils.htmlEscape(url),session.token(),session.expiresAt().toEpochMilli(),session.id());
        return ResponseEntity.ok().header("Content-Security-Policy","default-src 'self'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; frame-src "+publicCollabora+"; form-action "+publicCollabora+"; frame-ancestors 'self'").body(html);
    }
    static String actionUrl(String action,String publicOrigin,String engine,String source) {
        var uri=URI.create(action.replaceAll("<[^>]*>",""));
        String prefix=switch(engine){case "collabora"->"/browser/";case "onlyoffice"->"/hosting/wopi/";default->throw new DocumentStore.Failure(503,"Motor inválido");};
        if(!Set.of("http","https").contains(uri.getScheme())||uri.getPath()==null||!uri.getPath().startsWith(prefix)
                ||!uri.getPath().equals(uri.normalize().getPath())||uri.getPath().contains("/../")||uri.getPath().contains("/./"))
            throw new DocumentStore.Failure(503,"Discovery inválido");
        String query=uri.getRawQuery();
        return publicOrigin+uri.getRawPath()+"?"+(query==null||query.isBlank()?"":query+"&")
                +"WOPISrc="+URLEncoder.encode(source,java.nio.charset.StandardCharsets.UTF_8);
    }
}
