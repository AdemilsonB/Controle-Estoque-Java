package com.estoque.mapper;

import com.estoque.dto.request.CategoriaRequest;
import com.estoque.dto.response.CategoriaResponse;
import com.estoque.entity.Categoria;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    Categoria toEntity(CategoriaRequest request);

    CategoriaResponse toResponse(Categoria categoria);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(CategoriaRequest request, @MappingTarget Categoria categoria);
}
