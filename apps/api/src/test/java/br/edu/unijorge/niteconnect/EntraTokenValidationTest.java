package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.identity.application.EntraProperties;
import br.edu.unijorge.niteconnect.identity.infrastructure.EntraConfiguration;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;

class EntraTokenValidationTest {
    static RSAKey key;
    static NimbusJwtDecoder decoder;
    static final EntraProperties properties = new EntraProperties(
            UUID.fromString("11111111-1111-4111-8111-111111111111"), "test-api-audience", "access_as_user");

    @BeforeAll
    static void setup() throws Exception {
        key = new RSAKeyGenerator(2048).generate();
        decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        decoder.setJwtValidator(EntraConfiguration.tokenValidator(properties));
    }
    String signed(Consumer<JWTClaimsSet.Builder> change) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder().issuer(properties.issuer()).audience(properties.audience())
                .subject("test-subject").claim("tid", properties.tenantId().toString())
                .claim("oid", "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb").claim("ver", "2.0").claim("scp", "access_as_user")
                .issueTime(Date.from(now.minusSeconds(10))).notBeforeTime(Date.from(now.minusSeconds(10)))
                .expirationTime(Date.from(now.plusSeconds(300)));
        change.accept(claims);
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims.build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
    @Test
    void validDelegatedAccessTokenIsAccepted() throws Exception {
        assertThat(decoder.decode(signed(c -> {})).getSubject()).isEqualTo("test-subject");
    }
    @ParameterizedTest
    @ValueSource(strings={"tid", "oid", "scp", "ver", "exp", "nbf", "sub", "aud", "iss"})
    void mandatoryClaimsCannotBeOmitted(String claim) throws Exception {
        String token = signed(c -> c.claim(claim, null));
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
    @ParameterizedTest
    @ValueSource(strings={"issuer", "audience", "tenant", "expired", "future", "scope", "application", "object"})
    void invalidAccessTokenIsRejected(String change) throws Exception {
        String token = signed(c -> {
            switch (change) {
                case "issuer" -> c.issuer("https://login.microsoftonline.com/common/v2.0");
                case "audience" -> c.audience("some-other-api");
                case "tenant" -> c.claim("tid", "99999999-9999-4999-8999-999999999999");
                case "expired" -> c.expirationTime(Date.from(Instant.now().minusSeconds(300)));
                case "future" -> c.notBeforeTime(Date.from(Instant.now().plusSeconds(300)));
                case "scope" -> c.claim("scp", "not_access_as_user");
                case "application" -> c.claim("idtyp", "app");
                case "object" -> c.claim("oid", "invalid-uuid");
                default -> throw new IllegalArgumentException(change);
            }
        });
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
    @Test
    void incorrectSignatureIsRejected() throws Exception {
        SignedJWT original = SignedJWT.parse(signed(c -> {}));
        SignedJWT jwt = new SignedJWT(original.getHeader(), original.getJWTClaimsSet());
        jwt.sign(new RSASSASigner(new RSAKeyGenerator(2048).generate()));
        assertThatThrownBy(() -> decoder.decode(jwt.serialize())).isInstanceOf(JwtException.class);
    }
    @Test
    void idTokenWithoutDelegatedScopeIsRejected() throws Exception {
        String token = signed(c -> c.claim("scp", null).claim("nonce", "test-nonce"));
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
