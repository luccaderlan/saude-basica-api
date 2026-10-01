package com.saudebasica.dto;

import org.springframework.data.domain.Page;
import java.util.List;

/** Contrato JSON proprio, independente da serializacao interna de PageImpl. */
public record PaginaDTO<T>(List<T> content, int number, int size, long totalElements,
                           int totalPages, boolean first, boolean last, List<String> sort) {
    public static <T> PaginaDTO<T> fromPage(Page<T> page) {
        return new PaginaDTO<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast(),
                page.getSort().stream().map(order -> order.getProperty() + "," +
                        order.getDirection().name().toLowerCase(java.util.Locale.ROOT)).toList());
    }
}
