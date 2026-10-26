package br.edu.unijorge.niteconnect.documents.application;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public interface DocumentEditorProvider {
    EditorConfiguration open(String editorKey,String title,URI source,URI callback,boolean readOnly,UUID actor);
    /** Only an explicitly authorized local inspection service may issue this configuration. */
    default EditorConfiguration openInspection(String editorKey,String title,URI source,URI callback,UUID actor){
        throw new UnsupportedOperationException("Read-only inspection is not supported by this provider");
    }
    Callback verifyCallback(String token,Map<String,Object> body);
    byte[] download(URI uri,int maxBytes);
    byte[] convertPdf(String key,URI source);
    boolean enabled();

    record EditorConfiguration(String apiScriptUrl,Map<String,Object> config) {
        public EditorConfiguration { config=Map.copyOf(config); }
        @Override public String toString(){return "EditorConfiguration[config=REDACTED]";}
    }
    record Callback(String key,int status,URI url,String filetype,List<UUID> users) {
        public Callback { users=List.copyOf(users); }
        @Override public String toString(){return "Callback[status="+status+", url=REDACTED]";}
    }
}
