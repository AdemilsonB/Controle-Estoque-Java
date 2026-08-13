package com.estoque.exception;

public class RegistroDuplicadoException extends BusinessException {
    public RegistroDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
