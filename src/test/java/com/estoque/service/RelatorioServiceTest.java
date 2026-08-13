package com.estoque.service;

import com.estoque.dto.response.RelatorioEstoqueResponse;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.impl.RelatorioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private ProdutoService produtoService;
    @InjectMocks private RelatorioServiceImpl relatorioService;

    @Test
    void deveCalcularValorTotalDoEstoqueEQuantidadeDeProdutosAtivos() {
        when(produtoRepository.somaValorEstoque()).thenReturn(new BigDecimal("1250.75"));
        when(produtoRepository.countByAtivoTrue()).thenReturn(8L);

        RelatorioEstoqueResponse resultado = relatorioService.gerarRelatorioValorEstoque();

        assertThat(resultado.valorTotalEstoque()).isEqualByComparingTo("1250.75");
        assertThat(resultado.quantidadeProdutosAtivos()).isEqualTo(8L);
        assertThat(resultado.geradoEm()).isNotNull();
    }
}
