package studio.agendamento.agendamentos.domain;

public class DiaFeriadoException extends RuntimeException {
    public DiaFeriadoException() {
        super("Não é permitido agendar massagem em feriado na UF informada.");
    }
}
