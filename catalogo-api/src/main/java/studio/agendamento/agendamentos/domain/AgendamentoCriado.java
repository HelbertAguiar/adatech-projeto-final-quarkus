package studio.agendamento.agendamentos.domain;

/** Evento emitido na transação; a métrica observa somente o commit bem-sucedido. */
public record AgendamentoCriado(Long id) {}
