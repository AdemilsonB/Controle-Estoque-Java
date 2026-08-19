package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.repository.CategoriaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.context.WebApplicationContext;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.WebConfig;
import com.estoque.config.SecurityConfig;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, WebConfig.class, SecurityConfig.class})
@WebAppConfiguration
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
class CategoriaControllerIT {

    @Autowired private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarCategoriaComSucesso() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.CategoriaRequest("Eletrônicos", "Categoria de teste"));

        mockMvc.perform(post("/api/v1/categorias").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Eletrônicos"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void deveRejeitarCriacaoParaOperadorCom403() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.CategoriaRequest("Eletrônicos", "Categoria de teste"));

        mockMvc.perform(post("/api/v1/categorias").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void deveRejeitarNomeEmBrancoCom400() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.CategoriaRequest("", null));

        mockMvc.perform(post("/api/v1/categorias").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("nome"));
    }

    @Test
    @WithMockUser
    void deveListarCategoriasPaginado() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarCategoriaComSucesso() throws Exception {
        Categoria categoria = Categoria.builder().nome("Categoria Original").descricao("Descrição original").build();
        Categoria salva = categoriaRepository.save(categoria);

        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.CategoriaRequest("Categoria Atualizada", "Descrição atualizada"));

        mockMvc.perform(put("/api/v1/categorias/" + salva.getId()).contentType("application/json").content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Categoria Atualizada"))
                .andExpect(jsonPath("$.descricao").value("Descrição atualizada"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveExcluirCategoriaComSucesso() throws Exception {
        Categoria categoria = Categoria.builder().nome("Categoria Para Deletar").descricao("Será deletada").build();
        Categoria salva = categoriaRepository.save(categoria);

        mockMvc.perform(delete("/api/v1/categorias/" + salva.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categorias/" + salva.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveRetornar404AoBuscarCategoriaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/categorias/999999"))
                .andExpect(status().isNotFound());
    }
}
