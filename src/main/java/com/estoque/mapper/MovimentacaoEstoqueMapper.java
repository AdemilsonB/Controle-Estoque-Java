package com.estoque.mapper;

import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.entity.Entrada;
import com.estoque.entity.MovimentacaoEstoque;
import com.estoque.entity.Saida;
import org.springframework.stereotype.Component;

@Component
public class MovimentacaoEstoqueMapper {

    public MovimentacaoResponse toResponse(MovimentacaoEstoque movimentacao) {
        String nomeFuncionario = movimentacao.getFuncionario().getNome() + " " + movimentacao.getFuncionario().getSobrenome();

        if (movimentacao instanceof Entrada entrada) {
            return new MovimentacaoResponse(entrada.getId(), "ENTRADA", entrada.getProduto().getCodigo(),
                    entrada.getProduto().getNome(), nomeFuncionario, entrada.getQuantidade(),
                    entrada.getDataMovimentacao(), entrada.getObservacao(), entrada.getCustoUnitario(), null);
        }
        if (movimentacao instanceof Saida saida) {
            return new MovimentacaoResponse(saida.getId(), "SAIDA", saida.getProduto().getCodigo(),
                    saida.getProduto().getNome(), nomeFuncionario, saida.getQuantidade(),
                    saida.getDataMovimentacao(), saida.getObservacao(), null, saida.getMotivo().name());
        }
        throw new IllegalStateException("Tipo de movimentação não suportado: " + movimentacao.getClass());
    }
}
