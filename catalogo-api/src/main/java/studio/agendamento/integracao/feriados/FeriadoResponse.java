package studio.agendamento.integracao.feriados;

import java.time.LocalDate;

/** Contrato recebido do serviço externo, separado dos DTOs da nossa API. */
public record FeriadoResponse(String uf, LocalDate data, Boolean feriado, String descricao) {}
