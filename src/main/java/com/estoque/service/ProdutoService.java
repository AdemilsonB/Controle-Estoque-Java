package com.estoque.service;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProdutoService {
    Page<ProdutoResponse> listar(Pageable pageable);
    ProdutoResponse buscarPorId(Long id);
    List<ProdutoResponse> listarComEstoqueBaixo();
    ProdutoResponse criar(ProdutoRequest request);
    ProdutoResponse atualizar(Long id, ProdutoRequest request);
    void excluir(Long id);
}
