package studio.agendamento.agendamentos.domain;

import java.time.LocalDate;
import studio.agendamento.agendamentos.repository.PeriodoEnum;

/** Dados de negócio: não é contrato HTTP nem entidade JPA. */
public record AgendamentoBo(Long id, Long id_sala, Long id_usuario, String uf,
                            LocalDate data, PeriodoEnum periodo) {}
