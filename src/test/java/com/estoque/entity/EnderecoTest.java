package com.estoque.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnderecoTest {
    @Test
    void deveConstruirEnderecoValido() {
        Endereco endereco = new Endereco("Rua A", "100", "Centro", "Curitiba", "PR", "80000-000");

        assertThat(endereco.getLogradouro()).isEqualTo("Rua A");
        assertThat(endereco.getEstado()).isEqualTo("PR");
        assertThat(endereco.getCep()).isEqualTo("80000-000");
    }
}
