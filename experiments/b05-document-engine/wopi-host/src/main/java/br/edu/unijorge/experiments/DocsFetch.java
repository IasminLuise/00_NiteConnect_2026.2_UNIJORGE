package br.edu.unijorge.experiments;
import java.net.URI;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
final class DocsFetch {
    private final URI pinned;private final Set<String> origins;
    DocsFetch(URI pinned,Set<String> origins){
        if(!"http".equals(pinned.getScheme())||!pinned.getHost().matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")||pinned.getUserInfo()!=null||pinned.getRawQuery()!=null||!(pinned.getPath().isEmpty()||pinned.getPath().equals("/")))throw new IllegalArgumentException("Pinned IPv4 lab endpoint required");
        this.pinned=pinned;this.origins=Set.copyOf(origins);
    }
    URI resolve(String value,String key){
        try{
            if(value==null||value.length()>8192||!key.matches("[A-Za-z0-9_-]{1,128}"))throw new IllegalArgumentException();
            var uri=URI.create(value);String origin=uri.getScheme()+"://"+uri.getRawAuthority();String path=uri.getRawPath();
            if(!origins.contains(origin)||uri.getUserInfo()!=null||uri.getFragment()!=null||path==null||!path.matches("[A-Za-z0-9_./-]+")||path.contains("..")||path.contains("//")||!(path.startsWith("/cache/files/"+key+"_")||path.startsWith("/cache/files/data/"+key+"_")))throw new IllegalArgumentException();
            return URI.create(pinned.getScheme()+"://"+pinned.getRawAuthority()+path+(uri.getRawQuery()==null?"":"?"+uri.getRawQuery()));
        }catch(Exception e){throw new DocumentStore.Failure(400,"Origem da candidata recusada");}
    }
    byte[] download(String value,String key){return request(resolve(value,key),null,DocxValidator.MAX_BYTES);}
    byte[] command(byte[] bytes){return request(pinned.resolve("/coauthoring/CommandService.ashx"),bytes,65536);}
    private byte[] request(URI uri,byte[] body,int limit){
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)uri.toURL().openConnection();connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(2500);connection.setReadTimeout(5000);
            if(body!=null){connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");try(var out=connection.getOutputStream()){out.write(body);}}
            if(connection.getResponseCode()!=200||connection.getContentLengthLong()>limit)throw new IOException();
            try(var stream=connection.getInputStream();var output=new java.io.ByteArrayOutputStream()) {
                long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(12);byte[] buffer=new byte[8192];int count;
                while(true){long remaining=deadline-System.nanoTime();if(remaining<=0)throw new IOException();connection.setReadTimeout((int)Math.max(1,Math.min(5000,java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(remaining))));count=stream.read(buffer);if(count<0)break;if(output.size()+count>limit)throw new IOException();output.write(buffer,0,count);}
                return output.toByteArray();
            }
        }catch(IOException e){throw new DocumentStore.Failure(503,"Serviço de edição indisponível");}
        finally {if(connection!=null)connection.disconnect();}
    }
}
