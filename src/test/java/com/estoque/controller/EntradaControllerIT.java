package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, WebConfig.class, SecurityConfig.class})
@WebAppConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
@Transactional
class EntradaControllerIT {

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveRegistrarEntradaEAtualizarEstoque() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Entrada IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-ENTRADA-IT").nome("Produto Entrada IT").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());

        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.EntradaRequest(produto.getId(), null, 20, new BigDecimal("5.00"), "compra"));

        mockMvc.perform(post("/api/v1/entradas").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.quantidade").value(20));

        Produto produtoAtualizado = produtoRepository.findById(produto.getId()).orElseThrow();
        assertThat(produtoAtualizado.getQuantidadeEstoque()).isEqualTo(20);
        assertThat(produtoAtualizado.getCustoMedio()).isEqualByComparingTo("5.00");
    }

    @Test
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveListarEntradasPaginado() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Entrada IT 2").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-ENTRADA-IT-2").nome("Produto Entrada IT 2").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());

        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.EntradaRequest(produto.getId(), null, 15, new BigDecimal("3.00"), "compra listagem"));

        mockMvc.perform(post("/api/v1/entradas").contentType("application/json").content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/entradas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].tipo").value("ENTRADA"));
    }
}
