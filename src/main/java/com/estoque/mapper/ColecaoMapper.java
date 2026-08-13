package com.estoque.mapper;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.entity.Colecao;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ColecaoMapper {

    Colecao toEntity(ColecaoRequest request);

    ColecaoResponse toResponse(Colecao colecao);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void atualizarEntidade(ColecaoRequest request, @MappingTarget Colecao colecao);
}
