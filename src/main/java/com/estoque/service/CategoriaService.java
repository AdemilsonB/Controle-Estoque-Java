package com.estoque.service;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoriaService {
    Page<CategoriaResponse> listar(Pageable pageable);
    CategoriaResponse buscarPorId(Long id);
    CategoriaResponse criar(CategoriaRequest request);
    CategoriaResponse atualizar(Long id, CategoriaRequest request);
    void excluir(Long id);
}
