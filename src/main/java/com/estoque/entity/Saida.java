package com.estoque.entity;

import com.estoque.enums.MotivoSaida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "saida")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Saida extends MovimentacaoEstoque {

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 20)
    private MotivoSaida motivo;

    @Builder
    public Saida(Produto produto, Funcionario funcionario, int quantidade, String observacao, MotivoSaida motivo) {
        super(produto, funcionario, quantidade, observacao);
        this.motivo = motivo;
    }
}
