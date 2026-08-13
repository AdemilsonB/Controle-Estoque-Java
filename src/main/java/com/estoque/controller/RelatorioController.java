package com.estoque.controller;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.dto.response.RelatorioEstoqueResponse;
import com.estoque.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/valor-estoque")
    public ResponseEntity<RelatorioEstoqueResponse> valorEstoque() {
        return ResponseEntity.ok(relatorioService.gerarRelatorioValorEstoque());
    }

    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<ProdutoResponse>> estoqueBaixo() {
        return ResponseEntity.ok(relatorioService.listarProdutosComEstoqueBaixo());
    }
}
