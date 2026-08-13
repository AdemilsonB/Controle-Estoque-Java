package com.estoque.service;

import com.estoque.dto.request.EnderecoRequest;
import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import com.estoque.enums.Role;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FuncionarioMapper;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.service.impl.FuncionarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private FuncionarioMapper funcionarioMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private FuncionarioServiceImpl funcionarioService;

    private final EnderecoRequest enderecoRequest =
            new EnderecoRequest("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000");

    @Test
    void deveCriarFuncionarioComSenhaCriptografada() {
        FuncionarioRequest request = new FuncionarioRequest("Ana", "Silva", "11122233344",
                "ana@estoque.com", "senha123", "F001", LocalDate.now(), "Almoxarifado", Role.OPERADOR, enderecoRequest);
        Funcionario entidade = Funcionario.builder()
                .nome("Ana").sobrenome("Silva").cpf("11122233344").email("ana@estoque.com")
                .matricula("F001").dataAdmissao(LocalDate.now()).setor("Almoxarifado").role(Role.OPERADOR)
                .senha(null)
                .endereco(new Endereco("Rua B", "10", "Centro", "Curitiba", "PR", "80000-000"))
                .build();

        when(funcionarioRepository.existsByCpf("11122233344")).thenReturn(false);
        when(funcionarioRepository.existsByEmail("ana@estoque.com")).thenReturn(false);
        when(funcionarioMapper.toEntity(request)).thenReturn(entidade);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-seguro");
        when(funcionarioRepository.save(entidade)).thenReturn(entidade);
        when(funcionarioMapper.toResponse(entidade)).thenReturn(new FuncionarioResponse(
                1L, "Ana", "Silva", "11122233344", "ana@estoque.com", "F001", LocalDate.now(),
                "Almoxarifado", Role.OPERADOR, null, true));

        FuncionarioResponse resultado = funcionarioService.criar(request);

        assertThat(resultado.nome()).isEqualTo("Ana");
        assertThat(entidade.getSenha()).isEqualTo("hash-seguro");
        verify(funcionarioRepository).save(entidade);
    }

    @Test
    void deveLancarExcecaoAoCriarFuncionarioComCpfDuplicado() {
        FuncionarioRequest request = new FuncionarioRequest("Ana", "Silva", "11122233344",
                "ana@estoque.com", "senha123", "F001", LocalDate.now(), "Almoxarifado", Role.OPERADOR, enderecoRequest);
        when(funcionarioRepository.existsByCpf("11122233344")).thenReturn(true);

        assertThatThrownBy(() -> funcionarioService.criar(request))
                .isInstanceOf(RegistroDuplicadoException.class);
    }
}
