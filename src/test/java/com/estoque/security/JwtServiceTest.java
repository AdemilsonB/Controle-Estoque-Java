package com.estoque.security;

import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODk=";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    @Test
    void deveGerarTokenEExtrairSubject() {
        String token = jwtService.gerarToken("admin@estoque.com", Role.ADMIN);

        assertThat(jwtService.extrairSubject(token)).isEqualTo("admin@estoque.com");
    }

    @Test
    void deveValidarTokenParaOSubjectCorreto() {
        String token = jwtService.gerarToken("admin@estoque.com", Role.ADMIN);

        assertThat(jwtService.tokenValido(token, "admin@estoque.com")).isTrue();
        assertThat(jwtService.tokenValido(token, "outro@estoque.com")).isFalse();
    }

    @Test
    void deveRejeitarTokenExpirado() throws InterruptedException {
        JwtService servicoComExpiracaoCurta = new JwtService(SECRET, 1L);
        String token = servicoComExpiracaoCurta.gerarToken("admin@estoque.com", Role.ADMIN);

        Thread.sleep(50);

        assertThat(servicoComExpiracaoCurta.tokenValido(token, "admin@estoque.com")).isFalse();
    }
}
