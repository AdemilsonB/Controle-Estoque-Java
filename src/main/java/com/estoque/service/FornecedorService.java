package com.estoque.service;

import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FornecedorService {
    Page<FornecedorResponse> listar(Pageable pageable);
    FornecedorResponse buscarPorId(Long id);
    FornecedorResponse criar(FornecedorRequest request);
    FornecedorResponse atualizar(Long id, FornecedorRequest request);
    void excluir(Long id);
}
