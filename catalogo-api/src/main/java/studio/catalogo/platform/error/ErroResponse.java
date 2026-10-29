package studio.catalogo.platform.error;

import java.time.Instant;
import java.util.List;

/** Contrato HTTP único para as falhas intencionais demonstradas na Aula 06. */
public record ErroResponse(
        String codigo,
        String mensagem,
        String timestamp,
        String path,
        List<DetalheErro> detalhes
) {
    public static ErroResponse of(String codigo, String mensagem, String path, List<DetalheErro> detalhes) {
        return new ErroResponse(codigo, mensagem, Instant.now().toString(), path, List.copyOf(detalhes));
    }
}
