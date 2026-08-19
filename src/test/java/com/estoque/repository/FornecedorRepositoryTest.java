package com.estoque.repository;

import com.estoque.entity.Endereco;
import com.estoque.entity.Fornecedor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import com.estoque.config.PersistenceConfig;
import com.estoque.config.FlywayConfig;
import com.estoque.config.SecurityConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {PersistenceConfig.class, FlywayConfig.class, SecurityConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.profiles.active=test")
@Transactional
class FornecedorRepositoryTest {

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Test
    void devePersistirFornecedorComEnderecoEmbutido() {
        Endereco endereco = new Endereco("Av. Brasil", "500", "Centro", "São Paulo", "SP", "01000-000");
        Fornecedor fornecedor = Fornecedor.builder()
                .cnpj("12345678000199")
                .razaoSocial("Fornecedor Exemplo LTDA")
                .telefone("11999999999")
                .email("contato@fornecedor.com")
                .endereco(endereco)
                .build();

        Fornecedor salvo = fornecedorRepository.save(fornecedor);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.isAtivo()).isTrue();
        assertThat(fornecedorRepository.existsByCnpj("12345678000199")).isTrue();
    }
}
