package com.estoque.controller;

import com.estoque.dto.request.ColecaoRequest;
import com.estoque.dto.response.ColecaoResponse;
import com.estoque.service.ColecaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/colecoes")
@RequiredArgsConstructor
public class ColecaoController {

    private final ColecaoService colecaoService;

    @GetMapping
    public ResponseEntity<Page<ColecaoResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(colecaoService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColecaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(colecaoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColecaoResponse> criar(@Valid @RequestBody ColecaoRequest request) {
        ColecaoResponse resposta = colecaoService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/colecoes/" + resposta.id())).body(resposta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ColecaoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ColecaoRequest request) {
        return ResponseEntity.ok(colecaoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        colecaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
