package com.estoque.dto.response;

import com.estoque.enums.Role;

import java.time.LocalDate;

public record FuncionarioResponse(
        Long id, String nome, String sobrenome, String cpf, String email, String matricula,
        LocalDate dataAdmissao, String setor, Role role, EnderecoResponse endereco, boolean ativo) {
}
