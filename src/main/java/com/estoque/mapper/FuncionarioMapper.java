package com.estoque.mapper;

import com.estoque.dto.request.EnderecoRequest;
import com.estoque.dto.request.FuncionarioRequest;
import com.estoque.dto.response.EnderecoResponse;
import com.estoque.dto.response.FuncionarioResponse;
import com.estoque.entity.Endereco;
import com.estoque.entity.Funcionario;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FuncionarioMapper {

    @Mapping(target = "senha", ignore = true)
    Funcionario toEntity(FuncionarioRequest request);

    FuncionarioResponse toResponse(Funcionario funcionario);

    EnderecoResponse toEnderecoResponse(Endereco endereco);

    Endereco toEndereco(EnderecoRequest enderecoRequest);

    @Mapping(target = "senha", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(FuncionarioRequest request, @MappingTarget Funcionario funcionario);
}
