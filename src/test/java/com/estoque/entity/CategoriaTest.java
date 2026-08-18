package com.estoque.entity;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CategoriaTest {

    @Test
    void duasCategoriasComMesmoNomeSaoIguaisEDeduplicamEmSet() {
        Categoria a = Categoria.builder().nome("Calçados").descricao("Tênis e sapatos").build();
        Categoria b = Categoria.builder().nome("Calçados").descricao("Descrição diferente").build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        Set<Categoria> categorias = new HashSet<>();
        categorias.add(a);
        categorias.add(b);
        assertThat(categorias).hasSize(1);
    }

    @Test
    void categoriasComNomesDiferentesNaoSaoIguais() {
        Categoria a = Categoria.builder().nome("Calçados").build();
        Categoria b = Categoria.builder().nome("Vestuário").build();

        assertThat(a).isNotEqualTo(b);
    }
}
