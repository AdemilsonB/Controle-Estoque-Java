package com.estoque.service;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import com.estoque.entity.Categoria;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.CategoriaMapper;
import com.estoque.repository.CategoriaRepository;
import com.estoque.service.impl.CategoriaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private CategoriaMapper categoriaMapper;
    @InjectMocks private CategoriaServiceImpl categoriaService;

    @Test
    void deveCriarCategoriaQuandoNomeNaoExiste() {
        CategoriaRequest request = new CategoriaRequest("Calçados", "desc");
        Categoria entidade = Categoria.builder().nome("Calçados").descricao("desc").build();
        CategoriaResponse resposta = new CategoriaResponse(1L, "Calçados", "desc");

        when(categoriaRepository.existsByNomeIgnoreCase("Calçados")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(entidade);
        when(categoriaRepository.save(entidade)).thenReturn(entidade);
        when(categoriaMapper.toResponse(entidade)).thenReturn(resposta);

        CategoriaResponse resultado = categoriaService.criar(request);

        assertThat(resultado.nome()).isEqualTo("Calçados");
        verify(categoriaRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarCategoriaComNomeDuplicado() {
        CategoriaRequest request = new CategoriaRequest("Calçados", "desc");
        when(categoriaRepository.existsByNomeIgnoreCase("Calçados")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoAoBuscarCategoriaInexistente() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
