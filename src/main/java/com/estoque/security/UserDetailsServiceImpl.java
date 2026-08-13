package com.estoque.security;

import com.estoque.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final FuncionarioRepository funcionarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return funcionarioRepository.findByEmail(email)
                .map(FuncionarioUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Funcionário não encontrado para o e-mail informado"));
    }
}
