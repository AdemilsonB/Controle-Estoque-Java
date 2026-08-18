package com.estoque.service;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.entity.Categoria;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.enums.MotivoSaida;
import com.estoque.enums.Role;
import com.estoque.exception.EstoqueInsuficienteException;
import com.estoque.mapper.MovimentacaoEstoqueMapper;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.repository.SaidaRepository;
import com.estoque.service.impl.SaidaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class SaidaServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private SaidaRepository saidaRepository;
    @Spy private MovimentacaoEstoqueMapper movimentacaoMapper = new MovimentacaoEstoqueMapper();
    @Mock private AuditoriaService auditoriaService;
    @InjectMocks private SaidaServiceImpl saidaService;

    private Produto produtoComEstoque(int quantidade) {
        Produto produto = Produto.builder().codigo("SKU-1").nome("Camiseta")
                .categoria(Categoria.builder().nome("Roupas").build())
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(5).build();
        produto.registrarEntrada(quantidade, new BigDecimal("10.00"));
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
    void deveRegistrarSaidaEDecrementarEstoqueQuandoHaSaldo() {
        Produto produto = produtoComEstoque(10);
        Funcionario funcionario = novoFuncionario();
        SaidaRequest request = new SaidaRequest(1L, 4, MotivoSaida.VENDA, "venda balcão");

        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));
        when(funcionarioRepository.findByEmail("ana@estoque.com")).thenReturn(Optional.of(funcionario));
        when(produtoRepository.save(produto)).thenReturn(produto);
        when(saidaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        saidaService.registrar(request, "ana@estoque.com");

        assertThat(produto.getQuantidadeEstoque()).isEqualTo(6);
        verify(produtoRepository).save(produto);
    }

    @Test
    void deveLancarEstoqueInsuficienteQuandoQuantidadeMaiorQueSaldo() {
        Produto produto = produtoComEstoque(2);
        SaidaRequest request = new SaidaRequest(1L, 5, MotivoSaida.VENDA, null);

        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> saidaService.registrar(request, "ana@estoque.com"))
                .isInstanceOf(EstoqueInsuficienteException.class);

        verify(produtoRepository, org.mockito.Mockito.never()).save(any());
        verify(auditoriaService).registrarTentativaSaidaNegada("SKU-1", "ana@estoque.com", 5, 2);
    }
}
