package com.saudebasica.web;

import com.saudebasica.dto.AtendimentoResponseDTO;
import com.saudebasica.service.AtendimentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saudebasica.dto.PaginaDTO;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final AtendimentoService atendimentoService;

    public PacienteController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @GetMapping("/{id}/atendimentos")
    @Operation(summary = "Historico paginado do paciente; mesmos parametros da listagem geral")
    public ResponseEntity<PaginaDTO<AtendimentoResponseDTO>> historico(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dataAtendimento,desc") String sort) {
        return ResponseEntity.ok(atendimentoService.listarPorPaciente(id, page, size, sort));
    }
}
