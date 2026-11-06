package studio.agendamento.agendamentos.boundary;

import studio.agendamento.agendamentos.domain.AgendamentoBo;

public record AgendamentoResponseDto(
        Long id,
        Long id_sala,
        boolean salaDisponivel,
        Long id_usuario,
        String uf,
        String data,
        String periodo
) {
    public static AgendamentoResponseDto from(AgendamentoBo bo) {
        // Uma reserva existente ocupa a sala durante todo o período informado.
        return new AgendamentoResponseDto(bo.id(), bo.id_sala(), false, bo.id_usuario(),
                bo.uf(), bo.data().toString(), bo.periodo().name());
    }
}
