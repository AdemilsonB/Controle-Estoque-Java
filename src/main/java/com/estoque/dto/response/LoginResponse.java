package com.estoque.dto.response;

public record LoginResponse(String token, String tipo, long expiraEmSegundos) {
}
