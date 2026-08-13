package com.estoque.service;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ColecaoService {
    Page<ColecaoResponse> listar(Pageable pageable);
    ColecaoResponse buscarPorId(Long id);
    ColecaoResponse criar(ColecaoRequest request);
    ColecaoResponse atualizar(Long id, ColecaoRequest request);
    void excluir(Long id);
}
