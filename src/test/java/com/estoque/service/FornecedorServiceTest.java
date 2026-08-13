package com.estoque.service;

import com.estoque.dto.request.EnderecoRequest;
import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.FornecedorResponse;
import com.estoque.entity.Endereco;
import com.estoque.entity.Fornecedor;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FornecedorMapper;
import com.estoque.repository.FornecedorRepository;
import com.estoque.service.impl.FornecedorServiceImpl;
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
class FornecedorServiceTest {

    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private FornecedorMapper fornecedorMapper;
    @InjectMocks private FornecedorServiceImpl fornecedorService;

    private final EnderecoRequest enderecoRequest =
            new EnderecoRequest("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000");

    @Test
    void deveCriarFornecedorQuandoCnpjNaoExiste() {
        FornecedorRequest request = new FornecedorRequest("12345678000199", "Fornecedor LTDA",
                "11999999999", "contato@fornecedor.com", enderecoRequest);
        Fornecedor entidade = Fornecedor.builder()
                .cnpj("12345678000199").razaoSocial("Fornecedor LTDA")
                .telefone("11999999999").email("contato@fornecedor.com")
                .endereco(new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000"))
                .build();

        when(fornecedorRepository.existsByCnpj("12345678000199")).thenReturn(false);
        when(fornecedorMapper.toEntity(request)).thenReturn(entidade);
        when(fornecedorRepository.save(entidade)).thenReturn(entidade);
        when(fornecedorMapper.toResponse(entidade)).thenReturn(
                new FornecedorResponse(1L, "12345678000199", "Fornecedor LTDA", "11999999999",
                        "contato@fornecedor.com", null, true));

        FornecedorResponse resultado = fornecedorService.criar(request);

        assertThat(resultado.razaoSocial()).isEqualTo("Fornecedor LTDA");
        verify(fornecedorRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarFornecedorComCnpjDuplicado() {
        FornecedorRequest request = new FornecedorRequest("12345678000199", "Fornecedor LTDA",
                "11999999999", "contato@fornecedor.com", enderecoRequest);
        when(fornecedorRepository.existsByCnpj("12345678000199")).thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void deveLancarExcecaoAoBuscarFornecedorInativoOuInexistente() {
        when(fornecedorRepository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fornecedorService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveDesativarFornecedorAoExcluir() {
        Fornecedor entidade = Fornecedor.builder()
                .cnpj("12345678000199").razaoSocial("Fornecedor LTDA")
                .telefone("11999999999").email("contato@fornecedor.com")
                .endereco(new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000"))
                .build();
        when(fornecedorRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(entidade));

        fornecedorService.excluir(1L);

        assertThat(entidade.isAtivo()).isFalse();
        verify(fornecedorRepository).save(entidade);
    }
}
