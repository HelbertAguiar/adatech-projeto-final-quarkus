package studio.agendamento.agendamentos.boundary;

public record AgendamentoResponseDto(
        Long id,
        Long id_sala,
        boolean salaDisponivel,
        Long id_usuario,
        String uf,
        String data,
        String periodo
) {

}
