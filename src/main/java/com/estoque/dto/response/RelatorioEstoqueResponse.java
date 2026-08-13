package com.estoque.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RelatorioEstoqueResponse(
        BigDecimal valorTotalEstoque, long quantidadeProdutosAtivos, LocalDateTime geradoEm) {
}
