package studio.agendamento.platform.error;

import java.time.Instant;
import java.util.List;

public record ErroResponse(String codigo, String mensagem, String timestamp, String path, List<DetalheErro> detalhes) {
    public static ErroResponse of(String codigo, String mensagem, String path, List<DetalheErro> detalhes) {
        return new ErroResponse(codigo, mensagem, Instant.now().toString(), path, List.copyOf(detalhes));
    }
}
