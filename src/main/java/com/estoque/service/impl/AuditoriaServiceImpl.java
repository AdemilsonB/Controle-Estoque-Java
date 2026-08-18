package com.estoque.service.impl;

import com.estoque.entity.TentativaSaidaNegada;
import com.estoque.repository.TentativaSaidaNegadaRepository;
import com.estoque.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code REQUIRES_NEW} é essencial aqui: o chamador ({@code SaidaServiceImpl.registrar}) está prestes
 * a sofrer rollback por {@code EstoqueInsuficienteException}. Se este método participasse da mesma
 * transação ({@code REQUIRED}, o padrão), o registro de auditoria seria desfeito junto — perdendo
 * justamente o rastro da tentativa negada. Precisa estar em um bean separado: uma chamada interna
 * (this.registrarTentativaSaidaNegada(...)) não passaria pelo proxy do Spring e a anotação seria
 * ignorada.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private final TentativaSaidaNegadaRepository tentativaSaidaNegadaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarTentativaSaidaNegada(String produtoCodigo, String funcionarioEmail,
                                               int quantidadeSolicitada, int quantidadeDisponivel) {
        tentativaSaidaNegadaRepository.save(TentativaSaidaNegada.builder()
                .produtoCodigo(produtoCodigo)
                .funcionarioEmail(funcionarioEmail)
                .quantidadeSolicitada(quantidadeSolicitada)
                .quantidadeDisponivel(quantidadeDisponivel)
                .build());
    }
}
