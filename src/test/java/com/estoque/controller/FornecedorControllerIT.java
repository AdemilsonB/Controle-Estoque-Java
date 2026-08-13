package com.estoque.controller;

import com.estoque.entity.Endereco;
import com.estoque.entity.Fornecedor;
import com.estoque.repository.FornecedorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FornecedorControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FornecedorRepository fornecedorRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarFornecedorComEnderecoValido() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.FornecedorRequest(
                "11111111000181", "Nova Fornecedora LTDA", "41988887777", "contato@novafornecedora.com",
                new com.estoque.dto.request.EnderecoRequest("Rua X", "10", "Centro", "Curitiba", "PR", "80000-000")));

        mockMvc.perform(post("/api/v1/fornecedores").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.endereco.cidade").value("Curitiba"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRejeitarCnpjComFormatoInvalidoCom400() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.FornecedorRequest(
                "cnpj-invalido", "Nova Fornecedora LTDA", "41988887777", "contato@novafornecedora.com",
                new com.estoque.dto.request.EnderecoRequest("Rua X", "10", "Centro", "Curitiba", "PR", "80000-000")));

        mockMvc.perform(post("/api/v1/fornecedores").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarFornecedorComSucesso() throws Exception {
        Fornecedor fornecedor = Fornecedor.builder()
                .cnpj("22222222000182")
                .razaoSocial("Fornecedor Original")
                .telefone("11999999999")
                .email("contato@fornecedor.com")
                .endereco(new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000"))
                .build();
        Fornecedor salvo = fornecedorRepository.save(fornecedor);

        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.FornecedorRequest(
                "22222222000182", "Fornecedor Atualizado", "41988887777", "novo@fornecedor.com",
                new com.estoque.dto.request.EnderecoRequest("Rua Y", "20", "Bairro X", "Rio de Janeiro", "RJ", "20000-000")));

        mockMvc.perform(put("/api/v1/fornecedores/" + salvo.getId()).contentType("application/json").content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("Fornecedor Atualizado"))
                .andExpect(jsonPath("$.email").value("novo@fornecedor.com"))
                .andExpect(jsonPath("$.endereco.cidade").value("Rio de Janeiro"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveExcluirFornecedorComSucesso() throws Exception {
        Fornecedor fornecedor = Fornecedor.builder()
                .cnpj("33333333000183")
                .razaoSocial("Fornecedor para Deletar")
                .telefone("11999999999")
                .email("deletar@fornecedor.com")
                .endereco(new Endereco("Rua Z", "30", "Bairro Y", "Belo Horizonte", "MG", "30000-000"))
                .build();
        Fornecedor salvo = fornecedorRepository.save(fornecedor);

        mockMvc.perform(delete("/api/v1/fornecedores/" + salvo.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/fornecedores/" + salvo.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveRetornar404AoBuscarFornecedorInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/fornecedores/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void deveRejeitarCriacaoParaOperadorCom403() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.FornecedorRequest(
                "44444444000184", "Fornecedora Operador LTDA", "41988887777", "operador@fornecedora.com",
                new com.estoque.dto.request.EnderecoRequest("Rua X", "10", "Centro", "Curitiba", "PR", "80000-000")));

        mockMvc.perform(post("/api/v1/fornecedores").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
    }
}
