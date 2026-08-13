package com.estoque.dto.response;

public record FornecedorResponse(
        Long id, String cnpj, String razaoSocial, String telefone, String email,
        EnderecoResponse endereco, boolean ativo) {
}
