package com.estoque.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 500) String descricao,
        @NotNull Long categoriaId,
        Long colecaoId,
        Long fornecedorId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal precoVenda,
        @PositiveOrZero int estoqueMinimo) {
}
