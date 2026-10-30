package studio.agendamento.agendamentos.boundary;

import java.time.LocalDate;

import org.wildfly.common.annotation.NotNull;

import studio.agendamento.agendamentos.repository.PeriodoEnum;

public record AgendamentoRequestDto(
        @NotNull Long id_sala,
        Long id_usuario,
        @NotNull String uf,
        @NotNull LocalDate data,
        @NotNull PeriodoEnum periodo
) {
}