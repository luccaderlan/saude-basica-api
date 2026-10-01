package com.saudebasica.web;

import com.saudebasica.domain.AtendimentoStatus;
import com.saudebasica.dto.AtendimentoRequestDTO;
import com.saudebasica.dto.AtendimentoResponseDTO;
import com.saudebasica.dto.AtualizarStatusDTO;
import com.saudebasica.service.AtendimentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saudebasica.dto.PaginaDTO;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    public AtendimentoController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @PostMapping
    public ResponseEntity<AtendimentoResponseDTO> agendar(@RequestBody @Valid AtendimentoRequestDTO dados) {
        AtendimentoResponseDTO criado = atendimentoService.agendar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista paginada; page >= 0, size de 1 a 100, sort: id/dataAtendimento/status,asc/desc")
    public ResponseEntity<PaginaDTO<AtendimentoResponseDTO>> listar(
            @RequestParam(required = false) AtendimentoStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dataAtendimento,desc") String sort) {

        if (status == null) {
            return ResponseEntity.ok(atendimentoService.listarTodos(page, size, sort));
        }
        return ResponseEntity.ok(atendimentoService.listarPorStatus(status, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtendimentoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(atendimentoService.buscarPorId(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AtendimentoResponseDTO> atualizarStatus(@PathVariable Long id,
                                                                  @RequestBody @Valid AtualizarStatusDTO dados) {
        return ResponseEntity.ok(atendimentoService.atualizarStatus(id, dados.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        atendimentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
