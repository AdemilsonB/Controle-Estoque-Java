package com.estoque.service;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.dto.response.RelatorioEstoqueResponse;

import java.util.List;

public interface RelatorioService {
    RelatorioEstoqueResponse gerarRelatorioValorEstoque();
    List<ProdutoResponse> listarProdutosComEstoqueBaixo();
}
