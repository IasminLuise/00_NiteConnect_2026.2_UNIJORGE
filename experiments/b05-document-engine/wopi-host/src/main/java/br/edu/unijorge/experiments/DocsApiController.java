package br.edu.unijorge.experiments;

import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.io.IOException;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/docs-api")
@ConditionalOnProperty(name="b05.docs.enabled",havingValue="true")
final class DocsApiController {
    private final DocsApiService service;private final String operator,origin,editor,engineIp;
    @org.springframework.beans.factory.annotation.Autowired
    DocsApiController(DocsApiService service)throws IOException {
        this(service,Files.readString(Path.of(DocsApiConfig.required("B05_OPERATOR_FILE"))),DocsApiConfig.required("LAB_ORIGIN"),DocsApiConfig.required("B05_EDITOR_ORIGIN"),java.net.URI.create(DocsApiConfig.required("B05_ENGINE_PIN")).getHost());
    }
    DocsApiController(DocsApiService service,String operator,String origin,String editor,String engineIp){this.service=service;this.operator=operator;this.origin=origin;this.editor=editor;this.engineIp=engineIp;}
    private void sameOrigin(HttpServletRequest request){if(!origin.equals(request.getHeader("Origin")))throw new DocumentStore.Failure(403,"Origem inválida");}
    private void operator(String token){if(token==null||!MessageDigest.isEqual(operator.getBytes(StandardCharsets.US_ASCII),token.getBytes(StandardCharsets.US_ASCII)))throw new DocumentStore.Failure(401,"Operador inválido");}
    private JsonNode body(HttpServletRequest request)throws IOException {
        byte[] bytes=request.getInputStream().readNBytes(512*1024+1);if(bytes.length>512*1024)throw new DocumentStore.Failure(413,"Mensagem excede limite");
        try{return DocsJwt.JSON.readTree(bytes);}catch(Exception e){throw new DocumentStore.Failure(400,"Mensagem inválida");}
    }
    @PostMapping("/sessions") JsonNode create(@RequestHeader(name="X-Lab-Operator",required=false)String token,HttpServletRequest request)throws IOException {
        operator(token);sameOrigin(request);var body=body(request);return service.create(body.path("alias").asString(),body.path("mode").asString(),body.path("writable").asBoolean());
    }
    @GetMapping("/config/{id}") JsonNode config(@PathVariable UUID id,@RequestParam String cap){return service.config(id,cap);}
    @GetMapping("/state/{id}") JsonNode state(@PathVariable UUID id,@RequestParam String cap){return service.state(id,cap);}
    @PostMapping("/event/{id}") Map<String,Integer> event(@PathVariable UUID id,@RequestParam String cap,HttpServletRequest request)throws IOException {
        sameOrigin(request);service.event(id,cap,body(request).path("dirty").asBoolean());return Map.of("error",0);
    }
    @PostMapping("/force/{id}") JsonNode force(@PathVariable UUID id,@RequestParam String cap,HttpServletRequest request){sameOrigin(request);return service.force(id,cap);}
    @PostMapping("/reopen/{id}") JsonNode reopen(@PathVariable UUID id,@RequestParam String cap,HttpServletRequest request){sameOrigin(request);return service.reopen(id,cap);}
    @PostMapping("/callback/{id}") ResponseEntity<Map<String,Integer>> callback(@PathVariable UUID id,@RequestParam String cap,HttpServletRequest request) {
        try {
            if(!engineIp.equals(request.getRemoteAddr()))throw new DocumentStore.Failure(401,"Origem de callback inválida");
            int error=service.callback(id,cap,body(request),request.getHeader("Authorization"));return ResponseEntity.ok(Map.of("error",error));
        }catch(DocumentStore.Failure e){return ResponseEntity.status(e.status()).body(Map.of("error",1));}
        catch(IOException e){return ResponseEntity.status(503).body(Map.of("error",1));}
    }
    @GetMapping("/source/{id}") ResponseEntity<byte[]> source(@PathVariable UUID id,@RequestParam String cap,@RequestHeader(name="Authorization",required=false)String token) {
        return bytes(service.source(id,cap,token),"source.docx");
    }
    @GetMapping("/version/{id}/{version}") ResponseEntity<byte[]> version(@PathVariable UUID id,@PathVariable UUID version,@RequestParam String cap){return bytes(service.version(id,cap,version.toString()),"versao-confirmada.docx");}
    @GetMapping("/recovery/{id}") ResponseEntity<byte[]> recovery(@PathVariable UUID id,@RequestParam String cap){return bytes(service.recovery(id,cap),"tentativa-nao-confirmada.docx");}
    private ResponseEntity<byte[]> bytes(byte[] bytes,String name){return ResponseEntity.ok().header("Content-Disposition","attachment; filename=\""+name+"\"").header("X-Content-SHA256",DocsApiService.checksum(bytes)).contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).body(bytes);}
    @GetMapping(value="/lab",produces=MediaType.TEXT_HTML_VALUE) String lab(@RequestParam String access){
        operator(access);
        return """
            <!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>NITE — laboratório Docs API</title>
            <style>body{font:16px system-ui;margin:32px;background:#f5f6fa}button,select{padding:10px;margin:8px}</style>
            <h1>NITE CONNECT — laboratório Docs API</h1><p>Sem homologação de produto. Somente corpus sintético autorizado.</p>
            <select id="alias"><option>F01.docx</option><option>F02.docx</option></select><select id="policy"><option value="structural">Controle de transporte</option><option value="strict">Validação ativa</option><option value="reject">Recusa induzida</option></select>
            <label><input id="writable" type="checkbox" checked>Edição</label><button id="open">Abrir</button><p id="error"></p>
            <script>document.getElementById('open').onclick=async()=>{const r=await fetch('/docs-api/sessions',{method:'POST',headers:{'Content-Type':'application/json','X-Lab-Operator':new URLSearchParams(location.search).get('access')},body:JSON.stringify({alias:document.getElementById('alias').value,mode:document.getElementById('policy').value,writable:document.getElementById('writable').checked})});if(!r.ok){document.getElementById('error').textContent='Não foi possível abrir o ensaio';return;}const w=await r.json();window.open('/docs-api/editor/'+w.id+'?cap='+encodeURIComponent(w.cap),'_blank');};</script></html>
            """;
    }
    @GetMapping(value="/editor/{id}",produces=MediaType.TEXT_HTML_VALUE) ResponseEntity<String> editor(@PathVariable UUID id,@RequestParam String cap){
        service.state(id,cap);
        String html="""
            <!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>NITE — edição experimental</title>
            <style>html,body{height:100%%;margin:0;font:14px system-ui}header{padding:12px;background:#132b3a;color:white}button,a{margin:4px;padding:6px}#container{height:calc(100%% - 135px)}#host-state{font-weight:700}</style>
            <header><strong>NITE CONNECT · laboratório sem homologação</strong><p id="host-state">Nenhuma nova versão confirmada no NITE</p><span id="service-state">Abertura</span>
            <button id="save-nite">Salvar no NITE</button><button id="close-editor">Encerrar editor</button><button id="reopen">Reabrir versão confirmada</button><a id="recovery" hidden>Baixar tentativa não confirmada</a>
            <small>O indicador interno do editor não confirma a gravação no NITE.</small></header><div id="container"><div id="editor"></div></div>
            <script src="%s/web-apps/apps/api/documents/api.js"></script>
            <script>
            const session=%s,cap=new URLSearchParams(location.search).get('cap'),suffix=encodeURIComponent(session)+'?cap='+encodeURIComponent(cap);
            let editor,closed=false;const state=document.getElementById('host-state');
            async function poll(){try{const r=await fetch('/docs-api/state/'+suffix);if(!r.ok)throw Error();const s=await r.json();state.dataset.state=s.state;state.dataset.version=s.headVersion;state.dataset.commits=s.commits;
            state.textContent=s.state==='committed'?'Salvo no NITE CONNECT · versão '+s.headVersion: ['rejected','recovery-required','timeout'].includes(s.state)?'Gravação não confirmada. Recuperação necessária.':'Alterações pendentes de confirmação no NITE · '+s.state;
            document.getElementById('recovery').hidden=!s.canRecover;document.getElementById('recovery').href='/docs-api/recovery/'+suffix;
            }catch(e){state.dataset.state='unavailable';state.textContent='Estado do host indisponível; gravação não confirmada';}}
            async function start(){const r=await fetch('/docs-api/config/'+suffix);if(!r.ok){state.textContent='Sessão indisponível';return;}const config=await r.json();document.getElementById('save-nite').disabled=!config.document.permissions.edit;
            config.events={onDocumentStateChange:event=>{document.getElementById('service-state').textContent=event.data?'Alterações locais':'Alterações enviadas ao serviço de edição';fetch('/docs-api/event/'+suffix,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({dirty:!!event.data})}).then(poll);},onError:()=>{document.getElementById('service-state').textContent='O editor informou uma falha';}};
            editor=new DocsAPI.DocEditor('editor',config);window.labEditor=editor;setInterval(poll,1000);await poll();}
            document.getElementById('save-nite').onclick=async()=>{state.textContent='Solicitando gravação; aguardando confirmação do host';await fetch('/docs-api/force/'+suffix,{method:'POST'});await poll();};
            document.getElementById('close-editor').onclick=()=>{if(editor&&!closed){editor.destroyEditor();closed=true;}};
            document.getElementById('reopen').onclick=async()=>{const r=await fetch('/docs-api/reopen/'+suffix,{method:'POST'});if(!r.ok){state.textContent='Aguarde a finalização da sessão';return;}const w=await r.json();location.href='/docs-api/editor/'+w.id+'?cap='+encodeURIComponent(w.cap);};start();
            </script></html>
            """.formatted(editor,DocsJwt.JSON.writeValueAsString(id.toString()));
        return ResponseEntity.ok().header("Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline' "+editor+"; style-src 'unsafe-inline'; frame-src "+editor+"; frame-ancestors 'self'").body(html);
    }
}
