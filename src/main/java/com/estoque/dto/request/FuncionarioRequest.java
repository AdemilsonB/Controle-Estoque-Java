package com.estoque.dto.request;

import com.estoque.enums.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FuncionarioRequest(
        @NotBlank String nome,
        @NotBlank String sobrenome,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "deve conter 11 dígitos numéricos") String cpf,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "deve ter ao menos 6 caracteres") String senha,
        @NotBlank String matricula,
        @NotNull LocalDate dataAdmissao,
        @NotBlank String setor,
        @NotNull Role role,
        @NotNull @Valid EnderecoRequest endereco) {
}
