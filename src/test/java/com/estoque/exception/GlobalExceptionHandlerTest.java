package com.estoque.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveMapearRecursoNaoEncontradoPara404() {
        ProblemDetail problem = handler.handleRecursoNaoEncontrado(
                new RecursoNaoEncontradoException("Produto", 99L));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).contains("Produto").contains("99");
    }

    @Test
    void deveMapearEstoqueInsuficientePara409() {
        ProblemDetail problem = handler.handleEstoqueInsuficiente(
                new EstoqueInsuficienteException("SKU-1", 2, 5));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearRegistroDuplicadoPara409() {
        ProblemDetail problem = handler.handleRegistroDuplicado(
                new RegistroDuplicadoException("CNPJ já cadastrado"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearErroDeValidacaoPara400ComListaDeCampos() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "produtoRequest");
        bindingResult.addError(new FieldError("produtoRequest", "nome", "não pode ser vazio"));
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ProblemDetail problem = handler.handleValidacao(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsKey("errors");
    }

    @Test
    void deveMapearViolacaoDeIntegridadePara409() {
        ProblemDetail problem = handler.handleIntegridadeDados(
                new DataIntegrityViolationException("constraint violada"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearLockOtimistaPara409() {
        ProblemDetail problem = handler.handleLockOtimista(
                new ObjectOptimisticLockingFailureException(Object.class, 1L));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void deveMapearCredenciaisInvalidasPara401() {
        ProblemDetail problem = handler.handleCredenciaisInvalidas(new BadCredentialsException("inválido"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void deveMapearAcessoNegadoPara403() {
        ProblemDetail problem = handler.handleAcessoNegado(new AccessDeniedException("negado"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void deveMapearExcecaoGenericaPara500SemVazarDetalhe() {
        ProblemDetail problem = handler.handleGenerica(new RuntimeException("detalhe interno sensível"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).doesNotContain("detalhe interno sensível");
    }
}
