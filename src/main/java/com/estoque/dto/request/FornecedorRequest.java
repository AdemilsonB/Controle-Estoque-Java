package com.estoque.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FornecedorRequest(
        @NotBlank @Pattern(regexp = "\\d{14}", message = "deve conter 14 dígitos numéricos") String cnpj,
        @NotBlank String razaoSocial,
        @NotBlank String telefone,
        @NotBlank @Email String email,
        @NotNull @Valid EnderecoRequest endereco) {
}
