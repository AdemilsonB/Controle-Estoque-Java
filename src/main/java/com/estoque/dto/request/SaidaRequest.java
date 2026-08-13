package com.estoque.dto.request;

import com.estoque.enums.MotivoSaida;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaidaRequest(
        @NotNull Long produtoId,
        @Positive int quantidade,
        @NotNull MotivoSaida motivo,
        @Size(max = 255) String observacao) {
}
