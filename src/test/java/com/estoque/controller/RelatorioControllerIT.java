package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.WebConfig;
import com.estoque.config.SecurityConfig;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, WebConfig.class, SecurityConfig.class})
@WebAppConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
@Transactional
class RelatorioControllerIT {

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void deveRetornarValorTotalDoEstoque() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Relatorio IT").build());
        Produto produto = Produto.builder().codigo("SKU-RELATORIO-IT").nome("Produto Relatorio IT")
                .categoria(categoria).precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build();
        produto.registrarEntrada(4, new BigDecimal("25.00"));
        produtoRepository.save(produto);

        mockMvc.perform(get("/api/v1/relatorios/valor-estoque"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotalEstoque").value(100.00))
                .andExpect(jsonPath("$.quantidadeProdutosAtivos").value(1));
    }

    @Test
    @WithMockUser
    void deveRetornarProdutosComEstoqueBaixo() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Estoque Baixo IT").build());
        Produto produto = Produto.builder().codigo("SKU-ESTOQUE-BAIXO").nome("Produto Estoque Baixo")
                .categoria(categoria).precoVenda(new BigDecimal("15.00")).estoqueMinimo(10).build();
        produto.registrarEntrada(2, new BigDecimal("5.00"));
        produtoRepository.save(produto);

        mockMvc.perform(get("/api/v1/relatorios/estoque-baixo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("SKU-ESTOQUE-BAIXO"))
                .andExpect(jsonPath("$[0].quantidadeEstoque").value(2));
    }
}
