package com.saudebasica.web;

import com.saudebasica.dto.AtendimentoResponseDTO;
import com.saudebasica.service.AtendimentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final AtendimentoService atendimentoService;

    public PacienteController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @GetMapping("/{id}/atendimentos")
    public ResponseEntity<List<AtendimentoResponseDTO>> historico(@PathVariable Long id) {
        return ResponseEntity.ok(atendimentoService.listarPorPaciente(id));
    }
}
