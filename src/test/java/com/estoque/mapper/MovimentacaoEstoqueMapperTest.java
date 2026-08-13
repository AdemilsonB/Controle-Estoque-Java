package com.estoque.mapper;

import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Entrada;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.entity.Saida;
import com.estoque.enums.MotivoSaida;
import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MovimentacaoEstoqueMapperTest {

    private final MovimentacaoEstoqueMapper mapper = new MovimentacaoEstoqueMapper();

    private Produto produto() {
        return Produto.builder().codigo("SKU-1").nome("Camiseta")
                .categoria(Categoria.builder().nome("Roupas").build())
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(5).build();
    }

    private Funcionario funcionario() {
        return Funcionario.builder().nome("Ana").sobrenome("Silva").cpf("11122233344")
                .email("ana@estoque.com").senha("hash").matricula("F001").dataAdmissao(LocalDate.now())
                .setor("Loja").role(Role.OPERADOR)
                .endereco(new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"))
                .build();
    }

    @Test
    void deveMapearEntradaComTipoECustoUnitario() {
        Entrada entrada = Entrada.builder().produto(produto()).funcionario(funcionario())
                .quantidade(5).custoUnitario(new BigDecimal("12.50")).build();

        var resposta = mapper.toResponse(entrada);

        assertThat(resposta.tipo()).isEqualTo("ENTRADA");
        assertThat(resposta.custoUnitario()).isEqualByComparingTo("12.50");
        assertThat(resposta.motivo()).isNull();
    }

    @Test
    void deveMapearSaidaComTipoEMotivo() {
        Saida saida = Saida.builder().produto(produto()).funcionario(funcionario())
                .quantidade(2).motivo(MotivoSaida.PERDA).build();

        var resposta = mapper.toResponse(saida);

        assertThat(resposta.tipo()).isEqualTo("SAIDA");
        assertThat(resposta.motivo()).isEqualTo("PERDA");
        assertThat(resposta.custoUnitario()).isNull();
    }
}
