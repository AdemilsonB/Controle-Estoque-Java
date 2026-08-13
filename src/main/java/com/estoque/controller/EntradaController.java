package com.estoque.controller;

import com.estoque.dto.request.EntradaRequest;
import com.estoque.dto.response.MovimentacaoResponse;
import com.estoque.service.EntradaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/entradas")
@RequiredArgsConstructor
public class EntradaController {

    private final EntradaService entradaService;

    @GetMapping
    public ResponseEntity<Page<MovimentacaoResponse>> listar(
            @RequestParam(required = false) Long produtoId, Pageable pageable) {
        return ResponseEntity.ok(entradaService.listar(produtoId, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<MovimentacaoResponse> registrar(@Valid @RequestBody EntradaRequest request,
                                                            Authentication authentication) {
        MovimentacaoResponse resposta = entradaService.registrar(request, authentication.getName());
        return ResponseEntity.created(URI.create("/api/v1/entradas/" + resposta.id())).body(resposta);
    }
}
