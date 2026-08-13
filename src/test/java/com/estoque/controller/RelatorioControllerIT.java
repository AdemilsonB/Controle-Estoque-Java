package com.estoque.controller;

import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RelatorioControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

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
                .andExpect(jsonPath("$.valorTotalEstoque").exists())
                .andExpect(jsonPath("$.quantidadeProdutosAtivos").exists());
    }
}
