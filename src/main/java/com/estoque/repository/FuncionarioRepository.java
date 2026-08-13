package com.estoque.repository;

import com.estoque.entity.Funcionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    Optional<Funcionario> findByEmail(String email);
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    Page<Funcionario> findByAtivoTrue(Pageable pageable);
}
