package com.estoque.service;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.ProdutoMapper;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ColecaoRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.impl.ProdutoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private ColecaoRepository colecaoRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private ProdutoMapper produtoMapper;
    @InjectMocks private ProdutoServiceImpl produtoService;

    @Test
    void deveCriarProdutoQuandoCodigoNaoExisteECategoriaValida() {
        ProdutoRequest request = new ProdutoRequest("SKU-1", "Camiseta", "desc", 1L, null, null,
                new BigDecimal("49.90"), 10);
        Categoria categoria = Categoria.builder().nome("Camisetas").build();
        Produto produtoSalvo = Produto.builder().codigo("SKU-1").nome("Camiseta").categoria(categoria)
                .precoVenda(new BigDecimal("49.90")).estoqueMinimo(10).build();

        when(produtoRepository.existsByCodigo("SKU-1")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(produtoRepository.save(org.mockito.ArgumentMatchers.any(Produto.class))).thenReturn(produtoSalvo);
        when(produtoMapper.toResponse(produtoSalvo)).thenReturn(new ProdutoResponse(
                1L, "SKU-1", "Camiseta", "desc", "Camisetas", null, null,
                new BigDecimal("49.90"), BigDecimal.ZERO, 0, 10, true));

        ProdutoResponse resultado = produtoService.criar(request);

        assertThat(resultado.codigo()).isEqualTo("SKU-1");
        verify(produtoRepository).save(org.mockito.ArgumentMatchers.any(Produto.class));
    }

    @Test
    void deveLancarExcecaoAoCriarProdutoComCodigoDuplicado() {
        ProdutoRequest request = new ProdutoRequest("SKU-1", "Camiseta", "desc", 1L, null, null,
                new BigDecimal("49.90"), 10);
        when(produtoRepository.existsByCodigo("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> produtoService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoQuandoCategoriaInformadaNaoExiste() {
        ProdutoRequest request = new ProdutoRequest("SKU-1", "Camiseta", "desc", 99L, null, null,
                new BigDecimal("49.90"), 10);
        when(produtoRepository.existsByCodigo("SKU-1")).thenReturn(false);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.criar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveListarProdutosComEstoqueBaixo() {
        Produto produto = Produto.builder().codigo("SKU-2").nome("Bolsa")
                .categoria(Categoria.builder().nome("Bolsas").build())
                .precoVenda(BigDecimal.TEN).estoqueMinimo(5).build();
        when(produtoRepository.buscarComEstoqueAbaixoDoMinimo()).thenReturn(List.of(produto));
        when(produtoMapper.toResponse(produto)).thenReturn(new ProdutoResponse(
                2L, "SKU-2", "Bolsa", null, "Bolsas", null, null, BigDecimal.TEN, BigDecimal.ZERO, 0, 5, true));

        List<ProdutoResponse> resultado = produtoService.listarComEstoqueBaixo();

        assertThat(resultado).hasSize(1);
    }
}
