package com.estoque.dto.response;

public record EnderecoResponse(
        String logradouro, String numero, String bairro, String cidade, String estado, String cep) {
}
