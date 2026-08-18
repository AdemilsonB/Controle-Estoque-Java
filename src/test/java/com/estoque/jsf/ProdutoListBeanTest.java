package com.estoque.jsf;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoListBeanTest {

    @Mock private ProdutoService produtoService;

    @Test
    void deveCarregarPrimeiraPaginaAoIniciar() {
        ProdutoResponse produto = new ProdutoResponse(1L, "SKU-1", "Camiseta", null, "Roupas", null,
                null, new BigDecimal("49.90"), new BigDecimal("20.00"), 10, 5, true);
        Page<ProdutoResponse> pagina = new PageImpl<>(List.of(produto), PageRequest.of(0, 10), 1);
        when(produtoService.listar(any())).thenReturn(pagina);

        ProdutoListBean bean = new ProdutoListBean(produtoService);
        bean.iniciar();

        assertThat(bean.getProdutos()).hasSize(1);
        assertThat(bean.getProdutos().get(0).codigo()).isEqualTo("SKU-1");
        assertThat(bean.isTemProximaPagina()).isFalse();
    }

    @Test
    void proximaPaginaAvancaEChamaServiceComPaginaSeguinte() {
        Page<ProdutoResponse> paginaVazia = new PageImpl<>(List.of(), PageRequest.of(1, 10), 20);
        when(produtoService.listar(any())).thenReturn(paginaVazia);

        ProdutoListBean bean = new ProdutoListBean(produtoService);
        bean.iniciar();
        bean.proximaPagina();

        assertThat(bean.getPaginaAtual()).isEqualTo(1);
    }
}
