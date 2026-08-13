package com.estoque.service;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EntradaService {
    Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable);
    MovimentacaoResponse registrar(EntradaRequest request, String emailFuncionarioAutenticado);
}
