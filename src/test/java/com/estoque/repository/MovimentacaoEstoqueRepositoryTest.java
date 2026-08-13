package com.estoque.repository;

import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Entrada;
import com.estoque.entity.Funcionario;
import com.estoque.entity.MovimentacaoEstoque;
import com.estoque.entity.Produto;
import com.estoque.entity.Saida;
import com.estoque.enums.MotivoSaida;
import com.estoque.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MovimentacaoEstoqueRepositoryTest {

    @Autowired private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Autowired private EntradaRepository entradaRepository;
    @Autowired private SaidaRepository saidaRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;

    @Test
    void deveListarMovimentacoesPolimorficasPorProduto() {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Bolsas").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-9").nome("Bolsa").categoria(categoria)
                .precoVenda(new BigDecimal("100.00")).estoqueMinimo(5).build());
        Funcionario funcionario = funcionarioRepository.save(Funcionario.builder()
                .nome("Bia").sobrenome("Souza").cpf("99988877766").email("bia@estoque.com")
                .senha("hash").matricula("F002").dataAdmissao(LocalDate.now())
                .setor("Loja").role(Role.OPERADOR)
                .endereco(new Endereco("Rua C", "20", "Centro", "Curitiba", "PR", "80000-001"))
                .build());

        Entrada entrada = Entrada.builder().produto(produto).funcionario(funcionario)
                .quantidade(10).custoUnitario(new BigDecimal("40.00")).build();
        Saida saida = Saida.builder().produto(produto).funcionario(funcionario)
                .quantidade(2).motivo(MotivoSaida.VENDA).build();

        entradaRepository.save(entrada);
        saidaRepository.save(saida);

        Page<MovimentacaoEstoque> pagina = movimentacaoRepository
                .findByProdutoIdOrderByDataMovimentacaoDescIdDesc(produto.getId(), PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
    }
}
