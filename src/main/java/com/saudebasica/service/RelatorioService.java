package com.saudebasica.service;

import com.saudebasica.domain.AtendimentoStatus;
import com.saudebasica.dto.RelatorioAtendimentosDTO;
import com.saudebasica.dto.RelatorioAtendimentosDTO.TotalUnidadeDTO;
import com.saudebasica.exception.RegraDeNegocioException;
import com.saudebasica.repository.AtendimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class RelatorioService {
    private final AtendimentoRepository repository;

    public RelatorioService(AtendimentoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RelatorioAtendimentosDTO atendimentos(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null || dataFim == null || dataInicio.isAfter(dataFim)) {
            throw new RegraDeNegocioException("Informe dataInicio e dataFim validas, com dataInicio <= dataFim.");
        }
        if (dataInicio.getYear() < 1 || dataFim.getYear() > 9999) {
            throw new RegraDeNegocioException("As datas devem ter anos entre 0001 e 9999.");
        }
        Map<AtendimentoStatus, Long> porStatus = zerarStatus();
        Map<Long, TotalUnidadeDTO> unidades = new TreeMap<>();
        // Uma unica agregacao garante que totais globais e por unidade usem o mesmo resultado.
        var contagens = repository.contarPorUnidadeEStatus(
                dataInicio.atStartOfDay(), dataFim.plusDays(1).atStartOfDay());
        long total = 0;
        for (var linha : contagens) {
            porStatus.merge(linha.getStatus(), linha.getTotal(), Long::sum);
            total += linha.getTotal();
            var unidade = unidades.computeIfAbsent(linha.getUnidadeId(),
                    id -> new TotalUnidadeDTO(id, linha.getUnidadeNome(), 0, zerarStatus()));
            unidade.porStatus().merge(linha.getStatus(), linha.getTotal(), Long::sum);
            unidades.put(linha.getUnidadeId(), new TotalUnidadeDTO(unidade.unidadeId(),
                    unidade.unidadeNome(), unidade.total() + linha.getTotal(), unidade.porStatus()));
        }
        var porUnidade = unidades.values().stream().map(u -> new TotalUnidadeDTO(
                u.unidadeId(), u.unidadeNome(), u.total(), Collections.unmodifiableMap(u.porStatus()))).toList();
        return new RelatorioAtendimentosDTO(dataInicio, dataFim, total,
                Collections.unmodifiableMap(porStatus), porUnidade);
    }

    private static Map<AtendimentoStatus, Long> zerarStatus() {
        Map<AtendimentoStatus, Long> status = new EnumMap<>(AtendimentoStatus.class);
        for (var valor : AtendimentoStatus.values()) {
            status.put(valor, 0L);
        }
        return status;
    }
}
