package com.estoque.service;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.entity.Categoria;
import com.estoque.entity.Produto;
import com.estoque.enums.MotivoSaida;
import com.estoque.exception.BusinessException;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SaidaConcorrenciaIT {

    @Autowired private SaidaService saidaService;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    @Test
    void naoDevePermitirVenderMaisDoQueOEstoqueDisponivelSobConcorrencia() throws Exception {
        Categoria categoria = categoriaRepository.save(Categoria.builder().nome("Categoria Concorrencia IT").build());
        Produto produto = produtoRepository.save(Produto.builder()
                .codigo("SKU-CONCORRENCIA-IT").nome("Produto Concorrência").categoria(categoria)
                .precoVenda(new BigDecimal("10.00")).estoqueMinimo(1).build());
        produto.registrarEntrada(10, new BigDecimal("5.00"));
        produtoRepository.saveAndFlush(produto);

        int totalThreads = 5;
        int quantidadePorThread = 3; // 5 x 3 = 15 > 10 disponíveis: overselling deve ser impedido
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhasControladas = new AtomicInteger();

        List<Callable<Void>> tarefas = java.util.stream.IntStream.range(0, totalThreads)
                .<Callable<Void>>mapToObj(i -> () -> {
                    largada.await();
                    try {
                        saidaService.registrar(
                                new SaidaRequest(produto.getId(), quantidadePorThread, MotivoSaida.VENDA, "teste concorrência"),
                                "admin@estoque.com");
                        sucessos.incrementAndGet();
                    } catch (BusinessException | ObjectOptimisticLockingFailureException e) {
                        falhasControladas.incrementAndGet();
                    }
                    return null;
                })
                .toList();

        // submit() (não invokeAll) retorna imediatamente após agendar cada tarefa: como o pool tem
        // totalThreads threads, todas começam a executar e bloqueiam em largada.await() antes que o
        // countDown() abaixo as libere de uma vez, maximizando a chance real de colisão. invokeAll
        // não serve aqui porque ele só retorna quando todas as tarefas *terminam*, e cada tarefa
        // termina apenas depois de largada.countDown() — ou seja, causaria deadlock.
        List<Future<Void>> futures = tarefas.stream().map(executor::submit).toList();
        largada.countDown();
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        Produto produtoFinal = produtoRepository.findById(produto.getId()).orElseThrow();

        assertThat(produtoFinal.getQuantidadeEstoque()).isGreaterThanOrEqualTo(0);
        assertThat(sucessos.get() + falhasControladas.get()).isEqualTo(totalThreads);
        assertThat(sucessos.get() * quantidadePorThread).isLessThanOrEqualTo(10);
    }
}
