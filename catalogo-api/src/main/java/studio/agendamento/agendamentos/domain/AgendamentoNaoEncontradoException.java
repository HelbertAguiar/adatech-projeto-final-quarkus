package studio.agendamento.agendamentos.domain;

public class AgendamentoNaoEncontradoException extends RuntimeException {
    public AgendamentoNaoEncontradoException(Long id) {
        super("Agendamento " + id + " não encontrado.");
    }
}
