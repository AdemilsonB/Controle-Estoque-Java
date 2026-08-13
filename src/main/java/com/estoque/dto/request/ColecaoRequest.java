package com.estoque.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColecaoRequest(
        @NotBlank @Size(max = 80) String nome,
        @Size(max = 255) String descricao) {
}
