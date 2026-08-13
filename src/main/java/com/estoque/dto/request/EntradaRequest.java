package com.estoque.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EntradaRequest(
        @NotNull Long produtoId,
        Long fornecedorId,
        @Positive int quantidade,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal custoUnitario,
        @Size(max = 255) String observacao) {
}
