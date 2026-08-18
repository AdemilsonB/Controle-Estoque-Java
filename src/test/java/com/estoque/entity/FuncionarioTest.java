package com.estoque.entity;

import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FuncionarioTest {

    @Test
    void doisFuncionariosComMesmoCpfSaoIguaisEDeduplicamEmSet() {
        Funcionario a = Funcionario.builder()
                .nome("Ana").sobrenome("Silva").cpf("11122233344").email("ana@estoque.com")
                .senha("hash").matricula("F001").dataAdmissao(LocalDate.now())
                .setor("Almoxarifado").role(Role.OPERADOR).build();
        Funcionario b = Funcionario.builder()
                .nome("Ana Editada").sobrenome("Silva Editada").cpf("11122233344").email("ana2@estoque.com")
                .senha("outro-hash").matricula("F002").dataAdmissao(LocalDate.now())
                .setor("Vendas").role(Role.ADMIN).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());

        Set<Funcionario> funcionarios = new HashSet<>();
        funcionarios.add(a);
        funcionarios.add(b);
        assertThat(funcionarios).hasSize(1);
    }

    @Test
    void funcionariosComCpfsDiferentesNaoSaoIguais() {
        Funcionario a = Funcionario.builder().nome("A").sobrenome("A").cpf("11111111111")
                .email("a@a.com").senha("h").matricula("F1").dataAdmissao(LocalDate.now())
                .setor("S").role(Role.OPERADOR).build();
        Funcionario b = Funcionario.builder().nome("A").sobrenome("A").cpf("22222222222")
                .email("a@a.com").senha("h").matricula("F1").dataAdmissao(LocalDate.now())
                .setor("S").role(Role.OPERADOR).build();

        assertThat(a).isNotEqualTo(b);
    }
}
