package com.estoque.exception;

public class EstoqueInsuficienteException extends BusinessException {
    public EstoqueInsuficienteException(String codigoProduto, int disponivel, int solicitado) {
        super("Estoque insuficiente para o produto %s: disponível %d, solicitado %d"
                .formatted(codigoProduto, disponivel, solicitado));
    }
}
