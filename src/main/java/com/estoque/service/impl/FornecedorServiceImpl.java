package com.estoque.service.impl;

import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import com.estoque.entity.Fornecedor;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FornecedorMapper;
import com.estoque.repository.FornecedorRepository;
import com.estoque.service.FornecedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FornecedorServiceImpl implements FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final FornecedorMapper fornecedorMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(Pageable pageable) {
        return fornecedorRepository.findByAtivoTrue(pageable).map(fornecedorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorId(Long id) {
        return fornecedorMapper.toResponse(buscarEntidadeAtivaPorId(id));
    }

    @Override
    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        if (fornecedorRepository.existsByCnpj(request.cnpj())) {
            throw new RegistroDuplicadoException("Já existe um fornecedor com o CNPJ informado");
        }
        Fornecedor fornecedor = fornecedorMapper.toEntity(request);
        return fornecedorMapper.toResponse(fornecedorRepository.save(fornecedor));
    }

    @Override
    @Transactional
    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = buscarEntidadeAtivaPorId(id);
        fornecedorMapper.atualizarEntidade(request, fornecedor);
        return fornecedorMapper.toResponse(fornecedorRepository.save(fornecedor));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Fornecedor fornecedor = buscarEntidadeAtivaPorId(id);
        fornecedor.desativar();
        fornecedorRepository.save(fornecedor);
    }

    private Fornecedor buscarEntidadeAtivaPorId(Long id) {
        return fornecedorRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor", id));
    }
}
