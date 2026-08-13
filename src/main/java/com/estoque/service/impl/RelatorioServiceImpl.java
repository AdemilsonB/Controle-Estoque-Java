package com.estoque.service.impl;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.dto.response.RelatorioEstoqueResponse;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.ProdutoService;
import com.estoque.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioServiceImpl implements RelatorioService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoService produtoService;

    @Override
    @Transactional(readOnly = true)
    public RelatorioEstoqueResponse gerarRelatorioValorEstoque() {
        return new RelatorioEstoqueResponse(
                produtoRepository.somaValorEstoque(),
                produtoRepository.countByAtivoTrue(),
                LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProdutosComEstoqueBaixo() {
        return produtoService.listarComEstoqueBaixo();
    }
}
