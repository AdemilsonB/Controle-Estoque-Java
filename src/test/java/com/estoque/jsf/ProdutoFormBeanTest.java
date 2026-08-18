package com.estoque.jsf;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.ProdutoResponse;
import com.estoque.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoFormBeanTest {

    @Mock private ProdutoService produtoService;

    @Test
    void deveChamarProdutoServiceComOsCamposPreenchidos() {
        ProdutoFormBean bean = new ProdutoFormBean(produtoService);
        bean.setCodigo("SKU-JSF-1");
        bean.setNome("Produto via JSF");
        bean.setCategoriaId(1L);
        bean.setPrecoVenda(new BigDecimal("29.90"));
        bean.setEstoqueMinimo(5);

        when(produtoService.criar(new ProdutoRequest("SKU-JSF-1", "Produto via JSF", null, 1L, null, null,
                new BigDecimal("29.90"), 5)))
                .thenReturn(new ProdutoResponse(1L, "SKU-JSF-1", "Produto via JSF", null, "Roupas", null,
                        null, new BigDecimal("29.90"), BigDecimal.ZERO, 0, 5, true));

        String outcome = bean.criar();

        assertThat(outcome).isEqualTo("produtos?faces-redirect=true");
        verify(produtoService).criar(new ProdutoRequest("SKU-JSF-1", "Produto via JSF", null, 1L, null, null,
                new BigDecimal("29.90"), 5));
    }
}
