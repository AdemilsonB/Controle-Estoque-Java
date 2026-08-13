package com.estoque.service;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SaidaService {
    Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable);
    MovimentacaoResponse registrar(SaidaRequest request, String emailFuncionarioAutenticado);
}
