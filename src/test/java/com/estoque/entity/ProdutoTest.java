package com.estoque.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoTest {

    @Test
    void doisProdutosComMesmoCodigoSaoIguaisEDeduplicamEmSet() {
        Categoria categoria = Categoria.builder().nome("Calçados").build();
        Produto a = Produto.builder()
                .codigo("SKU-1").nome("Camiseta P").categoria(categoria)
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(10).build();
        Produto b = Produto.builder()
                .codigo("SKU-1").nome("Nome diferente").categoria(categoria)
                .precoVenda(new BigDecimal("99.90")).estoqueMinimo(5).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        Set<Produto> produtos = new HashSet<>();
        produtos.add(a);
        produtos.add(b);
        assertThat(produtos).hasSize(1);
    }

    @Test
    void produtosComCodigosDiferentesNaoSaoIguais() {
        Categoria categoria = Categoria.builder().nome("Calçados").build();
        Produto a = Produto.builder().codigo("SKU-1").nome("X").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build();
        Produto b = Produto.builder().codigo("SKU-2").nome("X").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build();

        assertThat(a).isNotEqualTo(b);
    }
}
