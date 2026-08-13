package com.estoque.repository;

import com.estoque.entity.Endereco;
import com.estoque.entity.Fornecedor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
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
