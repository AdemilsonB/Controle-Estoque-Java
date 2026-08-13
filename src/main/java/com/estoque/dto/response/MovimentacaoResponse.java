package com.estoque.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id, String tipo, String produtoCodigo, String produtoNome, String funcionarioNome,
        int quantidade, LocalDateTime dataMovimentacao, String observacao,
        BigDecimal custoUnitario, String motivo) {
}
