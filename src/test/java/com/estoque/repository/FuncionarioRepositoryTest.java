package com.estoque.repository;

import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FuncionarioRepositoryTest {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Test
    void devePersistirEBuscarFuncionarioPorEmail() {
        Endereco endereco = new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000");
        Funcionario funcionario = Funcionario.builder()
                .nome("Ana")
                .sobrenome("Silva")
                .cpf("11122233344")
                .email("ana.silva@estoque.com")
                .senha("hash-fake")
                .matricula("F001")
                .dataAdmissao(LocalDate.now())
                .setor("Almoxarifado")
                .role(Role.OPERADOR)
                .endereco(endereco)
                .build();

        funcionarioRepository.save(funcionario);

        assertThat(funcionarioRepository.findByEmail("ana.silva@estoque.com")).isPresent();
        assertThat(funcionarioRepository.existsByCpf("11122233344")).isTrue();
    }
}
