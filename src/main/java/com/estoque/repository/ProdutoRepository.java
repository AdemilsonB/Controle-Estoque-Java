package com.estoque.repository;

import com.estoque.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    boolean existsByCodigo(String codigo);
    Optional<Produto> findByIdAndAtivoTrue(Long id);

    @EntityGraph(attributePaths = {"categoria", "colecao", "fornecedor"})
    Page<Produto> findByAtivoTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "colecao", "fornecedor"})
    @Query("SELECT p FROM Produto p WHERE p.ativo = true AND p.quantidadeEstoque < p.estoqueMinimo ORDER BY p.codigo")
    List<Produto> buscarComEstoqueAbaixoDoMinimo();

    @Query("SELECT COALESCE(SUM(p.quantidadeEstoque * p.custoMedio), 0) FROM Produto p WHERE p.ativo = true")
    BigDecimal somaValorEstoque();

    long countByAtivoTrue();
}
