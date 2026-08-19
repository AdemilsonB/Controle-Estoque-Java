package com.estoque.repository;

import com.estoque.entity.Categoria;
import com.estoque.entity.Colecao;
import com.estoque.entity.Produto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.SecurityConfig;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
@Transactional
class ProdutoRepositoryTest {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ColecaoRepository colecaoRepository;

    @Test
    void devePersistirEEncontrarProdutosComEstoqueBaixo() {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Camisetas").build());
        Colecao colecao = colecaoRepository.save(Colecao.builder().nome("Inverno").build());

        Produto produtoBaixo = Produto.builder()
                .codigo("SKU-1")
                .nome("Camiseta P")
                .categoria(categoria)
                .colecao(colecao)
                .precoVenda(new BigDecimal("49.90"))
                .estoqueMinimo(10)
                .build();
        produtoBaixo.registrarEntrada(3, new BigDecimal("20.00"));

        Produto produtoOk = Produto.builder()
                .codigo("SKU-2")
                .nome("Camiseta M")
                .categoria(categoria)
                .colecao(colecao)
                .precoVenda(new BigDecimal("49.90"))
                .estoqueMinimo(10)
                .build();
        produtoOk.registrarEntrada(50, new BigDecimal("20.00"));

        produtoRepository.save(produtoBaixo);
        produtoRepository.save(produtoOk);

        List<Produto> abaixoDoMinimo = produtoRepository.buscarComEstoqueAbaixoDoMinimo();

        assertThat(abaixoDoMinimo).extracting(Produto::getCodigo).containsExactly("SKU-1");
    }
}
