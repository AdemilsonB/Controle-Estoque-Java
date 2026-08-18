package com.estoque.service;

public interface AuditoriaService {
    void registrarTentativaSaidaNegada(String produtoCodigo, String funcionarioEmail,
                                        int quantidadeSolicitada, int quantidadeDisponivel);
}
