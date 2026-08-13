package com.estoque.repository;

import com.estoque.entity.Colecao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColecaoRepository extends JpaRepository<Colecao, Long> {
    boolean existsByNomeIgnoreCase(String nome);
}
