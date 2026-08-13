package com.estoque.mapper;

import com.estoque.dto.request.EnderecoRequest;
import com.estoque.dto.request.FornecedorRequest;
import com.estoque.dto.response.EnderecoResponse;
import com.estoque.dto.response.FornecedorResponse;
import com.estoque.entity.Endereco;
import com.estoque.entity.Fornecedor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FornecedorMapper {

    Fornecedor toEntity(FornecedorRequest request);

    FornecedorResponse toResponse(Fornecedor fornecedor);

    EnderecoResponse toEnderecoResponse(Endereco endereco);

    Endereco toEndereco(EnderecoRequest enderecoRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(FornecedorRequest request, @MappingTarget Fornecedor fornecedor);
}
