package com.estoque.dto.response;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id, String codigo, String nome, String descricao, String categoriaNome, String colecaoNome,
        String fornecedorRazaoSocial, BigDecimal precoVenda, BigDecimal custoMedio, int quantidadeEstoque,
        int estoqueMinimo, boolean ativo) {
}
