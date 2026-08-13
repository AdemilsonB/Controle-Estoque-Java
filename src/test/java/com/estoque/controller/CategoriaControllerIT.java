package com.estoque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoriaControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

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
}
