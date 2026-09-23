package com.saudebasica.web;

import com.saudebasica.domain.AtendimentoStatus;
import com.saudebasica.dto.AtendimentoRequestDTO;
import com.saudebasica.dto.AtendimentoResponseDTO;
import com.saudebasica.exception.RecursoNaoEncontradoException;
import com.saudebasica.service.AtendimentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AtendimentoController.class)
class AtendimentoControllerTest {

    private static final String URL = "/api/atendimentos";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AtendimentoService atendimentoService;

    @Test
    void deveRetornar404QuandoAtendimentoNaoExistir() throws Exception {
        when(atendimentoService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Atendimento nao encontrado com id 99"));

        mockMvc.perform(get(URL + "/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Atendimento nao encontrado com id 99"));
    }

    @Test
    void deveRetornar400QuandoBodyDoAgendamentoForInvalido() throws Exception {
        String bodySemCamposObrigatorios = """
                { "descricao": "" }
                """;

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySemCamposObrigatorios))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Dados invalidos"));

        verify(atendimentoService, never()).agendar(any());
    }

    @Test
    void deveRetornar201QuandoAgendamentoForValido() throws Exception {
        LocalDateTime data = LocalDateTime.of(2030, 1, 10, 9, 0);
        AtendimentoResponseDTO criado = new AtendimentoResponseDTO(1L, "Consulta de rotina",
                AtendimentoStatus.AGENDADO, data, "Joao da Silva", "Dra. Ana Lima",
                "Clinica Geral", "USF Centro");
        when(atendimentoService.agendar(any(AtendimentoRequestDTO.class))).thenReturn(criado);

        String bodyValido = """
                {
                  "pacienteId": 1,
                  "profissionalId": 1,
                  "unidadeId": 1,
                  "descricao": "Consulta de rotina",
                  "dataAtendimento": "2030-01-10T09:00:00"
                }
                """;

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("AGENDADO"));
    }
}
