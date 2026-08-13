package com.estoque.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void deveAutenticarComCredenciaisValidasERetornarToken() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.LoginRequest("admin@estoque.com", "admin123"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test
    void deveRejeitarCredenciaisInvalidasCom401() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.LoginRequest("admin@estoque.com", "senha-errada"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarRequisicaoSemTokenCom401() throws Exception {
        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isUnauthorized());
    }
}
