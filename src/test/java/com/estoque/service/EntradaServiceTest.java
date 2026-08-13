package com.estoque.service;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.enums.Role;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.mapper.MovimentacaoEstoqueMapper;
import com.estoque.repository.EntradaRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.impl.EntradaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntradaServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private EntradaRepository entradaRepository;
    @Spy private MovimentacaoEstoqueMapper movimentacaoMapper = new MovimentacaoEstoqueMapper();
    @InjectMocks private EntradaServiceImpl entradaService;

    private Produto novoProduto(int estoqueInicial) {
        Produto produto = Produto.builder().codigo("SKU-1").nome("Camiseta")
                .categoria(Categoria.builder().nome("Roupas").build())
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(5).build();
        if (estoqueInicial > 0) {
            produto.registrarEntrada(estoqueInicial, new BigDecimal("10.00"));
        }
        return produto;
    }

    private Funcionario novoFuncionario() {
        return Funcionario.builder().nome("Ana").sobrenome("Silva").cpf("11122233344")
                .email("ana@estoque.com").senha("hash").matricula("F001").dataAdmissao(LocalDate.now())
                .setor("Loja").role(Role.OPERADOR)
                .endereco(new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"))
                .build();
    }

    @Test
    void deveRegistrarEntradaEAumentarEstoqueDoProduto() {
        Produto produto = novoProduto(0);
        Funcionario funcionario = novoFuncionario();
        EntradaRequest request = new EntradaRequest(1L, null, 10, new BigDecimal("20.00"), "compra inicial");

        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));
        when(funcionarioRepository.findByEmail("ana@estoque.com")).thenReturn(Optional.of(funcionario));
        when(produtoRepository.save(produto)).thenReturn(produto);
        when(entradaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        entradaService.registrar(request, "ana@estoque.com");

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(10);
        assertThat(produto.getCustoMedio()).isEqualByComparingTo("20.00");
        verify(produtoRepository).save(produto);
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExisteOuInativo() {
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());
        EntradaRequest request = new EntradaRequest(1L, null, 10, new BigDecimal("20.00"), null);

        assertThatThrownBy(() -> entradaService.registrar(request, "ana@estoque.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
