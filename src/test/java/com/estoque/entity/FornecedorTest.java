package com.estoque.entity;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FornecedorTest {

    @Test
    void doisFornecedoresComMesmoCnpjSaoIguaisEDeduplicamEmSet() {
        Endereco endereco = new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000");
        Fornecedor a = Fornecedor.builder()
                .cnpj("12345678000199").razaoSocial("Fornecedor A").telefone("11999999999")
                .email("a@fornecedor.com").endereco(endereco).build();
        Fornecedor b = Fornecedor.builder()
                .cnpj("12345678000199").razaoSocial("Razão social diferente").telefone("11888888888")
                .email("b@fornecedor.com").endereco(endereco).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        Set<Fornecedor> fornecedores = new HashSet<>();
        fornecedores.add(a);
        fornecedores.add(b);
        assertThat(fornecedores).hasSize(1);
    }

    @Test
    void fornecedoresComCnpjsDiferentesNaoSaoIguais() {
        Fornecedor a = Fornecedor.builder().cnpj("11111111000191").razaoSocial("X").telefone("1")
                .email("x@x.com").build();
        Fornecedor b = Fornecedor.builder().cnpj("22222222000192").razaoSocial("X").telefone("1")
                .email("x@x.com").build();

        assertThat(a).isNotEqualTo(b);
    }
}
