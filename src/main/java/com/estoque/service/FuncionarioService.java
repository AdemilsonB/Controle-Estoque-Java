package com.estoque.service;

import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FuncionarioService {
    Page<FuncionarioResponse> listar(Pageable pageable);
    FuncionarioResponse buscarPorId(Long id);
    FuncionarioResponse buscarPerfilPorEmail(String email);
    FuncionarioResponse criar(FuncionarioRequest request);
    FuncionarioResponse atualizar(Long id, FuncionarioRequest request);
    void excluir(Long id);
}
