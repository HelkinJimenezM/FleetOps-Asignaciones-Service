package com.fleetops.asignaciones.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Construye el {@link JwtDecoder} a partir de la llave pública RSA que el API Gateway
 * monta como volumen en el contenedor (JWT_PUBLIC_KEY_PATH). El Gateway firma con la
 * llave privada correspondiente usando JWT_ALGORITHM (RS256).
 */
@Configuration
public class JwtDecoderConfig {

    @Value("${JWT_ALGORITHM}")
    private String jwtAlgorithm;

    @Value("${JWT_PUBLIC_KEY_PATH}")
    private String jwtPublicKeyPath;

    @Bean
    public JwtDecoder jwtDecoder() {
        RSAPublicKey publicKey = loadPublicKey(jwtPublicKeyPath);
        SignatureAlgorithm algorithm = SignatureAlgorithm.from(jwtAlgorithm);
        if (algorithm == null) {
            throw new IllegalStateException("JWT_ALGORITHM no reconocido: " + jwtAlgorithm);
        }
        return NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(algorithm)
                .build();
    }

    private RSAPublicKey loadPublicKey(String path) {
        String pem;
        try {
            pem = Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la llave pública JWT en: " + path, e);
        }

        String base64Body = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        try {
            byte[] decoded = Base64.getDecoder().decode(base64Body);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Llave pública JWT inválida en: " + path, e);
        }
    }
}
