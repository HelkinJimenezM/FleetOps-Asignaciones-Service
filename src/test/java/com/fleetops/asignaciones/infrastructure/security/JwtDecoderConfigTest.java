package com.fleetops.asignaciones.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtDecoderConfig")
class JwtDecoderConfigTest {

    @TempDir
    Path tempDir;

    private RSAPrivateKey privateKey;
    private JwtDecoderConfig jwtDecoderConfig;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";

        Path publicKeyFile = tempDir.resolve("jwt_public.pem");
        Files.writeString(publicKeyFile, pem);

        jwtDecoderConfig = new JwtDecoderConfig();
        ReflectionTestUtils.setField(jwtDecoderConfig, "jwtAlgorithm", "RS256");
        ReflectionTestUtils.setField(jwtDecoderConfig, "jwtPublicKeyPath", publicKeyFile.toString());
    }

    @Test
    @DisplayName("dado un token firmado con la llave privada correspondiente, lo decodifica correctamente")
    void jwtDecoder_dadoTokenValido_loDecodifica() throws JOSEException {
        JwtDecoder decoder = jwtDecoderConfig.jwtDecoder();

        String token = firmarToken(new Date(System.currentTimeMillis() + 60_000));

        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("usuario-test");
    }

    @Test
    @DisplayName("dado un token expirado, lanza JwtException")
    void jwtDecoder_dadoTokenExpirado_lanzaExcepcion() throws JOSEException {
        JwtDecoder decoder = jwtDecoderConfig.jwtDecoder();

        String token = firmarToken(new Date(System.currentTimeMillis() - 60_000));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("dado un token firmado con otra llave privada, lanza JwtException")
    void jwtDecoder_dadoTokenConFirmaInvalida_lanzaExcepcion() throws Exception {
        JwtDecoder decoder = jwtDecoderConfig.jwtDecoder();

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPrivateKey otraLlave = (RSAPrivateKey) generator.generateKeyPair().getPrivate();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.RS256),
                new JWTClaimsSet.Builder()
                        .subject("usuario-test")
                        .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                        .build());
        signedJWT.sign(new RSASSASigner(otraLlave));

        assertThatThrownBy(() -> decoder.decode(signedJWT.serialize())).isInstanceOf(JwtException.class);
    }

    private String firmarToken(Date expiracion) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("usuario-test")
                .expirationTime(expiracion)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        JWSSigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
