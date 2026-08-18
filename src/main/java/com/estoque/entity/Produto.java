package com.estoque.entity;

import com.estoque.exception.EstoqueInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "produto", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(name = "codigo", nullable = false, length = 40)
    private String codigo;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "colecao_id")
    private Colecao colecao;

    @ManyToOne(fetch = FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Column(name = "preco_venda", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "custo_medio", nullable = false, precision = 12, scale = 2)
    @Setter(AccessLevel.NONE)
    private BigDecimal custoMedio;

    @Column(name = "quantidade_estoque", nullable = false)
    @Setter(AccessLevel.NONE)
    private int quantidadeEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    @Column(name = "ativo", nullable = false)
    @Setter(AccessLevel.NONE)
    private boolean ativo;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime atualizadoEm;

    @Builder
    public Produto(String codigo, String nome, String descricao, Categoria categoria, Colecao colecao,
                    Fornecedor fornecedor, BigDecimal precoVenda, int estoqueMinimo) {
        this.codigo = codigo;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.colecao = colecao;
        this.fornecedor = fornecedor;
        this.precoVenda = precoVenda;
        this.estoqueMinimo = estoqueMinimo;
        this.custoMedio = BigDecimal.ZERO;
        this.quantidadeEstoque = 0;
        this.ativo = true;
    }

    /** Soma a quantidade recebida e recalcula o custo médio ponderado. */
    public void registrarEntrada(int quantidade, BigDecimal custoUnitario) {
        BigDecimal valorAtual = custoMedio.multiply(BigDecimal.valueOf(quantidadeEstoque));
        BigDecimal valorEntrada = custoUnitario.multiply(BigDecimal.valueOf(quantidade));
        int novaQuantidade = quantidadeEstoque + quantidade;

        this.custoMedio = valorAtual.add(valorEntrada)
                .divide(BigDecimal.valueOf(novaQuantidade), 2, java.math.RoundingMode.HALF_UP);
        this.quantidadeEstoque = novaQuantidade;
    }

    /** Debita a quantidade do estoque; lança se não houver saldo suficiente. */
    public void registrarSaida(int quantidade) {
        if (quantidade > quantidadeEstoque) {
            throw new EstoqueInsuficienteException(codigo, quantidadeEstoque, quantidade);
        }
        this.quantidadeEstoque -= quantidade;
    }

    public boolean estoqueAbaixoDoMinimo() {
        return quantidadeEstoque < estoqueMinimo;
    }

    public void desativar() {
        this.ativo = false;
    }
}
