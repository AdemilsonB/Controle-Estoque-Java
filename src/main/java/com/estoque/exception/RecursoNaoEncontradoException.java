package com.estoque.exception;

public class RecursoNaoEncontradoException extends BusinessException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super("%s não encontrado(a) com id %s".formatted(recurso, id));
    }
}
