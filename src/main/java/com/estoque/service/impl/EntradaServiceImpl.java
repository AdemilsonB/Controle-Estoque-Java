package com.estoque.service.impl;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Entrada;
import com.estoque.entity.Fornecedor;
import com.estoque.entity.Funcionario;
import com.estoque.entity.Produto;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.mapper.MovimentacaoEstoqueMapper;
import com.estoque.repository.EntradaRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.EntradaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntradaServiceImpl implements EntradaService {

    private final ProdutoRepository produtoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FornecedorRepository fornecedorRepository;
    private final EntradaRepository entradaRepository;
    private final MovimentacaoEstoqueMapper movimentacaoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listar(Long produtoId, Pageable pageable) {
        Page<Entrada> pagina = produtoId != null
                ? entradaRepository.findByProdutoId(produtoId, pageable)
                : entradaRepository.findAll(pageable);
        return pagina.map(movimentacaoMapper::toResponse);
    }

    @Override
    @Transactional
    public MovimentacaoResponse registrar(EntradaRequest request, String emailFuncionarioAutenticado) {
        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", request.produtoId()));
        Funcionario funcionario = funcionarioRepository.findByEmail(emailFuncionarioAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", emailFuncionarioAutenticado));
        Fornecedor fornecedor = request.fornecedorId() != null
                ? fornecedorRepository.findByIdAndAtivoTrue(request.fornecedorId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", request.fornecedorId()))
                : null;

        produto.registrarEntrada(request.quantidade(), request.custoUnitario());
        produtoRepository.save(produto);

        Entrada entrada = Entrada.builder()
                .produto(produto)
                .funcionario(funcionario)
                .quantidade(request.quantidade())
                .custoUnitario(request.custoUnitario())
                .fornecedor(fornecedor)
                .observacao(request.observacao())
                .build();

        return movimentacaoMapper.toResponse(entradaRepository.save(entrada));
    }
}
