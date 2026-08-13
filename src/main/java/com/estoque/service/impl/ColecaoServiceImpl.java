package com.estoque.service.impl;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.entity.Colecao;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.ColecaoMapper;
import com.estoque.repository.ColecaoRepository;
import com.estoque.service.ColecaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ColecaoServiceImpl implements ColecaoService {

    private final ColecaoRepository colecaoRepository;
    private final ColecaoMapper colecaoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ColecaoResponse> listar(Pageable pageable) {
        return colecaoRepository.findAll(pageable).map(colecaoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ColecaoResponse buscarPorId(Long id) {
        return colecaoMapper.toResponse(buscarEntidadePorId(id));
    }

    @Override
    @Transactional
    public ColecaoResponse criar(ColecaoRequest request) {
        if (colecaoRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new RegistroDuplicadoException("Já existe uma coleção com o nome '%s'".formatted(request.nome()));
        }
        Colecao colecao = colecaoMapper.toEntity(request);
        return colecaoMapper.toResponse(colecaoRepository.save(colecao));
    }

    @Override
    @Transactional
    public ColecaoResponse atualizar(Long id, ColecaoRequest request) {
        Colecao colecao = buscarEntidadePorId(id);
        colecaoMapper.atualizarEntidade(request, colecao);
        return colecaoMapper.toResponse(colecaoRepository.save(colecao));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Colecao colecao = buscarEntidadePorId(id);
        colecaoRepository.delete(colecao);
    }

    private Colecao buscarEntidadePorId(Long id) {
        return colecaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleção", id));
    }
}
