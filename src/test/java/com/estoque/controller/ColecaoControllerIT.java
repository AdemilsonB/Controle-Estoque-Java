package com.estoque.controller;

import com.estoque.entity.Colecao;
import com.estoque.repository.ColecaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ColecaoControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ColecaoRepository colecaoRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarColecaoComSucesso() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.ColecaoRequest("Outono 2026", "Coleção de teste"));

        mockMvc.perform(post("/api/v1/colecoes").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nome").value("Outono 2026"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void deveRejeitarCriacaoParaOperadorCom403() throws Exception {
        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.ColecaoRequest("Outono 2026", "Coleção de teste"));

        mockMvc.perform(post("/api/v1/colecoes").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarColecaoComSucesso() throws Exception {
        Colecao colecao = Colecao.builder().nome("Coleção Original").descricao("Descrição original").build();
        Colecao salva = colecaoRepository.save(colecao);

        String corpo = objectMapper.writeValueAsString(
                new com.estoque.dto.request.ColecaoRequest("Coleção Atualizada", "Descrição atualizada"));

        mockMvc.perform(put("/api/v1/colecoes/" + salva.getId()).contentType("application/json").content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Coleção Atualizada"))
                .andExpect(jsonPath("$.descricao").value("Descrição atualizada"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveExcluirColecaoComSucesso() throws Exception {
        Colecao colecao = Colecao.builder().nome("Coleção Para Deletar").descricao("Será deletada").build();
        Colecao salva = colecaoRepository.save(colecao);

        mockMvc.perform(delete("/api/v1/colecoes/" + salva.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/colecoes/" + salva.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveRetornar404AoBuscarColecaoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/colecoes/999999"))
                .andExpect(status().isNotFound());
    }
}
