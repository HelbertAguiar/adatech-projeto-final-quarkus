package studio.agendamento.integracao.feriados;

public class FeriadosIndisponiveisException extends RuntimeException {
    public FeriadosIndisponiveisException() {
        super("Não foi possível verificar os feriados. Tente novamente mais tarde.");
    }
}
