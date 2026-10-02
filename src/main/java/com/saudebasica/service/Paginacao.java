package com.saudebasica.service;

import com.saudebasica.exception.RegraDeNegocioException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Set;

public final class Paginacao {
    private static final Set<String> CAMPOS = Set.of("id", "dataAtendimento", "status");

    private Paginacao() { }

    public static Pageable criar(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new RegraDeNegocioException("page deve ser >= 0 e size deve estar entre 1 e 100.");
        }
        String[] partes = sort == null ? new String[0] : sort.split(",", -1);
        if (partes.length != 2 || !CAMPOS.contains(partes[0]) ||
                !("asc".equalsIgnoreCase(partes[1]) || "desc".equalsIgnoreCase(partes[1]))) {
            throw new RegraDeNegocioException("sort deve ser campo,asc ou campo,desc; campos: id, dataAtendimento, status.");
        }
        Sort ordenacao = Sort.by(Sort.Direction.fromString(partes[1]), partes[0]);
        // O id unico desempata datas/status iguais sem repetir registros entre paginas.
        if (!"id".equals(partes[0])) {
            ordenacao = ordenacao.and(Sort.by("id"));
        }
        return PageRequest.of(page, size, ordenacao);
    }
}
