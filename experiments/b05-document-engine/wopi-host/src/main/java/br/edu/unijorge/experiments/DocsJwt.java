package br.edu.unijorge.experiments;

import java.time.Clock;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.StreamReadFeature;

final class DocsJwt {
    static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final byte[] secret;private final Clock clock;
    DocsJwt(byte[] secret,Clock clock){if(secret.length<32)throw new IllegalArgumentException("Lab secret too short");this.secret=secret.clone();this.clock=clock;}
    private static String encode(byte[] bytes){return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private byte[] mac(String value){try{var hmac=Mac.getInstance("HmacSHA256");hmac.init(new SecretKeySpec(secret,"HmacSHA256"));return hmac.doFinal(value.getBytes(StandardCharsets.US_ASCII));}catch(Exception e){throw new IllegalStateException("HMAC unavailable");}}
    String sign(Map<String,?> payload){String value=encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))+"."+encode(JSON.writeValueAsBytes(payload));return value+"."+encode(mac(value));}
    JsonNode verify(String token,String purpose){
        try {
            if(token==null||token.length()>65536)throw new IllegalArgumentException();
            String[] parts=token.split("\\.",-1);if(parts.length!=3)throw new IllegalArgumentException();
            var header=JSON.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if(!"HS256".equals(header.path("alg").asString())||header.has("crit")||header.has("jku")||header.has("jwk"))throw new IllegalArgumentException();
            if(!MessageDigest.isEqual(mac(parts[0]+"."+parts[1]),Base64.getUrlDecoder().decode(parts[2])))throw new IllegalArgumentException();
            var claims=JSON.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if(!claims.isObject()||!purpose.equals(claims.path("operation").asString()))throw new IllegalArgumentException();
            long now=clock.instant().getEpochSecond();
            if(claims.has("exp")&&(!claims.get("exp").isIntegralNumber()||now>=claims.get("exp").asLong()))throw new IllegalArgumentException();
            if(claims.has("nbf")&&(!claims.get("nbf").isIntegralNumber()||now<claims.get("nbf").asLong()))throw new IllegalArgumentException();
            return claims;
        }catch(Exception e){throw new DocumentStore.Failure(401,"JWT inválido");}
    }
}
