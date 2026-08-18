package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de uma saída negada por estoque insuficiente. Gravado em transação própria
 * ({@code Propagation.REQUIRES_NEW}) para sobreviver ao rollback da operação que a originou — ver
 * {@link com.estoque.service.AuditoriaService}.
 */
@Entity
@Table(name = "tentativa_saida_negada")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TentativaSaidaNegada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_codigo", nullable = false, length = 40)
    private String produtoCodigo;

    @Column(name = "funcionario_email", nullable = false, length = 150)
    private String funcionarioEmail;

    @Column(name = "quantidade_solicitada", nullable = false)
    private int quantidadeSolicitada;

    @Column(name = "quantidade_disponivel", nullable = false)
    private int quantidadeDisponivel;

    @Column(name = "registrada_em", nullable = false)
    private LocalDateTime registradaEm;

    @Builder
    public TentativaSaidaNegada(String produtoCodigo, String funcionarioEmail,
                                 int quantidadeSolicitada, int quantidadeDisponivel) {
        this.produtoCodigo = produtoCodigo;
        this.funcionarioEmail = funcionarioEmail;
        this.quantidadeSolicitada = quantidadeSolicitada;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.registradaEm = LocalDateTime.now();
    }
}
