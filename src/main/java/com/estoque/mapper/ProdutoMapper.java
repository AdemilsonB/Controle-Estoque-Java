package com.estoque.mapper;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.entity.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    @Mapping(source = "categoria.nome", target = "categoriaNome")
    @Mapping(source = "colecao.nome", target = "colecaoNome")
    @Mapping(source = "fornecedor.razaoSocial", target = "fornecedorRazaoSocial")
    ProdutoResponse toResponse(Produto produto);
}
