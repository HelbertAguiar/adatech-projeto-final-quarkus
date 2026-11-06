package studio.agendamento.agendamentos.domain;

public class ReservaConflitanteException extends RuntimeException {
    public ReservaConflitanteException() {
        super("A sala já está reservada nesta data e período.");
    }
}
