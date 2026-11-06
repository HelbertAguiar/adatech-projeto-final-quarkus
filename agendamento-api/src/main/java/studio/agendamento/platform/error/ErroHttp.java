package studio.agendamento.platform.error;

import java.util.List;
import jakarta.ws.rs.core.*;

public final class ErroHttp {
    private ErroHttp() {}

    public static Response resposta(int status, String codigo, String mensagem, String path) {
        return resposta(status, codigo, mensagem, path, List.of());
    }

    public static Response resposta(int status, String codigo, String mensagem, String path, List<DetalheErro> detalhes) {
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(ErroResponse.of(codigo, mensagem, path, detalhes)).build();
    }
}
