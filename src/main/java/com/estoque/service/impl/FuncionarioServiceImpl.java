package com.estoque.service.impl;

import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.FuncionarioResponse;
import com.estoque.entity.Funcionario;
import com.estoque.exception.RecursoNaoEncontradoException;
import com.estoque.exception.RegistroDuplicadoException;
import com.estoque.mapper.FuncionarioMapper;
import com.estoque.repository.FuncionarioRepository;
import com.estoque.service.FuncionarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioMapper funcionarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<FuncionarioResponse> listar(Pageable pageable) {
        return funcionarioRepository.findByAtivoTrue(pageable).map(funcionarioMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPorId(Long id) {
        return funcionarioMapper.toResponse(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPerfilPorEmail(String email) {
        Funcionario funcionario = funcionarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", email));
        return funcionarioMapper.toResponse(funcionario);
    }

    @Override
    @Transactional
    public FuncionarioResponse criar(FuncionarioRequest request) {
        if (funcionarioRepository.existsByCpf(request.cpf())) {
            throw new RegistroDuplicadoException("Já existe um funcionário com o CPF informado");
        }
        if (funcionarioRepository.existsByEmail(request.email())) {
            throw new RegistroDuplicadoException("Já existe um funcionário com o e-mail informado");
        }
        Funcionario funcionario = funcionarioMapper.toEntity(request);
        funcionario.trocarSenha(passwordEncoder.encode(request.senha()));
        return funcionarioMapper.toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public FuncionarioResponse atualizar(Long id, FuncionarioRequest request) {
        Funcionario funcionario = buscarEntidadePorId(id);
        funcionarioMapper.atualizarEntidade(request, funcionario);
        funcionario.trocarSenha(passwordEncoder.encode(request.senha()));
        return funcionarioMapper.toResponse(funcionarioRepository.save(funcionario));
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        Funcionario funcionario = buscarEntidadePorId(id);
        funcionario.desativar();
        funcionarioRepository.save(funcionario);
    }

    private Funcionario buscarEntidadePorId(Long id) {
        return funcionarioRepository.findById(id)
                .filter(Funcionario::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", id));
    }
}
