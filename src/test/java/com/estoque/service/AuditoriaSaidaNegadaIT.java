package com.estoque.service;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.entity.TentativaSaidaNegada;
import com.estoque.enums.MotivoSaida;
import com.estoque.exception.EstoqueInsuficienteException;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.repository.SaidaRepository;
import com.estoque.repository.TentativaSaidaNegadaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.SecurityConfig;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prova de que a auditoria de tentativa negada usa {@code Propagation.REQUIRES_NEW}: o registro de
 * auditoria sobrevive ao rollback da transação principal (que falhou por estoque insuficiente).
 */
@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
class AuditoriaSaidaNegadaIT {

    @Autowired private SaidaService saidaService;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private SaidaRepository saidaRepository;
    @Autowired private TentativaSaidaNegadaRepository tentativaSaidaNegadaRepository;

    @Test
    void registraAuditoriaMesmoComRollbackDaTransacaoPrincipal() {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Auditoria IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-AUDITORIA-IT").nome("Produto Auditoria").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());
        produto.registrarEntrada(5, new BigDecimal("5.00"));
        produtoRepository.saveAndFlush(produto);

        assertThatThrownBy(() -> saidaService.registrar(
                new SaidaRequest(produto.getId(), 999, MotivoSaida.VENDA, "tentativa acima do saldo"),
                "admin@estoque.com"))
                .isInstanceOf(EstoqueInsuficienteException.class);

        // Transação principal fez rollback: nem o estoque nem a Saida foram gravados.
        Produto produtoRecarregado = produtoRepository.findById(produto.getId()).orElseThrow();
        assertThat(produtoRecarregado.getQuantidadeEstoque()).isEqualTo(5);
        assertThat(saidaRepository.count()).isZero();

        // A auditoria, gravada em transação própria (REQUIRES_NEW), sobrevive ao rollback acima.
        // Filtra pelo código do produto deste teste: a suíte roda outras IT reais (ex.:
        // SaidaControllerIT) contra o mesmo H2 compartilhado, e elas também podem gravar auditoria.
        List<TentativaSaidaNegada> tentativas = tentativaSaidaNegadaRepository.findAll().stream()
                .filter(t -> t.getProdutoCodigo().equals("SKU-AUDITORIA-IT"))
                .toList();
        assertThat(tentativas).hasSize(1);
        assertThat(tentativas.get(0).getProdutoCodigo()).isEqualTo("SKU-AUDITORIA-IT");
        assertThat(tentativas.get(0).getFuncionarioEmail()).isEqualTo("admin@estoque.com");
        assertThat(tentativas.get(0).getQuantidadeSolicitada()).isEqualTo(999);
        assertThat(tentativas.get(0).getQuantidadeDisponivel()).isEqualTo(5);
    }
}
