package com.estoque.service.impl;

import com.estoque.dto.request.ProdutoRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.dto.response.ProdutoResponse;
import com.estoque.entity.Categoria;
import com.estoque.entity.Colecao;
import com.estoque.entity.Fornecedor;
import com.estoque.entity.Produto;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.MovimentacaoEstoqueMapper;
import com.estoque.mapper.ProdutoMapper;
import com.estoque.repository.CategoriaRepository;
import com.estoque.repository.ColecaoRepository;
import com.estoque.repository.FornecedorRepository;
import com.estoque.repository.MovimentacaoEstoqueRepository;
import com.estoque.repository.ProdutoRepository;
import com.estoque.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoServiceImpl implements ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ColecaoRepository colecaoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ProdutoMapper produtoMapper;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final MovimentacaoEstoqueMapper movimentacaoEstoqueMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(Pageable pageable) {
        return produtoRepository.findByAtivoTrue(pageable).map(produtoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return produtoMapper.toResponse(buscarEntidadeAtivaPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarComEstoqueBaixo() {
        return produtoRepository.buscarComEstoqueAbaixoDoMinimo().stream()
                .map(produtoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        if (produtoRepository.existsByCodigo(request.codigo())) {
            throw new RegistroDuplicadoException("Já existe um produto com o código '%s'".formatted(request.codigo()));
        }
        Categoria categoria = buscarCategoria(request.categoriaId());
        Colecao colecao = request.colecaoId() != null ? buscarColecao(request.colecaoId()) : null;
        Fornecedor fornecedor = request.fornecedorId() != null ? buscarFornecedor(request.fornecedorId()) : null;

        Produto produto = Produto.builder()
                .codigo(request.codigo())
                .nome(request.nome())
                .descricao(request.descricao())
                .categoria(categoria)
                .colecao(colecao)
                .fornecedor(fornecedor)
                .precoVenda(request.precoVenda())
                .estoqueMinimo(request.estoqueMinimo())
                .build();

        return produtoMapper.toResponse(produtoRepository.save(produto));
    }

    @Override
    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidadeAtivaPorId(id);
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setCategoria(buscarCategoria(request.categoriaId()));
        produto.setColecao(request.colecaoId() != null ? buscarColecao(request.colecaoId()) : null);
        produto.setFornecedor(request.fornecedorId() != null ? buscarFornecedor(request.fornecedorId()) : null);
        produto.setPrecoVenda(request.precoVenda());
        produto.setEstoqueMinimo(request.estoqueMinimo());

        return produtoMapper.toResponse(produtoRepository.save(produto));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Produto produto = buscarEntidadeAtivaPorId(id);
        produto.desativar();
        produtoRepository.save(produto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listarMovimentacoes(Long produtoId, Pageable pageable) {
        buscarEntidadeAtivaPorId(produtoId);
        return movimentacaoEstoqueRepository
                .findByProdutoIdOrderByDataMovimentacaoDescIdDesc(produtoId, pageable)
                .map(movimentacaoEstoqueMapper::toResponse);
    }

    private Produto buscarEntidadeAtivaPorId(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", id));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }

    private Colecao buscarColecao(Long id) {
        return colecaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleção", id));
    }

    private Fornecedor buscarFornecedor(Long id) {
        return fornecedorRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", id));
    }
}
