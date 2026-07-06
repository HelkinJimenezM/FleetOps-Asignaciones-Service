package com.fleetops.asignaciones.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.io.PrintWriter;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock JwtDecoder jwtDecoder;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("dado token Bearer válido, autentica y continúa la cadena")
    void doFilter_dadoTokenValido_autenticaYContinua() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(null);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtDecoder, entryPoint);

        Jwt jwt = Jwt.withTokenValue("token-valido")
                .header("alg", "RS256")
                .subject("usuario-test")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtDecoder.decode("token-valido")).thenReturn(jwt);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("usuario-test");
    }

    @Test
    @DisplayName("dado token inválido o expirado, responde 401 y no continúa la cadena")
    void doFilter_dadoTokenInvalido_respondeUnauthorized() throws Exception {
        var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtDecoder, entryPoint);

        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(jwtDecoder.decode("token-invalido")).thenThrow(new JwtException("firma inválida"));
        when(response.getWriter()).thenReturn(new PrintWriter(java.io.Writer.nullWriter()));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("dada petición sin header Authorization, continúa la cadena sin autenticar")
    void doFilter_sinHeader_continuaSinAutenticar() throws Exception {
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(null);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtDecoder, entryPoint);

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtDecoder);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
