package studio.agendamento.agendamentos.boundary;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import studio.agendamento.agendamentos.repository.PeriodoEnum;

public record AgendamentoRequestDto(
        @NotNull @Positive Long id_sala,
        @Positive Long id_usuario,
        @NotBlank @Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO") String uf,
        @NotNull @FutureOrPresent LocalDate data,
        @NotNull PeriodoEnum periodo
) {
}
