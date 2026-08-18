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
class ProdutoKardexIT {

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
    void deveListarEntradasESaidasMisturadasNoKardex() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Kardex IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-KARDEX-IT").nome("Produto Kardex IT").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());

        mockMvc.perform(post("/api/v1/entradas").contentType("application/json").content(
                        objectMapper.writeValueAsString(new com.estoque.dto.request.EntradaRequest(
                                produto.getId(), null, 10, new BigDecimal("5.00"), null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/saidas").contentType("application/json").content(
                        objectMapper.writeValueAsString(new com.estoque.dto.request.SaidaRequest(
                                produto.getId(), 3, com.estoque.enums.MotivoSaida.VENDA, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/produtos/" + produto.getId() + "/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].tipo").value("SAIDA"))
                .andExpect(jsonPath("$.content[0].motivo").value("VENDA"))
                .andExpect(jsonPath("$.content[1].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.content[1].custoUnitario").value(5.00));
    }
}
