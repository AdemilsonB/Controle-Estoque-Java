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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SaidaControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProdutoRepository produtoRepository;

    @Test
    @WithUserDetails(value = "admin@estoque.com", userDetailsServiceBeanName = "userDetailsServiceImpl")
    void deveRejeitarSaidaComEstoqueInsuficienteCom409() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Saida IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-SAIDA-IT").nome("Produto Saida IT").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());

        String corpo = objectMapper.writeValueAsString(new com.estoque.dto.request.SaidaRequest(
                produto.getId(), 5, com.estoque.enums.MotivoSaida.VENDA, "venda sem estoque"));

        mockMvc.perform(post("/api/v1/saidas").contentType("application/json").content(corpo))
                .andExpect(status().isConflict());
    }
}
