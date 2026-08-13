package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProdutoControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarProdutoComCategoriaValida() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Teste IT").build());

        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.ProdutoRequest(
                "SKU-IT-1", "Produto Teste", "desc", categoria.getId(), null, null,
                new BigDecimal("29.90"), 5));

        mockMvc.perform(post("/api/v1/produtos").contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaNome").value("Categoria Teste IT"))
                .andExpect(jsonPath("$.quantidadeEstoque").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRejeitarProdutoComCategoriaInexistenteCom404() throws Exception {
        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.ProdutoRequest(
                "SKU-IT-2", "Produto Teste", "desc", 99999L, null, null,
                new BigDecimal("29.90"), 5));

        mockMvc.perform(post("/api/v1/produtos").contentType("application/json").content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveListarProdutosComEstoqueBaixo() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/estoque-baixo"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarProdutoComSucesso() throws Exception {
        Categoria categoriaOriginal = categoriaRepository.save(Categoria.builder().nome("Categoria Original IT").build());
        Categoria categoriaNova = categoriaRepository.save(Categoria.builder().nome("Categoria Nova IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-IT-3")
                .nome("Produto Original")
                .descricao("desc original")
                .categoria(categoriaOriginal)
                .precoVenda(new BigDecimal("19.90"))
                .estoqueMinimo(3)
                .build());

        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.ProdutoRequest(
                "SKU-IT-3", "Produto Atualizado", "desc nova", categoriaNova.getId(), null, null,
                new BigDecimal("39.90"), 8));

        mockMvc.perform(put("/api/v1/produtos/" + produto.getId()).contentType("application/json").content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produto Atualizado"))
                .andExpect(jsonPath("$.categoriaNome").value("Categoria Nova IT"))
                .andExpect(jsonPath("$.precoVenda").value(39.90));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveExcluirProdutoComSucesso() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Exclusao IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-IT-4")
                .nome("Produto para Excluir")
                .descricao("desc")
                .categoria(categoria)
                .precoVenda(new BigDecimal("9.90"))
                .estoqueMinimo(1)
                .build());

        mockMvc.perform(delete("/api/v1/produtos/" + produto.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/produtos/" + produto.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveRetornar404AoBuscarProdutoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deveRetornar400AoBuscarProdutoComIdNaoNumericoEmVezDe500() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void deveRejeitarCriacaoParaOperadorCom403() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Operador IT").build());

        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.ProdutoRequest(
                "SKU-IT-OPERADOR", "Produto Operador", "desc", categoria.getId(), null, null,
                new BigDecimal("29.90"), 5));

        mockMvc.perform(post("/api/v1/produtos").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
    }
}
