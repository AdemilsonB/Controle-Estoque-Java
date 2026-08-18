package com.estoque.entity;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ColecaoTest {

    @Test
    void duasColecoesComMesmoNomeSaoIguaisEDeduplicamEmSet() {
        Colecao a = Colecao.builder().nome("Verão 2026").descricao("Coleção verão").build();
        Colecao b = Colecao.builder().nome("Verão 2026").descricao("Descrição diferente").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        Set<Colecao> colecoes = new HashSet<>();
        colecoes.add(a);
        colecoes.add(b);
        assertThat(colecoes).hasSize(1);
    }

    @Test
    void colecoesComNomesDiferentesNaoSaoIguais() {
        Colecao a = Colecao.builder().nome("Verão 2026").build();
        Colecao b = Colecao.builder().nome("Inverno 2026").build();

        assertThat(a).isNotEqualTo(b);
    }
}
