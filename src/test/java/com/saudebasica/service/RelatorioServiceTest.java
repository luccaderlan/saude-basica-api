package com.saudebasica.service;

import com.saudebasica.exception.RegraDeNegocioException;
import com.saudebasica.repository.AtendimentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("Regras do relatório de atendimentos")
class RelatorioServiceTest {
    private final AtendimentoRepository repository = mock(AtendimentoRepository.class);
    private final RelatorioService service = new RelatorioService(repository);

    @Test
    @DisplayName("Bloqueia a data inicial maior que a final antes de consultar o banco")
    void deveBloquearPeriodoInvertidoAntesDeConsultarBanco() {
        assertThatThrownBy(() -> service.atendimentos(LocalDate.of(2030, 1, 11), LocalDate.of(2030, 1, 10)))
                .isInstanceOf(RegraDeNegocioException.class);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Bloqueia a data fora do limite aceito antes de consultar o banco")
    void deveBloquearDataForaDaFaixaSuportadaAntesDeConsultarBanco() {
        assertThatThrownBy(() -> service.atendimentos(LocalDate.of(2030, 1, 10), LocalDate.MAX))
                .isInstanceOf(RegraDeNegocioException.class);
        verifyNoInteractions(repository);
    }
}
