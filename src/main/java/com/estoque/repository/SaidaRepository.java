package com.estoque.repository;

import com.estoque.entity.Saida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaidaRepository extends JpaRepository<Saida, Long> {
    @EntityGraph(attributePaths = {"produto", "funcionario"})
    Page<Saida> findByProdutoId(Long produtoId, Pageable pageable);
}
