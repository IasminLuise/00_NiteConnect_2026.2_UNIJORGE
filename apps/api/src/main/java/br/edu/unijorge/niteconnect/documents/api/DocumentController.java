package br.edu.unijorge.niteconnect.documents.api;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import br.edu.unijorge.niteconnect.identity.application.IdentityDirectory;
import br.edu.unijorge.niteconnect.documents.application.*;
import br.edu.unijorge.niteconnect.documents.domain.DocumentRecords.*;
import br.edu.unijorge.niteconnect.shared.api.*;
@RestController
public class DocumentController {
    private final DocumentService service;private final DocumentEditorService editor;private final IdentityDirectory identities;
    public DocumentController(DocumentService service,DocumentEditorService editor,IdentityDirectory identities){this.service=service;this.editor=editor;this.identities=identities;}
    @GetMapping("/api/v1/document-templates") PageResult<Template> templates(@AuthenticationPrincipal Jwt j,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.templates(identities.member(j),page,size);}
    @GetMapping("/api/v1/document-templates/{id}/versions") PageResult<DocumentService.TemplateVersionView> templateVersions(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.templateVersions(identities.member(j),id,page,size);}
    @PostMapping("/api/v1/document-templates") @ResponseStatus(HttpStatus.CREATED)
    Template template(@AuthenticationPrincipal Jwt j,@Valid @RequestBody DocumentService.CreateTemplate c){return service.createTemplate(identities.member(j),c);}
    @PostMapping(value="/api/v1/document-templates/{id}/versions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    DocumentService.TemplateVersionView templateVersion(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@RequestParam long expectedVersion,@RequestParam String reason,@RequestPart MultipartFile file){return service.templateVersion(identities.member(j),id,expectedVersion,bytes(file),reason);}
    @PostMapping("/api/v1/document-templates/{id}/retire") Template retire(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@Valid @RequestBody DocumentService.Command c){return service.retireTemplate(identities.member(j),id,c);}
    @GetMapping("/api/v1/documents") PageResult<Doc> list(@AuthenticationPrincipal Jwt j,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(identities.member(j),page,size);}
    @PostMapping("/api/v1/documents") @ResponseStatus(HttpStatus.CREATED)
    Doc create(@AuthenticationPrincipal Jwt j,@Valid @RequestBody DocumentService.CreateDocument c){return service.create(identities.member(j),c);}
    @GetMapping("/api/v1/documents/{id}") Doc read(@AuthenticationPrincipal Jwt j,@PathVariable UUID id){return service.read(identities.member(j),id);}
    @GetMapping("/api/v1/documents/{id}/versions") PageResult<VersionView> versions(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.versions(identities.member(j),id,page,size);}
    @GetMapping("/api/v1/documents/{id}/history") PageResult<Event> history(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.history(identities.member(j),id,page,size);}
    @PostMapping(value="/api/v1/documents/{id}/versions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    VersionView upload(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@RequestParam long expectedVersion,@RequestParam String reason,@RequestPart MultipartFile file){return service.upload(identities.member(j),id,expectedVersion,bytes(file),reason);}
    @GetMapping("/api/v1/documents/{id}/versions/{version}/download") ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@PathVariable UUID version){return response(service.download(identities.member(j),id,version),"application/vnd.openxmlformats-officedocument.wordprocessingml.document","document.docx").header("X-Document-Version",version.toString()).build();}
    @PostMapping("/api/v1/documents/{id}/grants") Doc grant(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@Valid @RequestBody DocumentService.GrantCommand c){return service.grant(identities.member(j),id,c);}
    @PostMapping("/api/v1/documents/{id}/grants/{member}/revoke") Doc revoke(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@PathVariable UUID member,@Valid @RequestBody DocumentService.Command c){return service.revoke(identities.member(j),id,member,c);}
    @PostMapping("/api/v1/documents/{id}/archive") Doc archive(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@Valid @RequestBody DocumentService.Command c){return service.archive(identities.member(j),id,c);}
    @PostMapping("/api/v1/documents/{id}/restore") Doc restore(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@Valid @RequestBody DocumentService.Restore c){return service.restore(identities.member(j),id,c);}
    @PostMapping("/api/v1/documents/{id}/editor-sessions") DocumentEditorService.Session open(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@Valid @RequestBody DocumentEditorService.Open c){return editor.open(identities.member(j),id,c);}
    @PostMapping("/api/v1/documents/{id}/editor-sessions/{cycle}/close") @ResponseStatus(HttpStatus.NO_CONTENT)
    void close(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@PathVariable UUID cycle){editor.close(identities.member(j),id,cycle);}
    @PostMapping("/api/v1/documents/{id}/versions/{version}/pdf") ResponseEntity<byte[]> pdf(@AuthenticationPrincipal Jwt j,@PathVariable UUID id,@PathVariable UUID version){var result=editor.pdf(identities.member(j),id,version);return response(result.bytes(),"application/pdf","document.pdf").header("X-Document-Version",version.toString()).build();}
    private static byte[] bytes(MultipartFile f){if(f==null||f.isEmpty()||f.getSize()>8*1024*1024)throw ApiException.invalid("DOCX obrigatório, máximo 8 MiB");try{return f.getBytes();}catch(IOException e){throw ApiException.invalid("Arquivo indisponível");}}
    /** Builder keeps a bounded body; no client filename or filesystem path is returned. */
    private static Body response(byte[] bytes,String mime,String filename){return new Body(bytes,mime,filename);}
    private static final class Body {
        private final byte[] bytes;private final ResponseEntity.BodyBuilder builder;
        Body(byte[] bytes,String mime,String name){this.bytes=bytes;this.builder=ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).contentLength(bytes.length).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString());}
        Body header(String k,String v){builder.header(k,v);return this;}ResponseEntity<byte[]> build(){return builder.body(bytes);}
    }
}
