package com.estoque.repository;

import com.estoque.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    Page<MovimentacaoEstoque> findByProdutoIdOrderByDataMovimentacaoDesc(Long produtoId, Pageable pageable);
}
