package com.estoque.service;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.entity.Colecao;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.ColecaoMapper;
import com.estoque.repository.ColecaoRepository;
import com.estoque.service.impl.ColecaoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColecaoServiceTest {

    @Mock private ColecaoRepository colecaoRepository;
    @Mock private ColecaoMapper colecaoMapper;
    @InjectMocks private ColecaoServiceImpl colecaoService;

    @Test
    void deveCriarColecaoQuandoNomeNaoExiste() {
        ColecaoRequest request = new ColecaoRequest("Verão 2026", "desc");
        Colecao entidade = Colecao.builder().nome("Verão 2026").descricao("desc").build();
        ColecaoResponse resposta = new ColecaoResponse(1L, "Verão 2026", "desc");

        when(colecaoRepository.existsByNomeIgnoreCase("Verão 2026")).thenReturn(false);
        when(colecaoMapper.toEntity(request)).thenReturn(entidade);
        when(colecaoRepository.save(entidade)).thenReturn(entidade);
        when(colecaoMapper.toResponse(entidade)).thenReturn(resposta);

        ColecaoResponse resultado = colecaoService.criar(request);

        assertThat(resultado.nome()).isEqualTo("Verão 2026");
        verify(colecaoRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarColecaoComNomeDuplicado() {
        ColecaoRequest request = new ColecaoRequest("Verão 2026", "desc");
        when(colecaoRepository.existsByNomeIgnoreCase("Verão 2026")).thenReturn(true);

        assertThatThrownBy(() -> colecaoService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoAoBuscarColecaoInexistente() {
        when(colecaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> colecaoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
