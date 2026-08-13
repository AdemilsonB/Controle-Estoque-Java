package com.estoque.repository;

import com.estoque.entity.Entrada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    Page<Entrada> findByProdutoId(Long produtoId, Pageable pageable);
}
