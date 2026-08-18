package com.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categoria", uniqueConstraints = @UniqueConstraint(columnNames = "nome"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Builder
    public Categoria(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
}
