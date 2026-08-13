package com.estoque.service.impl;

import com.estoque.dto.request.SaidaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.entity.Saida;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.repository.SaidaRepository;
import com.estoque.service.SaidaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaidaServiceImpl implements SaidaService {

    private final ProdutoRepository produtoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final SaidaRepository saidaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable) {
        Page<Saida> pagina = produtoId != null
                ? saidaRepository.findByProdutoId(produtoId, pageable)
                : saidaRepository.findAll(pageable);
        return pagina.map(this::toResponse);
    }

    /**
     * A validação de saldo (dentro de {@link Produto#registrarSaida}) e a persistência do produto
     * acontecem na mesma transação: se duas requisições concorrentes lerem o mesmo saldo, o
     * {@code @Version} de {@link Produto} garante que a segunda gravação falhe com
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException} (mapeada para 409),
     * em vez de sobrescrever silenciosamente o resultado da primeira (lost update).
     */
    @Override
    @Transactional
    public MovimentacaoResponse registrar(SaidaRequest request, String emailFuncionarioAutenticado) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));

        // Valida e decrementa o saldo antes de resolver o funcionário: se não houver estoque
        // suficiente, a exceção é lançada aqui e nenhuma gravação (produto ou funcionário) ocorre.
        produto.registrarSaida(request.quantidade());

        Funcionario funcionario = funcionarioRepository.findByEmail(emailFuncionarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", emailFuncionarioAutenticado));

        produtoRepository.save(produto);

        Saida saida = Saida.builder()
                .produto(produto)
                .funcionario(funcionario)
                .quantidade(request.quantidade())
                .motivo(request.motivo())
                .observacao(request.observacao())
                .build();

        return toResponse(saidaRepository.save(saida));
    }

    private MovimentacaoResponse toResponse(Saida saida) {
        return new MovimentacaoResponse(
                saida.getId(), "SAIDA", saida.getProduto().getCodigo(), saida.getProduto().getNome(),
                saida.getFuncionario().getNome() + " " + saida.getFuncionario().getSobrenome(),
                saida.getQuantidade(), saida.getDataMovimentacao(), saida.getObservacao(),
                null, saida.getMotivo().name());
    }
}
