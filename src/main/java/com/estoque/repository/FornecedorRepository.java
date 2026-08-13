package com.estoque.repository;

import com.estoque.entity.Fornecedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    boolean existsByCnpj(String cnpj);
    Optional<Fornecedor> findByIdAndAtivoTrue(Long id);
    Page<Fornecedor> findByAtivoTrue(Pageable pageable);
}
